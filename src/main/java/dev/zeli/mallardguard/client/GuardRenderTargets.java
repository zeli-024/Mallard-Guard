package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Copies color targets while restoring framebuffer and scissor state. */
final class GuardRenderTargets {
    private GuardRenderTargets(){}
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
