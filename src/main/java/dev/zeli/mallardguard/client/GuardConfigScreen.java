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
    private int page;
    private boolean parry, block, hud, shieldEffects;
    private boolean hitSounds, hitParticles, screenFlash;
    private int flashStrength;
    private int masterVolume, perfectVolume, parryVolume, blockVolume;
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
        shieldEffects = GuardConfig.SHIELD_EFFECTS.get();
        hitSounds = settings.hitSounds();
        hitParticles = settings.hitParticles();
        masterVolume = settings.masterVolume(); perfectVolume = settings.perfectVolume();
        parryVolume = settings.parryVolume(); blockVolume = settings.blockVolume();
        screenFlash = GuardConfig.SCREEN_FLASH.get();
        flashStrength = GuardConfig.FLASH_STRENGTH.get();
    }

    @Override protected void init() {
        int left = width / 2 - 110;
        int top = height / 2 - 123;
        String[] pages = {"Timing", "Combat", "Effects", "Audio"};
        for (int i = 0; i < pages.length; i++) {
            final int target = i;
            Button tab = addRenderableWidget(Button.builder(Component.literal(pages[i]), b -> {
                page = target;
                rebuildWidgets();
            }).bounds(left + i * 56, top + 20, 52, 20).build());
            tab.active = page != i;
        }
        if (page == 1) {
            slider(left, top + 44, "Follow-up safety", followUp, 0, 20, " ticks", "After a successful parry, automatically parry additional melee hits for this many ticks. The stance still ends immediately; follow-up hits do not extend the timer. 0 disables it.", v -> followUp = v);
            slider(left, top + 68, "Parry retaliation", parryReturnPercent, 0, 300, "%", "Damage returned to the attacker by a regular parry, as a percentage of the incoming hit. Follow-up safety uses this value. 0 disables retaliation.", v -> parryReturnPercent = v);
            slider(left, top + 92, "Perfect retaliation", perfectReturnPercent, 0, 300, "%", "Damage returned to the attacker by a perfect parry, as a percentage of the incoming hit. 0 disables retaliation.", v -> perfectReturnPercent = v);
            slider(left, top + 116, "Parry item wear", parryWear, 0, 100, "", "Base durability lost from a regular parry or a follow-up safety parry. Fragile items can lose more. 0 disables this wear.", v -> parryWear = v);
            slider(left, top + 140, "Perfect item wear", perfectWear, 0, 100, "", "Base durability lost from a perfect parry. Fragile items can lose more. 0 disables this wear.", v -> perfectWear = v);
            slider(left, top + 164, "Block item wear", blockWear, 0, 100, "", "Base durability lost each time a hit is caught during held block. Fragile items can lose more. 0 disables this wear.", v -> blockWear = v);
            slider(left, top + 188, "Added durability", addedDurability, 0, 10000, "", "Maximum durability granted to eligible attack items that normally have none, when they first take parry or block wear. 0 disables this feature.", v -> addedDurability = v);
        } else if (page == 0) {
            Button parryButton = addRenderableWidget(Button.builder(label("Parry", parry), b -> {
                parry = !parry; b.setMessage(label("Parry", parry));
            }).bounds(left, top + 44, 105, 20).build());
            Button blockButton = addRenderableWidget(Button.builder(label("Held block", block), b -> {
                block = !block; b.setMessage(label("Held block", block));
            }).bounds(left + 115, top + 44, 105, 20).build());
            tip(parryButton, "Enables the short parry window when you press the configured guard key (right-click by default) with an eligible attack item.");
            tip(blockButton, "Enables the held guarding stance after the parry window while the configured guard key stays pressed. A valid hit during held block has its damage reduced.");
            parryButton.active = blockButton.active = operator;
            slider(left, top + 68, "Perfect window", perfect, 0, 5, " ticks", "The opening part of a parry attempt that counts as a perfect parry. 0 disables perfect parries. Included within the total parry window.", v -> perfect = v);
            slider(left, top + 92, "Parry window", window, 1, 10, " ticks", "Total time after pressing the configured guard key when an eligible melee hit can be parried, including the perfect window.", v -> window = v);
            slider(left, top + 116, "Recharge", recharge, 1, 60, " ticks", "Cooldown before you can begin another parry attempt. The shield refills while you are not holding block.", v -> recharge = v);
            slider(left, top + 140, "Facing angle", angle, 0, 360, "°", "How wide the valid attack area is around your view direction. 180° covers the front half; 360° allows melee hits from every direction. Attacker must still be close.", v -> angle = v);
            slider(left, top + 164, "Block reduction", reductionPercent, 0, 100, "%", "Percentage of incoming melee damage prevented during held block. 50% halves the damage; 100% prevents it all.", v -> reductionPercent = v);
        } else if (page == 2) {
            Button soundsButton = addRenderableWidget(Button.builder(label("Hit sounds", hitSounds), b -> {
                hitSounds = !hitSounds; b.setMessage(label("Hit sounds", hitSounds));
            }).bounds(left, top + 44, 105, 20).build());
            tip(soundsButton, "Plays one of the supplied sounds when a parry, perfect parry, or held block catches a hit. Other nearby players can hear it. Server setting.");
            Button particlesButton = addRenderableWidget(Button.builder(label("Hit particles", hitParticles), b -> {
                hitParticles = !hitParticles; b.setMessage(label("Hit particles", hitParticles));
            }).bounds(left + 115, top + 44, 105, 20).build());
            tip(particlesButton, "Spawns bundled anvil-style sparks at valid melee impacts for nearby players. No additional mod required. Server setting.");
            soundsButton.active = particlesButton.active = operator;
            tip(addRenderableWidget(Button.builder(label("Shield", hud), b -> {
                hud = !hud; b.setMessage(label("Shield", hud));
            }).bounds(left, top + 68, 105, 20).build()), "Shows the small shield near the crosshair while parrying or recharging. Only changes your own display.");
            tip(addRenderableWidget(Button.builder(label("Shield effects", shieldEffects), b -> {
                shieldEffects = !shieldEffects; b.setMessage(label("Shield effects", shieldEffects));
            }).bounds(left + 115, top + 68, 105, 20).build()), "Flashes the shield gold for a perfect parry, white for a regular parry, or red for a held block. The shield shakes and grows; perfect parries also send out a faint gold echo. Only changes your own display.");
            tip(addRenderableWidget(Button.builder(label("Screen flash", screenFlash), b -> {
                screenFlash = !screenFlash; b.setMessage(label("Screen flash", screenFlash));
            }).bounds(left, top + 92, 220, 20).build()), "A brief white flash and expanding golden rings behind the shield after a hit. Only changes your own display.");
            Slider strength = addRenderableWidget(new Slider(left, top + 116, 220, "Flash strength", flashStrength, 0, 100, "%", v -> flashStrength = v));
            tip(strength, "Brightness of the white flash and expanding rings. 0% disables them. Only changes your own display.");
        } else {
            Button soundsButton = addRenderableWidget(Button.builder(label("Hit sounds", hitSounds), b -> {
                hitSounds = !hitSounds; b.setMessage(label("Hit sounds", hitSounds));
            }).bounds(left, top + 44, 220, 20).build());
            tip(soundsButton, "Plays parry, perfect parry, and block sounds to nearby players. This switch mutes all three. Server setting.");
            soundsButton.active = operator;
            slider(left, top + 68, "Master volume", masterVolume, 0, 200, "%", "Controls all three guard hit sounds together. Multiplies their individual volumes. 0% mutes them; 100% is normal. Server setting.", v -> masterVolume = v);
            slider(left, top + 92, "Perfect volume", perfectVolume, 0, 200, "%", "Volume of the perfect parry sound, multiplied by master volume. 0% mutes this result only. Server setting.", v -> perfectVolume = v);
            slider(left, top + 116, "Parry volume", parryVolume, 0, 200, "%", "Volume of the regular and follow-up parry sounds, multiplied by master volume. 0% mutes this result only. Server setting.", v -> parryVolume = v);
            slider(left, top + 140, "Block volume", blockVolume, 0, 200, "%", "Volume of the held block sound, multiplied by master volume. 0% mutes this result only. Server setting.", v -> blockVolume = v);
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
        GuardConfig.SHIELD_EFFECTS.set(shieldEffects);
        GuardConfig.SCREEN_FLASH.set(screenFlash);
        GuardConfig.FLASH_STRENGTH.set(flashStrength);
        GuardConfig.CLIENT_SPEC.save();
        if (operator) PacketDistributor.sendToServer(new GuardPackets.Save(parry, block, perfect, window, recharge, angle, reductionPercent, followUp, parryReturnPercent, perfectReturnPercent, parryWear, perfectWear, blockWear, addedDurability, hitSounds, hitParticles, masterVolume, perfectVolume, parryVolume, blockVolume));
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
