package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.GuardPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class GuardConfigScreen extends Screen {
    private final boolean operator;
    private boolean combatPage;
    private boolean parry, block, hud, resultText;
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
        resultText = GuardConfig.RESULT_TEXT.get();
    }

    @Override protected void init() {
        int left = width / 2 - 110;
        int top = height / 2 - 123;
        addRenderableWidget(Button.builder(Component.literal(combatPage ? "Timing" : "Combat"), b -> {
            combatPage = !combatPage;
            rebuildWidgets();
        }).bounds(left, top + 20, 220, 20).build());
        if (combatPage) {
            slider(left, top + 44, "Follow-up safety", followUp, 0, 20, " ticks", "After a successful parry, automatically parry additional melee hits for this many ticks. The stance still ends immediately; follow-up hits do not extend the timer. 0 disables it.", v -> followUp = v);
            slider(left, top + 68, "Parry retaliation", parryReturnPercent, 0, 300, "%", "Damage returned to the attacker by a regular parry, as a percentage of the incoming hit. Follow-up safety uses this value. 0 disables retaliation.", v -> parryReturnPercent = v);
            slider(left, top + 92, "Perfect retaliation", perfectReturnPercent, 0, 300, "%", "Damage returned to the attacker by a perfect parry, as a percentage of the incoming hit. 0 disables retaliation.", v -> perfectReturnPercent = v);
            slider(left, top + 116, "Parry item wear", parryWear, 0, 100, "", "Base durability lost from a regular parry or a follow-up safety parry. Fragile items can lose more. 0 disables this wear.", v -> parryWear = v);
            slider(left, top + 140, "Perfect item wear", perfectWear, 0, 100, "", "Base durability lost from a perfect parry. Fragile items can lose more. 0 disables this wear.", v -> perfectWear = v);
            slider(left, top + 164, "Block item wear", blockWear, 0, 100, "", "Base durability lost each time a hit is caught during held block. Fragile items can lose more. 0 disables this wear.", v -> blockWear = v);
            slider(left, top + 188, "Added durability", addedDurability, 0, 10000, "", "Maximum durability granted to eligible attack items that normally have none, when they first take parry or block wear. 0 disables this feature.", v -> addedDurability = v);
        } else {
            Button parryButton = addRenderableWidget(Button.builder(label("Parry", parry), b -> {
                parry = !parry; b.setMessage(label("Parry", parry));
            }).bounds(left, top + 44, 105, 20).build());
            Button blockButton = addRenderableWidget(Button.builder(label("Held block", block), b -> {
                block = !block; b.setMessage(label("Held block", block));
            }).bounds(left + 115, top + 44, 105, 20).build());
            tip(parryButton, "Enables the short parry window when you press Use Item with an eligible attack item.");
            tip(blockButton, "Enables the held guarding stance after the parry window while Use Item stays pressed. A valid hit during held block has its damage reduced.");
            parryButton.active = blockButton.active = operator;
            slider(left, top + 68, "Perfect window", perfect, 0, 5, " ticks", "The opening part of a parry attempt that counts as a perfect parry. 0 disables perfect parries. Included within the total parry window.", v -> perfect = v);
            slider(left, top + 92, "Parry window", window, 1, 10, " ticks", "Total time after pressing Use Item when an eligible melee hit can be parried, including the perfect window.", v -> window = v);
            slider(left, top + 116, "Recharge", recharge, 1, 60, " ticks", "Cooldown before you can begin another parry attempt. The shield refills while you are not holding block.", v -> recharge = v);
            slider(left, top + 140, "Facing angle", angle, 0, 360, "°", "How wide the valid attack area is around your view direction. 180° covers the front half; 360° allows melee hits from every direction. Attacker must still be close.", v -> angle = v);
            slider(left, top + 164, "Block reduction", reductionPercent, 0, 100, "%", "Percentage of incoming melee damage prevented during held block. 50% halves the damage; 100% prevents it all.", v -> reductionPercent = v);
            tip(addRenderableWidget(Button.builder(label("Shield", hud), b -> {
                hud = !hud; b.setMessage(label("Shield", hud));
            }).bounds(left, top + 188, 105, 20).build()), "Shows the small shield near the crosshair while parrying or recharging. Only changes your own display.");
            tip(addRenderableWidget(Button.builder(label("Result text", resultText), b -> {
                resultText = !resultText; b.setMessage(label("Result text", resultText));
            }).bounds(left + 115, top + 188, 105, 20).build()), "Shows brief PARRY, PERFECT PARRY, or BLOCK text near the crosshair after a valid hit. Only changes your own display.");
        }
        addRenderableWidget(Button.builder(Component.literal("Save & close"), b -> save()).bounds(left, top + 213, 105, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(left + 115, top + 213, 105, 20).build());
    }

    private void slider(int left, int y, String name, int current, int min, int max, String suffix, String description, Change change) {
        Slider slider = addRenderableWidget(new Slider(left, y, 220, name, current, min, max, suffix, change));
        tip(slider, description);
        slider.active = operator;
    }

    private static void tip(AbstractWidget widget, String description) {
        widget.setTooltip(Tooltip.create(Component.literal(description)));
    }

    private static Component label(String name, boolean value) {
        return Component.literal(name + ": " + (value ? "On" : "Off"));
    }

    private void save() {
        GuardConfig.HUD.set(hud);
        GuardConfig.RESULT_TEXT.set(resultText);
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
