package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.GuardItemRules;
import dev.zeli.mallardguard.GuardPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;

public final class GuardConfigScreen extends Screen {
    private final boolean operator;
    private int page;
    private boolean parry, block, hud, shieldEffects;
    private boolean hitSounds, hitParticles, screenFlash, screenShake;
    private int flashStrength, shakeStrength;
    private int masterVolume, perfectVolume, parryVolume, blockVolume;
    private int perfect, window, recharge, angle, reductionPercent;
    private int followUp, parryReturnPercent, perfectReturnPercent, parryWear, perfectWear, blockWear, addedDurability;
    private boolean fallParry, fallBreakBlocks, parryExplosions, perfectExplosionsOnly, blockExplosions;
    private boolean parryProjectiles, blockProjectiles, defenderKnockback;
    private int fallBlastStrength, fallLaunchPower, knockbackStrength;
    private boolean allowUsableItems;
    private String includedItems, excludedItems;
    private boolean invalidItemRules;

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
        screenShake = GuardConfig.SCREEN_SHAKE.get();
        shakeStrength = GuardConfig.SHAKE_STRENGTH.get();
        fallParry = settings.fallParry(); fallBreakBlocks = settings.fallBreakBlocks();
        fallBlastStrength = settings.fallBlastStrength(); fallLaunchPower = settings.fallLaunchPower();
        parryExplosions = settings.parryExplosions(); perfectExplosionsOnly = settings.perfectExplosionsOnly();
        blockExplosions = settings.blockExplosions(); parryProjectiles = settings.parryProjectiles();
        blockProjectiles = settings.blockProjectiles(); defenderKnockback = settings.defenderKnockback();
        knockbackStrength = settings.knockbackStrength();
        allowUsableItems = settings.allowUsableItems();
        includedItems = settings.includedItems(); excludedItems = settings.excludedItems();
    }

    @Override protected void init() {
        int left = width / 2 - 110;
        int top = height / 2 - 123;
        String[] pages = {"Timing", "Combat", "Effects", "Audio", "Hits", "Items"};
        for (int i = 0; i < pages.length; i++) {
            final int target = i;
            Button tab = addRenderableWidget(Button.builder(Component.literal(pages[i]), b -> {
                page = target;
                rebuildWidgets();
            }).bounds(left + i * 44, top + 20, 43, 20).build());
            tab.active = page != i;
        }
        if (page == 1) {
            slider(left, top + 44, "Follow-up safety", followUp, 0, 20, " ticks", "After a successful parry, automatically parry additional melee hits for this many ticks. The stance still ends immediately; follow-up hits do not extend the timer. 0 disables it.", v -> followUp = v);
            slider(left, top + 68, "Parry retaliation", parryReturnPercent, 0, 300, "%", "Damage returned to the attacker by a regular parry, as a percentage of the incoming hit. Follow-up safety uses this value. 0 disables retaliation.", v -> parryReturnPercent = v);
            slider(left, top + 92, "Perfect retaliation", perfectReturnPercent, 0, 300, "%", "Damage returned to the attacker by a perfect parry, as a percentage of the incoming hit. 0 disables retaliation.", v -> perfectReturnPercent = v);
            slider(left, top + 116, "Parry item wear", parryWear, 0, 100, "", "Base durability lost from a regular parry or a follow-up safety parry. Fragile items can lose more. 0 disables this wear.", v -> parryWear = v);
            slider(left, top + 140, "Perfect item wear", perfectWear, 0, 100, "", "Base durability lost from a perfect parry. Fragile items can lose more. 0 disables this wear.", v -> perfectWear = v);
            slider(left, top + 164, "Block item wear", blockWear, 0, 100, "", "Base durability lost each time a hit is caught during held block. Fragile items can lose more. 0 disables this wear.", v -> blockWear = v);
            slider(left, top + 188, "Added durability", addedDurability, 10, 1000, "", "Maximum durability granted to eligible attack items that normally have none, when they first take parry or block wear.", v -> addedDurability = v);
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
            tip(particlesButton, "Flying anvil sparks and one white firework orb on each parry; perfect parries also emit star-shaped sparks. Blocks emit none. Nearby players see these effects. Server setting.");
            soundsButton.active = particlesButton.active = operator;
            tip(addRenderableWidget(Button.builder(label("Shield", hud), b -> {
                hud = !hud; b.setMessage(label("Shield", hud));
            }).bounds(left, top + 68, 105, 20).build()), "Shows the shield centered below the crosshair while parrying or recharging. Only changes your own display.");
            tip(addRenderableWidget(Button.builder(label("Shield effects", shieldEffects), b -> {
                shieldEffects = !shieldEffects; b.setMessage(label("Shield effects", shieldEffects));
            }).bounds(left + 115, top + 68, 105, 20).build()), "Flashes the shield gold for a perfect parry, white for a regular parry, or red for a held block. The shield shakes and grows; perfect parries send out a large gold echo. Blocks briefly dim the screen edges. Only changes your own display.");
            tip(addRenderableWidget(Button.builder(label("Screen flash", screenFlash), b -> {
                screenFlash = !screenFlash; b.setMessage(label("Screen flash", screenFlash));
            }).bounds(left, top + 92, 220, 20).build()), "A brief white screen flash behind the shield and its echo after regular and perfect parries. Blocks do not flash. Only changes your own display.");
            Slider strength = addRenderableWidget(new Slider(left, top + 116, 220, "Flash strength", flashStrength, 0, 100, "%", v -> flashStrength = v));
            tip(strength, "Brightness of the white screen flash. 0% disables it. Only changes your own display.");
            tip(addRenderableWidget(Button.builder(label("Camera shake", screenShake), b -> {
                screenShake = !screenShake; b.setMessage(label("Camera shake", screenShake));
            }).bounds(left, top + 140, 220, 20).build()), "Briefly tilts your view after a hit: strongest for blocks, moderate for perfect parries, and lighter for regular parries. Only changes your own display.");
            Slider shake = addRenderableWidget(new Slider(left, top + 164, 220, "Shake strength", shakeStrength, 0, 100, "%", v -> shakeStrength = v));
            tip(shake, "Strength of camera shake after a successful guard. 0% disables it. Only changes your own display.");
        } else if (page == 4) {
            sourceToggle(left, top + 44, "Fall parry", () -> fallParry, v -> fallParry = v,
                "Parry fall damage. Launches in your look direction; perfect timing launches farther. Held block does not catch falls.");
            sourceToggle(left + 115, top + 44, "Break terrain", () -> fallBreakBlocks, v -> fallBreakBlocks = v,
                "Whether the blast from a fall of more than ten blocks can break terrain. Server setting.");
            slider(left, top + 68, "Fall blast", fallBlastStrength, 0, 500, "%",
                "Blast radius after a fall parry from more than ten blocks, relative to Parry It's normal scaling. 0% disables the blast. Server setting.", v -> fallBlastStrength = v);
            slider(left, top + 92, "Fall launch", fallLaunchPower, 0, 300, "%",
                "Launch power after a fall parry. Perfect falls launch twice as far. 0% absorbs the fall without launching. Server setting.", v -> fallLaunchPower = v);
            sourceToggle(left, top + 116, "Explosion parry", () -> parryExplosions, v -> parryExplosions = v,
                "Let a timed parry cancel explosion damage. The adjacent option can restrict this to perfect parries. Server setting.");
            sourceToggle(left + 115, top + 116, "Perfect only", () -> perfectExplosionsOnly, v -> perfectExplosionsOnly = v,
                "Explosion parrying requires a perfect timing window. A regular parry will not cancel explosion damage. Server setting.");
            sourceToggle(left, top + 140, "Explosion block", () -> blockExplosions, v -> blockExplosions = v,
                "Held guard reduces explosion damage using the normal block reduction. Independent of explosion parrying. Server setting.");
            sourceToggle(left + 115, top + 140, "Projectile block", () -> blockProjectiles, v -> blockProjectiles = v,
                "Held guard reduces incoming projectile damage using the normal block reduction. Server setting.");
            sourceToggle(left, top + 164, "Projectile parry", () -> parryProjectiles, v -> parryProjectiles = v,
                "Parries deflect projectile entities. Regular parries send them randomly; perfect parries aim them back toward the shooter. Server setting.");
            sourceToggle(left + 115, top + 164, "Defender push", () -> defenderKnockback, v -> defenderKnockback = v,
                "Pushes you away from melee, explosion, and projectile impacts after a successful parry. Fall parries use their own launch. Server setting.");
            slider(left, top + 188, "Defender push", knockbackStrength, 0, 200, "%",
                "Strength of the push you receive when parrying an impact. 0% disables it. Server setting.", v -> knockbackStrength = v);
        } else if (page == 5) {
            Button usable = addRenderableWidget(Button.builder(label("Allow usable items", allowUsableItems), b -> {
                allowUsableItems = !allowUsableItems;
                b.setMessage(label("Allow usable items", allowUsableItems));
            }).bounds(left, top + 44, 220, 20).build());
            tip(usable, "Whether attack-damage items with a hold-to-use action can also start guarding. Enabled by default. An explicit inclusion can still admit an item when off. Server setting.");
            usable.active = operator;
            EditBox included = addRenderableWidget(new EditBox(font, left, top + 88, 220, 20, Component.literal("Included items and tags")));
            included.setMaxLength(1024);
            included.setValue(includedItems);
            included.setResponder(s -> { includedItems = s; invalidItemRules = false; });
            included.setEditable(operator);
            tip(included, "Comma-separated item IDs or #item tags that can guard even without an attack-damage modifier. Example: minecraft:stick, #minecraft:swords. Exclusions always win.");
            EditBox excluded = addRenderableWidget(new EditBox(font, left, top + 136, 220, 20, Component.literal("Excluded items and tags")));
            excluded.setMaxLength(1024);
            excluded.setValue(excludedItems);
            excluded.setResponder(s -> { excludedItems = s; invalidItemRules = false; });
            excluded.setEditable(operator);
            tip(excluded, "Comma-separated item IDs or #item tags that cannot guard. Example: minecraft:bow, #yourmod:no_guard. Exclusions override inclusions.");
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

    private void sourceToggle(int x, int y, String name, BooleanSupplier current, Consumer<Boolean> change, String description) {
        Button button = addRenderableWidget(Button.builder(label(name, current.getAsBoolean()), b -> {
            change.accept(!current.getAsBoolean()); b.setMessage(label(name, current.getAsBoolean()));
        }).bounds(x, y, 105, 20).build());
        tip(button, description);
        button.active = operator;
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
        if (operator && (!GuardItemRules.valid(includedItems) || !GuardItemRules.valid(excludedItems))) {
            invalidItemRules = true;
            page = 5;
            rebuildWidgets();
            return;
        }
        GuardConfig.HUD.set(hud);
        GuardConfig.SHIELD_EFFECTS.set(shieldEffects);
        GuardConfig.SCREEN_FLASH.set(screenFlash);
        GuardConfig.FLASH_STRENGTH.set(flashStrength);
        GuardConfig.SCREEN_SHAKE.set(screenShake);
        GuardConfig.SHAKE_STRENGTH.set(shakeStrength);
        GuardConfig.CLIENT_SPEC.save();
        if (operator) PacketDistributor.sendToServer(new GuardPackets.Save(parry, block, perfect, window, recharge, angle, reductionPercent, followUp, parryReturnPercent, perfectReturnPercent, parryWear, perfectWear, blockWear, addedDurability, hitSounds, hitParticles, masterVolume, perfectVolume, parryVolume, blockVolume, fallParry, fallBreakBlocks, fallBlastStrength, fallLaunchPower, parryExplosions, perfectExplosionsOnly, blockExplosions, parryProjectiles, blockProjectiles, defenderKnockback, knockbackStrength, allowUsableItems, includedItems, excludedItems));
        onClose();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 121, 0xFFFFFF);
        if (page == 5) {
            int left = width / 2 - 110, top = height / 2 - 123;
            graphics.drawString(font, "Include IDs or #tags (comma-separated)", left, top + 76, 0xCCCCCC);
            graphics.drawString(font, "Exclude IDs or #tags (takes priority)", left, top + 124, 0xCCCCCC);
            graphics.drawString(font, invalidItemRules ? "Invalid item ID or tag. Check commas." : "Default: items with attack damage.", left, top + 164, invalidItemRules ? 0xFF6666 : 0xAAAAAA);
        }
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
