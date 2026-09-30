package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.MallardGuard;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** One reusable capture target; animated channel separation begins after hitlag/impact feedback. */
@EventBusSubscriber(modid = MallardGuard.ID, value = Dist.CLIENT)
public final class GuardChromatic {
    private static TextureTarget captured;
    private static long started;
    private GuardChromatic() {}
    public static void trigger(boolean perfect) {
        if (GuardConfig.CHROMATIC_INTENSITY.get() > 0 && (perfect || !GuardConfig.CHROMATIC_PERFECT_ONLY.get())) started = System.nanoTime();
    }
    public static void clear() {
        started = 0;
        if (captured != null) { captured.destroyBuffers(); captured = null; }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void beforeHud(RenderGuiEvent.Pre event) {
        if (!GuardConfig.CHROMATIC_HUD.get()) draw();
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterHud(RenderGuiEvent.Post event) {
        if (GuardConfig.CHROMATIC_HUD.get() && started != 0) { event.getGuiGraphics().flush(); draw(); }
    }
    private static void draw() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.player.isAlive()) { clear(); return; }
        if (started == 0 || mc.screen != null) return;
        double progress = (System.nanoTime() - started) / (GuardConfig.CHROMATIC_TICKS.get() * 50_000_000.0);
        if (progress < 0 || progress >= 1) { started = 0; return; }
        float strength = GuardConfig.CHROMATIC_INTENSITY.get() / 100.0F * (float) Math.sin(Math.PI * progress);
        if (strength <= 0) return;
        var destination = mc.getMainRenderTarget();
        if (destination.width <= 0 || destination.height <= 0) return;
        if (captured == null || captured.width != destination.width || captured.height != destination.height) {
            if (captured != null) captured.destroyBuffers();
            captured = new TextureTarget(destination.width, destination.height, false, false);
        }
        GuardHitlag.copy(destination, captured);
        GuardImpactFrame.drawChromatic(captured, destination, strength);
    }
}
