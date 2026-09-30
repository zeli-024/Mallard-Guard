package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.GuardDamageRules;
import dev.zeli.mallardguard.GuardClientPreset;
import dev.zeli.mallardguard.GuardItemRules;
import dev.zeli.mallardguard.GuardPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.ModList;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;

public final class GuardConfigScreen extends Screen {
    private final boolean operator;
    private final Screen parent;
    private static int rememberedClientPage = 7, rememberedServerPage, rememberedSection;
    private static int rememberedClientRow, rememberedServerRow, rememberedEnforceRow, rememberedExperimentalRow, rememberedExperimentalPage = 12;
    private static final Map<String, String> rememberedGroups = new HashMap<>();
    private int page;
    private int clientPage = rememberedClientPage, serverPage = rememberedServerPage, clientRow = rememberedClientRow, serverRow = rememberedServerRow, enforceRow = rememberedEnforceRow, experimentalRow = rememberedExperimentalRow, experimentalPage = rememberedExperimentalPage;
    private int sidebarOffset;
    private boolean experimentalSection = rememberedSection == 3;
    private boolean clientSection = rememberedSection == 0;
    private boolean enforceSection = rememberedSection == 2, enforceEnabled, enforceParticlesOpen, enforceHudOpen, enforceAudioOpen, enforceItemsOpen, enforceStaggerOpen;
    private int dontEnforceMask;
    private final int[] enforcedDefaults;
    private int activeClientCategory;
    private int layoutTop, panelLeft, panelWidth, firstRow, visibleRows, sidebarLeft, sidebarWidth, controlIndex;
    private boolean draggingScroll;
    private boolean dirty;
    private boolean dirtyClient, dirtyServer, dirtyServerRules, dirtyPolicy;
    private boolean dirtyDamageRules;
    private boolean dirtyShield, dirtyMobs, dirtyStagger;
    private boolean staggerEnabled;
    private int staggerCells, staggerParryLoss, staggerPerfectLoss, staggerRestore, staggerNearTicks, staggerFarTicks, staggerRadius, staggerStunTicks, staggerExitRecovery;
    private boolean staggerGaugeOpen, staggerRecoveryOpen, chromaticOpen, staggerDisplayOpen;
    private final int[] extraClient = GuardClientPreset.readLocal();
    private boolean mobGuard, mobEligibilityOpen, mobDefenseOpen, mobEquipmentOpen, mobTimingOpen, mobAppearanceOpen, mobBlocking, mobRetaliation;
    private int mobHitsBeforeGuard, mobPerfectTicks, mobParryTicks, mobCounterTicks;
    private int mobApproachChance, mobTacticalChance, mobTacticalSeconds, mobTacticalCooldownTicks;
    private String mobHumanoidIds;
    private int mobDifficulty, mobParryColor, mobPerfectColor, mobMovement, mobApproachDistance, mobApproachSeconds, mobRushChance, mobGearChance;
    private final LinkedHashMap<String, Boolean> damageRules = new LinkedHashMap<>();
    private final List<String> recentHits = new ArrayList<>();
    private final List<String> damageCatalog = new ArrayList<>();
    private String damageSearch = "";
    private boolean damageSearchChanged;
    private EditBox damageSearchBox;
    private EditBox settingsSearchBox;
    private String settingsSearch = "";
    private boolean settingsSearchChanged;
    private final Map<String, Integer> visibleSettingRows = new HashMap<>();
    private final Map<String, String> sourceMods = new HashMap<>();
    private boolean sparkOpen, streakOpen, sphericalFlashOpen, shieldOpen, screenFlashOpen, cameraFeedbackOpen, animationOpen;
    private boolean timingOpen, retaliationOpen, pushbackOpen, parryProjectileOpen, parryExplosionOpen;
    private boolean guardOpen, blockProjectileOpen, blockExplosionOpen;
    private boolean eligibilityOpen, durabilityOpen, itemListsOpen;
    private boolean shieldTimingOpen, shieldPerfectOpen, shieldIdsOpen;
    private boolean commonDamageOpen, recentDamageOpen, otherDamageOpen;
    private final List<DropdownLabel> dropdownLabels = new ArrayList<>();
    private final List<SettingLabel> sectionLabels = new ArrayList<>();
    private final List<Button> themedButtons = new ArrayList<>();
    private final Map<Button, String> buttonTips = new HashMap<>();
    private final Map<AbstractWidget, String> widgetTips = new HashMap<>();
    private static final int WARM_EDGE = 0xFFD8BEAA;
    private static final int TEXT = 0xFFF5F0F6;
    private static final int ROW_STEP = 25;
    private final List<SettingLabel> settingLabels = new ArrayList<>();
    private static final String[] SERVER_TABS = {"Parry", "Fall Impact", "Blocking", "Shields", "Damage Types", "Shared Effects", "Equipment"};
    private static final int[] SERVER_PAGES = {0, 5, 1, 11, 6, 2, 4};
    private static final String[] CLIENT_TABS = {"Particles", "Visuals", "Audio", "Hand Selection"};
    private static final int[] CLIENT_PAGES = {7, 2, 3, 4};
    private static final int[] EXPERIMENTAL_PAGES = {12, 13};
    private static final String[] EXPERIMENTAL_TABS = {"Mob Guard", "Stagger"};
    private final java.util.Set<Button> redButtons = new java.util.HashSet<>();
    private boolean parry, block, parryDrowningFire, parryStarvation, parryGenericKill, hud, shieldEffects, memeFlash;
    private boolean hitParticles, sparksEnabled, sparkRing, particleFlashesEnabled, streaksEnabled;
    private boolean perfectOnlyFlash, perfectOnlyHitlag, impactPerfectOnly;
    private int flashStrength, shakeStrength, hitlagFrames, impactFrames;
    private int impactBrightness, impactContrast, impactEdges, impactGrain;
    private int masterVolume, perfectVolume, parryVolume, blockVolume;
    private boolean serverHitSounds;
    private final int serverMasterVolume, serverPerfectVolume, serverParryVolume, serverBlockVolume;
    private int perfect, window, recharge, angle, reductionPercent;
    private int followUp, parryReturnPercent, perfectReturnPercent, retaliationCap, parryWear, perfectWear, blockWear;
    private boolean fallParry, fallPerfectParry, fallLookDown, fallBreakBlocks, parryExplosions, perfectExplosionsOnly, blockExplosions;
    private boolean parryProjectiles, blockProjectiles;
    private int fallBlastStrength, fallLaunchPower, knockbackStrength;
    private int guardMovementPercent, blockDeflectChance;
    private int regularOrbSize, perfectOrbSize, regularOrbOpacity, perfectOrbOpacity, regularOrbLifetime, perfectOrbLifetime;
    private int regularSparks, perfectSparks, stars, sparkLifetime, sparkLength, sparkExplosiveness, sparkReach;
    private int streakAmount, regularStreakAmount, streakLength, streakLifetime, streakDotSize, streakStretchSpeed, streakDotSpacing, regularStreakDotSpacing, streakBallisticCurve;
    private boolean allowAnyItem, allowEmptyHand, allowUsableItems, preferOffhand, firstPersonAnimation, thirdPersonAnimation;
    private boolean consumablePriority, cooldownPreventsGuard, randomShieldWeapon, coneSparks, randomDualTools;
    private int shieldPerfect, shieldWindow, shieldRechargeTicks, shieldMaxBlocks, toolMaxBlocks, shieldBreakTicks, shieldCone, shieldReach;
    private int shieldReturn, shieldStun, shieldPerfectPush, shieldRegularPush, toolPush;
    private String stunnableBosses;
    private String includedItems, excludedItems, shieldItems;
    private boolean invalidItemRules;

    public GuardConfigScreen(GuardPackets.Settings settings, Screen parent) {
        super(Component.literal("Mallard Guard"));
        this.parent = parent;
        this.page = experimentalSection ? experimentalPage : enforceSection ? 8 : clientSection ? clientPage : serverPage;
        this.firstRow = experimentalSection ? experimentalRow : enforceSection ? enforceRow : clientSection ? clientRow : serverRow;
        operator = settings.operator();
        GuardPackets.ClientPolicy policy = GuardClient.currentPolicy();
        enforceEnabled = policy.enabled();
        enforceParticlesOpen = enforceHudOpen = enforceAudioOpen = enforceItemsOpen = enforceStaggerOpen = false;
        dontEnforceMask = policy.dontEnforceMask();
        int[] received = GuardClientPreset.parse(policy.defaults());
        enforcedDefaults = received == null ? GuardClientPreset.DEFAULTS.clone() : received;
        serverHitSounds = settings.hitSounds(); serverMasterVolume = settings.masterVolume();
        serverPerfectVolume = settings.perfectVolume(); serverParryVolume = settings.parryVolume(); serverBlockVolume = settings.blockVolume();
        parry = settings.parry(); block = settings.block(); parryDrowningFire = settings.parryDrowningFire();
        parryStarvation = settings.parryStarvation(); parryGenericKill = settings.parryGenericKill();
        perfect = settings.perfect(); window = settings.window(); recharge = settings.recharge();
        angle = settings.angle(); reductionPercent = settings.reductionPercent();
        followUp = settings.followUp(); parryReturnPercent = Math.clamp(settings.parryReturnPercent(), 0, 200);
        perfectReturnPercent = Math.clamp(settings.perfectReturnPercent(), 0, 200); parryWear = settings.parryWear();
        perfectWear = settings.perfectWear(); blockWear = settings.blockWear();
        hud = GuardConfig.HUD.get();
        shieldEffects = GuardConfig.SHIELD_EFFECTS.get();
        memeFlash = GuardConfig.MEME_FLASH.get();
        hitlagFrames = GuardConfig.HITLAG_FRAMES.get();
        perfectOnlyHitlag = GuardConfig.PERFECT_ONLY_HITLAG.get();
        impactFrames = GuardConfig.IMPACT_FRAMES.get();
        impactPerfectOnly = GuardConfig.IMPACT_PERFECT_ONLY.get();
        impactBrightness = GuardConfig.IMPACT_BRIGHTNESS.get(); impactContrast = GuardConfig.IMPACT_CONTRAST.get();
        impactEdges = GuardConfig.IMPACT_EDGES.get(); impactGrain = GuardConfig.IMPACT_GRAIN.get();
        hitParticles = settings.hitParticles();
        sparksEnabled = GuardConfig.SPARKS_ENABLED.get();
        sparkRing = GuardConfig.SPARK_RING.get();
        streaksEnabled = GuardConfig.STREAKS_ENABLED.get();
        streakAmount = GuardConfig.STREAK_AMOUNT.get(); regularStreakAmount = GuardConfig.REGULAR_STREAK_AMOUNT.get(); streakLength = GuardConfig.STREAK_LENGTH.get();
        streakLifetime = GuardConfig.STREAK_LIFETIME.get();
        regularOrbLifetime = GuardConfig.REGULAR_ORB_LIFETIME.get();
        perfectOrbLifetime = GuardConfig.PERFECT_ORB_LIFETIME.get();
        streakDotSize = GuardConfig.STREAK_DOT_SIZE.get(); streakDotSpacing = GuardConfig.STREAK_DOT_SPACING.get();
            regularStreakDotSpacing = GuardConfig.REGULAR_STREAK_DOT_SPACING.get();
            streakBallisticCurve = GuardConfig.STREAK_BALLISTIC_CURVE.get();
        streakStretchSpeed = GuardConfig.STREAK_STRETCH_SPEED.get();
        particleFlashesEnabled = GuardConfig.PARTICLE_FLASHES_ENABLED.get();
        masterVolume = GuardConfig.LOCAL_MASTER_VOLUME.get(); perfectVolume = GuardConfig.LOCAL_PERFECT_VOLUME.get();
        parryVolume = GuardConfig.LOCAL_PARRY_VOLUME.get(); blockVolume = GuardConfig.LOCAL_BLOCK_VOLUME.get();
        flashStrength = GuardConfig.SCREEN_FLASH.get() ? GuardConfig.FLASH_STRENGTH.get() : 0;
        perfectOnlyFlash = GuardConfig.PERFECT_ONLY_FLASH.get();
        shakeStrength = GuardConfig.SCREEN_SHAKE.get() ? GuardConfig.SHAKE_STRENGTH.get() : 0;
        fallParry = settings.fallParry(); fallPerfectParry = settings.fallPerfectParry(); fallLookDown = settings.fallLookDown(); fallBreakBlocks = settings.fallBreakBlocks();
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
        regularSparks = GuardConfig.REGULAR_SPARK_COUNT.get(); perfectSparks = GuardConfig.PERFECT_SPARK_COUNT.get();
        stars = GuardConfig.STAR_COUNT.get(); sparkLifetime = GuardConfig.SPARK_LIFETIME.get();
        sparkLength = GuardConfig.SPARK_LENGTH.get(); sparkExplosiveness = GuardConfig.SPARK_EXPLOSIVENESS.get(); sparkReach = GuardConfig.SPARK_REACH.get();
        allowUsableItems = settings.allowUsableItems();
        allowAnyItem = settings.allowAnyItem();
        preferOffhand = GuardConfig.PREFER_OFFHAND.get();
        firstPersonAnimation = GuardConfig.FIRST_PERSON_ANIMATION.get();
        thirdPersonAnimation = GuardConfig.THIRD_PERSON_ANIMATION.get();
        includedItems = settings.includedItems(); excludedItems = settings.excludedItems(); shieldItems = settings.shieldItems();
        consumablePriority = settings.consumablePriority();
        setShieldSettings(GuardClient.currentShieldSettings());
        setMobSettings(GuardClient.currentMobSettings());
        setStaggerSettings(GuardClient.currentStaggerSettings());
        selectRememberedGroup();
    }

    private void setShieldSettings(GuardPackets.ShieldSettings settings) {
        shieldPerfect = settings.perfect(); shieldWindow = settings.window(); shieldRechargeTicks = settings.rechargeTicks(); shieldMaxBlocks = settings.maxBlocks(); toolMaxBlocks = settings.toolMaxBlocks();
        shieldBreakTicks = settings.breakTicks(); shieldCone = settings.cone(); shieldReach = settings.reach(); shieldReturn = settings.retaliation();
        cooldownPreventsGuard = settings.cooldownPreventsGuard(); allowEmptyHand = settings.allowEmptyHand();
        shieldStun = settings.stunTicks(); shieldPerfectPush = settings.perfectPushback();
        shieldRegularPush = settings.regularPushback(); toolPush = settings.weaponPushback();
        retaliationCap = settings.retaliationCap();
        randomDualTools = settings.randomDualTools(); randomShieldWeapon = settings.randomShieldWeapon(); coneSparks = settings.coneSparks();
        stunnableBosses = settings.stunnableBosses();
    }

    private void setMobSettings(GuardPackets.MobSettings data) {
        mobGuard = data.enabled(); mobDifficulty = data.difficulty(); mobBlocking = data.blocking();
        mobParryColor = data.parryColor(); mobPerfectColor = data.perfectColor();
        mobCounterTicks = data.counterTicks(); mobRetaliation = data.retaliation();
        mobMovement = data.movement(); mobApproachDistance = data.approachDistance(); mobApproachSeconds = data.approachSeconds();
        mobRushChance = data.rushChance(); mobGearChance = data.gearChance();
        mobHitsBeforeGuard = data.hitsBeforeGuard();
        mobPerfectTicks = data.perfectTicks(); mobParryTicks = data.parryTicks();
        mobApproachChance = data.approachChance(); mobTacticalChance = data.tacticalChance();
        mobTacticalSeconds = data.tacticalSeconds(); mobTacticalCooldownTicks = data.tacticalCooldownTicks();
        mobHumanoidIds = data.humanoidIds();
    }
    private void setStaggerSettings(GuardPackets.StaggerSettings d) {
        staggerEnabled = d.enabled(); staggerCells = d.cells(); staggerParryLoss = d.parryLoss(); staggerPerfectLoss = d.perfectLoss();
        staggerRestore = d.restore(); staggerNearTicks = d.nearTicks(); staggerFarTicks = d.farTicks(); staggerRadius = d.radius(); staggerStunTicks = d.stunTicks(); staggerExitRecovery = Math.min(staggerCells, d.exitRecovery());
    }
    public void updateStaggerSettings(GuardPackets.StaggerSettings d) { if (!dirtyStagger) { setStaggerSettings(d); rebuildWidgets(); } }
    public void updateMobSettings(GuardPackets.MobSettings data) {
        if (!dirtyMobs) { setMobSettings(data); rebuildWidgets(); }
    }

    public void updateShieldSettings(GuardPackets.ShieldSettings settings) {
        if (!dirtyShield) { setShieldSettings(settings); consumablePriority = settings.consumablePriority(); rebuildWidgets(); }
    }

