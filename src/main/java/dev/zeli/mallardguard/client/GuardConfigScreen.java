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
    private boolean combatPage;
    private boolean parry, block, hud;
    private int perfect, window, recharge, angle, reductionPercent;
    private int followUp, parryReturnPercent, perfectReturnPercent, parryWear, perfectWear, blockWear, addedDurability;

    public GuardConfigScreen(GuardPackets.Settings settings) {
        super(Component.literal("Mallard Guard"));
        operator = settings.operator();
        parry = settings.parry(); block = settings.block();
        perfect = settings.perfect(); window = settings.window(); recharge = settings.recharge();
        angle = settings.angle(); reductionPercent = settings.reductionPercent();
        followUp = settings.followUp(); parryReturnPercent = settings.parryReturnPercent();
        perfectReturnPercent = settings.perfectReturnPercent(); parryWear = settings.parryWear();
        perfectWear = settings.perfectWear(); blockWear = settings.blockWear(); addedDurability = settings.addedDurability();
        hud = GuardConfig.HUD.get();
    }

    @Override protected void init() {
        int left = width / 2 - 110;
        int top = height / 2 - 123;
        addRenderableWidget(Button.builder(Component.literal(combatPage ? "Timing" : "Combat"), b -> {
            combatPage = !combatPage;
            rebuildWidgets();
        }).bounds(left, top + 20, 220, 20).build());
        if (combatPage) {
            slider(left, top + 44, "Follow-up safety", followUp, 0, 20, " ticks", v -> followUp = v);
            slider(left, top + 68, "Regular return", parryReturnPercent, 0, 300, "%", v -> parryReturnPercent = v);
            slider(left, top + 92, "Perfect return", perfectReturnPercent, 0, 300, "%", v -> perfectReturnPercent = v);
            slider(left, top + 116, "Regular wear", parryWear, 0, 100, "", v -> parryWear = v);
            slider(left, top + 140, "Perfect wear", perfectWear, 0, 100, "", v -> perfectWear = v);
            slider(left, top + 164, "Block wear", blockWear, 0, 100, "", v -> blockWear = v);
            slider(left, top + 188, "Added durability", addedDurability, 0, 10000, "", v -> addedDurability = v);
        } else {
            Button parryButton = addRenderableWidget(Button.builder(label("Parry", parry), b -> {
                parry = !parry; b.setMessage(label("Parry", parry));
            }).bounds(left, top + 44, 105, 20).build());
            Button blockButton = addRenderableWidget(Button.builder(label("Held block", block), b -> {
                block = !block; b.setMessage(label("Held block", block));
            }).bounds(left + 115, top + 44, 105, 20).build());
            parryButton.active = blockButton.active = operator;
            slider(left, top + 68, "Perfect window", perfect, 0, 5, " ticks", v -> perfect = v);
            slider(left, top + 92, "Parry window", window, 1, 10, " ticks", v -> window = v);
            slider(left, top + 116, "Recharge", recharge, 1, 60, " ticks", v -> recharge = v);
            slider(left, top + 140, "Facing angle", angle, 0, 360, "°", v -> angle = v);
            slider(left, top + 164, "Block reduction", reductionPercent, 0, 100, "%", v -> reductionPercent = v);
            addRenderableWidget(Button.builder(label("Crosshair shield", hud), b -> {
                hud = !hud; b.setMessage(label("Crosshair shield", hud));
            }).bounds(left, top + 188, 220, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Save & close"), b -> save()).bounds(left, top + 213, 105, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(left + 115, top + 213, 105, 20).build());
    }

    private void slider(int left, int y, String name, int current, int min, int max, String suffix, Change change) {
        Slider slider = addRenderableWidget(new Slider(left, y, 220, name, current, min, max, suffix, change));
        slider.active = operator;
    }

    private static Component label(String name, boolean value) {
        return Component.literal(name + ": " + (value ? "On" : "Off"));
    }

    private void save() {
        GuardConfig.HUD.set(hud);
        GuardConfig.CLIENT_SPEC.save();
        if (operator) PacketDistributor.sendToServer(new GuardPackets.Save(parry, block, perfect, window, recharge, angle, reductionPercent, followUp, parryReturnPercent, perfectReturnPercent, parryWear, perfectWear, blockWear, addedDurability));
        onClose();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 121, 0xFFFFFF);
    }

    private interface Change { void accept(int value); }

    private static final class Slider extends AbstractSliderButton {
        private final String name, suffix;
        private final int min, max;
        private final Change change;

        private Slider(int x, int y, int width, String name, int current, int min, int max, String suffix, Change change) {
            super(x, y, width, 20, Component.empty(), (double) (current - min) / (max - min));
            this.name = name; this.min = min; this.max = max; this.suffix = suffix; this.change = change;
            updateMessage();
        }

        @Override protected void updateMessage() {
            setMessage(Component.literal(name + ": " + (min + (int) Math.round(value * (max - min))) + suffix));
        }

        @Override protected void applyValue() {
            change.accept(min + (int) Math.round(value * (max - min)));
        }
    }
}
