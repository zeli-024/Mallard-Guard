package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.MallardGuard;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.ArrayDeque;
import java.util.Queue;

/** Freeze a captured frame, then hold one freshly filtered impact scene. */
@EventBusSubscriber(modid = MallardGuard.ID, value = Dist.CLIENT)
public final class GuardHitlag {
    private static final long NANOS_PER_FRAME = 1_000_000_000L / 60L;
    private static final Queue<Runnable> afterFeedback = new ArrayDeque<>();
    private static TextureTarget captured;
    private static boolean freezeCapturePending, impactCapturePending;
    private static long frozenUntilNanos, impactStartNanos;
    private static boolean impactEnabled, impactWasDrawn, inverted;
    private static long impactDurationNanos;

    private GuardHitlag() {}

    public static void trigger(boolean enabledForResult, boolean perfect, Runnable feedback) {
        inverted = false;
        Minecraft mc = Minecraft.getInstance();
        boolean freeze = enabledForResult && GuardConfig.HITLAG_FRAMES.get() > 0;
        boolean impact = GuardConfig.IMPACT_FRAMES.get() > 0
            && (perfect || !GuardConfig.IMPACT_PERFECT_ONLY.get());
        if ((!freeze && !impact) || mc.player == null || mc.level == null || mc.screen != null) {
            feedback.run();
            return;
        }
        afterFeedback.add(feedback);
        impactEnabled = impact;
        impactWasDrawn = false;
        impactDurationNanos = Math.max(1, GuardConfig.HITLAG_FRAMES.get()) * NANOS_PER_FRAME;
        if (freeze) freezeCapturePending = true;
        else impactCapturePending = true;
    }

    public static void triggerMobCounter(Runnable feedback) {
        trigger(true, true, feedback);
        inverted = true;
    }

    /** Queue local effects until the freeze and impact image end. */
    public static void afterFreeze(Runnable action) {
        if (freezeCapturePending || frozenUntilNanos != 0 || impactCapturePending || impactStartNanos != 0)
            afterFeedback.add(action);
        else action.run();
    }

    public static void clear() {
        freezeCapturePending = impactCapturePending = impactEnabled = impactWasDrawn = false;
        frozenUntilNanos = impactStartNanos = 0; afterFeedback.clear();
        if (captured != null) { captured.destroyBuffers(); captured = null; }
    }

    private static void finish() {
        while (!afterFeedback.isEmpty()) afterFeedback.remove().run();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(RenderFrameEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.isAlive()) { clear(); return; }
        if (mc.level == null || mc.player == null || mc.screen != null) {
            freezeCapturePending = impactCapturePending = false;
            frozenUntilNanos = impactStartNanos = 0;
            if (mc.player != null && mc.level != null) finish();
            else afterFeedback.clear();
            if (captured != null && (mc.level == null || mc.player == null)) {
                captured.destroyBuffers();
                captured = null;
            }
            return;
        }
        if (!freezeCapturePending && frozenUntilNanos == 0 && !impactCapturePending && impactStartNanos == 0) {
            finish();
            return;
        }

        long now = System.nanoTime();
        if (frozenUntilNanos != 0 && now >= frozenUntilNanos) {
            frozenUntilNanos = 0;
            if (impactEnabled) impactCapturePending = true;
            else { finish(); return; }
        }
        if (impactStartNanos != 0 && now - impactStartNanos >= impactDurationNanos && impactWasDrawn) {
            impactStartNanos = 0;
            finish();
            return;
        }

        RenderTarget screen = mc.getMainRenderTarget();
        int width = screen.width, height = screen.height;
        if (width <= 0 || height <= 0) return;
        if (captured == null || captured.width != width || captured.height != height) {
            if (captured != null) captured.destroyBuffers();
            captured = new TextureTarget(width, height, false, false);
            frozenUntilNanos = impactStartNanos = 0;
        }
        if (freezeCapturePending) {
            copy(screen, captured);
            freezeCapturePending = false;
            frozenUntilNanos = System.nanoTime() + GuardConfig.HITLAG_FRAMES.get() * NANOS_PER_FRAME;
        }
        if (frozenUntilNanos != 0) {
            copy(captured, screen);
            return;
        }
        if (impactCapturePending) {
            // Hold one NEW scene after hitlag and apply one still grayscale treatment.
            copy(screen, captured);
            impactCapturePending = false;
            impactStartNanos = System.nanoTime();
        }
        if (impactStartNanos != 0) {
            if (!GuardImpactFrame.draw(captured, screen, inverted)) {
                impactStartNanos = 0;
                finish();
            } else impactWasDrawn = true;
        }
    }

    static void copy(RenderTarget source, RenderTarget target) {
        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        if (scissor) GL11.glDisable(GL11.GL_SCISSOR_TEST);
        try {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, source.width, source.height,
                0, 0, target.width, target.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
            if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
        }
    }
}