    @Override protected void init() {
        controlIndex = 0;
        damageSearchBox = null;
        settingsSearchBox = null;
        visibleSettingRows.clear();
        themedButtons.clear(); redButtons.clear();
        buttonTips.clear();
        widgetTips.clear();
        settingLabels.clear();
        dropdownLabels.clear();
        sectionLabels.clear();
        sidebarLeft = 10;
        sidebarWidth = Math.min(142, Math.max(118, width / 4));
        panelLeft = sidebarLeft + sidebarWidth + 20;
        panelWidth = Math.max(96, Math.min(640, width - panelLeft - 25));
        layoutTop = Math.min(14, Math.max(6, height / 16));
        visibleRows = Math.max(1, Math.min(pageRows(), (height - rowTop() - 70) / ROW_STEP));
        firstRow = Math.clamp(firstRow, 0, maxScroll());
        activeClientCategory = switch (page) {
            case 7, 8 -> GuardClientPreset.PARTICLES;
            case 2, 9, 10 -> GuardClientPreset.HUD;
            case 13 -> GuardClientPreset.STAGGER;
            case 3 -> GuardClientPreset.AUDIO;
            case 4 -> GuardClientPreset.ITEMS;
            default -> 0;
        };
        boolean compact = height < 260;
        int modeHeight = compact ? 18 : 22;
        int modeTop = layoutTop + 34;
        int currentSection = experimentalSection ? 3 : enforceSection ? 2 : clientSection ? 0 : 1;
        int modeArrowWidth = 16, gap = 3;
        Button previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> selectSection((currentSection + 3) % 4))
            .bounds(sidebarLeft, modeTop, modeArrowWidth, modeHeight).build());
        Button mode = addRenderableWidget(Button.builder(Component.literal(new String[]{"Client", "Server", "Enforce", "Experimental"}[currentSection]),
            b -> selectSection((currentSection + 1) % 4))
            .bounds(sidebarLeft + modeArrowWidth + gap, modeTop, sidebarWidth - 2 * (modeArrowWidth + gap), modeHeight).build());
        Button next = addRenderableWidget(Button.builder(Component.literal(">"), b -> selectSection((currentSection + 1) % 4))
            .bounds(sidebarLeft + sidebarWidth - modeArrowWidth, modeTop, modeArrowWidth, modeHeight).build());
        if (experimentalSection) redButtons.add(mode);
        tip(previous, "Previous section: Client, Server, Enforce, Experimental.");
        tip(mode, "Client: your presentation settings. Server: shared gameplay. Enforce: client defaults and locks. Experimental: developing mob gameplay, shared by the server. Click to cycle sections.");
        tip(next, "Next section: Client, Server, Enforce, Experimental.");
        EditBox search = addRenderableWidget(new EditBox(font, panelLeft + 10, layoutTop + 2, panelWidth - 20, 20,
            Component.literal("Search Mallard Guard settings")));
        settingsSearchBox = search;
        search.setMaxLength(80);
        search.setHint(Component.literal("Search settings across all sections..."));
        search.setValue(settingsSearch);
        search.setResponder(value -> { settingsSearch = value; settingsSearchChanged = true; });
        tip(search, "Search all settings by name or category. Choose a result to open its page and scroll to it. Clear to return to your current page.");
        if (!settingsSearch.isBlank()) buildSearchResults();
        else if (enforceSection) {
            toggle("Enforce client settings", () -> enforceEnabled, v -> {
                enforceEnabled = v;
                if (!v) enforceParticlesOpen = enforceHudOpen = enforceAudioOpen = enforceItemsOpen = enforceStaggerOpen = false;
                rebuildWidgets();
            }, true, "Apply these starting values to connected clients and lock categories without a Don't Enforce exception.");
            buildEnforcedDefaults();
        } else if (!clientSection) {
            if (page == 0) {
            dropdown("Timing", () -> timingOpen, "Show or hide timing settings.");
            if (timingOpen) {
            toggle("Parrying", () -> parry, v -> parry = v, true,
                "Enable timed parries. Guard defaults to Right Click and can be rebound in Controls under Mallard Guard.");
            slider("Perfect window", perfect, 0, 5, " ticks", 1, true, v -> perfect = v,
                "Ticks at the start of a parry that count as perfect. 0 disables perfect parries.");
            slider("Parry window", window, 1, 10, " ticks", 1, true, v -> window = v,
                "How long a parry can catch a hit, including the perfect window.");
            slider("Recharge", recharge, 1, 60, " ticks", 1, true, v -> recharge = v,
                "Time before another parry can begin after this attempt.");
            slider("Facing angle", angle, 0, 360, "°", 1, true, v -> angle = v,
                "Which directions attacks can come from. 180° covers your front half; 360° covers all directions.");
            slider("Follow-up parries", followUp, 0, 20, " ticks", 1, true, v -> followUp = v,
                "Extra time to catch melee hits after a successful parry. 0 disables follow-ups; follow-up hits do not extend the timer.");
                        }
            }
            if (page == 0) {
            dropdown("Damage Returned", () -> retaliationOpen, "Show or hide retaliation damage settings.");
            if (retaliationOpen) {
            slider("Parry retaliation", parryReturnPercent, 0, 200, "%", 1, true, v -> parryReturnPercent = v,
                "Damage returned on a regular parry, as a percentage of the incoming hit. This damage does not add knockback; attacker pushback has its own slider. 0% disables retaliation; 200% doubles damage.");
            slider("Perfect retaliation", perfectReturnPercent, 0, 200, "%", 1, true, v -> perfectReturnPercent = v,
                "Damage returned on a perfect parry, as a percentage of the incoming hit. This damage does not add knockback; attacker pushback has its own slider. 0% disables retaliation; 200% doubles damage.");
            slider("Retaliation damage cap", retaliationCap, 0, 1000, " damage", 1, true, v -> { retaliationCap = v; dirtyShield = true; },
                "Maximum damage returned to each target after the multiplier. 0 removes the cap. Default: 25 damage.");
                        }
            }
            if (page == 0) {
                dropdown("Parry Pushback", () -> pushbackOpen, "Set how parries push you and the attacker independently. Returned damage adds no extra knockback.");
                if (pushbackOpen) {
                    slider("Defender pushback", knockbackStrength, 0, 200, "%", 1, true, v -> knockbackStrength = v,
                        "How strongly parrying pushes you away from the hit. Shield parries push you at half this strength. 0% disables both.");
                    slider("Shield attacker pushback", shieldRegularPush, 0, 200, "%", 1, true, v -> { shieldRegularPush = v; dirtyShield = true; },
                        "Pushes the attacker away on a regular shield parry. Perfect shield parries use cone pushback instead. 0% disables this pushback.");
                    slider("Weapon attacker pushback", toolPush, 0, 200, "%", 1, true, v -> { toolPush = v; dirtyShield = true; },
                        "Pushes the attacker away on regular and perfect weapon parries. 0% disables this pushback.");
                }
            }
            if (page == 0) {
            dropdown("Projectiles", () -> parryProjectileOpen, "Show or hide projectiles settings.");
            if (parryProjectileOpen) {
            toggle("Projectile parry", () -> parryProjectiles, v -> parryProjectiles = v, true,
                "Let parries deflect projectiles. Regular parries send them in a random direction; perfect parries aim them at the attacker.");
                        }
            }
            if (page == 0) {
            dropdown("Explosions", () -> parryExplosionOpen, "Show or hide explosions settings.");
            if (parryExplosionOpen) {
            toggle("Explosion parry", () -> parryExplosions, v -> { parryExplosions = v; rebuildWidgets(); }, true,
                "Let a timed parry prevent explosion damage.");
            if (parryExplosions) toggle("Explosions: perfect only", () -> perfectExplosionsOnly, v -> perfectExplosionsOnly = v, true,
                "Require perfect timing to parry explosions. Only applies when explosion parrying is enabled.");
                        }
            }
            if (page == 6) buildDamageTypes();
            if (page == 13) {
                toggle("Stagger gauge", () -> staggerEnabled, v -> { staggerEnabled = v; dirtyStagger = true; rebuildWidgets(); }, true, "Track stagger cells centered above the shared status-bar stacks. At zero, the configured stagger duration prevents movement, attacking, and guarding. Only the configured exit-recovery cells return when it ends.");
                dropdown("Stagger Cells", () -> staggerGaugeOpen, "Cells are server-owned. Mob parries remove cells; your perfect parries restore them.");
                if (staggerEnabled && staggerGaugeOpen) {
                    slider("Stagger duration", staggerStunTicks, 0, 60, " ticks", 1, true, v -> { staggerStunTicks = v; dirtyStagger = true; }, "Stun duration at zero cells. 0 disables the stun; otherwise only the configured exit recovery is restored when it ends.");
                    slider("Maximum stagger cells", staggerCells, 1, 10, " cells", 1, true, v -> { staggerCells = v; staggerExitRecovery = Math.min(v, staggerExitRecovery); dirtyStagger = true; rebuildWidgets(); }, "Maximum cells shown in your gauge.");
                    slider("Parry cell loss", staggerParryLoss, 0, 20, " cells", 1, true, v -> { staggerParryLoss = v; dirtyStagger = true; }, "Cells lost when a player or mob regularly parries your attack.");
                    slider("Perfect parry cell loss", staggerPerfectLoss, 0, 20, " cells", 1, true, v -> { staggerPerfectLoss = v; dirtyStagger = true; }, "Cells lost when a player or mob perfectly parries your attack.");
                    slider("Perfect parry cell recovery", staggerRestore, 0, 20, " cells", 1, true, v -> { staggerRestore = v; dirtyStagger = true; }, "Cells restored when you perform a perfect parry.");
                    slider("Recovery after stagger", staggerExitRecovery, 0, staggerCells, " cells", 1, true,
                        v -> { staggerExitRecovery = v; dirtyStagger = true; }, "Cells restored when stagger ends. The maximum follows Maximum stagger cells. Other cells recover over time normally. Default: 2 cells.");
                }
                dropdown("Stagger Recovery", () -> staggerRecoveryOpen, "Recover one cell per interval. Enemy checks happen once per second only while your gauge is depleted.");
                if (staggerEnabled && staggerRecoveryOpen) {
                    slider("Recovery near enemies", staggerNearTicks, 0, 1200, " ticks", 1, true, v -> { staggerNearTicks = v; dirtyStagger = true; }, "Ticks to recover one cell near hostile mobs or mobs targeting you. 0 disables recovery.");
                    slider("Recovery away from enemies", staggerFarTicks, 0, 1200, " ticks", 1, true, v -> { staggerFarTicks = v; dirtyStagger = true; }, "Ticks to recover one cell with no nearby enemies. 0 disables recovery.");
                    slider("Stagger enemy radius", staggerRadius, 1, 32, " blocks", 1, true, v -> { staggerRadius = v; dirtyStagger = true; }, "Radius used to decide whether recovery is fast or slow.");
                }
            }
            if (page == 12) {
                toggle("Mob guarding", () -> mobGuard, v -> { mobGuard = v; dirtyMobs = true; rebuildWidgets(); }, true,
                    "Allow supported goal-driven humanoids holding a valid weapon or shield to guard combat hits. Vanilla zombie, skeleton, and vindicator families are supported automatically. Bow and crossbow holders never guard. Their damage-type and pushback rules follow the server. Empty hands do not qualify.");
                dropdown("Guard Behavior", () -> mobDefenseOpen, "Mobs can guard on a close approach, randomly hold a tactical stance, or react to landed hits. During guarding, this goal temporarily takes priority over normal movement and attacks. A successful parry can lead to another stance or a short counter approach.");
                if (mobDefenseOpen && mobGuard) {
                    slider("Mob guard difficulty", mobDifficulty, 0, 100, "%", 1, true, v -> { mobDifficulty = v; dirtyMobs = true; }, "Scales all three guard attempt chances. At 100%, hit pressure starts at 40% after Hits before guard, rising with additional landed hits. Close-approach and tactical chances use their own settings. Pressure resets after a stance or 10 seconds without damage. 0 prevents new attempts.");
                    toggle("Mob blocking", () -> mobBlocking, v -> { mobBlocking = v; dirtyMobs = true; }, false, "Keep guarding for 20 blocking ticks after each parry window. Shields block fully; weapons use the server damage reduction and block limit. Off: the mob lowers its guard when its parry window ends.");
                    toggle("Mob retaliation", () -> mobRetaliation, v -> { mobRetaliation = v; dirtyMobs = true; }, true, "Use server regular/perfect retaliation multipliers and cap for mob damage return. Shield perfect parries use shield retaliation; shield regular parries return no damage. Stagger loss is independent.");
                    slider("Hits before guard", mobHitsBeforeGuard, 1, 3, " hits", 1, true, v -> { mobHitsBeforeGuard = v; dirtyMobs = true; }, "Landed damaging combat hits before a pressure-based guard attempt. At the default 1, the first hit may trigger a stance for the next attack. This does not limit approach or tactical attempts. Cancelled hits and hits that cause no health damage do not count.");
                }
                if (mobDefenseOpen && mobGuard) {
                    slider("Counter approach duration", mobCounterTicks, 0, 40, " ticks", 1, true,
                        v -> { mobCounterTicks = v; dirtyMobs = true; }, "Maximum fast approach after a successful mob parry. The mob may approach straight or with a sideways step, then attempts one melee hit within reach and line of sight. 0 disables the approach; another stance can still follow. Default: 12 ticks.");
                    slider("Approach guard chance", mobApproachChance, 0, 100, "%", 1, true,
                        v -> { mobApproachChance = v; dirtyMobs = true; }, "One chance on the first close approach inside a random distance between 1 block and Approach guard distance. The mob raises guard while continuing forward. Scaled by Mob guard difficulty. Default: 100%.");
                    slider("Tactical guard chance", mobTacticalChance, 0, 100, "%", 1, true,
                        v -> { mobTacticalChance = v; dirtyMobs = true; }, "Chance per scheduled decision, every 2–4 seconds while engaged and ready. Starts a longer guarding sequence with one randomly chosen movement: strafe or stand still. Scaled by Mob guard difficulty. Default: 100%.");
                    slider("Tactical stance maximum", mobTacticalSeconds, 1, 10, " seconds", 1, true,
                        v -> { mobTacticalSeconds = v; dirtyMobs = true; }, "Sequence duration is randomly chosen from 1 second through this maximum. The mob either stands still or strafes and lowers its guard for 5–10 ticks between parry windows. A successful parry ends the sequence early. Default: 5 seconds.");
                    slider("Tactical guard cooldown", mobTacticalCooldownTicks, 20, 1200, " ticks", 1, true,
                        v -> { mobTacticalCooldownTicks = v; dirtyMobs = true; }, "Minimum time after the originally scheduled end of a tactical sequence before another tactical attempt. Approach and hit-pressure attempts have their own recharge. Default: 200 ticks (10 seconds).");
                }
                if (mobDefenseOpen && mobGuard) {
                    slider("Mob guard movement", mobMovement, 0, 100, "%", 1, true, v -> { mobMovement = v; dirtyMobs = true; }, "Movement speed retained during mob guarding, independent of player slowdown. 100% has no guard slowdown; 0% prevents deliberate movement. Default: 100%.");
                    slider("Approach guard distance", mobApproachDistance, 1, 5, " blocks", 1, true, v -> { mobApproachDistance = v; dirtyMobs = true; }, "Each encounter chooses a trigger distance randomly from 1 block through this maximum. Default: 2 blocks.");
                    slider("Approach stance maximum", mobApproachSeconds, 1, 10, " seconds", 1, true, v -> { mobApproachSeconds = v; dirtyMobs = true; }, "Each approach stance lasts randomly from 1 second through this maximum. The mob keeps approaching and raises guard again after short vulnerable gaps as needed. Default: 2 seconds.");
                    slider("Rush after parry chance", mobRushChance, 0, 100, "%", 1, true, v -> { mobRushChance = v; dirtyMobs = true; }, "Chance to rush you immediately after parrying your attack. Other outcomes lower guard or raise one follow-up stance. Counter approach duration limits the rush. Default: 70%.");
                }
                dropdown("Mob Parry Timing", () -> mobTimingOpen, "Mob-only windows. Player weapon and shield timing stays unchanged. The regular duration follows the perfect duration; both apply to mob weapons and shields.");
                if (mobTimingOpen && mobGuard) {
                    slider("Mob perfect parry window", mobPerfectTicks, 0, 20, " ticks", 1, true, v -> { mobPerfectTicks = v; dirtyMobs = true; }, "Perfect timing at the start of a mob guard stance, for weapons and shields. 0 disables perfect parries.");
                    slider("Mob regular parry window", mobParryTicks, 1, 60, " ticks", 1, true, v -> { mobParryTicks = v; dirtyMobs = true; }, "Regular parry duration AFTER the mob perfect window. At the defaults, the mob has 8 perfect ticks then 20 regular ticks: 28 ticks total.");
                }
                dropdown("Mob Colors", () -> mobAppearanceOpen, "Colors of mob particle flashes and streak tips. Local mob-parry and guard-break shield reactions use the red shield artwork.");
                if (mobAppearanceOpen && mobGuard) {
                    editBox("Mob parry color", String.format("#%06X", mobParryColor), v -> { Integer color = parseColor(v); if (color != null) { mobParryColor = color; dirtyMobs = true; } }, "Hex RGB color of mob regular-parry particle flashes and streak tips. Default: #FF5555.");
                    editBox("Mob perfect parry color", String.format("#%06X", mobPerfectColor), v -> { Integer color = parseColor(v); if (color != null) { mobPerfectColor = color; dirtyMobs = true; } }, "Hex RGB color of mob perfect-parry particle flashes and streak tips. This does not recolor the red shield reaction artwork. Default: #FF3344.");
                }
                dropdown("Spawn Equipment", () -> mobEquipmentOpen, "Fresh vanilla zombies, skeletons, pillagers, and vindicators can receive balanced gear. Melee tools use stone, iron, gold, or rarely diamond. Melee loadouts may have shields; ranged loadouts never do. Existing modded gear is preserved.");
                if (mobEquipmentOpen && mobGuard) {
                    slider("Mob gear chance", mobGearChance, 0, 100, "%", 1, true, v -> { mobGearChance = v; dirtyMobs = true; }, "Chance for a fresh eligible mob to receive a generated loadout. Melee choices are sword, axe, or pickaxe. Skeletons can retain a bow; pillagers retain crossbows and normal ranged AI. Tiers and shield rolls are internally balanced. Zombies and skeletons still get a helmet when their head slot is empty. Default: 50%.");
                }
                dropdown("Mob Eligibility", () -> mobEligibilityOpen, "Automatic support is limited to vanilla zombie and skeleton families and vindicators. Bosses with recognized boss tags are excluded. Add other goal-driven humanoids only after checking their AI and model compatibility.");
                if (mobEligibilityOpen && mobGuard) {
                    editBox("Additional humanoid mob IDs", mobHumanoidIds, v -> { mobHumanoidIds = v; dirtyMobs = true; }, "Comma-separated entity IDs, such as modid:entity. Only add humanoids using ordinary movement and attack goals. Their normal goals yield temporarily while guarding. Custom AI and animations outside the goal system need an integration. Known or boss-tagged entities are excluded; untagged mod bosses cannot be reliably identified. Default: empty.");
                }
            }
            if (page == 11) {
                toggle("Shield cone sparks", () -> coneSparks, v -> { coneSparks = v; dirtyShield = true; }, true,
                    "Off: shield sparks and stars spray in a sphere. On: a 90-degree fan points toward the attack. Streak Lines stay spherical. This controls particles, not the bash damage cone. Default: Off.");
                dropdown("Shield Timing", () -> shieldTimingOpen, "Show shield parry timing and guard break settings.");
                if (shieldTimingOpen) {
                slider("Shield perfect window", shieldPerfect, 0, 3, " ticks", 1, true, v -> {
                    shieldPerfect = Math.min(v, shieldWindow); dirtyShield = true;
                }, "Perfect parry timing for shields. It must fit within the total shield parry window. 0 disables shield perfect parries.");
                slider("Shield parry window", shieldWindow, 1, 10, " ticks", 1, true, v -> {
                    shieldWindow = v; shieldPerfect = Math.min(shieldPerfect, v); dirtyShield = true;
                }, "Total shield parry time, including its perfect window. Lowering this also limits perfect parry timing. Shield use continues into blocking until you release guard or guard breaks.");
                slider("Shield parry recharge", shieldRechargeTicks, 1, 60, " ticks", 1, true, v -> { shieldRechargeTicks = v; dirtyShield = true; },
                    "Time before another shield parry after releasing guard. Regular shield parries halve it; perfect shield parries reset it. Separate from the guard break penalty.");
                slider("Shield blocks before break", shieldMaxBlocks, 0, 10, "", 1, true, v -> { shieldMaxBlocks = v; dirtyShield = true; },
                    "Successful shield blocks allowed before the guard ends. 0 allows unlimited blocks. A disabled shield still follows vanilla's cooldown.");
                }
                dropdown("Shield Perfect Parry", () -> shieldPerfectOpen, "Show shield perfect parry cone effects.");
                if (shieldPerfectOpen) {
                slider("Cone half-angle", shieldCone, 0, 90, "°", 1, true, v -> { shieldCone = v; dirtyShield = true; },
                    "Angle to each side of where you look. 90° covers the whole front half (180° total); 0° disables cone retaliation, stun and pushback.");
                slider("Cone reach", shieldReach, 1, 10, " blocks", 1, true, v -> { shieldReach = v; dirtyShield = true; },
                    "Maximum distance between your eyes and a target's eyes for perfect shield parry cone effects.");
                slider("Shield retaliation", shieldReturn, 0, 30, "%", 1, true, v -> { shieldReturn = v; dirtyShield = true; },
                    "Percentage of the parried hit dealt to each target in the cone. 0% disables damage but allows configured stun and pushback.");
                slider("Stun duration", shieldStun, 0, 100, " ticks", 1, true, v -> { shieldStun = v; dirtyShield = true; },
                    "Prevents targets in the cone from moving voluntarily or attacking after pushback. 20 ticks = one second; 0 disables stun. Bosses are immune unless listed under Additional Shields and Bosses.");
                slider("Cone pushback", shieldPerfectPush, 0, 200, "%", 1, true, v -> { shieldPerfectPush = v; dirtyShield = true; },
                    "Pushes targets in the perfect parry cone away. 0% disables pushback.");
                }
                dropdown("Additional Shields and Bosses", () -> shieldIdsOpen, "Show extra shield item IDs and bosses eligible for stun.");
                if (shieldIdsOpen) {
                editBox("Extra shields and tags", shieldItems, v -> { shieldItems = v; invalidItemRules = false; dirtyShield = true; },
                    "Comma-separated modid:itemid or #modid:tag for items to treat as shields if their mod has no shield tag or block animation. Default: Empty.");
                editBox("Stunnable bosses", stunnableBosses, v -> { stunnableBosses = v; invalidItemRules = false; dirtyShield = true; },
                    "Comma-separated entity IDs or #entity tags. Listed bosses may be stunned by a shield perfect parry. Other bosses stay immune. Default: Empty.");
                }
            }
            if (page == 5) {
            toggle("Perfect fall parry", () -> fallPerfectParry, v -> fallPerfectParry = v, true,
                "On: falls and wall collisions caught during the perfect window count as perfect parries. Off: they count as regular parries even in that window.");
            slider("Fall blast", fallBlastStrength, 0, 200, "%", 1, true, v -> fallBlastStrength = v,
                "Size of the blast after parrying a fall of more than ten blocks. 0% disables the blast.");
            slider("Fall launch", fallLaunchPower, 0, 200, "%", 1, true, v -> fallLaunchPower = v,
                "Fall parry launch strength. With Look down enabled, carries current movement; otherwise launches where you look. Perfect parries launch farther. 0% prevents damage without launching.");
            toggle("Fall blast breaks blocks", () -> fallBreakBlocks, v -> fallBreakBlocks = v, true,
                "Allow the blast after a long fall parry to break terrain.");
            toggle("Look down to parry", () -> fallLookDown, v -> fallLookDown = v, true,
                "On: fall parry requires looking at least 40° down; standing still bounces in place and movement keeps its momentum. Off: parry falls while looking anywhere and lunge toward where you look.");
            }
            if (page == 1) {
            dropdown("Guard Stance", () -> guardOpen, "Show or hide blocking stance and movement settings.");
            if (guardOpen) {
            toggle("Blocking", () -> block, v -> { block = v; rebuildWidgets(); }, true,
                "Keep guarding when you hold the Guard key past the parry window. Guard defaults to Right Click and can be rebound in Controls.");
            if (block) slider("Damage reduction", reductionPercent, 0, 100, "%", 1, true, v -> reductionPercent = v,
                "Damage prevented by held block. 50% halves the damage; 100% prevents it.");
            if (block) slider("Weapon blocks before break", toolMaxBlocks, 0, 5, "", 1, true, v -> { toolMaxBlocks = v; dirtyShield = true; },
                "Successful weapon or empty-hand blocks before guard ends. 0 allows unlimited blocks. Attacking with your weapon remains available after guard breaks.");
            if (block) slider("Guard break recharge", shieldBreakTicks, recharge, 200, " ticks", 1, true, v -> { shieldBreakTicks = v; dirtyShield = true; },
                "Wait after the last allowed shield or weapon block before guarding again. A broken shield also receives an item cooldown. Minimum matches normal recharge; max 10 seconds.");
            slider("Guard movement", guardMovementPercent, 0, 100, "%", 1, true, v -> guardMovementPercent = v,
                "Movement speed while guarding, including parry and block. 100% means normal movement speed.");
                        }
            }
            if (page == 1) {
            if (block) {
            dropdown("Projectiles", () -> blockProjectileOpen, "Show or hide projectiles settings.");
            if (blockProjectileOpen) {
            if (block) slider("Block deflect chance", blockDeflectChance, 0, 100, "%", 1, true, v -> blockDeflectChance = v,
                "Chance that held block sends any incoming projectile in a random direction. 0% disables deflection.");
            if (block) toggle("Block projectiles", () -> blockProjectiles, v -> blockProjectiles = v, true,
                "Let held block reduce damage from projectiles that are not deflected.");
                        }
            }
            }
            if (page == 1) {
            if (block) {
            dropdown("Explosions", () -> blockExplosionOpen, "Show or hide explosions settings.");
            if (blockExplosionOpen) {
            if (block) toggle("Block explosions", () -> blockExplosions, v -> blockExplosions = v, true,
                "Let held block reduce explosion damage using your damage reduction setting.");
                        }
            }
            }
            if (page == 2) {
            toggle("Sparks and flashes", () -> hitParticles, v -> { hitParticles = v; rebuildWidgets(); }, true,
                "Allow nearby players to see parry sparks and flashes. Blocks do not create particles.");
            toggle("Hit sounds", () -> serverHitSounds, v -> serverHitSounds = v, true,
                "Allow parry and block sounds to play for nearby players. Everyone controls their own playback volume.");
            }
            if (page == 4) {
            dropdown("Eligibility", () -> eligibilityOpen, "Show or hide eligibility settings.");
            if (eligibilityOpen) {
                toggle("Random dual tools", () -> randomDualTools, v -> { randomDualTools = v; dirtyShield = true; }, true,
                    "With two eligible non-shield items, choose a random actual guard hand each stance, with at most two consecutive selections of the same hand while standing. Crouching always uses the eligible offhand. Off follows the client hand preference while standing. Timing and durability follow that item.");
                toggle("Random shield or weapon", () -> randomShieldWeapon, v -> { randomShieldWeapon = v; dirtyShield = true; }, true,
                "With a shield and an eligible weapon, randomly choose the actual guarding item for each new stance. Its timing, durability and defense rules apply. Off: crouching uses the shield and standing uses the weapon. On: random selection overrides crouching. Default: Off.");
            toggle("Any held item", () -> allowAnyItem, v -> { allowAnyItem = v; rebuildWidgets(); }, true,
                "Off: recognized weapons and shields plus Extra allowed items. On: any held item except Excluded items. Empty hands have a separate switch. Guarding always requires a fully recharged vanilla attack cooldown.");
            toggle("Empty-hand guard", () -> allowEmptyHand, v -> { allowEmptyHand = v; dirtyShield = true; }, true,
                "Allow guarding without an item in either hand. Independent of the Any held item switch. Default: Off.");
            toggle("Respect item cooldown", () -> cooldownPreventsGuard, v -> { cooldownPreventsGuard = v; dirtyShield = true; }, true,
                "On: an item on cooldown cannot start or continue guarding. Another eligible hand can still guard. Shields always obey their vanilla disable cooldown.");
            toggle("Consumables take priority", () -> consumablePriority, v -> { consumablePriority = v; dirtyShield = true; }, true,
                "When food or a drink is held in either hand, using it takes priority over guard, even with a weapon in the other hand.");
            if (!allowAnyItem) toggle("Allow usable weapons", () -> allowUsableItems, v -> allowUsableItems = v, true,
                "Allow recognized weapons with their own use action to guard in recognized-weapon mode. Extra allowed items can still allow them when this is off.");
                        }
            }
            if (page == 4) {
            dropdown("Durability", () -> durabilityOpen, "Show or hide durability settings.");
            if (durabilityOpen) {
            slider("Parry durability", parryWear, 0, 100, "%", 10, true, v -> parryWear = v,
                "Wear per regular or follow-up parry as tenths of a percent of the item's own durability, rounded up. 0% disables wear. Items without durability are unaffected.");
            slider("Perfect durability", perfectWear, 0, 100, "%", 10, true, v -> perfectWear = v,
                "Wear per perfect parry as tenths of a percent of the item's own durability, rounded up. 0% disables wear. Items without durability are unaffected.");
            slider("Block durability", blockWear, 0, 100, "%", 10, true, v -> blockWear = v,
                "Wear per blocked hit as tenths of a percent of the item's own durability, rounded up. 0% disables wear. Shields use their normal block wear.");
                        }
            }
            if (page == 4) {
            dropdown("Item Lists", () -> itemListsOpen, "Show or hide item lists settings.");
            if (itemListsOpen) {
            if (!allowAnyItem) editBox("Extra allowed items", includedItems, v -> { includedItems = v; invalidItemRules = false; },
                "Comma-separated item IDs or #item tags added to recognized weapons and shields. Example: minecraft:stick, #minecraft:swords. Default: Empty.");
            else editBox("Excluded items", excludedItems, v -> { excludedItems = v; invalidItemRules = false; },
                "Comma-separated item IDs or #item tags that cannot guard in Any held item mode. Empty hands use their own switch. Default: Empty.");
                        }
            }
        } else {
            if (page == 7) {
            dropdown("Spark Effects", () -> sparkOpen, "Show or hide spark effects settings.");
            if (sparkOpen) {
                toggle("Sparks", () -> sparksEnabled, v -> { sparksEnabled = v; rebuildWidgets(); }, false,
                    "Show bouncing flying sparks and perfect-parry stars. Streak Lines have separate controls. Turning this off preserves these settings. Default: On.");
                if (sparksEnabled && hitParticles) {
                    toggle("Ring sparks", () -> sparkRing, v -> sparkRing = v, false,
                        "Off: a rounded spherical spark spray. On: sparks form a compact X. Fall sparks always use the X; shield sparks use their own sphere/cone setting. Streak Lines keep their own pattern.");
                    slider("Parry sparks", regularSparks, 0, 200, "%", 1, false, v -> regularSparks = v,
                        "How many flying sparks regular parries show. 0% hides them. ");
                    slider("Perfect sparks", perfectSparks, 0, 200, "%", 1, false, v -> perfectSparks = v,
                        "Perfect-parry flying spark count on the same percentage scale as regular parries. 0% hides them. ");
                    slider("Perfect stars", stars, 0, 16, "", 1, false, v -> stars = v,
                        "Number of star-shaped sparks on perfect parries. 0 hides them. ");
                    slider("Spark lifetime", sparkLifetime, 0, 200, "%", 1, false, v -> sparkLifetime = v,
                        "Shared flight time for bouncing sparks and perfect-parry stars. 100% is 30 ticks; 0% hides them. Streak Lines have their own lifetime. ");
                    slider("Spark length", sparkLength, 0, 200, "%", 1, false, v -> sparkLength = v,
                        "Length of each moving spark trail relative to its full base length. 0% hides trails; stars use their own sprite. ");
                    slider("Spark speed", sparkExplosiveness, 0, 200, "%", 1, false, v -> sparkExplosiveness = v,
                        "How fast sparks and stars launch from the parry impact. Spark reach sets when gravity begins pulling them down. ");
                    slider("Spark reach", sparkReach, 0, 200, "%", 1, false, v -> sparkReach = v,
                        "How far sparks and stars travel before gravity starts pulling them down. 0% drops immediately; 10% is 0.15 blocks and 100% is 1.5 blocks. Does not change launch speed or angle. ");
                }
                        }
            }
            if (page == 7) {
            dropdown("Streak Lines", () -> streakOpen, "Show or hide Streak Lines settings.");
            if (streakOpen) {
                toggle("Reverse streak gradient", () -> extraClient[62] != 0, v -> extraClient[62] = v ? 1 : 0, false, "Reverse the existing gradient: orange/yellow or mob red at the center, white at the tips. Keeps the color proportions. Default: Off.");
                toggle("Streak Lines", () -> streaksEnabled, v -> { streaksEnabled = v; rebuildWidgets(); }, false,
                    "White square dots form anchored lines that stretch from the impact center. They shrink and fade from center to tip. Default: On.");
                if (streaksEnabled && hitParticles) {
                    slider("Regular streak count", regularStreakAmount, 0, 200, "%", 1, false, v -> regularStreakAmount = v,
                        "Number of Streak Lines on regular parries. 0% hides them. ");
                    slider("Perfect streak count", streakAmount, 0, 200, "%", 1, false, v -> streakAmount = v,
                        "Number of Streak Lines on perfect parries. 0% hides them. ");
                    slider("Streak length", streakLength, 0, 200, "%", 1, false, v -> streakLength = v,
                        "Maximum outward reach of each Streak Line. 100% uses a 3.5–6.75 block range; 0% hides Streak Lines. ");
                    slider("Streak lifetime", streakLifetime, 2, 40, " ticks", 1, false, v -> streakLifetime = v,
                        "How long each Streak Line lasts before the center-to-tip fade finishes. ");
                    slider("Streak dot size", streakDotSize, 0, 200, "%", 1, false, v -> streakDotSize = v,
                        "Square dot size relative to the base dot size. 0% hides Streak Lines. ");
                    slider("Regular dot spacing", regularStreakDotSpacing, 1, 200, "%", 1, false, v -> regularStreakDotSpacing = v,
                        "Distance between dots on regular parries. Lower values pack dots closer, down to 1%. Regular and perfect parries now use the same spacing scale. ");
                    slider("Perfect dot spacing", streakDotSpacing, 1, 200, "%", 1, false, v -> streakDotSpacing = v,
                        "Distance between dots on perfect parries. Lower values pack dots closer, down to 1%. Regular and perfect parries now use the same spacing scale. ");
                    slider("Streak ballistic curve", streakBallisticCurve, 0, 200, "%", 1, false, v -> streakBallisticCurve = v,
                        "Curves Streak Lines downward along a ballistic arc. The origin stays fixed and dots still glide along blocks. 0% keeps lines straight; higher values bend the tip down more. ");
                    slider("Streak stretch speed", streakStretchSpeed, 0, 200, "%", 1, false, v -> streakStretchSpeed = v,
                        "How fast the line extends from the center. 100% uses the base speed; 200% doubles it. 0% hides Streak Lines. ");
                }
            }
            }
            if (page == 7) {
            dropdown("Spherical Flash", () -> sphericalFlashOpen, "Show or hide spherical flash settings.");
            if (sphericalFlashOpen) {
                toggle("Spherical flash", () -> particleFlashesEnabled, v -> { particleFlashesEnabled = v; rebuildWidgets(); }, false,
                    "Show bright spherical impact particles. Turning this off preserves size, opacity, and lifetime settings. Default: On.");
                if (particleFlashesEnabled && hitParticles) {
                    slider("Parry flash size", regularOrbSize, 0, 200, "%", 1, false, v -> regularOrbSize = v,
                        "Regular spherical flash size relative to the full-size flash, using the same scale as perfect flashes. 0% hides this flash but keeps the sparks.");
                    slider("Perfect flash size", perfectOrbSize, 0, 200, "%", 1, false, v -> perfectOrbSize = v,
                        "Perfect spherical flash size relative to the full-size flash, using the same scale as regular flashes. 0% hides this flash but keeps the sparks.");
                    slider("Parry flash opacity", regularOrbOpacity, 0, 100, "%", 1, false, v -> regularOrbOpacity = v,
                        "How visible the regular parry flash is. 0% hides it.");
                    slider("Perfect flash opacity", perfectOrbOpacity, 0, 100, "%", 1, false, v -> perfectOrbOpacity = v,
                        "How visible the perfect parry flash is. 0% hides it.");
                    slider("Parry sphere lifetime", regularOrbLifetime, 1, 12, " ticks", 1, false, v -> regularOrbLifetime = v,
                        "Lifetime of the regular parry spherical flash. ");
                    slider("Perfect sphere lifetime", perfectOrbLifetime, 1, 12, " ticks", 1, false, v -> perfectOrbLifetime = v,
                        "Lifetime of the perfect parry spherical flash, independently of regular parry. ");
                }
                        }
            }
            if (page == 2) {
            dropdown("Shield Icon", () -> shieldOpen, "Show or hide shield icon settings.");
            if (shieldOpen) {
                toggle("Shield icon", () -> hud, v -> { hud = v; rebuildWidgets(); }, false,
                    "Show the shield by your crosshair while guarding and recharging.");
                if (hud) toggle("Shield reactions", () -> shieldEffects, v -> shieldEffects = v, false,
                    "Glow when ready; expand on parry, with a larger echo on perfect. Weapon and shield blocks share a dark trembling icon and small echoes. Only a guard-breaking block flashes red. Block hits also show a rounded vignette.");
                        }
            }
            if (page == 2) {
            dropdown("Screen Flash", () -> screenFlashOpen, "Show or hide screen flash settings.");
            if (screenFlashOpen) {
            slider("Screen flash", flashStrength, 0, 100, "%", 1, false, v -> flashStrength = v,
                "Strength of the ordinary white screen flash. 0% disables it; meme flash still uses a full white flash. Blocks never trigger it. ");
            toggle("Perfect only flash", () -> perfectOnlyFlash, v -> perfectOnlyFlash = v, false,
                "Show the ordinary white screen flash only on perfect parries. Meme flash still flashes on either parry. Default: On.");
            toggle("Meme flash", () -> memeFlash, v -> memeFlash = v, false,
                "Show one of four full-screen images on parries. Its white flash always plays at full strength after hitlag, even when ordinary screen flash is off. The image holds 0.5 seconds and fades over 0.2 seconds. Default: Off.");
                        }
            }
            if (page == 2) {
            dropdown("Camera Feedback", () -> cameraFeedbackOpen, "Show or hide camera feedback settings.");
            if (cameraFeedbackOpen) {
                slider("Camera shake", shakeStrength, 0, 100, "%", 1, false, v -> shakeStrength = v,
                    "Strength of camera shake after a hit. Blocking shakes the screen the most. 0% disables it.");
                slider("Hitlag frames", hitlagFrames, 0, 10, " frames", 1, false, v -> hitlagFrames = v,
                    "Freeze the captured screen for this many 60 FPS equivalent frames. The single impact image uses the same hold time; 0 gives it a one-frame minimum. Sound and local particles follow both. With hitlag enabled, perfect retaliation damage also waits for completion; the server applies it after the completion signal arrives. ");
                toggle("Perfect only hitlag", () -> perfectOnlyHitlag, v -> perfectOnlyHitlag = v, false,
                    "Only perfect parries freeze your screen when hitlag frames is above zero. The impact image has a separate Perfect only setting. Default: On.");
                toggle("Impact frame", () -> impactFrames > 0, v -> { impactFrames = v ? 1 : 0; rebuildWidgets(); }, false,
                    "Show one freshly captured grayscale image after hitlag. It lasts as long as the configured hitlag, or one frame if hitlag is zero. Default: On.");
                if (impactFrames > 0) {
                    toggle("Perfect only impact", () -> impactPerfectOnly, v -> impactPerfectOnly = v, false,
                        "Show the impact image only on perfect parries. Off lets regular parries show it too. Default: On.");
                    int previewRow = controlIndex++;
                    if (visible(previewRow)) {
                        settingLabel(previewRow, "Live preview", "Tune brightness, contrast, edges, and grain over a captured view. Return to settings and press Apply to save.", true);
                        Button preview = addRenderableWidget(Button.builder(Component.literal("Preview"), b ->
                            openImpactPreview(false)).bounds(settingX(), row(previewRow), settingWidth(), 20).build());
                        tip(preview, "Open the impact frame preview. Changes return here and save only when you press Apply.");
                    }
                }
                        }
            }
            if (page == 2) {
                dropdown("Hand Animation", () -> animationOpen, "Show or hide first-person and third-person guard pose settings.");
                if (animationOpen) {
                    toggle("Third-person guard pose", () -> thirdPersonAnimation, v -> thirdPersonAnimation = v, false,
                        "Show the simple block pose on yourself and all other guarding players and humanoid mobs. Off hides this pose for everyone on your client; guard mechanics are unaffected.");
                    toggle("First-person guard pose", () -> firstPersonAnimation,
                    v -> firstPersonAnimation = v, false,
                    "Animate the selected guarding hand. Shields retain their normal use animation. Disable if another mod animates your hands.");
                }
            }
            if (page == 3) {
            slider("Master volume", masterVolume, 0, 200, "%", 1, false, v -> masterVolume = v,
                "Your playback level for all Mallard Guard sounds, including the meme clip. 100% is the recorded level before other volume settings; 0% mutes them. Above 100% uses louder recordings where available. Only affects what you hear.");
            slider("Perfect volume", perfectVolume, 0, 200, "%", 1, false, v -> perfectVolume = v,
                "Your perfect parry and meme clip volume, multiplied by your master volume. Above 100% can use a louder mix when combined gain exceeds normal playback.");
            slider("Parry volume", parryVolume, 0, 200, "%", 1, false, v -> parryVolume = v,
                "Your regular parry and meme clip volume, multiplied by your master volume. Above 100% can use a louder mix when combined gain exceeds normal playback.");
            slider("Block volume", blockVolume, 0, 200, "%", 1, false, v -> blockVolume = v,
                "Your blocking and shield parry volume, multiplied by your master volume. Above 100% can use a louder mix when combined gain exceeds normal playback.");
            }
            if (page == 4) {
            toggle("Offhand guard priority", () -> preferOffhand, v -> preferOffhand = v, false,
                "When both hands can guard, prefer the offhand for timing, defense and durability. Random dual-tool selection and crouch-to-offhand rules take priority. Paired shield/weapon rules also take priority. With a shield and another eligible weapon, crouching selects the shield and standing selects the weapon, unless Random shield or weapon is enabled.");
            }
        }

        if (clientSection && page == 2) {
            dropdown("Chromatic Aberration", () -> chromaticOpen, "Animated color separation after hitlag and impact finish. Both parry types use it by default; the impact preview controls a separate static effect.");
            if (chromaticOpen) {
                slider("Chromatic intensity", extraClient[58], 0, 200, "%", 1, false, v -> extraClient[58] = v, "Strength of animated RGB separation. 0 disables it. Default: 200%.");
                slider("Chromatic duration", extraClient[59], 1, 40, " ticks", 1, false, v -> extraClient[59] = v, "How long color separation expands and settles. Duration also controls animation speed. Default: 10 ticks.");
                toggle("Perfect only chromatic", () -> extraClient[60] != 0, v -> extraClient[60] = v ? 1 : 0, false, "On: animate only perfect parries. Off: regular and perfect parries both trigger it. Default: Off.");
                toggle("Chromatic affects HUD", () -> extraClient[61] != 0, v -> extraClient[61] = v ? 1 : 0, false, "On processes the HUD too. Off processes the world before HUD drawing. Config screens stay unaffected. Default: On.");
            }

        }

        if (experimentalSection && page == 13) {
            dropdown("Stagger Display", () -> staggerDisplayOpen, "The server owns the cells; you choose their presentation. The row centers above the highest reported status-bar stack and reserves its space.");
            if (staggerDisplayOpen) {
                toggle("Compact stagger icon", () -> extraClient[63] != 0, v -> extraClient[63] = v ? 1 : 0, false, "Show an 8x8 icon and the remaining-cell count (0–10), instead of a row. Default: On.");
                slider("Stagger vertical offset", extraClient[64], 0, 100, " px", 1, false, v -> extraClient[64] = v, "Extra upward space above the shared HUD stacks. Automatic placement follows mods that report their occupied heights. Default: 1 pixel.");
            }
        }

        if (!enforceSection) {
            String[] tabs = clientSection ? CLIENT_TABS : experimentalSection ? EXPERIMENTAL_TABS : SERVER_TABS;
            int[] pages = clientSection ? CLIENT_PAGES : experimentalSection ? EXPERIMENTAL_PAGES : SERVER_PAGES;
            int shown = visibleSidebarTabs(tabs.length);
            sidebarOffset = Math.clamp(sidebarOffset, 0, tabs.length - shown);
            for (int i = sidebarOffset; i < Math.min(tabs.length, sidebarOffset + shown); i++) {
                final int selected = pages[i];
                int buttonY = sidebarTop() + (i - sidebarOffset) * 23;
                String shortName = tabs[i].equals("Hand Selection") ? "Hands" : tabs[i].equals("Shared Effects") ? "Effects"
                    : tabs[i].equals("Damage Types") ? "Damage" : tabs[i];
                Button tab = addRenderableWidget(Button.builder(Component.literal(shortName), b -> {
                    settingsSearch = "";
                    rememberLocation();
                    page = selected;
                    firstRow = 0;
                    selectRememberedGroup();
                    rememberLocation();
                    rebuildWidgets();
                }).bounds(sidebarLeft + 8, buttonY, sidebarWidth - 16, 20).build());
                tab.active = page != selected;
                tip(tab, "Open " + tabs[i] + " settings.");
            }
            if (shown < tabs.length) {
                int navigationY = sidebarTop() + shown * 23 + 2;
                int arrowWidth = (sidebarWidth - 21) / 2;
                Button up = addRenderableWidget(Button.builder(Component.literal("▲"), b -> { sidebarOffset--; rebuildWidgets(); })
                    .bounds(sidebarLeft + 8, navigationY, arrowWidth, 16).build());
                Button down = addRenderableWidget(Button.builder(Component.literal("▼"), b -> { sidebarOffset++; rebuildWidgets(); })
                    .bounds(sidebarLeft + 13 + arrowWidth, navigationY, arrowWidth, 16).build());
                up.active = sidebarOffset > 0;
                down.active = sidebarOffset + shown < tabs.length;
                tip(up, "Previous categories"); tip(down, "More categories");
            }
        }

        int actionWidth = (panelWidth - 20) / 3;
        Button resetPage = addRenderableWidget(Button.builder(Component.literal("Reset Page"), b -> reset(false))
            .bounds(panelLeft, height - 56, (panelWidth - 10) / 2, 20).build());
        Button resetSection = addRenderableWidget(Button.builder(Component.literal("Reset All"), b -> reset(true))
            .bounds(panelLeft + (panelWidth - 10) / 2 + 10, height - 56, (panelWidth - 10) / 2, 20).build());
        resetPage.active = experimentalSection && page == 13 ? operator || !GuardClient.clientCategoryLocked(GuardClientPreset.STAGGER) : clientSection ? !GuardClient.clientCategoryLocked(activeClientCategory) : operator;
        resetSection.active = clientSection || experimentalSection && !GuardClient.clientCategoryLocked(GuardClientPreset.STAGGER) || operator;
        tip(resetPage, "Restore the defaults for this page. Press Apply to save them.");
        tip(resetSection, "Restore all defaults in the current Client, Server, Enforce, or Experimental section. Locked client categories stay locked. Press Apply to save.");
        Button saveClose = addRenderableWidget(Button.builder(Component.literal(panelWidth < 295 ? "Save" : "Apply & Close"), b -> save(true))
            .bounds(panelLeft, height - 32, actionWidth, 20).build());
        Button apply = addRenderableWidget(Button.builder(Component.literal("Apply"), b -> save(false))
            .bounds(panelLeft + actionWidth + 10, height - 32, actionWidth, 20).build());
        Button cancel = addRenderableWidget(Button.builder(Component.literal(panelWidth < 295 ? "Back" : "Cancel"), b -> onClose())
            .bounds(panelLeft + (actionWidth + 10) * 2, height - 32, actionWidth, 20).build());
        tip(saveClose, "Save changes and leave settings.");
        tip(apply, "Save changes and keep editing.");
        tip(cancel, "Leave settings. Unsaved changes will need confirmation.");
    }

    private void openImpactPreview(boolean enforced) {
        int[] draft = enforced ? enforcedDefaults.clone() : extraClient.clone();
        if (!enforced) { draft[40] = impactBrightness; draft[41] = impactContrast; draft[42] = impactEdges; draft[43] = impactGrain; }
        Minecraft.getInstance().setScreen(new GuardImpactPreviewScreen(this, draft, enforced ? operator : editable(false), values -> {
            if (enforced) {
                for (int i = 40; i <= 43; i++) enforcedDefaults[i] = values[i];
                for (int i = 50; i <= 57; i++) enforcedDefaults[i] = values[i];
                dirty = dirtyServer = dirtyPolicy = true;
            } else {
                impactBrightness = values[40]; impactContrast = values[41]; impactEdges = values[42]; impactGrain = values[43];
                for (int i = 50; i <= 57; i++) extraClient[i] = values[i];
                dirty = dirtyClient = true;
            }
            rebuildWidgets();
        }));
    }

    private int categoryRows(int category) {
        if (clientSection) return switch (category) {
            case 2 -> 5 + (chromaticOpen ? 4 : 0) + (shieldOpen ? hud ? 2 : 1 : 0) + (screenFlashOpen ? 3 : 0) + (cameraFeedbackOpen ? 4 + (impactFrames > 0 ? 2 : 0) : 0) + (animationOpen ? 2 : 0);
            case 7 -> 3 + (sparkOpen ? 1 + (sparksEnabled && hitParticles ? 8 : 0) : 0)
                + (streakOpen ? 2 + (streaksEnabled && hitParticles ? 9 : 0) : 0)
                + (sphericalFlashOpen ? 1 + (particleFlashesEnabled && hitParticles ? 6 : 0) : 0);
            case 3 -> 4;
            default -> 1;
        };
        return switch (category) {
            case 0 -> 5 + (timingOpen ? 6 : 0) + (retaliationOpen ? 3 : 0) + (pushbackOpen ? 3 : 0)
                + (parryProjectileOpen ? 1 : 0) + (parryExplosionOpen ? parryExplosions ? 2 : 1 : 0)
                ;
            case 6 -> 3 + (commonDamageOpen ? GuardDamageRules.BUILTINS.size() : 0)
                + (recentDamageOpen ? Math.max(1, recentHits.size()) : 0)
                + (otherDamageOpen ? 1 + Math.max(1, filteredDamageTypes().size()) : 0);
            case 11 -> 4 + (shieldTimingOpen ? 4 : 0) + (shieldPerfectOpen ? 5 : 0) + (shieldIdsOpen ? 2 : 0);
            case 13 -> 4 + (staggerDisplayOpen ? 2 : 0) + (staggerEnabled && staggerGaugeOpen ? 6 : 0) + (staggerEnabled && staggerRecoveryOpen ? 3 : 0);
            case 12 -> 6 + (mobGuard && mobDefenseOpen ? 13 : 0) + (mobGuard && mobTimingOpen ? 2 : 0) + (mobGuard && mobAppearanceOpen ? 2 : 0) + (mobGuard && mobEquipmentOpen ? 1 : 0) + (mobGuard && mobEligibilityOpen ? 1 : 0);
            case 5 -> 5;
            case 1 -> 1 + (guardOpen ? block ? 5 : 2 : 0) + (block ? 2 + (blockProjectileOpen ? 2 : 0)
                + (blockExplosionOpen ? 1 : 0) : 0);
            case 2 -> 2;
            case 4 -> 3 + (eligibilityOpen ? allowAnyItem ? 6 : 7 : 0) + (durabilityOpen ? 3 : 0) + (itemListsOpen ? 1 : 0);
            default -> 1;
        };
    }

    private int pageRows() {
        if (!settingsSearch.isBlank()) return Math.max(1, searchResults().size());
        if (enforceSection) return 6 + (enforceEnabled ? (enforceParticlesOpen ? 28 : 0)
            + (enforceHudOpen ? 19 : 0) + (enforceAudioOpen ? 5 : 0) + (enforceItemsOpen ? 2 : 0) + (enforceStaggerOpen ? 3 : 0) : 0);
        return categoryRows(page);
    }

    private record SearchEntry(int section, int page, String category, String name) {}

    private void addSearchEntries(List<SearchEntry> entries, int section, int page, String category, String names) {
        for (String name : names.split("\\|")) entries.add(new SearchEntry(section, page, category, name));
    }

    private List<SearchEntry> searchResults() {
        List<SearchEntry> all = new ArrayList<>();
        addSearchEntries(all, 3, 13, "Stagger", "Stagger gauge|Stagger Cells|Stagger duration|Maximum stagger cells|Parry cell loss|Perfect parry cell loss|Perfect parry cell recovery|Recovery after stagger|Stagger Recovery|Recovery near enemies|Recovery away from enemies|Stagger enemy radius|Stagger Display|Compact stagger icon|Stagger vertical offset");
        addSearchEntries(all, 3, 12, "Mob Guard", "Mob guarding|Guard Behavior|Mob guard difficulty|Hits before guard|Counter approach duration|Approach guard chance|Tactical guard chance|Tactical stance maximum|Tactical guard cooldown|Mob guard movement|Approach guard distance|Approach stance maximum|Rush after parry chance|Mob Eligibility|Additional humanoid mob IDs|Mob Parry Timing|Mob perfect parry window|Mob regular parry window|Mob Colors|Mob blocking|Mob parry color|Mob perfect parry color|Mob retaliation|Spawn Equipment|Mob gear chance");
        addSearchEntries(all, 0, 7, "Particles", "Spark Effects|Sparks|Ring sparks|Parry sparks|Perfect sparks|Perfect stars|Spark lifetime|Spark length|Spark speed|Spark reach|Streak Lines|Reverse streak gradient|Regular streak count|Perfect streak count|Streak length|Streak lifetime|Streak dot size|Regular dot spacing|Perfect dot spacing|Streak ballistic curve|Streak stretch speed|Spherical Flash|Spherical flash|Parry flash size|Perfect flash size|Parry flash opacity|Perfect flash opacity|Parry sphere lifetime|Perfect sphere lifetime");
        addSearchEntries(all, 0, 2, "Visuals", "Shield Icon|Shield icon|Shield reactions|Screen Flash|Screen flash|Perfect only flash|Meme flash|Camera Feedback|Camera shake|Hitlag frames|Perfect only hitlag|Impact frame|Perfect only impact|Live preview|Hand Animation|Third-person guard pose|First-person guard pose|Chromatic Aberration|Chromatic intensity|Chromatic duration|Perfect only chromatic|Chromatic affects HUD");
        addSearchEntries(all, 0, 3, "Audio", "Master volume|Perfect volume|Parry volume|Block volume");
        addSearchEntries(all, 0, 4, "Hand Selection", "Offhand guard priority");
        addSearchEntries(all, 1, 0, "Parry", "Timing|Parrying|Perfect window|Parry window|Recharge|Facing angle|Follow-up parries|Damage Returned|Parry retaliation|Perfect retaliation|Retaliation damage cap|Parry Pushback|Defender pushback|Shield attacker pushback|Weapon attacker pushback|Projectiles|Projectile parry|Explosions|Explosion parry|Explosions: perfect only");
        addSearchEntries(all, 1, 5, "Fall Impact", "Perfect fall parry|Fall blast|Fall launch|Fall blast breaks blocks|Look down to parry");
        addSearchEntries(all, 1, 1, "Blocking", "Guard Stance|Blocking|Damage reduction|Weapon blocks before break|Guard break recharge|Guard movement|Projectiles|Block deflect chance|Block projectiles|Explosions|Block explosions");
        addSearchEntries(all, 1, 11, "Shields", "Shield cone sparks|Shield Timing|Shield perfect window|Shield parry window|Shield parry recharge|Shield blocks before break|Shield Perfect Parry|Cone half-angle|Cone reach|Shield retaliation|Stun duration|Cone pushback|Additional Shields and Bosses|Extra shields and tags|Stunnable bosses");
        addSearchEntries(all, 1, 6, "Damage Types", "Common Damage Types|Drowning|Standing in fire|Burning|Lava|Hot floor|Campfire|Starvation|Wither|Poison / Instant Damage|Indirect magic|Fall damage|Wall collision|Vanilla /kill|Recent Hits|Other Damage Sources");
        addSearchEntries(all, 1, 2, "Shared Effects", "Sparks and flashes|Hit sounds");
        addSearchEntries(all, 1, 4, "Equipment", "Eligibility|Random dual tools|Random shield or weapon|Any held item|Empty-hand guard|Respect item cooldown|Consumables take priority|Allow usable weapons|Durability|Parry durability|Perfect durability|Block durability|Item Lists|Extra allowed items|Excluded items");
        addSearchEntries(all, 2, 8, "Enforce", "Enforce client settings|Particles|Visuals|Audio|Hand Selection|Experimental Stagger|Compact stagger icon|Stagger vertical offset|Don't Enforce");
        for (SearchEntry entry : List.copyOf(all)) if (entry.section() == 0)
            all.add(new SearchEntry(2, 8, "Enforce " + entry.category(), entry.name()));
        String[] terms = settingsSearch.toLowerCase(Locale.ROOT).trim().split("\\s+");
        return all.stream().filter(entry -> {
            String match = (entry.category() + " " + entry.name()).toLowerCase(Locale.ROOT);
            for (String term : terms) if (!match.contains(term)) return false;
            return true;
        }).toList();
    }

    private void buildSearchResults() {
        List<SearchEntry> results = searchResults();
        if (results.isEmpty()) {
            int position = controlIndex++;
            if (visible(position)) settingLabel(position, "No matching settings", "Try a setting name or a category.", true);
        }
        for (SearchEntry entry : results) {
            int position = controlIndex++;
            if (!visible(position)) continue;
            String scopeName = entry.section() == 0 ? "Client" : entry.section() == 1 ? "Server" : entry.section() == 3 ? "Experimental" : "Enforce";
            Button result = addRenderableWidget(Button.builder(Component.literal(entry.name()), b -> jumpToSetting(entry))
                .bounds(panelLeft + 4, row(position), contentRight() - panelLeft - 8, 20).build());
            tip(result, scopeName + " > " + entry.category() + " > " + entry.name());
        }
    }

    private void jumpToSetting(SearchEntry entry) {
        settingsSearch = "";
        settingsSearchChanged = false;
        experimentalSection = entry.section() == 3;
        enforceSection = entry.section() == 2;
        clientSection = entry.section() == 0;
        page = entry.page();
        if (!enforceSection) {
            int[] categories = clientSection ? CLIENT_PAGES : experimentalSection ? EXPERIMENTAL_PAGES : SERVER_PAGES;
            for (int i = 0; i < categories.length; i++) if (categories[i] == page) {
                sidebarOffset = Math.max(0, i - visibleSidebarTabs(categories.length) + 1);
                break;
            }
        }
        firstRow = 0;
        String[] sections = enforceSection ? new String[] {"Particles", "Visuals", "Audio", "Hand Selection", "Experimental Stagger"}
            : clientSection ? page == 7 ? new String[] {"Spark Effects", "Streak Lines", "Spherical Flash"}
                : page == 2 ? new String[] {"Shield Icon", "Screen Flash", "Camera Feedback", "Hand Animation", "Chromatic Aberration"} : new String[0]
            : switch (page) {
                case 0 -> new String[] {"Timing", "Damage Returned", "Parry Pushback", "Projectiles", "Explosions"};
                case 1 -> new String[] {"Guard Stance", "Projectiles", "Explosions"};
                case 4 -> new String[] {"Eligibility", "Durability", "Item Lists"};
                case 6 -> new String[] {"Common Damage Types", "Recent Hits", "Other Damage Sources"};
                case 13 -> new String[] {"Stagger Cells", "Stagger Recovery", "Stagger Display"};
                case 12 -> new String[] {"Guard Behavior", "Mob Parry Timing", "Mob Colors", "Spawn Equipment", "Mob Eligibility"};
                case 11 -> new String[] {"Shield Timing", "Shield Perfect Parry", "Additional Shields and Bosses"};
                default -> new String[0];
            };
        String chosen = initialGroup();
        for (SearchEntry result : searchResultsForPage(entry)) {
            for (String group : sections) if (group.equals(result.name())) chosen = group;
            if (result.name().equals(entry.name())) break;
        }
        if (chosen != null) rememberedGroups.put(groupKey(), chosen);
        selectRememberedGroup();
        rememberLocation();
        rebuildWidgets();
        if (enforceSection && !enforceEnabled) return;
        Integer row = visibleSettingRows.get(entry.name());
        if (row != null) scrollTo(Math.max(0, row - 2));
    }

    private List<SearchEntry> searchResultsForPage(SearchEntry selected) {
        String old = settingsSearch;
        settingsSearch = "";
        try {
            return searchResults().stream().filter(result -> result.section() == selected.section()
                && result.page() == selected.page() && result.category().equals(selected.category())).toList();
        } finally { settingsSearch = old; }
    }

    private int maxScroll() { return Math.max(0, pageRows() - visibleRows); }
    private int rowTop() { return layoutTop + 33; }
    private int sidebarTop() { return layoutTop + (height < 260 ? 58 : 70); }
    private int visibleSidebarTabs(int count) { return Math.min(count, Math.max(1, (height - sidebarTop() - 30) / 23)); }

    private int contentRight() { return panelLeft + panelWidth - 22; }
    private int settingX() { return panelLeft + ((contentRight() - panelLeft) * 53 / 100); }
    private int settingWidth() { return contentRight() - settingX() - 4; }
    private boolean visible(int index) { return index >= firstRow && index < firstRow + visibleRows; }
    private int row(int index) { return rowTop() + (index - firstRow) * ROW_STEP; }

    public void updateDamageState(GuardPackets.DamageState state) {
        damageCatalog.clear();
        if (!state.catalog().isEmpty()) for (String id : state.catalog().split(","))
            if (GuardDamageRules.validId(id) && !damageCatalog.contains(id)) damageCatalog.add(id);
        sourceMods.clear();
        for (String line : state.modNames().split("\n")) {
            int separator = line.indexOf('\t');
            if (separator > 0) sourceMods.put(line.substring(0, separator), line.substring(separator + 1));
        }
        if (!dirtyDamageRules) {
            LinkedHashMap<String, Boolean> saved = GuardDamageRules.parseRules(state.rules());
            if (saved != null) { damageRules.clear(); damageRules.putAll(saved); }
        }
        recentHits.clear();
        if (!state.hits().isEmpty()) for (String id : state.hits().split(","))
            if (GuardDamageRules.validId(id) && !recentHits.contains(id) && recentHits.size() < 10)
                recentHits.add(id);
        if (!clientSection && page == 6) rebuildWidgets();
    }

    public void updateDamageHit(String id) {
        if (!GuardDamageRules.validId(id)) return;
        recentHits.remove(id);
        recentHits.add(0, id);
        while (recentHits.size() > 10) recentHits.remove(recentHits.size() - 1);
        if (!clientSection && page == 6) rebuildWidgets();
    }

    private void damageChanged() {
        dirty = dirtyServer = dirtyDamageRules = true;
        rebuildWidgets();
    }

    private List<String> filteredDamageTypes() {
        String[] terms = damageSearch.trim().toLowerCase(Locale.ROOT).split("\\s+");
        return damageCatalog.stream().filter(id -> !GuardDamageRules.builtin(id)).filter(id -> {
            String name = modName(id).toLowerCase(Locale.ROOT);
            String key = id.toLowerCase(Locale.ROOT);
            for (String term : terms) {
                if (term.isEmpty()) continue;
                if (term.equals("#enabled")) { if (!damageRules.getOrDefault(id, GuardDamageRules.defaultAllowed(id))) return false; }
                else if (term.equals("#disabled")) { if (damageRules.getOrDefault(id, GuardDamageRules.defaultAllowed(id))) return false; }
                else if (term.startsWith("@")) {
                    if (!key.substring(0, key.indexOf(':')).contains(term.substring(1)) && !name.contains(term.substring(1))) return false;
                } else if (!key.contains(term) && !name.contains(term)) return false;
            }
            return true;
        }).toList();
    }

    private String modName(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        String namespace = key == null ? "unknown" : key.getNamespace();
        if (namespace.equals("minecraft")) return "Minecraft";
        String known = sourceMods.get(namespace);
        return known != null ? known : ModList.get().getModContainerById(namespace)
            .map(container -> container.getModInfo().getDisplayName()).orElse(namespace);
    }

    private void section(String title) {
        int position = controlIndex++;
        if (visible(position)) sectionLabels.add(new SettingLabel(title, "", row(position), true));
    }

    private void emptyList() {
        int position = controlIndex++;
        if (visible(position)) settingLabel(position, "No sources yet", "Sources appear here when available.", false);
    }

    private String sourceTip(String id, String description) {
        return description + " Mod: " + modName(id) + ". Damage type: " + id + ".";
    }

    private void buildDamageTypes() {
        dropdown("Common Damage Types", () -> commonDamageOpen, "Show the built-in damage types and their parry rules.");
        if (commonDamageOpen) {
        for (String id : GuardDamageRules.BUILTINS) {
            String name = switch (id) {
                case "minecraft:drown" -> "Drowning";
                case "minecraft:in_fire" -> "Standing in fire";
                case "minecraft:on_fire" -> "Burning";
                case "minecraft:lava" -> "Lava";
                case "minecraft:hot_floor" -> "Hot floor";
                case "minecraft:campfire" -> "Campfire";
                case "minecraft:starve" -> "Starvation";
                case "minecraft:wither" -> "Wither";
                case "minecraft:magic" -> "Poison / Instant Damage";
                case "minecraft:indirect_magic" -> "Indirect magic";
                case "minecraft:fall" -> "Fall damage";
                case "minecraft:fly_into_wall" -> "Wall collision";
                default -> "Vanilla /kill";
            };
            toggle(name, () -> damageRules.getOrDefault(id, GuardDamageRules.defaultAllowed(id)), value -> {
                damageRules.put(id, value); dirtyDamageRules = true; rebuildWidgets();
            }, true, sourceTip(id, "Parry this source when enabled. This does not change normal blocking."
                + (id.equals("minecraft:magic") ? " Vanilla poison and instant damage share this damage type." : "")
                + " Default: " + (GuardDamageRules.defaultAllowed(id) ? "On" : "Off") + "."));
        }
        }
        dropdown("Recent Hits", () -> recentDamageOpen, "Show up to ten damage types that recently hurt players.");
        if (recentDamageOpen) {
        if (recentHits.isEmpty()) emptyList();
        for (String id : recentHits) {
            int position = controlIndex++;
            if (visible(position)) settingLabel(position, id, sourceTip(id,
                "Recently hurt a player. Find it in the list below to change its parry setting."), true);
        }
        }
        dropdown("Other Damage Sources", () -> otherDamageOpen, "Search all damage types found at runtime, including modded sources.");
        if (otherDamageOpen) {
        int searchRow = controlIndex++;
        if (visible(searchRow)) {
            settingLabel(searchRow, "Search", "Filter the list below. Type a name or ID, @mod for a mod name or ID,"
                + " #enabled or #disabled for the current parry setting. Combine terms with spaces, e.g. @minecraft #disabled."
                + " Recent hits and common damage types are unaffected. Default: empty (show all).", true);
            EditBox search = addRenderableWidget(new EditBox(font, settingX(), row(searchRow), settingWidth(), 20,
                Component.literal("Filter damage sources")));
            damageSearchBox = search;
            search.setMaxLength(128);
            search.setHint(Component.literal("name, @mod, #enabled, #disabled"));
            search.setValue(damageSearch);
            search.setResponder(value -> {
                damageSearch = value;
                damageSearchChanged = true;
            });
            tip(search, "Filter by damage source ID or mod name. @mod filters by mod ID/name; #enabled and #disabled"
                + " show the respective Parry states. Combine filters with spaces. Example: @minecraft #disabled."
                + " This search changes only the list below.");
        }
        List<String> filtered = filteredDamageTypes();
        if (filtered.isEmpty()) emptyList();
        for (String id : filtered) {
            boolean enabled = damageRules.getOrDefault(id, GuardDamageRules.defaultAllowed(id));
            int position = controlIndex++;
            if (!visible(position)) continue;
            settingLabel(position, modName(id) + " | " + id, sourceTip(id,
                "Parry is " + (enabled ? "enabled" : "disabled") + ". Default: "
                    + (GuardDamageRules.defaultAllowed(id) ? "On" : "Off") + "."), true);
            Button status = addRenderableWidget(Button.builder(damageStatus(enabled), b -> {
                damageRules.put(id, !damageRules.getOrDefault(id, GuardDamageRules.defaultAllowed(id)));
                damageChanged();
            }).bounds(settingX(), row(position), settingWidth(), 20).build());
            status.active = operator;
            tip(status, scope(true, sourceTip(id, "Toggle whether this damage type can be parried."
                + " Default: " + (GuardDamageRules.defaultAllowed(id) ? "On" : "Off") + ".")));
        }
        }
    }

    private static Component damageStatus(boolean enabled) {
        return Component.literal("Parry: " + (enabled ? "Enabled" : "Disabled"))
            .withStyle(style -> style.withColor(enabled ? 0xB6E8B6 : 0xA7A7A7));
    }

    private void dropdown(String name, BooleanSupplier expanded, String description) {
        int position = controlIndex++;
        visibleSettingRows.putIfAbsent(name, position);
        if (!visible(position)) return;
        dropdownLabels.add(new DropdownLabel(name, row(position), expanded, description));
    }

    private String groupKey() {
        return (enforceSection ? "enforce" : clientSection ? "client" : "server") + ":" + page;
    }

    private String initialGroup() {
        if (enforceSection) return null;
        return switch (page) {
            case 7 -> "Spark Effects";
            case 2 -> clientSection ? "Shield Icon" : null;
            case 0 -> "Timing";
            case 1 -> "Guard Stance";
            case 4 -> clientSection ? null : "Eligibility";
            case 6 -> "Common Damage Types";
            case 13 -> "Stagger Cells";
            case 12 -> "Guard Behavior";
            case 11 -> "Shield Timing";
            default -> null;
        };
    }

    private void selectRememberedGroup() {
        String selected = rememberedGroups.getOrDefault(groupKey(), initialGroup());
        sparkOpen = "Spark Effects".equals(selected) && page == 7;
        streakOpen = "Streak Lines".equals(selected) && page == 7;
        sphericalFlashOpen = "Spherical Flash".equals(selected) && page == 7;
        shieldOpen = "Shield Icon".equals(selected) && clientSection && page == 2;
        screenFlashOpen = "Screen Flash".equals(selected) && clientSection && page == 2;
        cameraFeedbackOpen = "Camera Feedback".equals(selected) && clientSection && page == 2;
        chromaticOpen = "Chromatic Aberration".equals(selected) && clientSection && page == 2;
        staggerDisplayOpen = "Stagger Display".equals(selected) && experimentalSection && page == 13;
        animationOpen = "Hand Animation".equals(selected) && clientSection && page == 2;
        timingOpen = "Timing".equals(selected) && !clientSection && page == 0;
        retaliationOpen = "Damage Returned".equals(selected) && page == 0;
        pushbackOpen = "Parry Pushback".equals(selected) && page == 0;
        parryProjectileOpen = "Projectiles".equals(selected) && page == 0;
        parryExplosionOpen = "Explosions".equals(selected) && page == 0;
        guardOpen = "Guard Stance".equals(selected) && page == 1;
        blockProjectileOpen = "Projectiles".equals(selected) && page == 1;
        blockExplosionOpen = "Explosions".equals(selected) && page == 1;
        eligibilityOpen = "Eligibility".equals(selected) && !clientSection && page == 4;
        durabilityOpen = "Durability".equals(selected) && page == 4;
        itemListsOpen = "Item Lists".equals(selected) && page == 4;
        staggerGaugeOpen = "Stagger Cells".equals(selected) && page == 13;
        staggerRecoveryOpen = "Stagger Recovery".equals(selected) && page == 13;
        mobDefenseOpen = "Guard Behavior".equals(selected) && page == 12;
        mobTimingOpen = "Mob Parry Timing".equals(selected) && page == 12;
        mobAppearanceOpen = "Mob Colors".equals(selected) && page == 12;
        mobEquipmentOpen = "Spawn Equipment".equals(selected) && page == 12;
        mobEligibilityOpen = "Mob Eligibility".equals(selected) && page == 12;
        shieldTimingOpen = "Shield Timing".equals(selected) && page == 11;
        shieldPerfectOpen = "Shield Perfect Parry".equals(selected) && page == 11;
        shieldIdsOpen = "Additional Shields and Bosses".equals(selected) && page == 11;
        commonDamageOpen = "Common Damage Types".equals(selected) && page == 6;
        recentDamageOpen = "Recent Hits".equals(selected) && page == 6;
        otherDamageOpen = "Other Damage Sources".equals(selected) && page == 6;
        enforceParticlesOpen = "Particles".equals(selected) && enforceEnabled;
        enforceHudOpen = "Visuals".equals(selected) && enforceEnabled;
        enforceAudioOpen = "Audio".equals(selected) && enforceEnabled;
        enforceItemsOpen = "Hand Selection".equals(selected) && enforceEnabled;
        enforceStaggerOpen = "Experimental Stagger".equals(selected) && enforceEnabled;
    }

    private void rememberLocation() {
        rememberedSection = experimentalSection ? 3 : enforceSection ? 2 : clientSection ? 0 : 1;
        if (experimentalSection) { rememberedExperimentalPage = page; rememberedExperimentalRow = firstRow; }
        else if (enforceSection) rememberedEnforceRow = firstRow;
        else if (clientSection) { rememberedClientPage = page; rememberedClientRow = firstRow; }
        else { rememberedServerPage = page; rememberedServerRow = firstRow; }
    }

    private void buildEnforcedDefaults() {
        dropdown("Particles", () -> enforceParticlesOpen, "Set the server's starting spark and spherical flash values.");
        if (enforceEnabled && enforceParticlesOpen) {
            dontEnforce(GuardClientPreset.PARTICLES);
            presetToggle("Sparks", 22);
            presetToggle("Ring sparks", 25);
            presetToggle("Streak Lines", 26);
            presetToggle("Reverse streak gradient", 62);
            presetSlider("Regular streak count", 39, 0, 200, "%");
            presetSlider("Perfect streak count", 29, 0, 200, "%");
            presetSlider("Regular dot spacing", 46, 1, 200, "%");
            presetSlider("Perfect dot spacing", 45, 1, 200, "%");
            presetSlider("Streak length", 30, 0, 200, "%");
            presetSlider("Streak lifetime", 31, 2, 40, " ticks");
            presetSlider("Streak dot size", 37, 0, 200, "%");
            presetSlider("Streak ballistic curve", 48, 0, 200, "%");
            presetSlider("Streak stretch speed", 38, 0, 200, "%");
            presetSlider("Parry sparks", 16, 0, 200, "%");
            presetSlider("Perfect sparks", 17, 0, 200, "%");
            presetSlider("Perfect stars", 18, 0, 16, "");
            presetSlider("Spark lifetime", 19, 0, 200, "%");
            presetSlider("Spark length", 20, 0, 200, "%");
            presetSlider("Spark speed", 21, 0, 200, "%");
            presetSlider("Spark reach", 44, 0, 200, "%");
            presetToggle("Spherical flash", 23);
            presetSlider("Parry flash size", 7, 0, 200, "%");
            presetSlider("Perfect flash size", 8, 0, 200, "%");
            presetSlider("Parry flash opacity", 9, 0, 100, "%");
            presetSlider("Perfect flash opacity", 10, 0, 100, "%");
            presetSlider("Parry sphere lifetime", 33, 1, 12, " ticks");
            presetSlider("Perfect sphere lifetime", 36, 1, 12, " ticks");
        }
        dropdown("Visuals", () -> enforceHudOpen, "Set the server's starting HUD and screen feedback values.");
        if (enforceEnabled && enforceHudOpen) {
            dontEnforce(GuardClientPreset.HUD);
            presetToggle("Shield icon", 0);
            presetToggle("Shield reactions", 1);
            presetToggle("Screen flash", 2);
            presetToggle("Meme flash", 24);
            presetToggle("Perfect only flash", 27);
            presetToggle("Perfect only hitlag", 28);
            presetSlider("Screen flash strength", 4, 0, 100, "%");
            presetSlider("Camera shake", 5, 0, 100, "%");
            presetSlider("Hitlag frames", 3, 0, 10, " frames");
            presetToggle("Impact frame", 34);
            presetToggle("Perfect only impact", 35);
            int previewRow = controlIndex++;
            if (visible(previewRow)) {
                settingLabel(previewRow, "Impact preview", "Tune the enforced impact image with a captured scene. Changes save when you press Apply.", operator);
                Button preview = addRenderableWidget(Button.builder(Component.literal("Preview"), b ->
                    openImpactPreview(true)).bounds(settingX(), row(previewRow), settingWidth(), 20).build());
                preview.active = operator;
                tip(preview, "Open the preview to tune enforced impact values.");
            }
            presetToggle("Third-person guard pose", 49);
            presetToggle("First-person guard pose", 11);
            presetSlider("Chromatic intensity", 58, 0, 200, "%");
            presetSlider("Chromatic duration", 59, 1, 40, " ticks");
            presetToggle("Perfect only chromatic", 60);
            presetToggle("Chromatic affects HUD", 61);

        }
        dropdown("Experimental Stagger", () -> enforceStaggerOpen, "Starting client presentation for the experimental stagger gauge. Server stagger mechanics remain authoritative.");
        if (enforceEnabled && enforceStaggerOpen) {
            dontEnforce(GuardClientPreset.STAGGER);
            presetToggle("Compact stagger icon", 63);
            presetSlider("Stagger vertical offset", 64, 0, 100, " px");
        }
        dropdown("Audio", () -> enforceAudioOpen, "Set the server's starting local playback volumes.");
        if (enforceEnabled && enforceAudioOpen) {
            dontEnforce(GuardClientPreset.AUDIO);
            presetSlider("Master volume", 12, 0, 200, "%");
            presetSlider("Perfect volume", 14, 0, 200, "%");
            presetSlider("Parry volume", 13, 0, 200, "%");
            presetSlider("Block volume", 15, 0, 200, "%");
        }
        dropdown("Hand Selection", () -> enforceItemsOpen, "Set which hand guards when both hands hold eligible items.");
        if (enforceEnabled && enforceItemsOpen) {
            dontEnforce(GuardClientPreset.ITEMS);
            presetToggle("Offhand guard priority", 6);
        }
    }

    private void dontEnforce(int category) {
        toggle("Don't Enforce", () -> (dontEnforceMask & category) != 0, value -> {
            dontEnforceMask = value ? dontEnforceMask | category : dontEnforceMask & ~category;
        }, true, "On: players receive this category's server defaults when they join, then can edit it themselves. Off: the server keeps it locked.");
    }

    private void presetToggle(String name, int index) {
        toggle(name, () -> enforcedDefaults[index] != 0, value -> enforcedDefaults[index] = value ? 1 : 0, true,
            "Starting client value set by the server. Locked for players unless this category has Don't Enforce enabled.");
    }

    private void presetSlider(String name, int index, int min, int max, String suffix) {
        slider(name, enforcedDefaults[index], min, max, suffix, 1, true, value -> enforcedDefaults[index] = value,
            "Starting client value set by the server. Locked for players unless this category has Don't Enforce enabled."
                + (index == 48 ? " Curves the tip downward along a ballistic arc; 0% keeps lines straight." : "")
                + (index == 3 ? " When Visuals is locked, this hitlag duration also controls whether perfect retaliation waits for feedback completion." : ""));
    }

    private boolean editable(boolean server) {
        return server ? operator : !GuardClient.clientCategoryLocked(activeClientCategory);
    }

    private String scope(boolean server, String description) {
        return description + (server ? operator ? " Server setting." : " OP only: you can view this server setting, but cannot change it."
            : editable(false) ? " Only changes your client." : " Enforced by the server; this category is locked.");
    }

    private void settingLabel(int index, String name, String description, boolean enabled) {
        settingLabels.add(new SettingLabel(name, description, row(index), enabled));
    }

    private String defaultText(String name, boolean server) {
        if (enforceSection && (name.equals("Enforce client settings") || name.equals("Don't Enforce"))) return "Off";
        if (enforceSection) server = false;
        if (server) return switch (name) {
            case "Random dual tools" -> "On";
            case "Mob guarding" -> "Off";
            case "Mob guard difficulty" -> "100%";
            case "Hits before guard" -> "1 hit";
            case "Mob perfect parry window" -> "20 ticks";
            case "Mob regular parry window" -> "60 ticks";
            case "Stagger gauge" -> "Off";
            case "Mob blocking" -> "Off";
            case "Counter approach duration" -> "12 ticks";
            case "Approach guard chance" -> "100%";
            case "Tactical guard chance" -> "100%";
            case "Tactical stance maximum" -> "5 seconds";
            case "Tactical guard cooldown" -> "200 ticks";
            case "Additional humanoid mob IDs" -> "empty";
            case "Stagger duration" -> "40 ticks";
            case "Maximum stagger cells" -> "5 cells";
            case "Parry cell loss", "Perfect parry cell recovery" -> "1 cell";
            case "Perfect parry cell loss" -> "2 cells";
            case "Recovery near enemies" -> "100 ticks";
            case "Recovery away from enemies" -> "20 ticks";
            case "Stagger enemy radius" -> "12 blocks";
            case "Mob retaliation" -> "On";
            case "Mob gear chance" -> "50%";
            case "Mob guard movement" -> "100%";
            case "Approach guard distance" -> "2 blocks";
            case "Approach stance maximum" -> "2 seconds";
            case "Rush after parry chance" -> "70%";
            case "Recovery after stagger" -> "2 cells";
            case "Parrying", "Blocking", "Projectile parry", "Explosion parry", "Fall parry", "Fall blast breaks blocks",
                 "Look down to parry", "Block projectiles", "Block explosions", "Allow usable weapons", "Respect item cooldown", "Sparks and flashes", "Hit sounds" -> "On";
            case "Fall damage", "Wall collision" -> "On";
            case "Explosions: perfect only", "Drowning", "Standing in fire", "Burning", "Lava", "Hot floor", "Campfire", "Starvation", "Wither", "Poison / Instant Damage", "Indirect magic", "Vanilla /kill" -> "Off";
            case "Any held item" -> "On";
            case "Perfect fall parry" -> "On";
            case "Empty-hand guard" -> "Off";
            case "Consumables take priority" -> "On";
            case "Shield perfect window" -> "2 ticks";
            case "Shield parry window" -> "6 ticks";
            case "Shield blocks before break" -> "5";
            case "Weapon blocks before break" -> "3";
            case "Shield parry recharge" -> "20 ticks (1 second)";
            case "Guard break recharge" -> "100 ticks (5 seconds)";
            case "Cone half-angle" -> "90°";
            case "Cone reach" -> "5 blocks";
            case "Shield retaliation" -> "10%";
            case "Retaliation damage cap" -> "25 damage";
            case "Stun duration" -> "30 ticks";
            case "Cone pushback" -> "35%";
            case "Weapon attacker pushback" -> "80%";
            case "Shield attacker pushback" -> "85%";
            case "Perfect window" -> "2 ticks";
            case "Parry window" -> "6 ticks";
            case "Recharge" -> "10 ticks";
            case "Facing angle" -> "180°";
            case "Follow-up parries" -> "3 ticks";
            case "Parry retaliation" -> "25%";
            case "Perfect retaliation", "Damage reduction" -> "50%";
            case "Fall launch" -> "10%";
            case "Defender pushback" -> "80%";
            case "Guard movement" -> "55%";
            case "Block deflect chance" -> "0%";
            case "Parry durability" -> "0.2%";
            case "Perfect durability" -> "0.1%";
            case "Block durability" -> "0.4%";
            case "Fall blast" -> "100%";
            case "Master volume" -> "100%";
            case "Perfect volume" -> "130%";
            case "Parry volume", "Block volume" -> "65%";
            default -> "Empty";
        };
        return switch (name) {
            case "Sparks", "Spherical flash", "Shield icon", "Shield reactions", "First-person guard pose", "Third-person guard pose" -> "On";
            case "Meme flash", "Ring sparks" -> "Off";
            case "Streak Lines", "Perfect only flash", "Perfect only hitlag", "Impact frame", "Perfect only impact" -> "On";
            case "Offhand guard priority" -> "Off";
            case "Master volume" -> "100%";
            case "Spark speed" -> "80%";
            case "Spark reach" -> "10%";
            case "Perfect volume" -> "130%";
            case "Parry sparks" -> "100%";
            case "Perfect sparks" -> "170%";
            case "Perfect flash opacity" -> "100%";
            case "Parry volume", "Block volume" -> "65%";
            case "Spark length" -> "50%";
            case "Perfect flash size" -> "30%";
            case "Spark lifetime" -> "100%";
            case "Streak lifetime" -> "30 ticks";
            case "Parry sphere lifetime" -> "5 ticks";
            case "Perfect sphere lifetime" -> "6 ticks";
            case "Regular streak count" -> "80%";
            case "Streak amount", "Perfect streak count" -> "90%";
            case "Streak dot size" -> "30%";
            case "Streak length" -> "80%";
            case "Streak stretch speed" -> "200%";
            case "Regular dot spacing" -> "60%";
            case "Perfect dot spacing" -> "20%";
            case "Streak ballistic curve" -> "40%";
            case "Impact brightness" -> "186";
            case "Impact contrast" -> "110";
            case "Impact edges" -> "200";
            case "Impact grain" -> "101";
            case "Perfect stars" -> "2";
            case "Parry flash size" -> "10%";
            case "Parry flash opacity" -> "100%";
            case "Camera shake" -> "100%";
            case "Screen flash" -> "On";
            case "Screen flash strength" -> "100%";
            case "Hitlag frames" -> "6 frames";
            default -> "Off";
        };
    }

    private void toggle(String name, BooleanSupplier current,
                        Consumer<Boolean> change, boolean server, String description) {
        int position = controlIndex++;
        visibleSettingRows.putIfAbsent(name, position);
        if (!visible(position)) return;
        String tooltip = scope(server, description + (description.contains("Default:") ? "" : " Default: " + defaultText(name, server) + "."));
        settingLabel(position, name, tooltip, editable(server));
        boolean damageToggle = server && !enforceSection && page == 6;
        Button button = addRenderableWidget(Button.builder(damageToggle ? damageStatus(current.getAsBoolean()) : label(current.getAsBoolean()), b -> {
            dirty = true;
            if (server) {
                dirtyServer = true;
                if (enforceSection) dirtyPolicy = true; else if (page == 12) dirtyMobs = true; else if (page == 13) dirtyStagger = true; else dirtyServerRules = true;
            } else dirtyClient = true;
            change.accept(!current.getAsBoolean());
            b.setMessage(damageToggle ? damageStatus(current.getAsBoolean()) : label(current.getAsBoolean()));
        }).bounds(settingX(), row(position), settingWidth(), 20).build());
        button.active = editable(server);
        tip(button, tooltip);
    }

    private void slider(String name, int current, int min, int max, String suffix,
                        int divisor, boolean server, Change change, String description) {
        int position = controlIndex++;
        visibleSettingRows.putIfAbsent(name, position);
        if (!visible(position)) return;
        String tooltip = scope(server, description + " Default: " + defaultText(name, server) + ".");
        settingLabel(position, name, tooltip, editable(server));
        Slider control = addRenderableWidget(new Slider(settingX(), row(position), settingWidth(),
            name, current, min, max, suffix, value -> {
                dirty = true;
                if (server) {
                    dirtyServer = true;
                    if (enforceSection) dirtyPolicy = true; else if (page == 12) dirtyMobs = true; else if (page == 13) dirtyStagger = true; else dirtyServerRules = true;
                } else dirtyClient = true;
                change.accept(value);
            }, divisor));
        control.active = editable(server);
        tip(control, tooltip);
    }

    private static Integer parseColor(String value) {
        String hex = value.startsWith("#") ? value.substring(1) : value;
        if (!hex.matches("[0-9a-fA-F]{6}")) return null;
        return Integer.parseInt(hex, 16);
    }
    private void editBox(String name, String initial, Consumer<String> update, String description) {
        int position = controlIndex++;
        visibleSettingRows.putIfAbsent(name, position);
        if (!visible(position)) return;
        String tooltip = scope(true, description + (description.contains("Default:") ? "" : " Default: " + defaultText(name, true) + "."));
        settingLabel(position, name, tooltip, operator);
        EditBox box = addRenderableWidget(new EditBox(font, settingX(), row(position), settingWidth(), 20,
            Component.literal(name)));
        box.setMaxLength(1024);
        box.setValue(initial);
        box.setResponder(value -> { dirty = true; dirtyServer = true; if (page == 12) dirtyMobs = true; else if (page == 13) dirtyStagger = true; else dirtyServerRules = true; update.accept(value); });
        box.setEditable(operator);
        box.active = operator;
        tip(box, tooltip);
    }

    private void tip(AbstractWidget widget, String description) {
        widgetTips.put(widget, description);
        if (widget instanceof Button button) {
            themedButtons.add(button);
            buttonTips.put(button, description);
        }
    }

    private static Component label(boolean value) {
        return Component.literal(value ? "On" : "Off");
    }

    public void refreshPolicy() {
        if (GuardClient.clientCategoryLocked(GuardClientPreset.PARTICLES)) {
            sparksEnabled = GuardConfig.SPARKS_ENABLED.get();
            sparkRing = GuardConfig.SPARK_RING.get();
            streaksEnabled = GuardConfig.STREAKS_ENABLED.get();
            streakAmount = GuardConfig.STREAK_AMOUNT.get(); regularStreakAmount = GuardConfig.REGULAR_STREAK_AMOUNT.get(); streakLength = GuardConfig.STREAK_LENGTH.get();
            streakLifetime = GuardConfig.STREAK_LIFETIME.get();
            regularOrbLifetime = GuardConfig.REGULAR_ORB_LIFETIME.get();
            perfectOrbLifetime = GuardConfig.PERFECT_ORB_LIFETIME.get();
            streakDotSize = GuardConfig.STREAK_DOT_SIZE.get(); streakDotSpacing = GuardConfig.STREAK_DOT_SPACING.get();
            regularStreakDotSpacing = GuardConfig.REGULAR_STREAK_DOT_SPACING.get();
            streakBallisticCurve = GuardConfig.STREAK_BALLISTIC_CURVE.get();
            streakStretchSpeed = GuardConfig.STREAK_STRETCH_SPEED.get();
            particleFlashesEnabled = GuardConfig.PARTICLE_FLASHES_ENABLED.get();
            regularOrbSize = GuardConfig.REGULAR_ORB_SIZE.get(); perfectOrbSize = GuardConfig.PERFECT_ORB_SIZE.get();
            regularOrbOpacity = GuardConfig.REGULAR_ORB_OPACITY.get(); perfectOrbOpacity = GuardConfig.PERFECT_ORB_OPACITY.get();
            regularSparks = GuardConfig.REGULAR_SPARK_COUNT.get(); perfectSparks = GuardConfig.PERFECT_SPARK_COUNT.get();
            stars = GuardConfig.STAR_COUNT.get(); sparkLifetime = GuardConfig.SPARK_LIFETIME.get();
            sparkLength = GuardConfig.SPARK_LENGTH.get(); sparkExplosiveness = GuardConfig.SPARK_EXPLOSIVENESS.get(); sparkReach = GuardConfig.SPARK_REACH.get();
        }
        int[] effectiveClient = GuardClientPreset.readLocal();
        for (int i = 50; i < extraClient.length; i++) if (GuardClient.clientCategoryLocked(GuardClientPreset.categoryOf(i))) extraClient[i] = effectiveClient[i];
        if (GuardClient.clientCategoryLocked(GuardClientPreset.HUD)) {
            hud = GuardConfig.HUD.get(); shieldEffects = GuardConfig.SHIELD_EFFECTS.get(); memeFlash = GuardConfig.MEME_FLASH.get();
            flashStrength = GuardConfig.SCREEN_FLASH.get() ? GuardConfig.FLASH_STRENGTH.get() : 0;
            perfectOnlyFlash = GuardConfig.PERFECT_ONLY_FLASH.get();
            shakeStrength = GuardConfig.SCREEN_SHAKE.get() ? GuardConfig.SHAKE_STRENGTH.get() : 0;
            hitlagFrames = GuardConfig.HITLAG_FRAMES.get();
            perfectOnlyHitlag = GuardConfig.PERFECT_ONLY_HITLAG.get();
            impactFrames = GuardConfig.IMPACT_FRAMES.get();
            impactPerfectOnly = GuardConfig.IMPACT_PERFECT_ONLY.get();
            impactBrightness = GuardConfig.IMPACT_BRIGHTNESS.get(); impactContrast = GuardConfig.IMPACT_CONTRAST.get();
            impactEdges = GuardConfig.IMPACT_EDGES.get(); impactGrain = GuardConfig.IMPACT_GRAIN.get();
            firstPersonAnimation = GuardConfig.FIRST_PERSON_ANIMATION.get();
        thirdPersonAnimation = GuardConfig.THIRD_PERSON_ANIMATION.get();
        }
        if (GuardClient.clientCategoryLocked(GuardClientPreset.AUDIO)) {
            masterVolume = GuardConfig.LOCAL_MASTER_VOLUME.get(); perfectVolume = GuardConfig.LOCAL_PERFECT_VOLUME.get();
            parryVolume = GuardConfig.LOCAL_PARRY_VOLUME.get(); blockVolume = GuardConfig.LOCAL_BLOCK_VOLUME.get();
        }
        if (GuardClient.clientCategoryLocked(GuardClientPreset.ITEMS)) {
            preferOffhand = GuardConfig.PREFER_OFFHAND.get();
        }
        rebuildWidgets();
    }

    /** Reset values in memory; Apply is still required to commit. */
    private void reset(boolean wholeSection) {
        if ((enforceSection || !clientSection && !experimentalSection) && !operator) return;
        if (enforceSection) {
            enforceEnabled = false;
            dontEnforceMask = 0;
            System.arraycopy(GuardClientPreset.DEFAULTS, 0, enforcedDefaults, 0, enforcedDefaults.length);
            dirtyServer = dirtyPolicy = dirty = true;
        } else if (clientSection) {
            if (wholeSection) {
                for (int category : CLIENT_PAGES) if (!GuardClient.clientCategoryLocked(categoryMask(category))) resetClientPage(category);
            } else if (!GuardClient.clientCategoryLocked(categoryMask(page))) resetClientPage(page);
        } else if (experimentalSection) {
            if ((wholeSection || page == 13) && !GuardClient.clientCategoryLocked(GuardClientPreset.STAGGER)) resetClientPage(13);
            if (operator) {
                GuardPackets.Settings defaults = GuardConfig.defaultSnapshot(true);
                if (wholeSection) for (int category : EXPERIMENTAL_PAGES) resetServerPage(category, defaults);
                else resetServerPage(page, defaults);
            }
        } else {
            GuardPackets.Settings defaults = GuardConfig.defaultSnapshot(operator);
            if (wholeSection) for (int category : SERVER_PAGES) resetServerPage(category, defaults);
            else resetServerPage(page, defaults);
        }
        rebuildWidgets();
    }

    private int categoryMask(int category) {
        return switch (category) {
            case 7 -> GuardClientPreset.PARTICLES;
            case 2 -> GuardClientPreset.HUD;
            case 13 -> GuardClientPreset.STAGGER;
            case 3 -> GuardClientPreset.AUDIO;
            default -> GuardClientPreset.ITEMS;
        };
    }

    private void resetClientPage(int category) {
        int[] values = GuardClientPreset.DEFAULTS;
        for (int i = 50; i < extraClient.length; i++) if (GuardClientPreset.categoryOf(i) == categoryMask(category)) extraClient[i] = values[i];
        switch (category) {
            case 7 -> {
                sparksEnabled = values[22] != 0; particleFlashesEnabled = values[23] != 0;
                sparkRing = values[25] != 0; streaksEnabled = values[26] != 0;
                streakAmount = values[29]; regularStreakAmount = values[39]; streakLength = values[30]; streakLifetime = values[31];
                regularOrbLifetime = values[33]; perfectOrbLifetime = values[36];
                streakDotSize = values[37]; streakStretchSpeed = values[38]; streakDotSpacing = values[45]; regularStreakDotSpacing = values[46]; streakBallisticCurve = values[48];
                regularSparks = values[16]; perfectSparks = values[17]; stars = values[18];
                sparkLifetime = values[19]; sparkLength = values[20]; sparkExplosiveness = values[21]; sparkReach = values[44];
                regularOrbSize = values[7]; perfectOrbSize = values[8];
                regularOrbOpacity = values[9]; perfectOrbOpacity = values[10];
            }
            case 2 -> {
                hud = values[0] != 0; shieldEffects = values[1] != 0; memeFlash = values[24] != 0;
                flashStrength = values[4]; shakeStrength = values[5];
                hitlagFrames = values[3]; firstPersonAnimation = values[11] != 0; thirdPersonAnimation = values[49] != 0;
                perfectOnlyFlash = values[27] != 0; perfectOnlyHitlag = values[28] != 0;
                impactFrames = values[34]; impactPerfectOnly = values[35] != 0;
                impactBrightness = values[40]; impactContrast = values[41];
                impactEdges = values[42]; impactGrain = values[43];
            }
            case 3 -> { masterVolume = values[12]; parryVolume = values[13];
                perfectVolume = values[14]; blockVolume = values[15]; }
            case 13 -> { extraClient[63] = values[63]; extraClient[64] = values[64]; }
            case 4 -> preferOffhand = values[6] != 0;
            default -> { return; }
        }
        dirty = dirtyClient = true;
    }

    private void resetServerPage(int category, GuardPackets.Settings d) {
        switch (category) {
            case 13 -> { setStaggerSettings(GuardConfig.staggerSnapshot(true)); dirtyStagger = true; }
            case 12 -> { setMobSettings(GuardConfig.mobSnapshot(true)); dirtyMobs = true; }
            case 0 -> {
                parry = d.parry(); perfect = d.perfect(); window = d.window(); recharge = d.recharge();
                angle = d.angle(); followUp = d.followUp(); parryReturnPercent = d.parryReturnPercent();
                perfectReturnPercent = d.perfectReturnPercent(); retaliationCap = GuardConfig.defaultShieldSnapshot().retaliationCap(); dirtyShield = true; knockbackStrength = d.knockbackStrength();
                parryProjectiles = d.parryProjectiles(); parryExplosions = d.parryExplosions();
                perfectExplosionsOnly = d.perfectExplosionsOnly();
                GuardPackets.ShieldSettings shieldDefaults = GuardConfig.defaultShieldSnapshot();
                shieldRegularPush = shieldDefaults.regularPushback(); toolPush = shieldDefaults.weaponPushback();
                dirtyShield = true;
            }
            case 1 -> {
                block = d.block(); reductionPercent = d.reductionPercent();
                guardMovementPercent = d.guardMovementPercent(); blockProjectiles = d.blockProjectiles();
                blockDeflectChance = d.blockDeflectChance(); blockExplosions = d.blockExplosions();
                GuardPackets.ShieldSettings shieldDefaults = GuardConfig.defaultShieldSnapshot();
                toolMaxBlocks = shieldDefaults.toolMaxBlocks(); shieldBreakTicks = shieldDefaults.breakTicks();
                dirtyShield = true;
            }
            case 2 -> { hitParticles = d.hitParticles(); serverHitSounds = d.hitSounds(); }
            case 4 -> {
                allowAnyItem = d.allowAnyItem(); allowUsableItems = d.allowUsableItems();
                consumablePriority = d.consumablePriority();
                cooldownPreventsGuard = GuardConfig.defaultShieldSnapshot().cooldownPreventsGuard();
                allowEmptyHand = GuardConfig.defaultShieldSnapshot().allowEmptyHand();
                randomDualTools = GuardConfig.defaultShieldSnapshot().randomDualTools(); randomShieldWeapon = GuardConfig.defaultShieldSnapshot().randomShieldWeapon(); dirtyShield = true;
                parryWear = d.parryWear(); perfectWear = d.perfectWear(); blockWear = d.blockWear();
                includedItems = d.includedItems(); excludedItems = d.excludedItems();
            }
            case 5 -> {
                fallParry = d.fallParry(); fallPerfectParry = d.fallPerfectParry(); fallLookDown = d.fallLookDown();
                fallBreakBlocks = d.fallBreakBlocks(); fallBlastStrength = d.fallBlastStrength();
                fallLaunchPower = d.fallLaunchPower();
            }
            case 6 -> { damageRules.clear(); dirtyDamageRules = true; }
            case 11 -> {
                int regularPush = shieldRegularPush, weaponPush = toolPush, weaponBlocks = toolMaxBlocks, sharedBreak = shieldBreakTicks, sharedCap = retaliationCap;
                boolean dualRule = randomDualTools, cooldownRule = cooldownPreventsGuard, randomRule = randomShieldWeapon, emptyRule = allowEmptyHand, foodRule = consumablePriority;
                setShieldSettings(GuardConfig.defaultShieldSnapshot());
                shieldRegularPush = regularPush; toolPush = weaponPush; toolMaxBlocks = weaponBlocks; shieldBreakTicks = sharedBreak; retaliationCap = sharedCap;
                randomDualTools = dualRule; cooldownPreventsGuard = cooldownRule; randomShieldWeapon = randomRule; allowEmptyHand = emptyRule; consumablePriority = foodRule;
                shieldItems = d.shieldItems(); dirtyShield = true;
            }
            default -> { return; }
        }
        dirty = dirtyServer = true;
        if (category != 6 && category != 12) dirtyServerRules = true;
    }

    private void save(boolean close) {
        if (operator && (!GuardItemRules.valid(includedItems) || !GuardItemRules.valid(excludedItems) || !GuardItemRules.valid(shieldItems)
            || !GuardItemRules.valid(stunnableBosses))) {
            invalidItemRules = true;
            experimentalSection = false;
            enforceSection = false;
            clientSection = false;
            page = !GuardItemRules.valid(shieldItems) || !GuardItemRules.valid(stunnableBosses) ? 11 : 4;
            itemListsOpen = true;
            firstRow = 0;
            rebuildWidgets();
            if (page == 4) scrollTo(2 + (eligibilityOpen ? 5 : 0) + (durabilityOpen ? 3 : 0));
            return;
        }
        if (operator && (!GuardItemRules.valid(mobHumanoidIds) || mobHumanoidIds.contains("#"))) {
            experimentalSection = true; enforceSection = clientSection = false; page = 12;
            rememberedGroups.put(groupKey(), "Mob Eligibility"); selectRememberedGroup(); firstRow = 0; rebuildWidgets();
            Minecraft.getInstance().player.displayClientMessage(Component.literal("Use comma-separated humanoid entity IDs in Mob Eligibility, without tags."), false);
            return;
        }
        GuardConfig.HUD.set(hud);
        GuardConfig.SHIELD_EFFECTS.set(shieldEffects);
        GuardConfig.MEME_FLASH.set(memeFlash);
        GuardConfig.SPARKS_ENABLED.set(sparksEnabled);
        GuardConfig.SPARK_RING.set(sparkRing);
        GuardConfig.STREAKS_ENABLED.set(streaksEnabled);
        GuardConfig.STREAK_AMOUNT.set(streakAmount); GuardConfig.REGULAR_STREAK_AMOUNT.set(regularStreakAmount); GuardConfig.STREAK_LENGTH.set(streakLength);
        GuardConfig.STREAK_LIFETIME.set(streakLifetime);
        GuardConfig.REGULAR_ORB_LIFETIME.set(regularOrbLifetime);
        GuardConfig.PERFECT_ORB_LIFETIME.set(perfectOrbLifetime);
        GuardConfig.STREAK_DOT_SIZE.set(streakDotSize); GuardConfig.STREAK_DOT_SPACING.set(streakDotSpacing); GuardConfig.REGULAR_STREAK_DOT_SPACING.set(regularStreakDotSpacing); GuardConfig.STREAK_BALLISTIC_CURVE.set(streakBallisticCurve);
        GuardConfig.STREAK_STRETCH_SPEED.set(streakStretchSpeed);
        GuardConfig.PARTICLE_FLASHES_ENABLED.set(particleFlashesEnabled);
        GuardConfig.HITLAG_FRAMES.set(hitlagFrames);
        GuardConfig.IMPACT_FRAMES.set(impactFrames);
        GuardConfig.IMPACT_PERFECT_ONLY.set(impactPerfectOnly);
        GuardConfig.IMPACT_BRIGHTNESS.set(impactBrightness); GuardConfig.IMPACT_CONTRAST.set(impactContrast);
        GuardConfig.IMPACT_EDGES.set(impactEdges); GuardConfig.IMPACT_GRAIN.set(impactGrain);
        GuardConfig.PERFECT_ONLY_HITLAG.set(perfectOnlyHitlag);
        GuardConfig.PERFECT_ONLY_FLASH.set(perfectOnlyFlash);
        GuardConfig.SCREEN_FLASH.set(flashStrength > 0);
        GuardConfig.FLASH_STRENGTH.set(flashStrength);
        GuardConfig.SCREEN_SHAKE.set(shakeStrength > 0);
        GuardConfig.SHAKE_STRENGTH.set(shakeStrength);
        GuardConfig.PREFER_OFFHAND.set(preferOffhand);
        GuardConfig.FIRST_PERSON_ANIMATION.set(firstPersonAnimation); GuardConfig.THIRD_PERSON_ANIMATION.set(thirdPersonAnimation);
        GuardConfig.LOCAL_MASTER_VOLUME.set(masterVolume);
        GuardConfig.LOCAL_PERFECT_VOLUME.set(perfectVolume);
        GuardConfig.LOCAL_PARRY_VOLUME.set(parryVolume);
        GuardConfig.LOCAL_BLOCK_VOLUME.set(blockVolume);
        GuardConfig.REGULAR_SPARK_COUNT.set(regularSparks);
        GuardConfig.PERFECT_SPARK_COUNT.set(perfectSparks);
        GuardConfig.STAR_COUNT.set(stars);
        GuardConfig.SPARK_LIFETIME.set(sparkLifetime);
        GuardConfig.SPARK_LENGTH.set(sparkLength);
        GuardConfig.SPARK_EXPLOSIVENESS.set(sparkExplosiveness);
        GuardConfig.SPARK_REACH.set(sparkReach);
        GuardConfig.REGULAR_ORB_SIZE.set(regularOrbSize);
        GuardConfig.PERFECT_ORB_SIZE.set(perfectOrbSize);
        GuardConfig.REGULAR_ORB_OPACITY.set(regularOrbOpacity);
        GuardConfig.PERFECT_ORB_OPACITY.set(perfectOrbOpacity);
        GuardConfig.IMPACT_CHROMATIC.set(extraClient[50]);
        GuardConfig.MOB_IMPACT_BRIGHTNESS.set(extraClient[51]); GuardConfig.MOB_IMPACT_CONTRAST.set(extraClient[52]); GuardConfig.MOB_IMPACT_EDGES.set(extraClient[53]); GuardConfig.MOB_IMPACT_GRAIN.set(extraClient[54]); GuardConfig.MOB_IMPACT_CHROMATIC.set(extraClient[55]);
        GuardConfig.MOB_IMPACT_ADAPT.set(extraClient[56] != 0); GuardConfig.MOB_IMPACT_INVERT.set(extraClient[57] != 0);
        GuardConfig.CHROMATIC_INTENSITY.set(extraClient[58]); GuardConfig.CHROMATIC_TICKS.set(extraClient[59]); GuardConfig.CHROMATIC_PERFECT_ONLY.set(extraClient[60] != 0); GuardConfig.CHROMATIC_HUD.set(extraClient[61] != 0);
        GuardConfig.STREAK_REVERSE.set(extraClient[62] != 0); GuardConfig.STAGGER_COMPACT.set(extraClient[63] != 0); GuardConfig.STAGGER_HUD_OFFSET.set(extraClient[64]);
        if (dirtyClient) GuardClient.saveClientConfig();
        if (operator && dirtyServerRules) PacketDistributor.sendToServer(new GuardPackets.Save(parry, block, parryDrowningFire, parryStarvation, parryGenericKill, perfect, window, recharge, angle, reductionPercent, followUp, parryReturnPercent, perfectReturnPercent, parryWear, perfectWear, blockWear, serverHitSounds, hitParticles, serverMasterVolume, serverPerfectVolume, serverParryVolume, serverBlockVolume, fallParry, fallPerfectParry, fallLookDown, fallBreakBlocks, fallBlastStrength, fallLaunchPower, parryExplosions, perfectExplosionsOnly, blockExplosions, parryProjectiles, blockProjectiles, knockbackStrength > 0, knockbackStrength, guardMovementPercent, blockDeflectChance, allowAnyItem, allowUsableItems, includedItems, excludedItems, shieldItems, consumablePriority));
        if (operator && dirtyStagger) PacketDistributor.sendToServer(new GuardPackets.SaveStagger(new GuardPackets.StaggerSettings(
            staggerEnabled, staggerCells, staggerParryLoss, staggerPerfectLoss, staggerRestore, staggerNearTicks, staggerFarTicks, staggerRadius, staggerStunTicks, Math.min(staggerCells, staggerExitRecovery))));
        if (operator && dirtyMobs) PacketDistributor.sendToServer(new GuardPackets.SaveMobs(new GuardPackets.MobSettings(
            mobGuard, mobDifficulty, mobBlocking, mobRetaliation, mobParryColor, mobPerfectColor, mobHitsBeforeGuard, mobPerfectTicks, mobParryTicks, mobCounterTicks, mobApproachChance, mobTacticalChance, mobTacticalSeconds, mobTacticalCooldownTicks, mobHumanoidIds, mobMovement, mobApproachDistance, mobApproachSeconds, mobRushChance, mobGearChance)));
        if (operator && dirtyShield) PacketDistributor.sendToServer(new GuardPackets.SaveShield(new GuardPackets.ShieldSettings(
            shieldPerfect, shieldWindow, shieldRechargeTicks, shieldMaxBlocks, toolMaxBlocks, Math.max(recharge, shieldBreakTicks), shieldCone, shieldReach, shieldReturn,
            shieldStun, shieldPerfectPush, shieldRegularPush, toolPush, stunnableBosses, consumablePriority, cooldownPreventsGuard, allowEmptyHand, retaliationCap, randomShieldWeapon, coneSparks, randomDualTools)));
        if (operator && dirtyDamageRules) PacketDistributor.sendToServer(new GuardPackets.SaveDamageRules(
            GuardDamageRules.encodeRules(damageRules)));
        if (operator && dirtyPolicy) PacketDistributor.sendToServer(new GuardPackets.SaveClientPolicy(enforceEnabled, dontEnforceMask,
            GuardClientPreset.encode(enforcedDefaults)));
        if (dirtyClient || dirtyServer) {
            String scope = dirtyClient && dirtyServer ? "Client and Server" : dirtyClient ? "Client" : "Server";
            Minecraft.getInstance().player.displayClientMessage(Component.literal("Mallard Guard configs are saved for the " + scope + "."), false);
        }
        dirty = false;
        dirtyClient = dirtyServer = false;
        dirtyServerRules = dirtyPolicy = dirtyDamageRules = dirtyShield = dirtyMobs = dirtyStagger = false;
        if (close) onClose();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (settingsSearchChanged) {
            settingsSearchChanged = false;
            rebuildWidgets();
            if (settingsSearchBox != null) {
                setFocused(settingsSearchBox);
                settingsSearchBox.setFocused(true);
                settingsSearchBox.setCursorPosition(settingsSearch.length());
            }
        }
        if (damageSearchChanged) {
            damageSearchChanged = false;
            rebuildWidgets();
            if (damageSearchBox != null) {
                setFocused(damageSearchBox);
                damageSearchBox.setFocused(true);
                damageSearchBox.setCursorPosition(damageSearch.length());
            }
        }
        // Blur the game world before drawing either surface or any controls.
        renderBackground(graphics, mouseX, mouseY, delta);
        graphics.fill(0, 0, width, height, 0x3219141E);
        graphics.fill(sidebarLeft - 6, layoutTop - 5, sidebarLeft + sidebarWidth + 7, height - 5, 0x962C2535);
        graphics.fill(sidebarLeft - 6, layoutTop - 5, sidebarLeft + sidebarWidth + 7, layoutTop - 3, WARM_EDGE);
        graphics.fill(sidebarLeft - 6, layoutTop - 5, sidebarLeft - 5, height - 5, 0xFFA88FAE);
        graphics.fill(sidebarLeft + sidebarWidth + 6, layoutTop - 5, sidebarLeft + sidebarWidth + 7, height - 5, 0xFFA88FAE);
        graphics.fill(sidebarLeft - 6, height - 6, sidebarLeft + sidebarWidth + 7, height - 5, 0xFFA88FAE);
        graphics.fill(panelLeft - 8, layoutTop - 5, panelLeft + panelWidth + 14, height - 5, 0x92211B2A);
        graphics.fill(panelLeft - 8, layoutTop - 5, panelLeft + panelWidth + 14, layoutTop - 3, WARM_EDGE);
        graphics.fill(panelLeft - 8, layoutTop - 5, panelLeft - 7, height - 5, 0xFFA88FAE);
        graphics.fill(panelLeft + panelWidth + 13, layoutTop - 5, panelLeft + panelWidth + 14, height - 5, 0xFFA88FAE);
        graphics.fill(panelLeft - 8, height - 6, panelLeft + panelWidth + 14, height - 5, 0xFFA88FAE);
        graphics.fill(contentRight() + 4, rowTop(), contentRight() + 5,
            rowTop() + visibleRows * ROW_STEP - 7, 0xFF76647F);
        // Keep the panel and every widget on the same render layer.
        for (var widget : renderables) if (!(widget instanceof Button)) widget.render(graphics, mouseX, mouseY, delta);
        for (Button button : themedButtons) paintButton(graphics, button, mouseX, mouseY);
        // Draw the scrollbar last so the screen backdrop and widgets cannot wash it out.
        if (maxScroll() > 0) {
            int trackX = contentRight() + 8;
            int trackY = rowTop();
            int trackHeight = visibleRows * ROW_STEP - 7;
            int thumbHeight = Math.max(14, trackHeight * visibleRows / pageRows());
            int thumbY = trackY + firstRow * (trackHeight - thumbHeight) / maxScroll();
            graphics.fill(trackX - 1, trackY, trackX + 7, trackY + trackHeight, 0x77302737);
            graphics.fill(trackX, thumbY, trackX + 6, thumbY + thumbHeight, WARM_EDGE);
            graphics.fill(trackX + 5, thumbY, trackX + 6, thumbY + thumbHeight, 0xFFB29A89);
        }
        graphics.drawString(font, title, sidebarLeft, layoutTop + 8, TEXT);
        graphics.drawString(font, "v" + GuardConfig.version(), sidebarLeft, layoutTop + 21, 0xFFC3B4CA);
        graphics.fill(panelLeft - 11, layoutTop + 31, panelLeft - 10, height - 35, 0xFF907D9A);
        if (!enforceSection && settingsSearch.isBlank()) {
            int[] categories = clientSection ? CLIENT_PAGES : experimentalSection ? EXPERIMENTAL_PAGES : SERVER_PAGES;
            for (int i = sidebarOffset; i < Math.min(categories.length, sidebarOffset + visibleSidebarTabs(categories.length)); i++) {
                if (page == categories[i]) graphics.fill(sidebarLeft + 2, sidebarTop() + (i - sidebarOffset) * 23 + 3,
                    sidebarLeft + 5, sidebarTop() + (i - sidebarOffset) * 23 + 19, 0xFFD8BEAA);
            }
        }
        for (DropdownLabel dropdown : dropdownLabels) {
            boolean hover = mouseX >= panelLeft && mouseX < contentRight()
                && mouseY >= dropdown.y() && mouseY < dropdown.y() + 22;
            if (hover || dropdown.expanded().getAsBoolean()) graphics.fill(panelLeft, dropdown.y(), contentRight(), dropdown.y() + 22,
                hover ? 0x88493E55 : 0x49493E55);
            if (dropdown.expanded().getAsBoolean()) graphics.fill(panelLeft, dropdown.y(), panelLeft + 2, dropdown.y() + 22, WARM_EDGE);
            int dropdownColor = enforceSection && !enforceEnabled ? 0xFF888888 : WARM_EDGE;
            graphics.drawString(font, dropdown.name(), panelLeft + 4, dropdown.y() + 3, dropdownColor);
            graphics.fill(panelLeft + 4, dropdown.y() + 18, contentRight() - 4,
                dropdown.y() + 19, enforceSection && !enforceEnabled ? 0x88777777 : 0xFFD8BEAA);
            graphics.drawString(font, dropdown.expanded().getAsBoolean() ? "▾" : "▸",
                contentRight() - 12, dropdown.y() + 5, dropdownColor);
        }
        for (SettingLabel section : sectionLabels) {
            graphics.drawString(font, section.name(), panelLeft + 4, section.y() + 4, WARM_EDGE);
            graphics.fill(panelLeft + 4, section.y() + 18, contentRight() - 4, section.y() + 19, WARM_EDGE);
        }
        for (SettingLabel setting : settingLabels) {
            String name = setting.name();
            int limit = settingX() - panelLeft - 8;
            while (font.width(name) > limit && name.length() > 1) name = name.substring(0, name.length() - 2) + "…";
            int textColor = setting.enabled() ? TEXT : 0xFF8B8190;
            int separator = setting.name().indexOf(" | ");
            if (!clientSection && page == 6 && separator > 0) {
                String id = setting.name().substring(separator + 3);
                textColor = damageRules.getOrDefault(id, GuardDamageRules.defaultAllowed(id)) ? 0xB6E8B6 : 0xA7A7A7;
            }
            graphics.drawString(font, name, panelLeft, setting.y() + 6, textColor);
        }
        if (!clientSection && invalidItemRules) {
            graphics.drawString(font, "Check the item IDs and commas.", panelLeft, rowTop() - 9, 0xFF6666);
        }
        if (mouseX >= panelLeft && mouseX < settingX() - 5) {
            for (SettingLabel setting : settingLabels) {
                if (mouseY >= setting.y() && mouseY < setting.y() + 20) {
                    graphics.renderTooltip(font, font.split(Component.literal(setting.description()), Math.min(320, width - 24)), mouseX, mouseY);
                    break;
                }
            }
        }
        for (DropdownLabel dropdown : dropdownLabels) {
            if (mouseX >= panelLeft && mouseX < contentRight()
                && mouseY >= dropdown.y() && mouseY < dropdown.y() + 22) {
                graphics.renderTooltip(font, font.split(Component.literal(dropdown.description()), Math.min(320, width - 24)), mouseX, mouseY);
                break;
            }
        }
        for (Button button : themedButtons) {
            if (button.isMouseOver(mouseX, mouseY)) {
                graphics.renderTooltip(font, font.split(Component.literal(buttonTips.get(button)), Math.min(320, width - 24)), mouseX, mouseY);
                break;
            }
        }
        for (var entry : widgetTips.entrySet()) if (!(entry.getKey() instanceof Button)
            && entry.getKey().isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(font, font.split(Component.literal(entry.getValue()), Math.min(320, width - 24)), mouseX, mouseY);
            break;
        }
    }

    private void paintButton(GuiGraphics graphics, Button button, int mouseX, int mouseY) {
        int x = button.getX(), y = button.getY(), w = button.getWidth(), h = button.getHeight();
        boolean hover = button.isMouseOver(mouseX, mouseY) && button.active;
        boolean selected = !button.active && buttonTips.get(button).startsWith("Open ");
        int edge = selected ? WARM_EDGE : hover ? 0xFFC3A5BE : 0xFF76647F;
        boolean red = redButtons.contains(button);
        int body = red ? hover ? 0xFF753E50 : 0xFF552B3C : selected ? 0x98493E55 : hover ? 0xA45C4D68 : button.active ? 0x84372D42 : 0x69292431;
        graphics.fill(x, y, x + w, y + h, edge);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, body);
        if (hover) graphics.fill(x + 2, y + 2, x + w - 2, y + 3, 0x88F5E1F3);
        if (selected) graphics.fill(x + 1, y + 1, x + 4, y + h - 1, WARM_EDGE);
        String label = button.getMessage().getString();
        while (font.width(label) > w - 8 && label.length() > 1) label = label.substring(0, label.length() - 2) + "…";
        graphics.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, button.active || selected ? TEXT : 0xFF978C9E);
    }

    private void scrollTo(int row) {
        int next = Math.clamp(row, 0, maxScroll());
        if (next != firstRow) {
            firstRow = next;
            rememberLocation();
            rebuildWidgets();
        }
    }

    private void scrollAt(double mouseY) {
        int trackHeight = visibleRows * ROW_STEP - 7;
        int thumbHeight = Math.max(14, trackHeight * visibleRows / pageRows());
        double fraction = (mouseY - rowTop() - thumbHeight / 2.0) / Math.max(1, trackHeight - thumbHeight);
        scrollTo((int) Math.round(Math.clamp(fraction, 0.0, 1.0) * maxScroll()));
    }

    private void selectSection(int section) {
        rememberLocation();
        settingsSearch = "";
        settingsSearchChanged = false;
        if (experimentalSection) { experimentalPage = page; experimentalRow = firstRow; }
        else if (enforceSection) enforceRow = firstRow;
        else if (clientSection) { clientPage = page; clientRow = firstRow; }
        else { serverPage = page; serverRow = firstRow; }
        experimentalSection = section == 3;
        enforceSection = section == 2;
        clientSection = section == 0;
        page = experimentalSection ? experimentalPage : enforceSection ? 8 : clientSection ? clientPage : serverPage;
        if (!enforceSection) {
            int[] categories = clientSection ? CLIENT_PAGES : experimentalSection ? EXPERIMENTAL_PAGES : SERVER_PAGES;
            sidebarOffset = 0;
            for (int i = 0; i < categories.length; i++) if (categories[i] == page) {
                sidebarOffset = Math.max(0, i - visibleSidebarTabs(categories.length) + 1);
                break;
            }
        }
        firstRow = experimentalSection ? experimentalRow : enforceSection ? enforceRow : clientSection ? clientRow : serverRow;
        selectRememberedGroup();
        rememberLocation();
        rebuildWidgets();
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!enforceSection && mouseX >= sidebarLeft && mouseX < sidebarLeft + sidebarWidth
            && mouseY >= sidebarTop() && mouseY < height - 5) {
            int count = clientSection ? CLIENT_TABS.length : SERVER_TABS.length;
            int next = Math.clamp(sidebarOffset - (int) Math.signum(scrollY), 0, count - visibleSidebarTabs(count));
            if (next != sidebarOffset) { sidebarOffset = next; rebuildWidgets(); }
            return true;
        }
        if (mouseX >= panelLeft && mouseX <= panelLeft + panelWidth + 18
            && mouseY >= rowTop() && mouseY < rowTop() + visibleRows * ROW_STEP && maxScroll() > 0) {
            scrollTo(firstRow - (int) Math.signum(scrollY) * Math.max(1, (int) Math.abs(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && maxScroll() > 0 && mouseX >= contentRight() + 6
            && mouseX <= contentRight() + 17 && mouseY >= rowTop()
            && mouseY <= rowTop() + visibleRows * ROW_STEP) {
            draggingScroll = true;
            scrollAt(mouseY);
            return true;
        }
        if (button == 0 && enforceSection && !enforceEnabled && mouseX >= panelLeft && mouseX < contentRight())
            return super.mouseClicked(mouseX, mouseY, button);
        if (button == 0 && mouseX >= panelLeft && mouseX < contentRight()) {
            for (DropdownLabel dropdown : dropdownLabels) {
                if (mouseY >= dropdown.y() && mouseY < dropdown.y() + 22) {
                    if (dropdown.expanded().getAsBoolean()) return true;
                    rememberedGroups.put(groupKey(), dropdown.name());
                    selectRememberedGroup();
                    firstRow = 0;
                    rememberLocation();
                    rebuildWidgets();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScroll && button == 0) { scrollAt(mouseY); return true; }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingScroll && button == 0) { draggingScroll = false; return true; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public void onClose() {
        rememberLocation();
        if (!dirty) {
            Minecraft.getInstance().setScreen(parent);
            return;
        }
        Minecraft.getInstance().setScreen(new ConfirmScreen(discard -> {
            if (discard) Minecraft.getInstance().setScreen(parent);
            else Minecraft.getInstance().setScreen(this);
        }, Component.literal("Unsaved changes"), Component.literal("Leave without saving your Mallard Guard settings?"),
            Component.literal("Discard changes"), Component.literal("Keep editing")));
    }

    private interface Change { void accept(int value); }

    private record SettingLabel(String name, String description, int y, boolean enabled) {}
    private record DropdownLabel(String name, int y, BooleanSupplier expanded, String description) {}

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
            setMessage(Component.literal((divisor == 1 ? Integer.toString(shown) : String.format(java.util.Locale.ROOT, divisor == 10 ? "%.1f" : "%.2f", shown / (double) divisor)) + suffix));
        }

        @Override protected void applyValue() {
            int selected = min + (int) Math.round(value * (max - min));
            int step = suffix.equals("%") && divisor == 1 && !name.contains("dot spacing")
                ? name.contains("volume") || name.contains("chance") || name.contains("retaliation")
                    || name.contains("pushback") || name.contains("movement") ? 5 : 10 : 1;
            selected = Math.clamp((int) Math.round(selected / (double) step) * step, min, max);
            value = (selected - min) / (double) (max - min);
            change.accept(selected);
        }

        @Override public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean hover = active && isHoveredOrFocused();
            int edge = !active ? 0xFF665B6A : hover ? 0xFFE7D2BF : 0xFFA88FAE;
            graphics.fill(x, y, x + w, y + h, edge);
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, !active ? 0x69292431 : 0x84372D42);
            if (hover) graphics.fill(x + 2, y + 2, x + w - 2, y + 3, 0x99EFE0F2);
            int trackX = x + 6, trackW = Math.max(1, w - 12);
            int filled = Math.clamp((int) Math.round(value * trackW), 0, trackW);
            graphics.fill(trackX, y + h - 6, trackX + trackW, y + h - 3, 0xFF65576C);
            graphics.fill(trackX, y + h - 6, trackX + filled, y + h - 3, active ? 0xFFE4BFCF : 0xFF857682);
            graphics.fill(trackX + filled - 2, y + h - 8, trackX + filled + 3, y + h - 1, edge);
            graphics.fill(trackX + filled - 1, y + h - 7, trackX + filled + 2, y + h - 2, 0xFF493E55);
            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), x + w / 2, y + 2,
                active ? 0xFFF5F0F6 : 0xFF978C9E);
        }
    }
}
