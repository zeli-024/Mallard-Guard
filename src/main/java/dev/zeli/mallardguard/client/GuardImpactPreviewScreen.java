package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Preview an actual captured scene before committing impact-image settings. */
final class GuardImpactPreviewScreen extends Screen {
    private final Screen parent;
    private final Consumer<int[]> accept;
    private final boolean editable;
    private final int[] values;
    private final List<Button> buttons = new ArrayList<>();
    private final java.util.Map<Button, String> buttonTips = new java.util.HashMap<>();
    private TextureTarget scene;
    private boolean capturePending = true;
    private boolean shaderAvailable = true;

    private boolean mob;
    private int panelWidth, left, top, rowGap, panelHeight;
    private static final String[] NAMES = {"Brightness", "Contrast", "Dark outlines", "Grain", "Chromatic aberration"};
    GuardImpactPreviewScreen(Screen parent, int[] values, boolean editable, Consumer<int[]> accept) {
        super(Component.literal("Impact frame preview"));
        this.parent = parent; this.accept = accept; this.editable = editable; this.values = values.clone();
    }
    private int slot(int index) { return mob ? 51 + index : index == 4 ? 50 : 40 + index; }
    @Override protected void init() {
        buttons.clear(); buttonTips.clear();
        panelWidth = Math.min(420, Math.max(120, width - 20)); left = (width - panelWidth) / 2;
        rowGap = Math.min(23, Math.max(10, (height - 98) / 5)); panelHeight = 94 + rowGap * 5;
        top = Math.max(0, height - panelHeight - 4);
        int half = (panelWidth - 18) / 2;
        buttons.add(addRenderableWidget(Button.builder(Component.literal("Your parry"), b -> { mob = false; rebuild(); })
            .bounds(left + 6, top + 22, half, 18).build()));
        buttons.add(addRenderableWidget(Button.builder(Component.literal("Mob parry"), b -> { mob = true; rebuild(); })
            .bounds(left + 12 + half, top + 22, half, 18).build()));
        buttons.get(mob ? 1 : 0).active = false;
        if (mob) {
            Button adapt = addRenderableWidget(Button.builder(Component.literal("Adapt: " + (values[56] != 0 ? "On" : "Off")), b -> { values[56] = values[56] == 0 ? 1 : 0; rebuild(); })
                .bounds(left + 6, top + 43, half, 18).build()); adapt.active = editable; buttons.add(adapt);
            buttonTips.put(adapt, "Follow your parry impact settings instead of independent mob settings. Default: On.");
            Button invert = addRenderableWidget(Button.builder(Component.literal("Invert: " + (values[57] != 0 ? "On" : "Off")), b -> { values[57] = values[57] == 0 ? 1 : 0; rebuild(); })
                .bounds(left + 12 + half, top + 43, half, 18).build()); invert.active = editable; buttons.add(invert);
            buttonTips.put(invert, "Invert the mob impact filter. Works independently of Adapt. Default: On.");
        }
        int labelWidth = Math.min(130, panelWidth / 2);
        for (int i = 0; i < NAMES.length; i++) {
            final int index = i;
            int source = mob && values[56] != 0 ? i == 4 ? 50 : 40 + i : slot(i);
            ImpactSlider slider = addRenderableWidget(new ImpactSlider(left + labelWidth, top + 65 + i * rowGap,
                panelWidth - labelWidth - 8, Math.min(20, rowGap - 2), values[source], v -> values[slot(index)] = v));
            slider.active = editable && (!mob || values[56] == 0);
            slider.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                NAMES[i] + ": 0–400%. Default: " + dev.zeli.mallardguard.GuardClientPreset.DEFAULTS[slot(i)] + "%."
                + (mob ? " Adapt follows your parry settings; turn it off for independent sliders. Invert is independent." : " Changes are saved by Apply on the main config screen."))));
        }
        Button done = addRenderableWidget(Button.builder(Component.literal("Use these values"), b -> {
            accept.accept(values.clone()); Minecraft.getInstance().setScreen(parent);
        }).bounds(left + 6, top + panelHeight - 25, half, 20).build()); done.active = editable; buttons.add(done);
        buttons.add(addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
            .bounds(left + 12 + half, top + panelHeight - 25, half, 20).build()));
    }
    private void rebuild() { clearWidgets(); init(); }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        if (target.width > 0 && target.height > 0) {
            if (scene == null || scene.width != target.width || scene.height != target.height) {
                if (scene != null) scene.destroyBuffers();
                scene = new TextureTarget(target.width, target.height, false, false);
                capturePending = true;
            }
            target.bindWrite(false);
            if (capturePending) {
                copy(target, scene);
                capturePending = false;
            }
            int base = mob && values[56] == 0 ? 51 : 40;
            int chromatic = mob && values[56] == 0 ? values[55] : values[50];
            shaderAvailable = GuardImpactFrame.draw(scene, target, values[base], values[base + 1], values[base + 2], values[base + 3], chromatic, mob && values[57] != 0);
            if (!shaderAvailable) copy(scene, target);
        }
        graphics.fill(left - 2, top, left + panelWidth + 2, top + panelHeight, 0xED211B2A);
        graphics.fill(left - 2, top, left + panelWidth + 2, top + 1, 0xFF947D9A);
        graphics.drawCenteredString(font, "IMPACT PREVIEW", width / 2, top + 7, 0xFFF5F0F6);
        for (int i = 0; i < NAMES.length; i++) {
            String label = NAMES[i];
            int max = Math.min(120, panelWidth / 2 - 12);
            while (font.width(label) > max && label.length() > 1) label = label.substring(0, label.length() - 2) + "…";
            graphics.drawString(font, label, left + 7, top + 69 + i * rowGap, 0xFFF5F0F6);
        }
        if (!shaderAvailable) graphics.drawCenteredString(font, "Impact filter unavailable on this renderer", width / 2,
            Math.max(3, top - 18), 0xFFFFAAAA);
        for (var widget : renderables) if (!(widget instanceof Button)) widget.render(graphics, mouseX, mouseY, delta);
        for (Button button : buttons) {
            int x = button.getX(), y = button.getY(), w = button.getWidth(), h = button.getHeight();
            int edge = button.active && button.isMouseOver(mouseX, mouseY) ? 0xFFD8BEAA : 0xFF947D9A;
            graphics.fill(x, y, x + w, y + h, edge);
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, button.active ? 0xFF372D42 : 0xFF292431);
            graphics.drawCenteredString(font, button.getMessage(), x + w / 2, y + (h - 8) / 2,
                button.active ? 0xFFF5F0F6 : 0xFF978C9E);
        }
        for (var entry : buttonTips.entrySet()) if (entry.getKey().isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(font, font.split(Component.literal(entry.getValue()), Math.min(300, width - 24)), mouseX, mouseY);
            break;
        }
    }

    private static void copy(RenderTarget source, RenderTarget destination) {
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        if (scissor) GL11.glDisable(GL11.GL_SCISSOR_TEST);
        try {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destination.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, source.width, source.height,
                0, 0, destination.width, destination.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
            if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
        }
    }

    @Override public void removed() {
        if (scene != null) scene.destroyBuffers();
        scene = null;
    }

    @Override public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private static final class ImpactSlider extends AbstractSliderButton {
        private final Consumer<Integer> change;

        ImpactSlider(int x, int y, int width, int height, int current, Consumer<Integer> change) {
            super(x, y, width, height, Component.empty(), current / 400.0D);
            this.change = change;
            updateMessage();
        }

        @Override protected void updateMessage() {
            setMessage(Component.literal(Math.round(value * 400) + "%"));
        }

        @Override protected void applyValue() {
            change.accept((int) Math.round(value * 400));
        }

        @Override public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            graphics.fill(x, y, x + w, y + h, isHoveredOrFocused() ? 0xFFD8BEAA : 0xFF947D9A);
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF372D42);
            int offset = (int) Math.round(value * (w - 10));
            graphics.fill(x + 4, y + h - 5, x + w - 4, y + h - 3, 0xFF776581);
            graphics.fill(x + 4, y + h - 5, x + 4 + offset, y + h - 3, 0xFFD8BEAA);
            graphics.fill(x + 3 + offset, y + 2, x + 7 + offset, y + h - 6, 0xFFD8BEAA);
            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), x + w / 2, y + 4,
                active ? 0xFFF5F0F6 : 0xFF978C9E);
        }
    }
}
