package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.GuardPackets;
import dev.zeli.mallardguard.GuardParticles;
import dev.zeli.mallardguard.MallardGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;

@EventBusSubscriber(modid = MallardGuard.ID, value = Dist.CLIENT)
public final class GuardClient {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "textures/gui/backgroundlayer.png");
    private static final ResourceLocation ANIMATED = ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "textures/gui/animatedlayer.png");
    private static final ResourceLocation FLASH_MASK = ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "textures/gui/flashmask.png");
    private static boolean wasDown;
    private static int phase;
    private static int elapsed;
    private static int recharge;
    private static int window = 7;
    private static int rechargeMax = 12;
    private static int hitResult;
    private static long resultStartMs;
    private static final long FLASH_DURATION_MS = 420;
    private static final long ECHO_DURATION_MS = 550;
    private static final long RESULT_HOLD_MS = 620;
    private static AttackIndicatorStatus savedAttackIndicator;

    private GuardClient() {}

    public static boolean isStanceActive() {
        return phase != 0;
    }

    public static boolean isCurrentMainHand(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, minecraft.player.getMainHandItem());
    }

    public static void status(GuardPackets.Status data) {
        phase = data.phase();
        elapsed = data.elapsed();
        recharge = data.recharge();
        window = data.window();
        rechargeMax = data.rechargeMax();
    }

    public static void settings(GuardPackets.Settings data) {
        Minecraft.getInstance().setScreen(new GuardConfigScreen(data));
    }

    public static void hitResult(GuardPackets.HitResult data) {
        if (data.result() < 1 || data.result() > 3) return;
        hitResult = data.result();
        resultStartMs = System.currentTimeMillis();
    }

    public static void sparks(GuardPackets.Sparks data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || data.count() < 1 || data.count() > 32) return;
        // Our particles use the vanilla firework orb sprite at controlled sizes and opacity.
        mc.level.addParticle(data.perfect() ? GuardParticles.PERFECT_FLASH.get() : GuardParticles.PARRY_FLASH.get(),
            data.x(), data.y(), data.z(), 0, 0, 0);
        if (data.perfect()) {
            // Particle Interactions' separate star sprites accompany only perfect parries.
            for (int i = 0; i < 12; i++) {
                double x = data.x() + (mc.level.random.nextDouble() - 0.5D) * 0.36D;
                double y = data.y() + (mc.level.random.nextDouble() - 0.5D) * 0.30D;
                double z = data.z() + (mc.level.random.nextDouble() - 0.5D) * 0.36D;
                mc.level.addParticle(GuardParticles.SPARK_FLASH.get(), x, y, z, 0, 0, 0);
            }
        }
        double angleOffset = mc.level.random.nextDouble() * Math.PI * 2.0D;
        for (int i = 0; i < data.count(); i++) {
            double angle = angleOffset + Math.PI * 2.0D * i / data.count();
            double dx = Math.cos(angle) * 0.08D + (mc.level.random.nextDouble() - 0.5D) * 0.32D;
            double dz = Math.sin(angle) * 0.08D + (mc.level.random.nextDouble() - 0.5D) * 0.32D;
            // One anvil-style spray per hit. The old three delayed emissions
            // made a single parry look as though the effect had played twice.
            double x = data.x() + dx + (mc.level.random.nextDouble() - 0.5D) * 0.25D;
            double z = data.z() + dz + (mc.level.random.nextDouble() - 0.5D) * 0.25D;
            double vx = Math.clamp(dx, -1.0D, 1.0D) * 2.5D;
            double vz = Math.clamp(dz, -1.0D, 1.0D) * 2.5D;
            double vy = 0.4D * (0.24D + Math.max(Math.abs(dx), Math.abs(dz)));
            mc.level.addParticle(GuardParticles.FLYING_SPARK.get(), x, data.y(), z, vx, vy, vz);
        }
    }

    @SubscribeEvent
    public static void cameraShake(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || !GuardConfig.SCREEN_SHAKE.get() || GuardConfig.SHAKE_STRENGTH.get() == 0) return;
        long age = System.currentTimeMillis() - resultStartMs;
        if (hitResult == 0 || age < 0 || age >= 320) return;
        float resultScale = hitResult == 3 ? 1.4F : hitResult == 2 ? 1.0F : 0.55F;
        float amplitude = resultScale * GuardConfig.SHAKE_STRENGTH.get() / 100.0F * (1.0F - age / 320.0F);
        event.setRoll(event.getRoll() + (float) Math.sin(age * 0.095D) * 2.5F * amplitude);
        event.setPitch(event.getPitch() + (float) Math.sin(age * 0.135D) * 1.4F * amplitude);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            restoreAttackIndicator();
            wasDown = false;
            phase = recharge = 0;
            hitResult = 0;
            resultStartMs = 0;
            return;
        }
        boolean down = mc.screen == null && mc.options.keyUse.isDown();
        if (down != wasDown) {
            wasDown = down;
            PacketDistributor.sendToServer(new GuardPackets.Input(down));
        }
    }

    // The attack indicator shares vanilla's crosshair/hotbar layers. Hide it only
    // while each layer renders, then restore the player's option immediately.
    @SubscribeEvent
    public static void beforeLayer(RenderGuiLayerEvent.Pre event) {
        if (savedAttackIndicator != null) restoreAttackIndicator();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !GuardConfig.HUD.get() || (phase == 0 && recharge == 0 && !resultVisible())) return;
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR) || event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            savedAttackIndicator = mc.options.attackIndicator().get();
            mc.options.attackIndicator().set(AttackIndicatorStatus.OFF);
        }
    }

    @SubscribeEvent
    public static void afterLayer(RenderGuiLayerEvent.Post event) {
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR) || event.getName().equals(VanillaGuiLayers.HOTBAR)) restoreAttackIndicator();
    }

    private static void restoreAttackIndicator() {
        if (savedAttackIndicator != null) {
            Minecraft.getInstance().options.attackIndicator().set(savedAttackIndicator);
            savedAttackIndicator = null;
        }
    }

    private static boolean resultVisible() {
        long age = System.currentTimeMillis() - resultStartMs;
        return GuardConfig.SHIELD_EFFECTS.get() && hitResult != 0 && age >= 0 && age < RESULT_HOLD_MS;
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        restoreAttackIndicator();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        int crosshairX = (event.getGuiGraphics().guiWidth() - 15) / 2;
        int crosshairY = (event.getGuiGraphics().guiHeight() - 15) / 2;
        int shieldSize = 18;
        float shieldCenterX = crosshairX + 7.5F;
        float shieldCenterY = crosshairY + 15 + 2 + shieldSize / 2.0F;
        long resultAge = System.currentTimeMillis() - resultStartMs;
        if (GuardConfig.SCREEN_FLASH.get() && GuardConfig.FLASH_STRENGTH.get() > 0 && (hitResult == 1 || hitResult == 2) && resultAge >= 0 && resultAge < 260) {
            renderScreenFlash(event.getGuiGraphics(), resultAge);
            // Commit the white screen flash before submitting the echo and shield textures.
            event.getGuiGraphics().flush();
        }
        if (GuardConfig.SHIELD_EFFECTS.get() && hitResult == 3 && resultAge >= 0 && resultAge < FLASH_DURATION_MS) {
            renderBlockVignette(event.getGuiGraphics(), resultAge);
        }
        if (!GuardConfig.HUD.get()) return;
        boolean showingResult = resultVisible();
        if (phase == 0 && recharge == 0 && !showingResult) return;
        float progress = showingResult ? Math.min(1.0F, resultAge / (float) FLASH_DURATION_MS) : 1.0F;
        float expansion = hitResult == 2 ? 0.82F : hitResult == 1 ? 0.30F : 0.18F;
        float pulse = showingResult && resultAge < FLASH_DURATION_MS ? (float) Math.sin(Math.PI * progress) * expansion : 0.0F;
        float shakeStrength = hitResult == 2 ? 3.5F : 1.1F;
        float shake = showingResult && resultAge < FLASH_DURATION_MS ? (float) Math.sin(resultAge * 0.13D) * shakeStrength * (1.0F - progress) : 0.0F;
        float verticalShake = showingResult && hitResult == 2 && resultAge < FLASH_DURATION_MS ? (float) Math.sin(resultAge * 0.18D) * 1.3F * (1.0F - progress) : 0.0F;
        boolean draining = phase == 1 || phase == 2;
        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().translate(shieldCenterX, shieldCenterY, 0);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (showingResult && hitResult == 2 && resultAge < ECHO_DURATION_MS) {
            float echoProgress = resultAge / (float) ECHO_DURATION_MS;
            float echoScale = shieldSize / 32.0F * (1.0F + 3.15F * echoProgress);
            event.getGuiGraphics().pose().pushPose();
            event.getGuiGraphics().pose().scale(echoScale, echoScale, 1.0F);
            event.getGuiGraphics().pose().translate(-16, -16, 0);
            RenderSystem.setShaderColor(1.0F, 0.78F, 0.16F, 0.52F * (1.0F - echoProgress));
            event.getGuiGraphics().blit(FLASH_MASK, 0, 0, 0, 0, 32, 32, 32, 32);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            event.getGuiGraphics().pose().popPose();
        }
        event.getGuiGraphics().pose().translate(shake, verticalShake, 0);
        float iconScale = shieldSize / 32.0F * (1.0F + pulse);
        event.getGuiGraphics().pose().scale(iconScale, iconScale, 1.0F);
        event.getGuiGraphics().pose().translate(-16, -16, 0);
        RenderSystem.setShaderColor(0.24f, 0.24f, 0.24f, 1f);
        event.getGuiGraphics().blit(BACKGROUND, 0, 0, 0, 0, 32, 32, 32, 32);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        if (showingResult && phase == 0 && recharge == 0) {
            // Perfect parries otherwise clear the shield immediately. Hold its normal
            // color briefly after the flash, then let the icon disappear as usual.
            event.getGuiGraphics().blit(ANIMATED, 0, 0, 0, 0, 32, 32, 32, 32);
        } else if (draining) {
            int removed = Math.min(32, Math.max(0, Math.round(32f * elapsed / Math.max(1, window))));
            if (removed < 32) {
                event.getGuiGraphics().blit(ANIMATED, removed, 0, removed, 0, 32 - removed, 32, 32, 32);
            }
        } else if (phase != 3) {
            int filled = Math.min(32, Math.max(0, Math.round(32f * (1f - (float) recharge / Math.max(1, rechargeMax)))));
            if (filled > 0) {
                int start = 32 - filled;
                event.getGuiGraphics().blit(ANIMATED, start, 0, start, 0, filled, 32, 32, 32);
            }
        }
        if (showingResult && resultAge < FLASH_DURATION_MS) {
            int rgb = switch (hitResult) {
                case 2 -> 0xFFD43B; // perfect parry: gold
                case 3 -> 0xFF3D4E; // held block: red
                default -> 0xFFFFFF; // regular and follow-up parry: white
            };
            float fade = 1.0F - progress * progress;
            RenderSystem.setShaderColor(((rgb >> 16) & 255) / 255.0F, ((rgb >> 8) & 255) / 255.0F, (rgb & 255) / 255.0F, 0.5F * fade);
            event.getGuiGraphics().blit(FLASH_MASK, 0, 0, 0, 0, 32, 32, 32, 32);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        RenderSystem.disableBlend();
        event.getGuiGraphics().pose().popPose();
    }

    private static void renderBlockVignette(net.minecraft.client.gui.GuiGraphics graphics, long age) {
        float fade = 1.0F - age / (float) FLASH_DURATION_MS;
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        int step = Math.max(6, Math.min(width, height) / 28);
        for (int i = 0; i < 4; i++) {
            int inset = i * step;
            int alpha = Math.round((14 - i * 2.5F) * fade);
            int color = alpha << 24 | 0x15080B;
            graphics.fill(inset, inset, width - inset, inset + step, color);
            graphics.fill(inset, height - inset - step, width - inset, height - inset, color);
            graphics.fill(inset, inset + step, inset + step, height - inset - step, color);
            graphics.fill(width - inset - step, inset + step, width - inset, height - inset - step, color);
        }
    }

    private static void renderScreenFlash(net.minecraft.client.gui.GuiGraphics graphics, long age) {
        float progress = age / 260.0F;
        float intensity = GuardConfig.FLASH_STRENGTH.get() / 100.0F * (hitResult == 2 ? 1.0F : 0.82F);
        int alpha = Math.round(255.0F * 0.85F * (1.0F - progress) * intensity);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24 | 0xFFFFFF);
    }

}
