package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.GuardItemRules;
import dev.zeli.mallardguard.GuardPackets;
import net.minecraft.client.Minecraft;
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
    private final Screen parent;
    private int page;
    private int layoutTop, panelLeft, panelWidth, firstRow, visibleRows;
    private static final int ROW_STEP = 32;
    private static final String[] TABS = {"Parry", "Blocking", "Effects", "Audio", "Items"};
    private boolean parry, block, parryDrowningFire, hud, shieldEffects;
    private boolean hitParticles;
    private int flashStrength, shakeStrength;
    private int masterVolume, perfectVolume, parryVolume, blockVolume;
    private int perfect, window, recharge, angle, reductionPercent;
    private int followUp, parryReturnPercent, perfectReturnPercent, parryWear, perfectWear, blockWear, addedDurability;
    private boolean fallParry, fallBreakBlocks, parryExplosions, perfectExplosionsOnly, blockExplosions;
    private boolean parryProjectiles, blockProjectiles;
    private int fallBlastStrength, fallLaunchPower, knockbackStrength;
    private int guardMovementPercent, blockDeflectChance;
    private int regularOrbSize, perfectOrbSize, regularOrbOpacity, perfectOrbOpacity;
    private boolean allowUsableItems, preferOffhand;
    private String includedItems, excludedItems, shieldItems;
    private boolean invalidItemRules;

    public GuardConfigScreen(GuardPackets.Settings settings, Screen parent) {
        super(Component.literal("Mallard Guard"));
        this.parent = parent;
        operator = settings.operator();
        parry = settings.parry(); block = settings.block(); parryDrowningFire = settings.parryDrowningFire();
        perfect = settings.perfect(); window = settings.window(); recharge = settings.recharge();
        angle = settings.angle(); reductionPercent = settings.reductionPercent();
        followUp = settings.followUp(); parryReturnPercent = Math.clamp(settings.parryReturnPercent(), 0, 200);
        perfectReturnPercent = Math.clamp(settings.perfectReturnPercent(), 0, 200); parryWear = settings.parryWear();
        perfectWear = settings.perfectWear(); blockWear = settings.blockWear(); addedDurability = settings.addedDurability();
        hud = GuardConfig.HUD.get();
        shieldEffects = GuardConfig.SHIELD_EFFECTS.get();
        hitParticles = settings.hitParticles();
        masterVolume = settings.hitSounds() ? settings.masterVolume() : 0; perfectVolume = settings.perfectVolume();
        parryVolume = settings.parryVolume(); blockVolume = settings.blockVolume();
        flashStrength = GuardConfig.SCREEN_FLASH.get() ? GuardConfig.FLASH_STRENGTH.get() : 0;
        shakeStrength = GuardConfig.SCREEN_SHAKE.get() ? GuardConfig.SHAKE_STRENGTH.get() : 0;
        fallParry = settings.fallParry(); fallBreakBlocks = settings.fallBreakBlocks();
        fallBlastStrength = settings.fallBlastStrength(); fallLaunchPower = settings.fallLaunchPower();
        parryExplosions = settings.parryExplosions(); perfectExplosionsOnly = settings.perfectExplosionsOnly();
        blockExplosions = settings.blockExplosions(); parryProjectiles = settings.parryProjectiles();
        blockProjectiles = settings.blockProjectiles();
        knockbackStrength = settings.defenderKnockback() ? settings.knockbackStrength() : 0;
        guardMovementPercent = settings.guardMovementPercent();
        blockDeflectChance = settings.blockDeflectChance();
        regularOrbSize = GuardConfig.REGULAR_ORB_SIZE.get();
        perfectOrbSize = GuardConfig.PERFECT_ORB_SIZE.get();
        regularOrbOpacity = GuardConfig.REGULAR_ORB_OPACITY.get();
        perfectOrbOpacity = GuardConfig.PERFECT_ORB_OPACITY.get();
        allowUsableItems = settings.allowUsableItems();
        preferOffhand = GuardConfig.PREFER_OFFHAND.get();
        includedItems = settings.includedItems(); excludedItems = settings.excludedItems(); shieldItems = settings.shieldItems();
    }

    @Override protected void init() {
        panelWidth = Math.min(440, width - 18);
        panelLeft = (width - panelWidth) / 2;
        visibleRows = Math.max(3, Math.min(8, (height - 90) / 36));
        layoutTop = Math.max(2, (height - (58 + visibleRows * ROW_STEP + 47)) / 2);
        // Show whole, non-overlapping pages when the window size changes.
        firstRow = Math.clamp((firstRow / visibleRows) * visibleRows, 0, lastPageStart());
        int gap = 10;
        int tabWidth = (panelWidth - 4 * gap) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            final int selected = i;
            Button tab = addRenderableWidget(Button.builder(Component.literal(page == i ? "• " + TABS[i] : TABS[i]), b -> {
                page = selected;
                firstRow = 0;
                rebuildWidgets();
            }).bounds(panelLeft + i * (tabWidth + gap), layoutTop + 28, tabWidth, 20).build());
            tip(tab, page == i ? "Current section: " + TABS[i] + "." : "Show " + TABS[i].toLowerCase(java.util.Locale.ROOT) + " settings.");
        }

        if (page == 0) {
            toggle(0, 0, "Parrying", () -> parry, v -> parry = v, true,
                "Enable timed parries. Guard defaults to Right Click and can be rebound in Controls under Mallard Guard.");
            slider(1, 0, "Perfect window", perfect, 0, 5, " ticks", 1, true, v -> perfect = v,
                "Ticks at the start of a parry that count as perfect. 0 disables perfect parries.");
            slider(0, 1, "Parry window", window, 1, 10, " ticks", 1, true, v -> window = v,
                "How long a parry can catch a hit, including the perfect window.");
            slider(1, 1, "Recharge", recharge, 1, 60, " ticks", 1, true, v -> recharge = v,
                "Time before another parry can begin after this attempt.");
            slider(0, 2, "Facing angle", angle, 0, 360, "°", 1, true, v -> angle = v,
                "Which directions attacks can come from. 180° covers your front half; 360° covers all directions.");
            slider(1, 2, "Follow-up parries", followUp, 0, 20, " ticks", 1, true, v -> followUp = v,
                "Extra time to catch melee hits after a successful parry. 0 disables follow-ups; follow-up hits do not extend the timer.");
            slider(0, 3, "Parry retaliation", parryReturnPercent, 0, 200, "%", 1, true, v -> parryReturnPercent = v,
                "Damage dealt back on a regular parry, as a percentage of the incoming hit. 0% disables it; 200% doubles it.");
            slider(1, 3, "Perfect retaliation", perfectReturnPercent, 0, 200, "%", 1, true, v -> perfectReturnPercent = v,
                "Damage dealt back on a perfect parry, as a percentage of the incoming hit. 0% disables it; 200% doubles it.");
            toggle(0, 4, "Projectile parry", () -> parryProjectiles, v -> parryProjectiles = v, true,
                "Let parries deflect projectiles. Regular parries send them in a random direction; perfect parries aim them at the attacker.");
            toggle(1, 4, "Fall parry", () -> fallParry, v -> fallParry = v, true,
                "Let a timed parry prevent fall damage while looking at least 40° below the horizon, then launch forward. Perfect timing launches farther.");
            toggle(0, 5, "Explosion parry", () -> parryExplosions, v -> parryExplosions = v, true,
                "Let a timed parry prevent explosion damage.");
            toggle(1, 5, "Explosions: perfect only", () -> perfectExplosionsOnly, v -> perfectExplosionsOnly = v, true,
                "Require perfect timing to parry explosions. Only applies when explosion parrying is enabled.");
            slider(0, 6, "Fall blast", fallBlastStrength, 0, 500, "%", 1, true, v -> fallBlastStrength = v,
                "Size of the blast after parrying a fall of more than ten blocks. 0% disables the blast.");
            slider(1, 6, "Fall launch", fallLaunchPower, 0, 300, "%", 1, true, v -> fallLaunchPower = v,
                "How strongly a fall parry launches you. Perfect parries launch farther. 0% prevents damage without launching.");
            toggle(0, 7, "Fall blast breaks blocks", () -> fallBreakBlocks, v -> fallBreakBlocks = v, true,
                "Allow the blast after a long fall parry to break terrain.");
            toggle(0, 8, "Parry drowning and fire", () -> parryDrowningFire, v -> parryDrowningFire = v, true,
                "Let timed parries stop drowning and fire damage, including lava. Other damage types remain parryable when this is off.");
            slider(1, 7, "Parry pushback", knockbackStrength, 0, 200, "%", 1, true, v -> knockbackStrength = v,
                "How far a successful parry pushes you away from the hit. 0% disables pushback.");
        } else if (page == 1) {
            toggle(0, 0, "Blocking", () -> block, v -> block = v, true,
                "Keep guarding when you hold the Guard key past the parry window. Guard defaults to Right Click and can be rebound in Controls.");
            slider(1, 0, "Damage reduction", reductionPercent, 0, 100, "%", 1, true, v -> reductionPercent = v,
                "Damage prevented by held block. 50% halves the damage; 100% prevents it.");
            slider(0, 1, "Guard movement", guardMovementPercent, 0, 100, "%", 1, true, v -> guardMovementPercent = v,
                "Your speed during perfect parry, regular parry and block. This slowdown no longer changes camera FOV. 100% removes the slowdown.");
            slider(1, 1, "Block deflect chance", blockDeflectChance, 0, 100, "%", 1, true, v -> blockDeflectChance = v,
                "Chance that held block sends any incoming projectile in a random direction. 0% disables deflection.");
            toggle(0, 2, "Block projectiles", () -> blockProjectiles, v -> blockProjectiles = v, true,
                "Let held block reduce damage from projectiles that are not deflected.");
            toggle(1, 2, "Block explosions", () -> blockExplosions, v -> blockExplosions = v, true,
                "Let held block reduce explosion damage using your damage reduction setting.");
        } else if (page == 2) {
            toggle(0, 0, "Sparks and flashes", () -> hitParticles, v -> hitParticles = v, true,
                "Show sparks and flashes on parries. Perfect parries also have star-shaped sparks. Blocks do not create particles. Nearby players can see these effects.");
            toggle(1, 0, "Shield icon", () -> hud, v -> hud = v, false,
                "Show the shield by your crosshair while guarding and recharging.");
            toggle(0, 1, "Shield reactions", () -> shieldEffects, v -> shieldEffects = v, false,
                "Glow when ready; expand on parry, with a larger echo on perfect. Blocking hits flash red with a rounded vignette, then the dark shield trembles and echoes until you let go.");
            slider(1, 1, "Screen flash", flashStrength, 0, 100, "%", 1, false, v -> flashStrength = v,
                "Strength of the brief white screen flash after parries. 0% disables it. Blocks never trigger it.");
            slider(0, 2, "Camera shake", shakeStrength, 0, 100, "%", 1, false, v -> shakeStrength = v,
                "Strength of camera shake after a hit. Blocking shakes the screen the most. 0% disables it.");
            slider(1, 2, "Parry flash size", regularOrbSize, 0, 150, "%", 1, false, v -> regularOrbSize = v,
                "Size of the bright flash on a regular parry. 0% hides this flash but keeps the sparks.");
            slider(0, 3, "Perfect flash size", perfectOrbSize, 0, 150, "%", 1, false, v -> perfectOrbSize = v,
                "Size of the bright flash on a perfect parry. 0% hides this flash but keeps the sparks.");
            slider(1, 3, "Parry flash opacity", regularOrbOpacity, 0, 100, "%", 1, false, v -> regularOrbOpacity = v,
                "How visible the regular parry flash is. 0% hides it.");
            slider(0, 4, "Perfect flash opacity", perfectOrbOpacity, 0, 100, "%", 1, false, v -> perfectOrbOpacity = v,
                "How visible the perfect parry flash is. 0% hides it.");
        } else if (page == 3) {
            slider(0, 0, "Master volume", masterVolume, 0, 200, "%", 1, true, v -> masterVolume = v,
                "Volume of all Mallard Guard hit sounds. 0% mutes them; 200% reaches full game sound volume when an individual slider is at 100%.");
            slider(1, 0, "Perfect volume", perfectVolume, 0, 200, "%", 1, true, v -> perfectVolume = v,
                "Volume of perfect parry sounds, multiplied by the master volume. 0% mutes perfect parries.");
            slider(0, 1, "Parry volume", parryVolume, 0, 200, "%", 1, true, v -> parryVolume = v,
                "Volume of regular and follow-up parry sounds, multiplied by the master volume. 0% mutes them.");
            slider(1, 1, "Block volume", blockVolume, 0, 200, "%", 1, true, v -> blockVolume = v,
                "Volume of blocking sounds, multiplied by the master volume. 0% mutes them.");
        } else {
            toggle(0, 0, "Allow usable weapons", () -> allowUsableItems, v -> allowUsableItems = v, true,
                "Allow eligible weapons with a hold-to-use action to guard. Explicitly included items can still guard when this is off.");
            toggle(1, 0, "Prefer offhand", () -> preferOffhand, v -> preferOffhand = v, false,
                "When both hands hold eligible items, choose your offhand for guarding. When only one is eligible, use that hand.");
            slider(0, 1, "Parry durability", parryWear, 0, 100, "%", 10, true, v -> parryWear = v,
                "Maximum durability lost per regular or follow-up parry, rounded up. At 600 durability, 0.2% costs 2 points. 0% disables wear.");
            slider(1, 1, "Perfect durability", perfectWear, 0, 100, "%", 10, true, v -> perfectWear = v,
                "Maximum durability lost per perfect parry, rounded up. At 600 durability, 0.1% costs 1 point. 0% disables wear.");
            slider(0, 2, "Block durability", blockWear, 0, 100, "%", 10, true, v -> blockWear = v,
                "Maximum durability lost per blocked hit, rounded up. At 600 durability, 0.4% costs 3 points. 0% disables wear. Real shields use their own block wear.");
            slider(1, 2, "Added durability", addedDurability, 10, 1000, "", 1, true, v -> addedDurability = v,
                "Maximum durability given to eligible items that normally have none, when they first take wear.");
            editBox(3, "Included items and tags", includedItems, v -> { includedItems = v; invalidItemRules = false; },
                "Comma-separated item IDs or #item tags that may guard without an attack-damage modifier. Example: minecraft:stick, #minecraft:swords. Excluded items always win.");
            editBox(4, "Excluded items and tags", excludedItems, v -> { excludedItems = v; invalidItemRules = false; },
                "Comma-separated item IDs or #item tags that cannot guard. Excluded items win over included items.");
            editBox(5, "Extra shields and tags", shieldItems, v -> { shieldItems = v; invalidItemRules = false; },
                "Comma-separated item IDs or #item tags for other shield-like items. Shields and blocking items work automatically. Listed items parry first, then use their own blocking action while held; excluded items still win.");
        }

        int footer = layoutTop + 58 + visibleRows * ROW_STEP;
        Button previous = addRenderableWidget(Button.builder(Component.literal("Previous"), b -> {
            firstRow = Math.max(0, firstRow - visibleRows);
            rebuildWidgets();
        }).bounds(width / 2 - 141, footer + 3, 85, 20).build());
        Button next = addRenderableWidget(Button.builder(Component.literal("Next"), b -> {
            firstRow = Math.min(lastPageStart(), firstRow + visibleRows);
            rebuildWidgets();
        }).bounds(width / 2 + 56, footer + 3, 85, 20).build());
        previous.active = firstRow > 0;
        next.active = firstRow + visibleRows < pageRows();
        addRenderableWidget(Button.builder(Component.literal("Save & close"), b -> save())
            .bounds(columnX(0), footer + 27, columnWidth(), 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
            .bounds(columnX(1), footer + 27, columnWidth(), 20).build());
    }

    private int pageRows() {
        return switch (page) { case 0 -> 9; case 1 -> 3; case 2 -> 5; case 3 -> 2; default -> 6; };
    }

    private int lastPageStart() { return ((pageRows() - 1) / visibleRows) * visibleRows; }

    private int columnWidth() { return (panelWidth - 14) / 2; }
    private int columnX(int column) { return panelLeft + column * (columnWidth() + 14); }
    private boolean visible(int index) { return index >= firstRow && index < firstRow + visibleRows; }
    private int row(int index) { return layoutTop + 58 + (index - firstRow) * ROW_STEP; }

    private void toggle(int column, int index, String name, BooleanSupplier current,
                        Consumer<Boolean> change, boolean server, String description) {
        if (!visible(index)) return;
        Button button = addRenderableWidget(Button.builder(label(name, current.getAsBoolean()), b -> {
            change.accept(!current.getAsBoolean());
            b.setMessage(label(name, current.getAsBoolean()));
        }).bounds(columnX(column), row(index), columnWidth(), 22).build());
        button.active = !server || operator;
        tip(button, description + (server ? operator ? " Server setting." : " OP only: you can view this server setting, but cannot change it." : " Only changes your display."));
    }

    private void slider(int column, int index, String name, int current, int min, int max, String suffix,
                        int divisor, boolean server, Change change, String description) {
        if (!visible(index)) return;
        Slider control = addRenderableWidget(new Slider(columnX(column), row(index), columnWidth(),
            name, current, min, max, suffix, change, divisor));
        control.active = !server || operator;
        tip(control, description + (server ? operator ? " Server setting." : " OP only: you can view this server setting, but cannot change it." : " Only changes your display."));
    }

    private void editBox(int index, String name, String initial, Consumer<String> update, String description) {
        if (!visible(index)) return;
        EditBox box = addRenderableWidget(new EditBox(font, panelLeft, row(index) + 12, panelWidth, 18,
            Component.literal(name)));
        box.setMaxLength(1024);
        box.setValue(initial);
        box.setResponder(update);
        box.setEditable(operator);
        box.active = operator;
        tip(box, description + (operator ? " Server setting." : " OP only: you can view this server setting, but cannot change it."));
    }

    private static void tip(AbstractWidget widget, String description) {
        widget.setTooltip(Tooltip.create(Component.literal(description)));
    }

    private static Component label(String name, boolean value) {
        return Component.literal(name + ": " + (value ? "On" : "Off"));
    }

    private void save() {
        if (operator && (!GuardItemRules.valid(includedItems) || !GuardItemRules.valid(excludedItems) || !GuardItemRules.valid(shieldItems))) {
            invalidItemRules = true;
            page = 4;
            firstRow = (5 / visibleRows) * visibleRows;
            rebuildWidgets();
            return;
        }
        GuardConfig.HUD.set(hud);
        GuardConfig.SHIELD_EFFECTS.set(shieldEffects);
        GuardConfig.SCREEN_FLASH.set(flashStrength > 0);
        GuardConfig.FLASH_STRENGTH.set(flashStrength);
        GuardConfig.SCREEN_SHAKE.set(shakeStrength > 0);
        GuardConfig.SHAKE_STRENGTH.set(shakeStrength);
        GuardConfig.PREFER_OFFHAND.set(preferOffhand);
        GuardConfig.REGULAR_ORB_SIZE.set(regularOrbSize);
        GuardConfig.PERFECT_ORB_SIZE.set(perfectOrbSize);
        GuardConfig.REGULAR_ORB_OPACITY.set(regularOrbOpacity);
        GuardConfig.PERFECT_ORB_OPACITY.set(perfectOrbOpacity);
        GuardConfig.CLIENT_SPEC.save();
        if (operator) PacketDistributor.sendToServer(new GuardPackets.Save(parry, block, parryDrowningFire, perfect, window, recharge, angle, reductionPercent, followUp, parryReturnPercent, perfectReturnPercent, parryWear, perfectWear, blockWear, addedDurability, masterVolume > 0, hitParticles, masterVolume, perfectVolume, parryVolume, blockVolume, fallParry, fallBreakBlocks, fallBlastStrength, fallLaunchPower, parryExplosions, perfectExplosionsOnly, blockExplosions, parryProjectiles, blockProjectiles, knockbackStrength > 0, knockbackStrength, guardMovementPercent, blockDeflectChance, allowUsableItems, includedItems, excludedItems, shieldItems));
        onClose();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, layoutTop + 8, 0xFFFFFF);
        if (!operator) graphics.drawCenteredString(font, "Gray settings require OP", width / 2, layoutTop + 18, 0xAAAAAA);
        int footer = layoutTop + 58 + visibleRows * ROW_STEP;
        graphics.drawCenteredString(font, "Page " + (firstRow / visibleRows + 1) + " / "
            + ((pageRows() - 1) / visibleRows + 1), width / 2, footer + 9, 0xCCCCCC);
        if (page == 4) {
            if (visible(3)) graphics.drawString(font, "Included items and tags", panelLeft, row(3), operator ? 0xCCCCCC : 0x777777);
            if (visible(4)) graphics.drawString(font, "Excluded items and tags", panelLeft, row(4), operator ? 0xCCCCCC : 0x777777);
            if (visible(5)) graphics.drawString(font, "Extra shields and tags", panelLeft, row(5), operator ? 0xCCCCCC : 0x777777);
            if (invalidItemRules) graphics.drawCenteredString(font, "Check the item IDs and commas.", width / 2,
                layoutTop + 50, 0xFF6666);
        }
    }

    @Override public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private interface Change { void accept(int value); }

    private static final class Slider extends AbstractSliderButton {
        private final String name, suffix;
        private final int min, max;
        private final int divisor;
        private final Change change;

        private Slider(int x, int y, int width, String name, int current, int min, int max, String suffix, Change change) {
            this(x, y, width, name, current, min, max, suffix, change, 1);
        }

        private Slider(int x, int y, int width, String name, int current, int min, int max, String suffix, Change change, int divisor) {
            super(x, y, width, 20, Component.empty(), (double) (current - min) / (max - min));
            this.name = name; this.min = min; this.max = max; this.suffix = suffix; this.change = change; this.divisor = divisor;
            updateMessage();
        }

        @Override protected void updateMessage() {
            int shown = min + (int) Math.round(value * (max - min));
            setMessage(Component.literal(name + ": " + (divisor == 1 ? Integer.toString(shown) : String.format(java.util.Locale.ROOT, divisor == 10 ? "%.1f" : "%.2f", shown / (double) divisor)) + suffix));
        }

        @Override protected void applyValue() {
            change.accept(min + (int) Math.round(value * (max - min)));
        }
    }
}
