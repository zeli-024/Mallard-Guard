package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.GuardPackets;
import dev.zeli.mallardguard.MallardGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;

@EventBusSubscriber(modid = MallardGuard.ID, value = Dist.CLIENT)
public final class GuardClient {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "textures/gui/backgroundlayer.png");
    private static final ResourceLocation ANIMATED = ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "textures/gui/animatedlayer.png");
    private static boolean wasDown;
    private static int phase;
    private static int elapsed;
    private static int recharge;
    private static int window = 7;
    private static int rechargeMax = 12;
    private static int hitResult;
    private static int resultTicks;
    private static final int RESULT_DURATION = 18;
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
        resultTicks = RESULT_DURATION;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            restoreAttackIndicator();
            wasDown = false;
            phase = recharge = 0;
            hitResult = resultTicks = 0;
            return;
        }
        if (resultTicks > 0) resultTicks--;
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
        if (mc.player == null || mc.options.hideGui || !GuardConfig.HUD.get() || (phase == 0 && recharge == 0)) return;
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

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        restoreAttackIndicator();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        int crosshairX = (event.getGuiGraphics().guiWidth() - 15) / 2;
        int crosshairY = (event.getGuiGraphics().guiHeight() - 15) / 2;
        int shieldX = crosshairX + (15 - 8) / 2;
        int shieldY = crosshairY + 15 + 2;
        if (GuardConfig.RESULT_TEXT.get() && resultTicks > 0) {
            Component message = switch (hitResult) {
                case 1 -> Component.literal("PARRY");
                case 2 -> Component.literal("PERFECT PARRY").withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC);
                case 3 -> Component.literal("BLOCK");
                default -> Component.empty();
            };
            int rgb = switch (hitResult) {
                case 2 -> 0xFFD700;
                case 3 -> 0xD5D8DC;
                default -> 0xFFFFFF;
            };
            int alpha = resultTicks >= 5 ? 255 : Math.max(0, resultTicks * 255 / 5);
            int glowAlpha = Math.min(100, alpha / 2);
            int x = crosshairX + 7;
            int y = shieldY + 11;
            event.getGuiGraphics().pose().pushPose();
            event.getGuiGraphics().pose().translate(x, y, 0);
            event.getGuiGraphics().pose().scale(0.65F, 0.65F, 1.0F);
            int center = 0;
            int glow = (glowAlpha << 24) | rgb;
            event.getGuiGraphics().drawCenteredString(mc.font, message, center - 2, 0, glow);
            event.getGuiGraphics().drawCenteredString(mc.font, message, center + 2, 0, glow);
            event.getGuiGraphics().drawCenteredString(mc.font, message, center, -2, glow);
            event.getGuiGraphics().drawCenteredString(mc.font, message, center, 2, glow);
            event.getGuiGraphics().drawCenteredString(mc.font, message, center, 0, (alpha << 24) | rgb);
            event.getGuiGraphics().pose().popPose();
        }
        if (!GuardConfig.HUD.get()) return;
        if (phase == 0 && recharge == 0) return;
        boolean draining = phase == 1 || phase == 2;
        int x = shieldX;
        int y = shieldY;
        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().translate(x, y, 0);
        event.getGuiGraphics().pose().scale(0.25f, 0.25f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0.24f, 0.24f, 0.24f, 1f);
        event.getGuiGraphics().blit(BACKGROUND, 0, 0, 0, 0, 32, 32, 32, 32);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        if (draining) {
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
        RenderSystem.disableBlend();
        event.getGuiGraphics().pose().popPose();
    }
}
