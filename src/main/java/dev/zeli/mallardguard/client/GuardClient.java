package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.GuardPackets;
import dev.zeli.mallardguard.MallardGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
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

    private GuardClient() {}

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

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            wasDown = false;
            phase = recharge = 0;
            return;
        }
        boolean down = mc.screen == null && mc.options.keyUse.isDown();
        if (down != wasDown) {
            wasDown = down;
            PacketDistributor.sendToServer(new GuardPackets.Input(down));
        }
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !GuardConfig.HUD.get()) return;
        if (phase == 0 && recharge == 0) return;
        boolean draining = phase == 1 || phase == 2;
        int x = event.getGuiGraphics().guiWidth() / 2 - 4;
        int y = event.getGuiGraphics().guiHeight() / 2 + 12;
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
                event.getGuiGraphics().blit(ANIMATED, 0, 0, 0, 0, filled, 32, 32, 32);
            }
        }
        RenderSystem.disableBlend();
        event.getGuiGraphics().pose().popPose();
    }
}
