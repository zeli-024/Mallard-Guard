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
import net.minecraft.client.gui.components.Tooltip;
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
    private int page;
    private int clientPage = 7, serverPage, clientRow, serverRow, enforceRow;
    private boolean clientSection = true;
    private boolean enforceSection, enforceEnabled, enforceParticlesOpen, enforceHudOpen, enforceAudioOpen, enforceItemsOpen;
    private int dontEnforceMask;
    private final int[] enforcedDefaults;
    private int activeClientCategory;
    private int layoutTop, panelLeft, panelWidth, firstRow, visibleRows, sidebarLeft, sidebarWidth, controlIndex;
    private boolean draggingScroll;
    private boolean dirty;
    private boolean dirtyClient, dirtyServer, dirtyServerRules, dirtyPolicy;
    private boolean dirtyDamageRules;
    private boolean dirtyShield;
    private final LinkedHashMap<String, Boolean> damageRules = new LinkedHashMap<>();
    private final List<String> recentHits = new ArrayList<>();
    private final List<String> damageCatalog = new ArrayList<>();
    private String damageSearch = "";
    private boolean damageSearchChanged;
    private EditBox damageSearchBox;
    private final Map<String, String> sourceMods = new HashMap<>();
    private boolean sparkOpen, sphericalFlashOpen, shieldOpen, screenFlashOpen, cameraFeedbackOpen, animationOpen;
    private boolean timingOpen, retaliationOpen, pushbackOpen, parryProjectileOpen, parryExplosionOpen;
    private boolean guardOpen, blockProjectileOpen, blockExplosionOpen;
    private boolean eligibilityOpen, durabilityOpen, itemListsOpen;
    private boolean shieldTimingOpen, shieldPerfectOpen, shieldIdsOpen;
    private boolean commonDamageOpen, recentDamageOpen, otherDamageOpen;
    private final List<DropdownLabel> dropdownLabels = new ArrayList<>();
    private final List<SettingLabel> sectionLabels = new ArrayList<>();
    private static final int ROW_STEP = 25;
    private final List<SettingLabel> settingLabels = new ArrayList<>();
    private static final String[] SERVER_TABS = {"Parry", "Fall Impact", "Blocking", "Shields", "Damage Types", "Shared Effects", "Equipment"};
    private static final int[] SERVER_PAGES = {0, 5, 1, 11, 6, 2, 4};
    private static final String[] CLIENT_TABS = {"Particles", "Visuals", "Audio", "Hand Selection"};
    private static final int[] CLIENT_PAGES = {7, 2, 3, 4};
    private boolean parry, block, parryDrowningFire, parryStarvation, parryGenericKill, hud, shieldEffects, memeFlash;
    private boolean hitParticles, sparksEnabled, sparkRing, particleFlashesEnabled;
    private int flashStrength, shakeStrength, hitlagFrames;
    private int masterVolume, perfectVolume, parryVolume, blockVolume;
    private boolean serverHitSounds;
    private final int serverMasterVolume, serverPerfectVolume, serverParryVolume, serverBlockVolume;
    private int perfect, window, recharge, angle, reductionPercent;
    private int followUp, parryReturnPercent, perfectReturnPercent, parryWear, perfectWear, blockWear;
    private boolean fallParry, fallPerfectParry, fallLookDown, fallBreakBlocks, parryExplosions, perfectExplosionsOnly, blockExplosions;
    private boolean parryProjectiles, blockProjectiles;
    private int fallBlastStrength, fallLaunchPower, knockbackStrength;
    private int guardMovementPercent, blockDeflectChance;
    private int regularOrbSize, perfectOrbSize, regularOrbOpacity, perfectOrbOpacity;
    private int regularSparks, perfectSparks, stars, sparkLifetime, sparkLength, sparkExplosiveness;
    private boolean allowAnyItem, allowUsableItems, preferOffhand, firstPersonAnimation;
    private boolean consumablePriority, cooldownPreventsGuard;
    private int shieldPerfect, shieldWindow, shieldRechargeTicks, shieldMaxBlocks, toolMaxBlocks, shieldBreakTicks, shieldCone, shieldReach;
    private int shieldReturn, shieldStun, shieldPerfectPush, shieldRegularPush, toolPush;
    private String stunnableBosses;
    private String includedItems, excludedItems, shieldItems;
    private boolean invalidItemRules;

    public GuardConfigScreen(GuardPackets.Settings settings, Screen parent) {
        super(Component.literal("Mallard Guard"));
        this.parent = parent;
        this.page = 7;
        operator = settings.operator();
        GuardPackets.ClientPolicy policy = GuardClient.currentPolicy();
        enforceEnabled = policy.enabled();
        enforceParticlesOpen = enforceHudOpen = enforceAudioOpen = enforceItemsOpen = false;
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
        hitParticles = settings.hitParticles();
        sparksEnabled = GuardConfig.SPARKS_ENABLED.get();
        sparkRing = GuardConfig.SPARK_RING.get();
        particleFlashesEnabled = GuardConfig.PARTICLE_FLASHES_ENABLED.get();
        masterVolume = GuardConfig.LOCAL_MASTER_VOLUME.get(); perfectVolume = GuardConfig.LOCAL_PERFECT_VOLUME.get();
        parryVolume = GuardConfig.LOCAL_PARRY_VOLUME.get(); blockVolume = GuardConfig.LOCAL_BLOCK_VOLUME.get();
        flashStrength = GuardConfig.SCREEN_FLASH.get() ? GuardConfig.FLASH_STRENGTH.get() : 0;
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
        sparkLength = GuardConfig.SPARK_LENGTH.get(); sparkExplosiveness = GuardConfig.SPARK_EXPLOSIVENESS.get();
        allowUsableItems = settings.allowUsableItems();
        allowAnyItem = settings.allowAnyItem();
        preferOffhand = GuardConfig.PREFER_OFFHAND.get();
        firstPersonAnimation = GuardConfig.FIRST_PERSON_ANIMATION.get();
        includedItems = settings.includedItems(); excludedItems = settings.excludedItems(); shieldItems = settings.shieldItems();
        consumablePriority = settings.consumablePriority();
        setShieldSettings(GuardClient.currentShieldSettings());
    }

    private void setShieldSettings(GuardPackets.ShieldSettings settings) {
        shieldPerfect = settings.perfect(); shieldWindow = settings.window(); shieldRechargeTicks = settings.rechargeTicks(); shieldMaxBlocks = settings.maxBlocks(); toolMaxBlocks = settings.toolMaxBlocks();
        shieldBreakTicks = settings.breakTicks(); shieldCone = settings.cone(); shieldReach = settings.reach(); shieldReturn = settings.retaliation();
        cooldownPreventsGuard = settings.cooldownPreventsGuard();
        shieldStun = settings.stunTicks(); shieldPerfectPush = settings.perfectPushback();
        shieldRegularPush = settings.regularPushback(); toolPush = settings.weaponPushback();
        stunnableBosses = settings.stunnableBosses();
    }

    public void updateShieldSettings(GuardPackets.ShieldSettings settings) {
        if (!dirtyShield) { setShieldSettings(settings); consumablePriority = settings.consumablePriority(); rebuildWidgets(); }
    }

    @Override protected void init() {
        controlIndex = 0;
        damageSearchBox = null;
        settingLabels.clear();
        dropdownLabels.clear();
        sectionLabels.clear();
        sidebarLeft = 12;
        sidebarWidth = Math.min(128, Math.max(88, width / 4));
        panelLeft = sidebarLeft + sidebarWidth + 20;
        panelWidth = Math.max(96, Math.min(520, width - panelLeft - 25));
        layoutTop = 14;
        visibleRows = Math.max(1, Math.min(pageRows(), (height - rowTop() - 70) / ROW_STEP));
        firstRow = Math.clamp(firstRow, 0, maxScroll());
        activeClientCategory = switch (page) {
            case 7, 8 -> GuardClientPreset.PARTICLES;
            case 2, 9, 10 -> GuardClientPreset.HUD;
            case 3 -> GuardClientPreset.AUDIO;
            case 4 -> GuardClientPreset.ITEMS;
            default -> 0;
        };
        Button clientTab = addRenderableWidget(Button.builder(Component.literal("Client"), b -> selectSection(0))
            .bounds(sidebarLeft, layoutTop + 32, sidebarWidth, 22).build());
        Button serverTab = addRenderableWidget(Button.builder(Component.literal("Server"), b -> selectSection(1))
            .bounds(sidebarLeft, layoutTop + 60, sidebarWidth, 22).build());
        Button enforceTab = addRenderableWidget(Button.builder(Component.literal("Enforce"), b -> selectSection(2))
            .bounds(sidebarLeft, layoutTop + 88, sidebarWidth, 22).build());
        clientTab.active = !clientSection || enforceSection;
        serverTab.active = clientSection || enforceSection;
        enforceTab.active = !enforceSection;
        if (enforceSection) {
            toggle("Enforce client settings", () -> enforceEnabled, v -> {
                enforceEnabled = v;
                rebuildWidgets();
            }, true, "Apply these starting values to connected clients and lock categories without a Don't Enforce exception.");
            if (enforceEnabled) buildEnforcedDefaults();
        } else if (!clientSection) {
            if (page == 0) {
            dropdown("Timing", () -> timingOpen, v -> timingOpen = v, "Show or hide timing settings.");
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
            dropdown("Damage Returned", () -> retaliationOpen, v -> retaliationOpen = v, "Show or hide retaliation damage settings.");
            if (retaliationOpen) {
            slider("Parry retaliation", parryReturnPercent, 0, 200, "%", 1, true, v -> parryReturnPercent = v,
                "Damage dealt back on a regular parry, as a percentage of the incoming hit. 0% disables it; 200% doubles it.");
            slider("Perfect retaliation", perfectReturnPercent, 0, 200, "%", 1, true, v -> perfectReturnPercent = v,
                "Damage dealt back on a perfect parry, as a percentage of the incoming hit. 0% disables it; 200% doubles it.");
                        }
            }
            if (page == 0) {
                dropdown("Defender Pushback", () -> pushbackOpen, v -> pushbackOpen = v,
                    "Show or hide the pushback applied to you after a parry.");
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
            dropdown("Projectiles", () -> parryProjectileOpen, v -> parryProjectileOpen = v, "Show or hide projectiles settings.");
            if (parryProjectileOpen) {
            toggle("Projectile parry", () -> parryProjectiles, v -> parryProjectiles = v, true,
                "Let parries deflect projectiles. Regular parries send them in a random direction; perfect parries aim them at the attacker.");
                        }
            }
            if (page == 0) {
            dropdown("Explosions", () -> parryExplosionOpen, v -> parryExplosionOpen = v, "Show or hide explosions settings.");
            if (parryExplosionOpen) {
            toggle("Explosion parry", () -> parryExplosions, v -> { parryExplosions = v; rebuildWidgets(); }, true,
                "Let a timed parry prevent explosion damage.");
            if (parryExplosions) toggle("Explosions: perfect only", () -> perfectExplosionsOnly, v -> perfectExplosionsOnly = v, true,
                "Require perfect timing to parry explosions. Only applies when explosion parrying is enabled.");
                        }
            }
            if (page == 6) buildDamageTypes();
            if (page == 11) {
                dropdown("Shield Timing", () -> shieldTimingOpen, v -> shieldTimingOpen = v,
                    "Show shield parry timing and guard break settings.");
                if (shieldTimingOpen) {
                slider("Shield perfect window", shieldPerfect, 0, 3, " ticks", 1, true, v -> {
                    shieldPerfect = Math.min(v, shieldWindow); dirtyShield = true;
                }, "Perfect parry timing for shields. It must fit within the total shield parry window. 0 disables shield perfect parries.");
                slider("Shield parry window", shieldWindow, 1, 10, " ticks", 1, true, v -> {
                    shieldWindow = v; shieldPerfect = Math.min(shieldPerfect, v); dirtyShield = true;
                }, "Total shield parry time, including its perfect window. Lowering this also limits perfect parry timing. Shield use continues into blocking until you release guard or guard breaks.");
                slider("Shield parry recharge", shieldRechargeTicks, 1, 60, " ticks", 1, true, v -> { shieldRechargeTicks = v; dirtyShield = true; },
                    "Time before another shield parry after releasing guard. Regular shield parries halve it; perfect shield parries reset it. Separate from the guard break penalty.");
                slider("Shield blocks before break", shieldMaxBlocks, 0, 5, "", 1, true, v -> { shieldMaxBlocks = v; dirtyShield = true; },
                    "Successful shield blocks allowed before the guard ends. 0 allows unlimited blocks. A disabled shield still follows vanilla's cooldown.");
                }
                dropdown("Shield Perfect Parry", () -> shieldPerfectOpen, v -> shieldPerfectOpen = v,
                    "Show shield perfect parry cone effects.");
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
                dropdown("Additional Shields and Bosses", () -> shieldIdsOpen, v -> shieldIdsOpen = v,
                    "Show extra shield item IDs and bosses eligible for stun.");
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
            slider("Fall blast", fallBlastStrength, 0, 500, "%", 1, true, v -> fallBlastStrength = v,
                "Size of the blast after parrying a fall of more than ten blocks. 0% disables the blast.");
            slider("Fall launch", fallLaunchPower, 0, 300, "%", 1, true, v -> fallLaunchPower = v,
                "Fall parry launch strength. With Look down enabled, carries current movement; otherwise launches where you look. Perfect parries launch farther. 0% prevents damage without launching.");
            toggle("Fall blast breaks blocks", () -> fallBreakBlocks, v -> fallBreakBlocks = v, true,
                "Allow the blast after a long fall parry to break terrain.");
            toggle("Look down to parry", () -> fallLookDown, v -> fallLookDown = v, true,
                "On: fall parry requires looking at least 40° down; standing still bounces in place and movement keeps its momentum. Off: parry falls while looking anywhere and lunge toward where you look.");
            }
            if (page == 1) {
            dropdown("Guard Stance", () -> guardOpen, v -> guardOpen = v, "Show or hide blocking stance and movement settings.");
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
            dropdown("Projectiles", () -> blockProjectileOpen, v -> blockProjectileOpen = v, "Show or hide projectiles settings.");
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
            dropdown("Explosions", () -> blockExplosionOpen, v -> blockExplosionOpen = v, "Show or hide explosions settings.");
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
            dropdown("Eligibility", () -> eligibilityOpen, v -> eligibilityOpen = v, "Show or hide eligibility settings.");
            if (eligibilityOpen) {
            toggle("Any item / empty hand", () -> allowAnyItem, v -> { allowAnyItem = v; rebuildWidgets(); }, true,
                "Off: recognized weapons and shields plus Extra allowed items. On: every item and empty hands, except Excluded items. The other list stays saved.");
            toggle("Respect item cooldown", () -> cooldownPreventsGuard, v -> { cooldownPreventsGuard = v; dirtyShield = true; }, true,
                "On: an item on cooldown cannot start or continue guarding. Another eligible hand can still guard. Shields always obey their vanilla disable cooldown.");
            toggle("Consumables take priority", () -> consumablePriority, v -> { consumablePriority = v; dirtyShield = true; }, true,
                "When food or a drink is held in either hand, using it takes priority over guard, even with a weapon in the other hand.");
            if (!allowAnyItem) toggle("Allow usable weapons", () -> allowUsableItems, v -> allowUsableItems = v, true,
                "Allow recognized weapons with their own use action to guard in recognized-weapon mode. Extra allowed items can still allow them when this is off.");
                        }
            }
            if (page == 4) {
            dropdown("Durability", () -> durabilityOpen, v -> durabilityOpen = v, "Show or hide durability settings.");
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
            dropdown("Item Lists", () -> itemListsOpen, v -> itemListsOpen = v, "Show or hide item lists settings.");
            if (itemListsOpen) {
            if (!allowAnyItem) editBox("Extra allowed items", includedItems, v -> { includedItems = v; invalidItemRules = false; },
                "Comma-separated item IDs or #item tags added to recognized weapons and shields. Example: minecraft:stick, #minecraft:swords. Default: Empty.");
            else editBox("Excluded items", excludedItems, v -> { excludedItems = v; invalidItemRules = false; },
                "Comma-separated item IDs or #item tags that cannot guard in Any item mode. Empty hands are allowed. Default: Empty.");
                        }
            }
        } else {
            if (page == 7) {
            dropdown("Spark Effects", () -> sparkOpen, v -> sparkOpen = v, "Show or hide spark effects settings.");
            if (sparkOpen) {
                toggle("Sparks", () -> sparksEnabled, v -> { sparksEnabled = v; rebuildWidgets(); }, false,
                    "Show flying sparks and perfect parry stars on this client. Turning off hides the sliders while preserving their values.");
                if (sparksEnabled && hitParticles) {
                    toggle("Ring sparks", () -> sparkRing, v -> sparkRing = v, false,
                        "Off: a rounded spherical spray. On: four shorter diagonal sprays form a compact X. Fall parries always use the X; shield parries arc toward the attacker across 90 degrees.");
                    slider("Parry sparks", regularSparks, 0, 200, "%", 1, false, v -> regularSparks = v,
                        "How many flying sparks regular parries show. 0% hides these sparks.");
                    slider("Perfect sparks", perfectSparks, 0, 200, "%", 1, false, v -> perfectSparks = v,
                        "How many flying sparks perfect parries show. 0% hides these sparks.");
                    slider("Perfect stars", stars, 0, 16, "", 1, false, v -> stars = v,
                        "Number of star-shaped sparks on perfect parries. 0 hides them.");
                    slider("Spark lifetime", sparkLifetime, 25, 200, "%", 1, false, v -> sparkLifetime = v,
                        "How long flying sparks and stars last before disappearing.");
                    slider("Spark length", sparkLength, 50, 200, "%", 1, false, v -> sparkLength = v,
                        "How long each flying spark streak looks while moving.");
                    slider("Spark burst", sparkExplosiveness, 0, 200, "%", 1, false, v -> sparkExplosiveness = v,
                        "How fast sparks and stars fly out from the parry impact.");
                }
                        }
            }
            if (page == 7) {
            dropdown("Spherical Flash", () -> sphericalFlashOpen, v -> sphericalFlashOpen = v, "Show or hide spherical flash settings.");
            if (sphericalFlashOpen) {
                toggle("Spherical flash", () -> particleFlashesEnabled, v -> { particleFlashesEnabled = v; rebuildWidgets(); }, false,
                    "Show bright impact particles on this client. Turning off hides their size and opacity settings without changing their values.");
                if (particleFlashesEnabled && hitParticles) {
                    slider("Parry flash size", regularOrbSize, 0, 150, "%", 1, false, v -> regularOrbSize = v,
                        "Size of the bright flash on a regular parry. 0% hides this flash but keeps the sparks.");
                    slider("Perfect flash size", perfectOrbSize, 0, 150, "%", 1, false, v -> perfectOrbSize = v,
                        "Size of the bright flash on a perfect parry. 0% hides this flash but keeps the sparks.");
                    slider("Parry flash opacity", regularOrbOpacity, 0, 100, "%", 1, false, v -> regularOrbOpacity = v,
                        "How visible the regular parry flash is. 0% hides it.");
                    slider("Perfect flash opacity", perfectOrbOpacity, 0, 100, "%", 1, false, v -> perfectOrbOpacity = v,
                        "How visible the perfect parry flash is. 0% hides it.");
                }
                        }
            }
            if (page == 2) {
            dropdown("Shield Icon", () -> shieldOpen, v -> shieldOpen = v, "Show or hide shield icon settings.");
            if (shieldOpen) {
                toggle("Shield icon", () -> hud, v -> { hud = v; rebuildWidgets(); }, false,
                    "Show the shield by your crosshair while guarding and recharging.");
                if (hud) toggle("Shield reactions", () -> shieldEffects, v -> shieldEffects = v, false,
                    "Glow when ready; expand on parry, with a larger echo on perfect. Weapon and shield blocks share a dark trembling icon and small echoes. Only a guard-breaking block flashes red. Block hits also show a rounded vignette.");
                        }
            }
            if (page == 2) {
            dropdown("Screen Flash", () -> screenFlashOpen, v -> screenFlashOpen = v, "Show or hide screen flash settings.");
            if (screenFlashOpen) {
            slider("Screen flash", flashStrength, 0, 100, "%", 1, false, v -> flashStrength = v,
                "Strength of the brief white screen flash after parries. 0% disables it. Blocks never trigger it.");
            toggle("Meme flash", () -> memeFlash, v -> memeFlash = v, false,
                "Show one of four full-screen images on parries. After hitlag, the white flash draws over it; the image holds 0.5 seconds, then fades over 0.2 seconds. The separate meme clip starts with your parry sound and may outlast the image. Plankton is brighter. Blocks never trigger it.");
                        }
            }
            if (page == 2) {
            dropdown("Camera Feedback", () -> cameraFeedbackOpen, v -> cameraFeedbackOpen = v, "Show or hide camera feedback settings.");
            if (cameraFeedbackOpen) {
                slider("Camera shake", shakeStrength, 0, 100, "%", 1, false, v -> shakeStrength = v,
                    "Strength of camera shake after a hit. Blocking shakes the screen the most. 0% disables it.");
                slider("Hitlag frames", hitlagFrames, 0, 8, " frames", 1, false, v -> hitlagFrames = v,
                    "Hold the captured screen for this many frames at 60 FPS timing, so high FPS will not shorten it. Your white flash and parry sound follow the freeze. Nearby players hear it immediately. 0 disables the freeze.");
                        }
            }
            if (page == 2) {
                dropdown("Hand Animation", () -> animationOpen, v -> animationOpen = v,
                    "Show or hide the first-person guard pose setting.");
                if (animationOpen) toggle("First-person guard pose", () -> firstPersonAnimation,
                    v -> firstPersonAnimation = v, false,
                    "Animate the selected guarding hand. Shields retain their normal use animation. Disable if another mod animates your hands.");
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
                "When both hands can guard, choose the offhand item for parries, blocks, and durability wear. The animation follows the chosen hand; shields use vanilla animation.");
            }
        }

        if (!enforceSection) {
            String[] tabs = clientSection ? CLIENT_TABS : SERVER_TABS;
            int[] pages = clientSection ? CLIENT_PAGES : SERVER_PAGES;
            int sidebarY = sidebarTop();
            int sidebarStep = sidebarStep(tabs.length);
            for (int i = 0; i < tabs.length; i++) {
                final int selected = pages[i];
                int buttonY = sidebarY;
                Button tab = addRenderableWidget(Button.builder(Component.literal(tabs[i]), b -> {
                    page = selected;
                    firstRow = 0;
                    rebuildWidgets();
                }).bounds(sidebarLeft + 8, buttonY, sidebarWidth - 16, Math.min(21, sidebarStep - 2)).build());
                tab.active = page != selected;
                tip(tab, "Open " + tabs[i] + " settings.");
                sidebarY += sidebarStep(clientSection ? CLIENT_PAGES.length : SERVER_PAGES.length);
            }
        }

        int actionWidth = (panelWidth - 20) / 3;
        Button resetPage = addRenderableWidget(Button.builder(Component.literal("Reset Page"), b -> reset(false))
            .bounds(panelLeft, height - 56, (panelWidth - 10) / 2, 20).build());
        Button resetSection = addRenderableWidget(Button.builder(Component.literal("Reset All"), b -> reset(true))
            .bounds(panelLeft + (panelWidth - 10) / 2 + 10, height - 56, (panelWidth - 10) / 2, 20).build());
        resetPage.active = clientSection ? !GuardClient.clientCategoryLocked(activeClientCategory) : operator;
        resetSection.active = clientSection || operator;
        tip(resetPage, "Restore the defaults for this page. Press Apply to save them.");
        tip(resetSection, "Restore all defaults in the current Client, Server, or Enforce section. Locked client categories stay locked. Press Apply to save.");
        addRenderableWidget(Button.builder(Component.literal("Apply & Close"), b -> save(true))
            .bounds(panelLeft, height - 32, actionWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> save(false))
            .bounds(panelLeft + actionWidth + 10, height - 32, actionWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
            .bounds(panelLeft + (actionWidth + 10) * 2, height - 32, actionWidth, 20).build());
    }

    private int categoryRows(int category) {
        if (clientSection) return switch (category) {
            case 2 -> 4 + (shieldOpen ? hud ? 2 : 1 : 0) + (screenFlashOpen ? 2 : 0) + (cameraFeedbackOpen ? 2 : 0) + (animationOpen ? 1 : 0);
            case 7 -> 2 + (sparkOpen ? 1 + (sparksEnabled && hitParticles ? 7 : 0) : 0)
                + (sphericalFlashOpen ? 1 + (particleFlashesEnabled && hitParticles ? 4 : 0) : 0);
            case 3 -> 4;
            default -> 1;
        };
        return switch (category) {
            case 0 -> 5 + (timingOpen ? 6 : 0) + (retaliationOpen ? 2 : 0) + (pushbackOpen ? 3 : 0)
                + (parryProjectileOpen ? 1 : 0) + (parryExplosionOpen ? parryExplosions ? 2 : 1 : 0)
                ;
            case 6 -> 3 + (commonDamageOpen ? GuardDamageRules.BUILTINS.size() : 0)
                + (recentDamageOpen ? Math.max(1, recentHits.size()) : 0)
                + (otherDamageOpen ? 1 + Math.max(1, filteredDamageTypes().size()) : 0);
            case 11 -> 3 + (shieldTimingOpen ? 4 : 0) + (shieldPerfectOpen ? 5 : 0) + (shieldIdsOpen ? 2 : 0);
            case 5 -> 5;
            case 1 -> 1 + (guardOpen ? block ? 5 : 2 : 0) + (block ? 2 + (blockProjectileOpen ? 2 : 0)
                + (blockExplosionOpen ? 1 : 0) : 0);
            case 2 -> 2;
            case 4 -> 3 + (eligibilityOpen ? allowAnyItem ? 3 : 4 : 0) + (durabilityOpen ? 3 : 0) + (itemListsOpen ? 1 : 0);
            default -> 1;
        };
    }

    private int pageRows() {
        if (enforceSection) return 1 + (enforceEnabled ? 4 + (enforceParticlesOpen ? 14 : 0)
            + (enforceHudOpen ? 8 : 0) + (enforceAudioOpen ? 5 : 0) + (enforceItemsOpen ? 2 : 0) : 0);
        return categoryRows(page);
    }

    private int maxScroll() { return Math.max(0, pageRows() - visibleRows); }
    private int rowTop() { return layoutTop + 43; }
    private int sidebarTop() { return layoutTop + 122; }
    private int sidebarStep(int count) { return Math.max(19, Math.min(29, (height - sidebarTop() - 8) / count)); }

    private int columnWidth() { return (panelWidth - 14) / 2; }
    private int columnX(int column) { return panelLeft + column * (columnWidth() + 14); }
    private int settingX() { return panelLeft + (panelWidth * 53 / 100); }
    private int settingWidth() { return panelLeft + panelWidth - settingX(); }
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
        dropdown("Common Damage Types", () -> commonDamageOpen, v -> commonDamageOpen = v,
            "Show the built-in damage types and their parry rules.");
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
        dropdown("Recent Hits", () -> recentDamageOpen, v -> recentDamageOpen = v,
            "Show up to ten damage types that recently hurt players.");
        if (recentDamageOpen) {
        if (recentHits.isEmpty()) emptyList();
        for (String id : recentHits) {
            int position = controlIndex++;
            if (visible(position)) settingLabel(position, id, sourceTip(id,
                "Recently hurt a player. Find it in the list below to change its parry setting."), true);
        }
        }
        dropdown("Other Damage Sources", () -> otherDamageOpen, v -> otherDamageOpen = v,
            "Search all damage types found at runtime, including modded sources.");
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

    private void dropdown(String name, BooleanSupplier expanded, Consumer<Boolean> change, String description) {
        int position = controlIndex++;
        if (!visible(position)) return;
        dropdownLabels.add(new DropdownLabel(name, row(position), expanded, change, description));
    }

    private void buildEnforcedDefaults() {
        dropdown("Particles", () -> enforceParticlesOpen, v -> enforceParticlesOpen = v,
            "Set the server's starting spark and spherical flash values.");
        if (enforceParticlesOpen) {
            dontEnforce(GuardClientPreset.PARTICLES);
            presetToggle("Sparks", 22);
            presetToggle("Ring sparks", 25);
            presetSlider("Parry sparks", 16, 0, 200, "%");
            presetSlider("Perfect sparks", 17, 0, 200, "%");
            presetSlider("Perfect stars", 18, 0, 16, "");
            presetSlider("Spark lifetime", 19, 25, 200, "%");
            presetSlider("Spark length", 20, 50, 200, "%");
            presetSlider("Spark burst", 21, 0, 200, "%");
            presetToggle("Spherical flash", 23);
            presetSlider("Parry flash size", 7, 0, 150, "%");
            presetSlider("Perfect flash size", 8, 0, 150, "%");
            presetSlider("Parry flash opacity", 9, 0, 100, "%");
            presetSlider("Perfect flash opacity", 10, 0, 100, "%");
        }
        dropdown("Visuals", () -> enforceHudOpen, v -> enforceHudOpen = v,
            "Set the server's starting HUD and screen feedback values.");
        if (enforceHudOpen) {
            dontEnforce(GuardClientPreset.HUD);
            presetToggle("Shield icon", 0);
            presetToggle("Shield reactions", 1);
            presetToggle("Screen flash", 2);
            presetToggle("Meme flash", 24);
            presetSlider("Screen flash strength", 4, 0, 100, "%");
            presetSlider("Camera shake", 5, 0, 100, "%");
            presetSlider("Hitlag frames", 3, 0, 8, " frames");
            presetToggle("First-person guard pose", 11);
        }
        dropdown("Audio", () -> enforceAudioOpen, v -> enforceAudioOpen = v,
            "Set the server's starting local playback volumes.");
        if (enforceAudioOpen) {
            dontEnforce(GuardClientPreset.AUDIO);
            presetSlider("Master volume", 12, 0, 200, "%");
            presetSlider("Perfect volume", 14, 0, 200, "%");
            presetSlider("Parry volume", 13, 0, 200, "%");
            presetSlider("Block volume", 15, 0, 200, "%");
        }
        dropdown("Hand Selection", () -> enforceItemsOpen, v -> enforceItemsOpen = v,
            "Set which hand guards when both hands hold eligible items.");
        if (enforceItemsOpen) {
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
            "Starting client value set by the server. Locked for players unless this category has Don't Enforce enabled.");
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
        if (enforceSection && name.equals("Screen flash")) return "Off";
        if (enforceSection) server = false;
        if (server) return switch (name) {
            case "Parrying", "Blocking", "Projectile parry", "Explosion parry", "Fall parry", "Fall blast breaks blocks",
                 "Look down to parry", "Block projectiles", "Block explosions", "Allow usable weapons", "Respect item cooldown", "Sparks and flashes", "Hit sounds" -> "On";
            case "Fall damage", "Wall collision" -> "On";
            case "Explosions: perfect only", "Any item / empty hand", "Drowning", "Standing in fire", "Burning", "Lava", "Hot floor", "Campfire", "Starvation", "Wither", "Poison / Instant Damage", "Indirect magic", "Vanilla /kill" -> "Off";
            case "Consumables take priority" -> "On";
            case "Shield perfect window" -> "2 ticks";
            case "Shield parry window" -> "8 ticks";
            case "Shield blocks before break", "Weapon blocks before break" -> "3";
            case "Shield parry recharge" -> "30 ticks (1.5 seconds)";
            case "Guard break recharge" -> "100 ticks (5 seconds)";
            case "Cone half-angle" -> "90°";
            case "Cone reach" -> "5 blocks";
            case "Shield retaliation" -> "10%";
            case "Stun duration" -> "30 ticks";
            case "Cone pushback", "Weapon attacker pushback" -> "35%";
            case "Shield attacker pushback" -> "85%";
            case "Perfect window" -> "2 ticks";
            case "Parry window" -> "6 ticks";
            case "Recharge" -> "10 ticks";
            case "Facing angle" -> "180°";
            case "Follow-up parries" -> "3 ticks";
            case "Parry retaliation" -> "25%";
            case "Perfect retaliation", "Damage reduction" -> "50%";
            case "Perfect fall parry" -> "Off";
            case "Fall launch" -> "10%";
            case "Defender pushback" -> "150%";
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
            case "Sparks", "Spherical flash", "Shield icon", "Shield reactions", "First-person guard pose" -> "On";
            case "Meme flash", "Ring sparks" -> "Off";
            case "Offhand guard priority" -> "Off";
            case "Master volume" -> "100%";
            case "Spark burst" -> "200%";
            case "Perfect volume" -> "130%";
            case "Parry sparks", "Perfect sparks" -> "100%";
            case "Perfect flash opacity" -> "35%";
            case "Parry volume", "Block volume" -> "65%";
            case "Spark length" -> "50%";
            case "Perfect flash size" -> "30%";
            case "Spark lifetime" -> "70%";
            case "Perfect stars" -> "2";
            case "Parry flash size" -> "20%";
            case "Parry flash opacity" -> "35%";
            case "Camera shake" -> "60%";
            case "Screen flash" -> "Off";
            case "Screen flash strength" -> "0%";
            case "Hitlag frames" -> "0 frames";
            default -> "Off";
        };
    }

    private void toggle(String name, BooleanSupplier current,
                        Consumer<Boolean> change, boolean server, String description) {
        int position = controlIndex++;
        if (!visible(position)) return;
        String tooltip = scope(server, description + (description.contains("Default:") ? "" : " Default: " + defaultText(name, server) + "."));
        settingLabel(position, name, tooltip, editable(server));
        boolean damageToggle = server && !enforceSection && page == 6;
        Button button = addRenderableWidget(Button.builder(damageToggle ? damageStatus(current.getAsBoolean()) : label(current.getAsBoolean()), b -> {
            dirty = true;
            if (server) {
                dirtyServer = true;
                if (enforceSection) dirtyPolicy = true; else dirtyServerRules = true;
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
        if (!visible(position)) return;
        String tooltip = scope(server, description + " Default: " + defaultText(name, server) + ".");
        settingLabel(position, name, tooltip, editable(server));
        Slider control = addRenderableWidget(new Slider(settingX(), row(position), settingWidth(),
            name, current, min, max, suffix, value -> {
                dirty = true;
                if (server) {
                    dirtyServer = true;
                    if (enforceSection) dirtyPolicy = true; else dirtyServerRules = true;
                } else dirtyClient = true;
                change.accept(value);
            }, divisor));
        control.active = editable(server);
        tip(control, tooltip);
    }

    private void editBox(String name, String initial, Consumer<String> update, String description) {
        int position = controlIndex++;
        if (!visible(position)) return;
        String tooltip = scope(true, description + (description.contains("Default:") ? "" : " Default: " + defaultText(name, true) + "."));
        settingLabel(position, name, tooltip, operator);
        EditBox box = addRenderableWidget(new EditBox(font, settingX(), row(position), settingWidth(), 20,
            Component.literal(name)));
        box.setMaxLength(1024);
        box.setValue(initial);
        box.setResponder(value -> { dirty = true; dirtyServer = true; dirtyServerRules = true; update.accept(value); });
        box.setEditable(operator);
        box.active = operator;
        tip(box, tooltip);
    }

    private static void tip(AbstractWidget widget, String description) {
        widget.setTooltip(Tooltip.create(Component.literal(description)));
    }

    private static Component label(boolean value) {
        return Component.literal(value ? "On" : "Off");
    }

    public void refreshPolicy() {
        if (GuardClient.clientCategoryLocked(GuardClientPreset.PARTICLES)) {
            sparksEnabled = GuardConfig.SPARKS_ENABLED.get();
            sparkRing = GuardConfig.SPARK_RING.get();
            particleFlashesEnabled = GuardConfig.PARTICLE_FLASHES_ENABLED.get();
            regularOrbSize = GuardConfig.REGULAR_ORB_SIZE.get(); perfectOrbSize = GuardConfig.PERFECT_ORB_SIZE.get();
            regularOrbOpacity = GuardConfig.REGULAR_ORB_OPACITY.get(); perfectOrbOpacity = GuardConfig.PERFECT_ORB_OPACITY.get();
            regularSparks = GuardConfig.REGULAR_SPARK_COUNT.get(); perfectSparks = GuardConfig.PERFECT_SPARK_COUNT.get();
            stars = GuardConfig.STAR_COUNT.get(); sparkLifetime = GuardConfig.SPARK_LIFETIME.get();
            sparkLength = GuardConfig.SPARK_LENGTH.get(); sparkExplosiveness = GuardConfig.SPARK_EXPLOSIVENESS.get();
        }
        if (GuardClient.clientCategoryLocked(GuardClientPreset.HUD)) {
            hud = GuardConfig.HUD.get(); shieldEffects = GuardConfig.SHIELD_EFFECTS.get(); memeFlash = GuardConfig.MEME_FLASH.get();
            flashStrength = GuardConfig.SCREEN_FLASH.get() ? GuardConfig.FLASH_STRENGTH.get() : 0;
            shakeStrength = GuardConfig.SCREEN_SHAKE.get() ? GuardConfig.SHAKE_STRENGTH.get() : 0;
            hitlagFrames = GuardConfig.HITLAG_FRAMES.get();
            firstPersonAnimation = GuardConfig.FIRST_PERSON_ANIMATION.get();
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
        if ((enforceSection || !clientSection) && !operator) return;
        if (enforceSection) {
            enforceEnabled = false;
            dontEnforceMask = 0;
            System.arraycopy(GuardClientPreset.DEFAULTS, 0, enforcedDefaults, 0, enforcedDefaults.length);
            dirtyServer = dirtyPolicy = dirty = true;
        } else if (clientSection) {
            if (wholeSection) {
                for (int category : CLIENT_PAGES) if (!GuardClient.clientCategoryLocked(categoryMask(category))) resetClientPage(category);
            } else if (!GuardClient.clientCategoryLocked(categoryMask(page))) resetClientPage(page);
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
            case 3 -> GuardClientPreset.AUDIO;
            default -> GuardClientPreset.ITEMS;
        };
    }

    private void resetClientPage(int category) {
        int[] values = GuardClientPreset.DEFAULTS;
        switch (category) {
            case 7 -> {
                sparksEnabled = values[22] != 0; particleFlashesEnabled = values[23] != 0;
                sparkRing = values[25] != 0;
                regularSparks = values[16]; perfectSparks = values[17]; stars = values[18];
                sparkLifetime = values[19]; sparkLength = values[20]; sparkExplosiveness = values[21];
                regularOrbSize = values[7]; perfectOrbSize = values[8];
                regularOrbOpacity = values[9]; perfectOrbOpacity = values[10];
            }
            case 2 -> {
                hud = values[0] != 0; shieldEffects = values[1] != 0; memeFlash = values[24] != 0;
                flashStrength = values[4]; shakeStrength = values[5];
                hitlagFrames = values[3]; firstPersonAnimation = values[11] != 0;
            }
            case 3 -> { masterVolume = values[12]; parryVolume = values[13];
                perfectVolume = values[14]; blockVolume = values[15]; }
            case 4 -> preferOffhand = values[6] != 0;
            default -> { return; }
        }
        dirty = dirtyClient = true;
    }

    private void resetServerPage(int category, GuardPackets.Settings d) {
        switch (category) {
            case 0 -> {
                parry = d.parry(); perfect = d.perfect(); window = d.window(); recharge = d.recharge();
                angle = d.angle(); followUp = d.followUp(); parryReturnPercent = d.parryReturnPercent();
                perfectReturnPercent = d.perfectReturnPercent(); knockbackStrength = d.knockbackStrength();
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
                cooldownPreventsGuard = GuardConfig.defaultShieldSnapshot().cooldownPreventsGuard(); dirtyShield = true;
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
                int regularPush = shieldRegularPush, weaponPush = toolPush, weaponBlocks = toolMaxBlocks, sharedBreak = shieldBreakTicks;
                boolean cooldownRule = cooldownPreventsGuard;
                setShieldSettings(GuardConfig.defaultShieldSnapshot());
                shieldRegularPush = regularPush; toolPush = weaponPush; toolMaxBlocks = weaponBlocks; shieldBreakTicks = sharedBreak;
                cooldownPreventsGuard = cooldownRule;
                shieldItems = d.shieldItems(); dirtyShield = true;
            }
            default -> { return; }
        }
        dirty = dirtyServer = true;
        if (category != 6) dirtyServerRules = true;
    }

    private void save(boolean close) {
        if (operator && (!GuardItemRules.valid(includedItems) || !GuardItemRules.valid(excludedItems) || !GuardItemRules.valid(shieldItems)
            || !GuardItemRules.valid(stunnableBosses))) {
            invalidItemRules = true;
            enforceSection = false;
            clientSection = false;
            page = !GuardItemRules.valid(shieldItems) || !GuardItemRules.valid(stunnableBosses) ? 11 : 4;
            itemListsOpen = true;
            firstRow = 0;
            rebuildWidgets();
            if (page == 4) scrollTo(2 + (eligibilityOpen ? 4 : 0) + (durabilityOpen ? 3 : 0));
            return;
        }
        GuardConfig.HUD.set(hud);
        GuardConfig.SHIELD_EFFECTS.set(shieldEffects);
        GuardConfig.MEME_FLASH.set(memeFlash);
        GuardConfig.SPARKS_ENABLED.set(sparksEnabled);
        GuardConfig.SPARK_RING.set(sparkRing);
        GuardConfig.PARTICLE_FLASHES_ENABLED.set(particleFlashesEnabled);
        GuardConfig.HITLAG_FRAMES.set(hitlagFrames);
        GuardConfig.SCREEN_FLASH.set(flashStrength > 0);
        GuardConfig.FLASH_STRENGTH.set(flashStrength);
        GuardConfig.SCREEN_SHAKE.set(shakeStrength > 0);
        GuardConfig.SHAKE_STRENGTH.set(shakeStrength);
        GuardConfig.PREFER_OFFHAND.set(preferOffhand);
        GuardConfig.FIRST_PERSON_ANIMATION.set(firstPersonAnimation);
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
        GuardConfig.REGULAR_ORB_SIZE.set(regularOrbSize);
        GuardConfig.PERFECT_ORB_SIZE.set(perfectOrbSize);
        GuardConfig.REGULAR_ORB_OPACITY.set(regularOrbOpacity);
        GuardConfig.PERFECT_ORB_OPACITY.set(perfectOrbOpacity);
        if (dirtyClient) GuardClient.saveClientConfig();
        if (operator && dirtyServerRules) PacketDistributor.sendToServer(new GuardPackets.Save(parry, block, parryDrowningFire, parryStarvation, parryGenericKill, perfect, window, recharge, angle, reductionPercent, followUp, parryReturnPercent, perfectReturnPercent, parryWear, perfectWear, blockWear, serverHitSounds, hitParticles, serverMasterVolume, serverPerfectVolume, serverParryVolume, serverBlockVolume, fallParry, fallPerfectParry, fallLookDown, fallBreakBlocks, fallBlastStrength, fallLaunchPower, parryExplosions, perfectExplosionsOnly, blockExplosions, parryProjectiles, blockProjectiles, knockbackStrength > 0, knockbackStrength, guardMovementPercent, blockDeflectChance, allowAnyItem, allowUsableItems, includedItems, excludedItems, shieldItems, consumablePriority));
        if (operator && dirtyShield) PacketDistributor.sendToServer(new GuardPackets.SaveShield(new GuardPackets.ShieldSettings(
            shieldPerfect, shieldWindow, shieldRechargeTicks, shieldMaxBlocks, toolMaxBlocks, Math.max(recharge, shieldBreakTicks), shieldCone, shieldReach, shieldReturn,
            shieldStun, shieldPerfectPush, shieldRegularPush, toolPush, stunnableBosses, consumablePriority, cooldownPreventsGuard)));
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
        dirtyServerRules = dirtyPolicy = dirtyDamageRules = dirtyShield = false;
        if (close) onClose();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (damageSearchChanged) {
            damageSearchChanged = false;
            rebuildWidgets();
            if (damageSearchBox != null) {
                setFocused(damageSearchBox);
                damageSearchBox.setFocused(true);
                damageSearchBox.setCursorPosition(damageSearch.length());
            }
        }
        // Soft cinematic shading along the screen edges; no dark panels behind the controls.
        int fadeHeight = Math.min(80, height / 3);
        for (int y = 0; y < fadeHeight; y += 2) {
            int topAlpha = 210 * (fadeHeight - y) / fadeHeight;
            int bottomAlpha = 210 * (y + 2) / fadeHeight;
            graphics.fill(0, y, width, Math.min(y + 2, fadeHeight), topAlpha << 24);
            graphics.fill(0, height - fadeHeight + y, width,
                Math.min(height - fadeHeight + y + 2, height), Math.min(210, bottomAlpha) << 24);
        }
        super.render(graphics, mouseX, mouseY, delta);
        // Draw the scrollbar last so the screen backdrop and widgets cannot wash it out.
        if (maxScroll() > 0) {
            int trackX = panelLeft + panelWidth + 7;
            int trackY = rowTop();
            int trackHeight = visibleRows * ROW_STEP - 7;
            int thumbHeight = Math.max(14, trackHeight * visibleRows / pageRows());
            int thumbY = trackY + firstRow * (trackHeight - thumbHeight) / maxScroll();
            graphics.fill(trackX - 1, trackY, trackX + 9, trackY + trackHeight, 0xE0181818);
            graphics.fill(trackX, thumbY, trackX + 8, thumbY + thumbHeight, 0xFFE2E2E2);
        }
        graphics.drawString(font, title, sidebarLeft, layoutTop + 8, 0xFFFFFF);
        graphics.drawString(font, "v" + GuardConfig.version(), sidebarLeft, layoutTop + 21, 0xBEBEBE);
        graphics.fill(panelLeft - 11, layoutTop + 31, panelLeft - 10, height - 35, 0x88D8D8D8);
        if (!enforceSection) {
            int sidebarY = sidebarTop();
            for (int category : clientSection ? CLIENT_PAGES : SERVER_PAGES) {
                if (page == category) graphics.fill(sidebarLeft + 2, sidebarY + 3,
                    sidebarLeft + 5, sidebarY + 19, 0xFFFFFFFF);
                sidebarY += sidebarStep(clientSection ? CLIENT_PAGES.length : SERVER_PAGES.length);
            }
        }
        graphics.drawString(font, currentTitle(), panelLeft,
            rowTop() - 19, 0xFFFFFF);
        if (!operator && (!clientSection || enforceSection)) graphics.drawString(font, "Gray settings require OP", panelLeft,
            rowTop() - 9, 0xAAAAAA);
        for (DropdownLabel dropdown : dropdownLabels) {
            boolean hover = mouseX >= panelLeft && mouseX < panelLeft + panelWidth
                && mouseY >= dropdown.y() && mouseY < dropdown.y() + 22;
            if (hover) graphics.fill(panelLeft, dropdown.y(), panelLeft + panelWidth, dropdown.y() + 22, 0x55FFFFFF);
            graphics.drawString(font, dropdown.name(), panelLeft + 4, dropdown.y() + 3, 0xFFE6C976);
            graphics.fill(panelLeft + 4, dropdown.y() + 18, panelLeft + panelWidth - 16,
                dropdown.y() + 19, 0xCCC4A863);
            graphics.drawString(font, dropdown.expanded().getAsBoolean() ? "▾" : "▸",
                panelLeft + panelWidth - 12, dropdown.y() + 5, 0xFFE6C976);
        }
        for (SettingLabel section : sectionLabels) {
            graphics.drawString(font, section.name(), panelLeft + 4, section.y() + 4, 0xFFE6C976);
            graphics.fill(panelLeft + 4, section.y() + 18, panelLeft + panelWidth - 16, section.y() + 19, 0xCCC4A863);
        }
        for (SettingLabel setting : settingLabels) {
            String name = setting.name();
            int limit = settingX() - panelLeft - 8;
            while (font.width(name) > limit && name.length() > 1) name = name.substring(0, name.length() - 2) + "…";
            int textColor = setting.enabled() ? 0xEEEEEE : 0x777777;
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
            if (mouseX >= panelLeft && mouseX < panelLeft + panelWidth
                && mouseY >= dropdown.y() && mouseY < dropdown.y() + 22) {
                graphics.renderTooltip(font, font.split(Component.literal(dropdown.description()), Math.min(320, width - 24)), mouseX, mouseY);
                break;
            }
        }
    }

    private void scrollTo(int row) {
        int next = Math.clamp(row, 0, maxScroll());
        if (next != firstRow) {
            firstRow = next;
            rebuildWidgets();
        }
    }

    private void scrollAt(double mouseY) {
        int trackHeight = visibleRows * ROW_STEP - 7;
        int thumbHeight = Math.max(14, trackHeight * visibleRows / pageRows());
        double fraction = (mouseY - rowTop() - thumbHeight / 2.0) / Math.max(1, trackHeight - thumbHeight);
        scrollTo((int) Math.round(Math.clamp(fraction, 0.0, 1.0) * maxScroll()));
    }

    private String currentTitle() {
        if (enforceSection) return "Enforce";
        String[] names = clientSection ? CLIENT_TABS : SERVER_TABS;
        int[] pages = clientSection ? CLIENT_PAGES : SERVER_PAGES;
        for (int i = 0; i < pages.length; i++) if (pages[i] == page) return names[i];
        return names[0];
    }

    private void selectSection(int section) {
        if (enforceSection) enforceRow = firstRow;
        else if (clientSection) { clientPage = page; clientRow = firstRow; }
        else { serverPage = page; serverRow = firstRow; }
        enforceSection = section == 2;
        clientSection = section == 0;
        page = enforceSection ? 8 : clientSection ? clientPage : serverPage;
        firstRow = enforceSection ? enforceRow : clientSection ? clientRow : serverRow;
        rebuildWidgets();
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= panelLeft && mouseX <= panelLeft + panelWidth + 18
            && mouseY >= rowTop() && mouseY < rowTop() + visibleRows * ROW_STEP && maxScroll() > 0) {
            scrollTo(firstRow - (int) Math.signum(scrollY) * Math.max(1, (int) Math.abs(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= panelLeft && mouseX < panelLeft + panelWidth) {
            for (DropdownLabel dropdown : dropdownLabels) {
                if (mouseY >= dropdown.y() && mouseY < dropdown.y() + 22) {
                    dropdown.change().accept(!dropdown.expanded().getAsBoolean());
                    rebuildWidgets();
                    return true;
                }
            }
        }
        if (button == 0 && maxScroll() > 0 && mouseX >= panelLeft + panelWidth + 4
            && mouseX <= panelLeft + panelWidth + 17 && mouseY >= rowTop()
            && mouseY <= rowTop() + visibleRows * ROW_STEP) {
            draggingScroll = true;
            scrollAt(mouseY);
            return true;
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
    private record DropdownLabel(String name, int y, BooleanSupplier expanded, Consumer<Boolean> change, String description) {}

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
            change.accept(min + (int) Math.round(value * (max - min)));
        }
    }
}
