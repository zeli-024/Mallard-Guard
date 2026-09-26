package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.GuardPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class GuardConfigScreen extends Screen {
    private final boolean operator;
    private boolean parry;
    private boolean block;
    private boolean hud;
    private int perfect;
    private int window;
    private int recharge;
    private int angle;
    private int reductionPercent;

    public GuardConfigScreen(GuardPackets.Settings settings) {
        super(Component.literal("Mallard Guard"));
        operator = settings.operator();
        parry = settings.parry();
        block = settings.block();
        perfect = settings.perfect();
        window = settings.window();
        recharge = settings.recharge();
        angle = settings.angle();
        reductionPercent = settings.reductionPercent();
        hud = GuardConfig.HUD.get();
    }

    @Override
    protected void init() {
        int left = width / 2 - 110;
        int top = height / 2 - 118;
        Button parryButton = addRenderableWidget(Button.builder(label("Parry", parry), b -> {
            parry = !parry;
            b.setMessage(label("Parry", parry));
        }).bounds(left, top + 28, 105, 20).build());
        Button blockButton = addRenderableWidget(Button.builder(label("Held block", block), b -> {
            block = !block;
            b.setMessage(label("Held block", block));
        }).bounds(left + 115, top + 28, 105, 20).build());
        Slider perfectSlider = addRenderableWidget(new Slider(left, top + 54, 220, "Perfect window", perfect, 0, 5, value -> perfect = value));
        Slider windowSlider = addRenderableWidget(new Slider(left, top + 78, 220, "Parry window", window, 1, 10, value -> window = value));
        Slider rechargeSlider = addRenderableWidget(new Slider(left, top + 102, 220, "Recharge", recharge, 1, 60, value -> recharge = value));
        Slider angleSlider = addRenderableWidget(new Slider(left, top + 126, 220, "Facing angle", angle, 0, 360, value -> angle = value));
        Slider reductionSlider = addRenderableWidget(new Slider(left, top + 150, 220, "Block reduction", reductionPercent, 0, 100, value -> reductionPercent = value));
        parryButton.active = blockButton.active = perfectSlider.active = windowSlider.active = rechargeSlider.active = angleSlider.active = reductionSlider.active = operator;
        addRenderableWidget(Button.builder(label("Crosshair shield", hud), b -> {
            hud = !hud;
            b.setMessage(label("Crosshair shield", hud));
        }).bounds(left, top + 174, 220, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save & close"), b -> save()).bounds(left, top + 201, 105, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(left + 115, top + 201, 105, 20).build());
    }

    private static Component label(String name, boolean value) {
        return Component.literal(name + ": " + (value ? "On" : "Off"));
    }

    private void save() {
        GuardConfig.HUD.set(hud);
        GuardConfig.CLIENT_SPEC.save();
        if (operator) PacketDistributor.sendToServer(new GuardPackets.Save(parry, block, perfect, window, recharge, angle, reductionPercent));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        int left = width / 2 - 110;
        int top = height / 2 - 118;
        graphics.drawCenteredString(font, title, width / 2, top + 3, 0xFFFFFF);
        graphics.drawString(font, operator ? "Server gameplay" : "Server gameplay (operator only)", left, top + 16, 0xAAAAAA);
        graphics.drawString(font, "Client display", left, top + 136, 0xAAAAAA);
    }

    private interface Change { void accept(int value); }

    private static final class Slider extends AbstractSliderButton {
        private final String name;
        private final int min;
        private final int max;
        private final Change change;

        private Slider(int x, int y, int width, String name, int current, int min, int max, Change change) {
            super(x, y, width, 20, Component.empty(), (double) (current - min) / (max - min));
            this.name = name;
            this.min = min;
            this.max = max;
            this.change = change;
            updateMessage();
        }

        @Override protected void updateMessage() {
            setMessage(Component.literal(name + ": " + (min + (int) Math.round(value * (max - min))) + " ticks"));
        }

        @Override protected void applyValue() {
            change.accept(min + (int) Math.round(value * (max - min)));
        }
    }
}
