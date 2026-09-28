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

/** Holds one captured frame for a 60 FPS equivalent interval; world ticks continue. */
@EventBusSubscriber(modid = MallardGuard.ID, value = Dist.CLIENT)
public final class GuardHitlag {
    private static final long NANOS_PER_FRAME = 1_000_000_000L / 60L;
    private static final Queue<Runnable> afterFreeze = new ArrayDeque<>();
    private static TextureTarget frozen;
    private static boolean capturePending;
    private static long frozenUntilNanos;

    private GuardHitlag() {}

    public static void trigger(Runnable feedback) {
        Minecraft mc = Minecraft.getInstance();
        if (GuardConfig.HITLAG_FRAMES.get() == 0 || mc.player == null || mc.level == null || mc.screen != null) {
            feedback.run();
            return;
        }
        afterFreeze.add(feedback);
        capturePending = true;
    }

    /** Play a defender's sound when the active freeze ends; never starts a new freeze. */
    public static void afterFreeze(Runnable sound) {
        if (capturePending || frozenUntilNanos != 0) afterFreeze.add(sound);
        else sound.run();
    }

    private static void finish() {
        while (!afterFreeze.isEmpty()) afterFreeze.remove().run();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(RenderFrameEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null || GuardConfig.HITLAG_FRAMES.get() == 0) {
            capturePending = false;
            frozenUntilNanos = 0;
            if (mc.player != null && mc.level != null) finish();
            else afterFreeze.clear();
            if (frozen != null && (mc.level == null || mc.player == null)) {
                frozen.destroyBuffers();
                frozen = null;
            }
            return;
        }
        if (!capturePending && frozenUntilNanos == 0) {
            finish();
            return;
        }
        if (!capturePending && System.nanoTime() >= frozenUntilNanos) {
            frozenUntilNanos = 0;
            finish();
            return;
        }

        RenderTarget frame = mc.getMainRenderTarget();
        int width = frame.width, height = frame.height;
        if (width <= 0 || height <= 0) return;
        if (frozen == null || frozen.width != width || frozen.height != height) {
            if (frozen != null) frozen.destroyBuffers();
            frozen = new TextureTarget(width, height, false, false);
            frozenUntilNanos = 0;
        }

        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        if (scissor) GL11.glDisable(GL11.GL_SCISSOR_TEST);
        try {
            if (capturePending) {
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, frame.frameBufferId);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, frozen.frameBufferId);
                GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
                capturePending = false;
                frozenUntilNanos = System.nanoTime() + GuardConfig.HITLAG_FRAMES.get() * NANOS_PER_FRAME;
            }
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, frozen.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, frame.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
            if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
        }
    }
}
