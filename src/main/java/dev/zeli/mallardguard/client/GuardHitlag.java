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
    private static long impactDurationNanos, freezeDurationNanos;
    public static int frames(boolean perfect) { return perfect ? GuardConfig.HITLAG_FRAMES.get() : GuardConfig.REGULAR_HITLAG_FRAMES.get(); }

    private GuardHitlag() {}

    public static void trigger(boolean perfect, Runnable feedback) {
        inverted = false;
        Minecraft mc = Minecraft.getInstance();
        int frames = frames(perfect);
        boolean freeze = frames > 0;
        boolean impact = GuardConfig.IMPACT_FRAMES.get() > 0
            && (perfect || !GuardConfig.IMPACT_PERFECT_ONLY.get());
        if ((!freeze && !impact) || mc.player == null || mc.level == null || mc.screen != null) {
            feedback.run();
            return;
        }
        enqueue(feedback);
        impactEnabled = impact;
        impactWasDrawn = false;
        impactDurationNanos = Math.max(1, frames) * NANOS_PER_FRAME;
        freezeDurationNanos = frames * NANOS_PER_FRAME;
        if (freeze) { freezeCapturePending = true; PunchyGuardCompat.parryHitlagStarted(); }
        else impactCapturePending = true;
    }

    public static void triggerMobCounter(Runnable feedback) {
        trigger(true, feedback);
        inverted = true;
    }

    /** Queue local effects until the freeze and impact image end. */
    public static void afterFreeze(Runnable action) {
        if (freezeCapturePending || frozenUntilNanos != 0 || impactCapturePending || impactStartNanos != 0)
            enqueue(action);
        else action.run();
    }

    public static boolean freezeActive() { return freezeCapturePending || frozenUntilNanos != 0 && System.nanoTime() < frozenUntilNanos; }

    private static void enqueue(Runnable action){if(afterFeedback.size()>=256)afterFeedback.remove().run();afterFeedback.add(action);}

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
            frozenUntilNanos = System.nanoTime() + freezeDurationNanos;
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

    static void copy(RenderTarget source,RenderTarget target){GuardRenderTargets.copy(source,target);}
}
