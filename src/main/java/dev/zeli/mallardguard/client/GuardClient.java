package dev.zeli.mallardguard.client;

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
    private static final ResourceLocation FILLED = ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "textures/gui/shield_full.png");
    private static final ResourceLocation FADED = ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "textures/gui/shield_faded.png");
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
        float fraction = phase == 1 || phase == 2
            ? 1f - (float) elapsed / Math.max(1, window)
            : phase == 3 ? 0f : 1f - (float) recharge / Math.max(1, rechargeMax);
        int x = event.getGuiGraphics().guiWidth() / 2 - 6;
        int y = event.getGuiGraphics().guiHeight() / 2 + 12;
        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().translate(x, y, 0);
        event.getGuiGraphics().pose().scale(0.375f, 0.375f, 1f);
        event.getGuiGraphics().blit(FILLED, 0, 0, 0, 0, 32, 32, 32, 32);
        int width = Math.min(12, Math.max(0, Math.round(fraction * 12)));
        if (width > 0) {
            event.getGuiGraphics().enableScissor(x, y, x + width, y + 12);
            event.getGuiGraphics().blit(FADED, 0, 0, 0, 0, 32, 32, 32, 32);
            event.getGuiGraphics().disableScissor();
        }
        event.getGuiGraphics().pose().popPose();
    }
}
