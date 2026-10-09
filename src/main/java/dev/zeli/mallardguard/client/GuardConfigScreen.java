package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardParticleConfig;
import dev.zeli.mallardguard.GuardParticleColors;
import dev.zeli.mallardguard.GuardSparkTracerConfig;


import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.GuardShieldReactions;
import dev.zeli.mallardguard.GuardDiagnostics;
import dev.zeli.mallardguard.GuardPoseSettings;
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
import net.minecraft.network.chat.Component;
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
    private static int rememberedClientPage = 7, rememberedServerPage, rememberedSection, rememberedEnforcePage = 8, rememberedDebugPage = 6;
    private static int rememberedClientRow, rememberedServerRow, rememberedEnforceRow, rememberedExperimentalRow, rememberedExperimentalPage = 12;
    private static final Map<String, String> rememberedGroups = new HashMap<>();
    private int page, enforcePage=rememberedEnforcePage, debugPage=rememberedDebugPage;
    private int clientPage = rememberedClientPage, serverPage = rememberedServerPage, clientRow = rememberedClientRow, serverRow = rememberedServerRow, enforceRow = rememberedEnforceRow, experimentalRow = rememberedExperimentalRow, experimentalPage = rememberedExperimentalPage;
    private int sidebarOffset;
    private boolean debugSection = rememberedSection == 4;
    private boolean experimentalSection = rememberedSection == 3;
    private boolean clientSection = rememberedSection == 0;
    private boolean enforceSection = rememberedSection == 2, enforceEnabled, enforceParticlesOpen, enforceHudOpen, enforceAudioOpen, enforcePunchyOpen;
    private int dontEnforceMask;
    private final int[] enforcedDefaults;
    private int activeClientCategory;
    private int layoutTop, panelLeft, panelWidth, firstRow, visibleRows, sidebarLeft, sidebarWidth, controlIndex;
    private boolean draggingScroll;
    private double scrollGrab;
    private boolean dirty;
    private boolean dirtyClient, dirtyServer, dirtyServerRules, dirtyPolicy;
    private boolean dirtyDamageRules;
    private boolean dirtyShield, dirtyMobs;
    private boolean chromaticOpen;
    private final int[] extraClient = GuardClientPreset.readLocal();
    private boolean mobGuard, mobEligibilityOpen, mobColorsOpen, mobEquipmentOpen;
    private String mobWhitelist,mobGearWhitelist,mobGearBlacklist;
    private int mobTracerStartColor,mobTracerMiddleColor,mobTracerEndColor;
    private static final java.util.Set<String> STATE_FIELDS = java.util.Set.of("parryHealing", "parryHealingHearts", "serverMasterVolume", "serverParryVolume", "serverPerfectVolume", "serverBlockVolume", "debugFlags", "allowAnyItem", "allowEmptyHand", "allowUsableItems", "angle", "block", "blockDeflectChance", "blockExplosions", "blockProjectiles", "blockVolume", "blockWear", "consumablePriority", "cooldownPreventsGuard", "damageRules", "dontEnforceMask", "enforceEnabled", "enforcedDefaults", "enforcedPoseData", "excludedItems", "extraClient", "fallBlastStrength", "fallBreakBlocks", "fallLaunchPower", "fallLookDown", "fallParry", "fallPerfectParry", "firstPersonAnimation", "flashStrength", "followUp", "guardMovementPercent", "hitParticles", "hitlagFrames", "hud", "impactBrightness", "impactContrast", "impactEdges", "impactFrames", "impactGrain", "impactPerfectOnly", "includedItems", "invalidItemRules", "knockbackStrength", "masterVolume", "memeFlash", "mobDifficulty", "mobGearChance", "mobGuard", "mobWhitelist", "parry", "parryDrowningFire", "parryExplosions", "parryGenericKill", "parryProjectiles", "parryReturnPercent", "parryStarvation", "parryVolume", "parryWear", "perfect", "perfectExplosionsOnly", "perfectOnlyFlash", "perfectReturnPercent", "perfectVolume", "perfectWear", "forceCrouchOffhand", "parryHandPriority", "shieldParryPriority", "itemBlockCounts", "shieldBlacklist", "recharge", "reductionPercent", "retaliationCap", "serverHitSounds", "shakeStrength", "shieldBreakTicks", "shieldCone", "shieldEffects", "shieldItems", "shieldMaxBlocks", "shieldPerfect", "shieldPerfectPush", "shieldReach", "shieldRechargeTicks", "shieldRegularPush", "shieldReturn", "shieldStun", "shieldWindow", "stunnableBosses", "thirdPersonAnimation", "toolMaxBlocks", "toolPush", "window", "mobGearWhitelist", "mobGearBlacklist", "mobTracerStartColor", "mobTracerMiddleColor", "mobTracerEndColor");
    private SavedState appliedState;
    private static int nextSaveRequest;
    private PendingSave pendingSave;
    private boolean promptAfterSave;
    private record PendingSave(int request, int mask, SavedState submitted, long started, boolean clientSaved, boolean close) {}
    private static final java.util.Set<String> RULES_FIELDS = java.util.Set.of("parryHealing", "parryHealingHearts", "allowAnyItem", "allowUsableItems", "angle", "block", "blockDeflectChance", "blockExplosions", "blockProjectiles", "blockWear", "consumablePriority", "excludedItems", "fallBlastStrength", "fallBreakBlocks", "fallLaunchPower", "fallLookDown", "fallParry", "fallPerfectParry", "followUp", "guardMovementPercent", "hitParticles", "includedItems", "knockbackStrength", "parry", "parryDrowningFire", "parryExplosions", "parryGenericKill", "parryProjectiles", "parryReturnPercent", "parryStarvation", "parryWear", "perfect", "perfectExplosionsOnly", "perfectReturnPercent", "perfectWear", "recharge", "reductionPercent", "serverBlockVolume", "serverHitSounds", "serverMasterVolume", "serverParryVolume", "serverPerfectVolume", "shieldItems", "window");
    private static final java.util.Set<String> MOBS_FIELDS = java.util.Set.of("mobDifficulty", "mobGearChance", "mobGuard", "mobWhitelist", "mobGearWhitelist", "mobGearBlacklist", "mobTracerStartColor", "mobTracerMiddleColor", "mobTracerEndColor");
    private static final java.util.Set<String> SHIELD_FIELDS = java.util.Set.of("allowEmptyHand", "consumablePriority", "cooldownPreventsGuard", "forceCrouchOffhand", "parryHandPriority", "retaliationCap", "itemBlockCounts", "shieldBlacklist", "shieldBreakTicks", "shieldCone", "shieldMaxBlocks", "shieldParryPriority", "shieldPerfect", "shieldPerfectPush", "shieldReach", "shieldRechargeTicks", "shieldRegularPush", "shieldReturn", "shieldStun", "shieldWindow", "stunnableBosses", "toolMaxBlocks", "toolPush");
    private static final java.util.Set<String> DAMAGE_FIELDS = java.util.Set.of("damageRules");
    private static final java.util.Set<String> POLICY_FIELDS = java.util.Set.of("dontEnforceMask", "enforceEnabled", "enforcedDefaults", "enforcedPoseData");

    private static final java.util.List<java.lang.reflect.Field> STATE_FIELDS_REFLECTION=java.util.Arrays.stream(GuardConfigScreen.class.getDeclaredFields()).filter(field->STATE_FIELDS.contains(field.getName())).toList();
    private record SavedState(java.util.Map<java.lang.reflect.Field,Object> fields, GuardPoseManagerScreen.Draft poses) {}
    private SavedState captureState() {
        var result = new java.util.LinkedHashMap<java.lang.reflect.Field,Object>();
        try { for (var field : STATE_FIELDS_REFLECTION) {
            Object value = field.get(this);
            if (value instanceof int[] a) value=a.clone(); else if (value instanceof Map<?,?> m) value=new LinkedHashMap<>(m);
            result.put(field,value);
        } } catch (IllegalAccessException error) { throw new IllegalStateException(error); }
        return new SavedState(result, poseDraft == null ? null : poseDraft.copy());
    }

    private void acceptReceivedValues(SavedState before) {
        if(appliedState==null)return;
        SavedState after=captureState();
        var baseline=new LinkedHashMap<>(appliedState.fields());
        for(var entry:after.fields().entrySet())if(!java.util.Objects.deepEquals(entry.getValue(),before.fields().get(entry.getKey())))baseline.put(entry.getKey(),entry.getValue());
        appliedState=new SavedState(baseline,appliedState.poses());
    }

    @SuppressWarnings("unchecked") private void restoreField(java.lang.reflect.Field field,Object value) {
        try { Object current=field.get(this);if(current instanceof int[] a)System.arraycopy((int[])value,0,a,0,a.length);
            else if(current instanceof Map<?,?> m){((Map<Object,Object>)m).clear();((Map<Object,Object>)m).putAll((Map<Object,Object>)value);}else field.set(this,value);
        }catch(IllegalAccessException error){throw new IllegalStateException(error);}
    }

    boolean applyExternalChanges() {
        save(false);
        if (pendingSave != null || dirty) { Minecraft.getInstance().setScreen(this); return false; }
        return true;
    }
    private void showAppliedPrompt() {
        rebuildWidgets();
        GuardUi.choices(this,"Changes applied. Stay on this screen?","",
            new GuardUi.Choice("Stay",()->Minecraft.getInstance().setScreen(this)),
            new GuardUi.Choice("Close",()->Minecraft.getInstance().setScreen(parent)));
    }
    private void applyPrompt() {
        promptAfterSave = true;
        save(false);
        if (pendingSave == null) {
            if (!dirty) showAppliedPrompt();
            promptAfterSave = false;
        }
    }
    private static int serverGroups(String name) {
        return (RULES_FIELDS.contains(name) ? GuardPackets.SAVE_RULES : 0)
            | (MOBS_FIELDS.contains(name) ? GuardPackets.SAVE_MOBS : 0)
            | (SHIELD_FIELDS.contains(name) ? GuardPackets.SAVE_SHIELD : 0)
            | (DAMAGE_FIELDS.contains(name) ? GuardPackets.SAVE_DAMAGE : 0)
            | (POLICY_FIELDS.contains(name) ? GuardPackets.SAVE_POLICY : 0);
    }
    private void acceptSavedValues(SavedState submitted, int requested, int accepted, boolean clientSaved) {
        var baseline = new LinkedHashMap<>(appliedState.fields());
        SavedState current = captureState();
        for (var entry : submitted.fields().entrySet()) {
            int groups = serverGroups(entry.getKey().getName());
            boolean saved = groups == 0 ? clientSaved : (groups & requested) != 0 && (groups & requested & ~accepted) == 0;
            if (saved) baseline.put(entry.getKey(), groups == 0 ? current.fields().get(entry.getKey()) : entry.getValue());
        }
        appliedState = new SavedState(baseline, clientSaved ? current.poses() : appliedState.poses());
    }
    public void saveResult(GuardPackets.SaveResult result) {
        PendingSave pending = pendingSave;
        if (pending == null || result.request() != pending.request()) return;
        pendingSave = null;
        int accepted = result.accepted() & pending.mask();
        acceptSavedValues(pending.submitted(), pending.mask(), accepted, pending.clientSaved());
        dirtyServerRules = (pending.mask() & GuardPackets.SAVE_RULES & ~accepted) != 0;
        dirtyMobs = (pending.mask() & GuardPackets.SAVE_MOBS & ~accepted) != 0;
        dirtyShield = (pending.mask() & GuardPackets.SAVE_SHIELD & ~accepted) != 0;
        dirtyDamageRules = (pending.mask() & GuardPackets.SAVE_DAMAGE & ~accepted) != 0;
        dirtyPolicy = (pending.mask() & GuardPackets.SAVE_POLICY & ~accepted) != 0;
        dirtyServer = dirtyServerRules || dirtyMobs || dirtyShield || dirtyDamageRules || dirtyPolicy;
        dirtyClient = false;
        dirty = hasChanges();
        if (accepted == pending.mask()) {
            GuardClient.configMessage(Component.literal("Mallard Guard configs are saved for the " + (pending.clientSaved() ? "Client and Server" : "Server") + "."));
            if (pending.close()) Minecraft.getInstance().setScreen(parent);
            else if (promptAfterSave) showAppliedPrompt();
            else rebuildWidgets();
        } else {
            if (pending.clientSaved()) GuardClient.configMessage(Component.literal("Mallard Guard configs are saved for the Client."));
            GuardClient.configMessage(Component.literal("Mallard Guard: " + (result.error().isEmpty() ? "The server rejected some settings. These changes remain unsaved." : result.error())));
            rebuildWidgets();
        }
        promptAfterSave = false;
    }
    @Override public void tick() {
        super.tick();
        if (pendingSave != null && (Minecraft.getInstance().getConnection() == null || System.nanoTime() - pendingSave.started() > 15_000_000_000L)) {
            saveResult(new GuardPackets.SaveResult(pendingSave.request(), 0, "The server did not confirm this save. Server changes remain unsaved; reconnect or try again."));
        }
    }
    private void confirmReset(boolean ignored) {
        GuardUi.choices(this,"Reset settings","Restore defaults. Resetting Punchy also clears custom presets and assignments. Apply to save the changes.",
            new GuardUi.Choice("Reset Current Page",()->{reset(false);Minecraft.getInstance().setScreen(this);}),
            new GuardUi.Choice("Reset All Settings",()->{resetAllSettings();Minecraft.getInstance().setScreen(this);}),
            new GuardUi.Choice("Go Back",()->Minecraft.getInstance().setScreen(this)));
    }
    private void resetAllSettings(){
        debugFlags=0;dirtyClient=dirty=true;
        for(int category:CLIENT_PAGES)if(!GuardClient.clientCategoryLocked(categoryMask(category)))resetClientPage(category);
        if(!GuardClient.clientCategoryLocked(GuardClientPreset.PUNCHY))resetClientPage(14);
        if(operator){
            var defaults=GuardConfig.defaultSnapshot(true);
            for(int category:SERVER_PAGES)resetServerPage(category,defaults);
            resetServerPage(6,defaults);
            for(int category:EXPERIMENTAL_PAGES)resetServerPage(category,defaults);
            enforceEnabled=false;dontEnforceMask=0;enforcedPoseData="";
            System.arraycopy(GuardClientPreset.DEFAULTS,0,enforcedDefaults,0,enforcedDefaults.length);
            dirtyServer=dirtyPolicy=dirty=true;
        }
        rebuildWidgets();
    }

    private GuardPoseManagerScreen.Draft poseDraft;
    private String enforcedPoseData="";
    private int mobDifficulty, mobGearChance;
    private final LinkedHashMap<String, Boolean> damageRules = new LinkedHashMap<>();
    private final List<String> recentHits = new ArrayList<>();
    private final List<String> damageCatalog = new ArrayList<>();
    private EditBox settingsSearchBox;
    private String settingsSearch = "";
    private boolean settingsSearchChanged;
    private final Map<String, Integer> visibleSettingRows = new HashMap<>();
    private final Map<String, String> sourceMods = new HashMap<>();
    private boolean sparkTracersOpen, dotStreaksOpen, animatedOpen, shieldOpen, screenFlashOpen, cameraFeedbackOpen, animationOpen;
    private boolean healingOpen, centerImpactOpen;
    private boolean parryHealing;
    private int parryHealingHearts;
    private boolean timingOpen, retaliationOpen, pushbackOpen, parryProjectileOpen, parryExplosionOpen;
    private boolean guardOpen, blockProjectileOpen, blockExplosionOpen;
    private boolean eligibilityOpen, priorityOpen, durabilityOpen;
    private boolean shieldTimingOpen, shieldPerfectOpen, shieldIdsOpen;
    private String reactionGroup="Parry";
    private boolean commonDamageOpen, fallOpen, diagnosticOpen;
    private int debugFlags = GuardConfig.DEBUG_CLIENT_FLAGS.get(), totalRows;
    private final Map<Integer, String> changedRows = new HashMap<>();
    private final Map<Integer, String> originalRows = new HashMap<>();
    private final Map<Integer,Runnable> rowRestores=new HashMap<>();
    private final Map<Integer,Button> rowUndoButtons=new HashMap<>();
    private record RowControl(AbstractWidget widget, int x, int width, boolean header) {}
    private final Map<Integer,RowControl> rowControls=new HashMap<>();
    private final Map<Integer, java.util.function.Supplier<String>> rowValues = new HashMap<>();
    private final List<DropdownLabel> dropdownLabels = new ArrayList<>();
    private final List<SettingLabel> sectionLabels = new ArrayList<>();
    private Button logoButton, reportButton;
    private final List<Button> themedButtons = new ArrayList<>();
    private final Map<Button, String> buttonTips = new HashMap<>();
    private final Map<AbstractWidget, String> widgetTips = new HashMap<>();
    private static final int WARM_EDGE = GuardUi.WARM;
    private static final int TEXT = GuardUi.TEXT;
    private static final int ROW_STEP = 25;
    private final List<SettingLabel> settingLabels = new ArrayList<>();
    private static final String[] SERVER_TABS = {"Parrying", "Eligibility", "Blocking", "Shields", "Multiplayer"};
    private static final int[] SERVER_PAGES = {0, 4, 1, 11, 2};
    private static final String[] CLIENT_TABS = {"World Particles", "Screen Effects", "HUD Icons", "Audio Sliders", "Animations"};
    private static final int[] CLIENT_PAGES = {7, 2, 17, 3, 15};
    private static final int[] EXPERIMENTAL_PAGES = {12, 14};
    private static final String[] EXPERIMENTAL_TABS = {"Mob Guard", "Punchy"};
    private static final int[] ENFORCE_PAGES={8,19,20,21,22,23};
    private static final String[] ENFORCE_TABS={"World Particles","Screen Effects","HUD Icons","Audio Sliders","Animations","Punchy"};
    private static final int[] DEBUG_PAGES={6,18};
    private static final String[] DEBUG_TABS={"Damage Sources","Diagnostics"};
    private final java.util.Set<Button> redButtons = new java.util.HashSet<>();
    private boolean parry, block, parryDrowningFire, parryStarvation, parryGenericKill, hud, shieldEffects, memeFlash;
    private boolean hitParticles;
    private boolean perfectOnlyFlash, impactPerfectOnly;
    private int flashStrength, shakeStrength, hitlagFrames, impactFrames;
    private int impactBrightness, impactContrast, impactEdges, impactGrain;
    private int masterVolume, perfectVolume, parryVolume, blockVolume;
    private boolean serverHitSounds;
    private int serverMasterVolume, serverPerfectVolume, serverParryVolume, serverBlockVolume;
    private int perfect, window, recharge, angle, reductionPercent;
    private int followUp, parryReturnPercent, perfectReturnPercent, retaliationCap, parryWear, perfectWear, blockWear;
    private boolean fallParry, fallPerfectParry, fallLookDown, fallBreakBlocks, parryExplosions, perfectExplosionsOnly, blockExplosions;
    private boolean parryProjectiles, blockProjectiles;
    private int fallBlastStrength, fallLaunchPower, knockbackStrength;
    private int guardMovementPercent, blockDeflectChance;
    private boolean allowAnyItem, allowEmptyHand, allowUsableItems, firstPersonAnimation, thirdPersonAnimation;
    private boolean consumablePriority, cooldownPreventsGuard, forceCrouchOffhand;
    private String itemBlockCounts="";
    private int shieldPerfect, shieldWindow, shieldRechargeTicks, shieldMaxBlocks, toolMaxBlocks, shieldBreakTicks, shieldCone, shieldReach;
    private int shieldReturn, shieldStun, shieldPerfectPush, shieldRegularPush, toolPush;
    private String stunnableBosses;
    private String includedItems, excludedItems, shieldItems, shieldBlacklist;
    private int parryHandPriority, shieldParryPriority;
    private boolean invalidItemRules;

    public GuardConfigScreen(GuardPackets.Settings settings, Screen parent) {
        super(Component.literal("Mallard Guard"));
        this.parent = parent;
        this.page = debugSection ? debugPage : experimentalSection ? experimentalPage : enforceSection ? enforcePage : clientSection ? clientPage : serverPage;
        this.firstRow = experimentalSection ? experimentalRow : enforceSection ? enforceRow : clientSection ? clientRow : serverRow;
        operator = settings.operator();
        GuardPackets.ClientPolicy policy = GuardClient.currentPolicy();
        enforcedPoseData=policy.animations();
        enforceEnabled = policy.enabled();
        enforceParticlesOpen = enforceHudOpen = enforceAudioOpen = false;
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
        impactFrames = GuardConfig.IMPACT_FRAMES.get();
        impactPerfectOnly = GuardConfig.IMPACT_PERFECT_ONLY.get();
        impactBrightness = GuardConfig.IMPACT_BRIGHTNESS.get(); impactContrast = GuardConfig.IMPACT_CONTRAST.get();
        impactEdges = GuardConfig.IMPACT_EDGES.get(); impactGrain = GuardConfig.IMPACT_GRAIN.get();
        hitParticles = settings.hitParticles();
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
        allowUsableItems = settings.allowUsableItems();
        allowAnyItem = settings.allowAnyItem();

        firstPersonAnimation = GuardConfig.FIRST_PERSON_ANIMATION.get();
        thirdPersonAnimation = GuardConfig.THIRD_PERSON_ANIMATION.get();
        includedItems = settings.includedItems(); excludedItems = settings.excludedItems(); shieldItems = settings.shieldItems();
        consumablePriority = settings.consumablePriority();
        parryHealing = settings.parryHealing(); parryHealingHearts = settings.parryHealingHearts();
        setShieldSettings(GuardClient.currentShieldSettings());
        setMobSettings(GuardClient.currentMobSettings());
        var savedPoses = dev.zeli.mallardguard.GuardPoseLibrary.snapshot();
        if (savedPoses.isEmpty()) savedPoses = java.util.stream.IntStream.range(0,GuardPoseSettings.COUNT).mapToObj(dev.zeli.mallardguard.GuardPoseLibrary::preset).toList();
        poseDraft = new GuardPoseManagerScreen.Draft(savedPoses);poseDraft.enabled=extraClient[65]!=0;
        selectRememberedGroup();
    }

    private void setShieldSettings(GuardPackets.ShieldSettings settings) {
        shieldPerfect = settings.perfect(); shieldWindow = settings.window(); shieldRechargeTicks = settings.rechargeTicks(); shieldMaxBlocks = settings.maxBlocks(); toolMaxBlocks = settings.toolMaxBlocks();
        shieldBreakTicks = settings.breakTicks(); shieldCone = settings.cone(); shieldReach = settings.reach(); shieldReturn = settings.retaliation();
        cooldownPreventsGuard = settings.cooldownPreventsGuard(); allowEmptyHand = settings.allowEmptyHand();
        shieldStun = settings.stunTicks(); shieldPerfectPush = settings.perfectPushback();
        shieldRegularPush = settings.regularPushback(); toolPush = settings.weaponPushback();
        retaliationCap = settings.retaliationCap();
        parryHandPriority = settings.parryHandPriority(); shieldParryPriority = settings.shieldParryPriority(); forceCrouchOffhand = settings.forceCrouchOffhand(); shieldBlacklist = settings.shieldBlacklist(); itemBlockCounts=settings.itemBlockCounts(); stunnableBosses = settings.stunnableBosses();
    }

    private void setMobSettings(GuardPackets.MobSettings data) {
        mobGuard=data.enabled();mobDifficulty=data.difficulty();mobWhitelist=data.whitelist();mobGearChance=data.gearChance();
        mobGearWhitelist=data.gearWhitelist();mobGearBlacklist=data.gearBlacklist();
        mobTracerStartColor=data.tracerStartColor();mobTracerMiddleColor=data.tracerMiddleColor();mobTracerEndColor=data.tracerEndColor();
    }

    public void updateMobSettings(GuardPackets.MobSettings data) {
        if (!dirtyMobs) { SavedState before=captureState();setMobSettings(data);acceptReceivedValues(before);rebuildWidgets(); }
    }

    public void updateShieldSettings(GuardPackets.ShieldSettings settings) {
        if (!dirtyShield) { SavedState before=captureState();setShieldSettings(settings); consumablePriority = settings.consumablePriority();acceptReceivedValues(before);rebuildWidgets(); }
    }

    @Override protected void init() {
        if(appliedState==null)appliedState=captureState();
        controlIndex = 0;
        settingsSearchBox = null;
        visibleSettingRows.clear(); rowValues.clear(); changedRows.clear(); originalRows.clear();rowRestores.clear();rowUndoButtons.clear();rowControls.clear();
        themedButtons.clear(); redButtons.clear();colorPreviews.clear();
        buttonTips.clear();
        widgetTips.clear();
        settingLabels.clear();
        dropdownLabels.clear();
        sectionLabels.clear();
        GuardUiLayout.Panels panels=GuardUiLayout.panels(width);
        sidebarLeft=panels.sidebarLeft();sidebarWidth=panels.sidebarWidth();
        panelLeft=panels.contentLeft();panelWidth=panels.contentWidth();
        layoutTop = Math.min(14, Math.max(6, height / 16));
        visibleRows = Math.max(1, (height - rowTop() - 5) / ROW_STEP);
        activeClientCategory = switch (page) {
            case 7, 8 -> GuardClientPreset.PARTICLES;
            case 2,9,10->GuardClientPreset.SCREEN;
            case 15->GuardClientPreset.ANIMATIONS;
            case 17->GuardClientPreset.HUD;
            case 14 -> GuardClientPreset.PUNCHY;
            case 3 -> GuardClientPreset.AUDIO;
            case 4 -> 0;
            default -> 0;
        };
        boolean compact = height < 260;
        int modeHeight = compact ? 18 : 22;
        int modeTop = layoutTop + 34;
        int currentSection = debugSection ? 4 : experimentalSection ? 3 : enforceSection ? 2 : clientSection ? 0 : 1;
        int sectionArrowWidth=GuardUiLayout.SECTION_ARROW_WIDTH, modeGap=GuardUiLayout.CONTROL_GAP;
        Button previous=addRenderableWidget(GuardUi.builder(Component.literal("<"),b->selectSection((currentSection+4)%5))
            .bounds(sidebarLeft,modeTop,sectionArrowWidth,modeHeight).build());
        Button mode=addRenderableWidget(GuardUi.builder(Component.literal(new String[]{"Client","Server","Enforce","Experimental","Debug"}[currentSection]+" ▾"),
            b->GuardUi.sections(this,currentSection,this::selectSection,b.getX(),b.getY(),b.getWidth(),b.getHeight()))
            .bounds(sidebarLeft+sectionArrowWidth+modeGap,modeTop,sidebarWidth-2*(sectionArrowWidth+modeGap),modeHeight).build());
        Button next=addRenderableWidget(GuardUi.builder(Component.literal(">"),b->selectSection((currentSection+1)%5))
            .bounds(sidebarLeft+sidebarWidth-sectionArrowWidth,modeTop,sectionArrowWidth,modeHeight).build());
        if(experimentalSection)redButtons.add(mode);
        tip(previous,"Previous config section.");tip(next,"Next config section.");
        tip(mode,"Choose a config section from the dropdown. The current section is dimmed; arrows cycle sections.");
        EditBox search = addRenderableWidget(GuardUi.editBox(font,panelLeft+10,layoutTop+2,panelWidth-20,"Search Mallard Guard settings"));
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
                if (!v) enforceParticlesOpen = enforceHudOpen = enforceAudioOpen = enforcePunchyOpen = false;
                else selectRememberedGroup();
                rebuildWidgets();
            }, true, "Apply these starting values to connected clients and lock categories without a Don't Enforce exception.");
            buildEnforcedDefaults();
        } else if (!clientSection) {
            if(debugSection && page==18) buildDiagnostics();
            if (page == 0) {
            featureDivider("Timing", () -> timingOpen, ()->parry, value->parry=value, true, "Enable timed parries and configure their windows.");
            if (timingOpen && parry) {
            sliderBound("Perfect window", () -> perfect, " ticks", 1, true, v -> perfect = Math.min(v,window), "Ticks at the start of a parry that count as perfect. 0 disables perfect parries.");
            sliderBound("Parry window", () -> window, " ticks", 1, true, v -> {window=v;perfect=Math.min(perfect,v);}, "How long a parry can catch a hit, including the perfect window.");
            sliderBound("Recharge", () -> recharge, " ticks", 1, true, v -> recharge = v, "Time before another parry can begin after this attempt.");
            sliderBound("Facing angle", () -> angle, "°", 1, true, v -> angle = v, "Which directions attacks can come from. 180° covers your front half; 360° covers all directions.");
            sliderBound("Follow-up parries", () -> followUp, " ticks", 1, true, v -> followUp = v, "Extra time to catch melee hits after a successful parry. 0 disables follow-ups; follow-up hits do not extend the timer.");
                        }
            }
            if (page == 0) {
            dropdown("Healing", () -> healingOpen, "Players and guarding mobs heal on successful parries. Off by default.");
            if (healingOpen) {
                toggle("Parry Healing", () -> parryHealing, v -> parryHealing = v, true, "Players and guarding mobs heal once per successful parry attempt.\nBlocking and follow-up safety hits do not heal. Default: Off.");
                sliderBound("Parry healing amount", () -> parryHealingHearts, " hearts", 1, true, v -> parryHealingHearts = v, "Perfect parries heal this many hearts; regular parries heal half.\n0 disables healing. Default: 1 heart.");
            }
            dropdown("Damage Returned", () -> retaliationOpen, "Show or hide retaliation damage settings.");
            if (retaliationOpen) {
            sliderBound("Parry retaliation", () -> parryReturnPercent, "%", 1, true, v -> parryReturnPercent = v, "Return this percentage of the parried damage. 0% disables return; 200% doubles it. Pushback has its own setting.");
            sliderBound("Perfect retaliation", () -> perfectReturnPercent, "%", 1, true, v -> perfectReturnPercent = v, "Return this percentage of perfect-parried damage. 0% disables return; 200% doubles it. Pushback has its own setting.");
            sliderBound("Retaliation damage cap", () -> retaliationCap, " damage", 1, true, v -> { retaliationCap = v; dirtyShield = true; }, "Maximum damage returned to each target after the multiplier. 0 removes the cap. Default: 25 damage.");
                        }
            }
            if (page == 0) {
                dropdown("Parry Pushback", () -> pushbackOpen, "Set how parries push you and the attacker independently. Returned damage adds no extra knockback.");
                if (pushbackOpen) {
                    sliderBound("Defender pushback", () -> knockbackStrength, "%", 1, true, v -> knockbackStrength = v, "How strongly parrying pushes you away from the hit. Shield parries push you at half this strength. 0% disables both.");
                    sliderBound("Shield attacker pushback", () -> shieldRegularPush, "%", 1, true, v -> { shieldRegularPush = v; dirtyShield = true; }, "Pushes the attacker away on a regular shield parry. Perfect shield parries use cone pushback instead. 0% disables this pushback.");
                    sliderBound("Weapon attacker pushback", () -> toolPush, "%", 1, true, v -> { toolPush = v; dirtyShield = true; }, "Pushes the attacker away on regular and perfect weapon parries. 0% disables this pushback.");
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

            if (page == 12) {
                toggle("Mob guarding",()->mobGuard,v->{mobGuard=v;dirtyMobs=true;},true,
                    "Let whitelisted mobs guard with eligible weapons or shields.\nEmpty hands and ranged weapons cannot guard. Default: Off.");
                sliderBound("Mob guard difficulty",()->mobDifficulty,"%",1,true,v->{mobDifficulty=v;dirtyMobs=true;},
                    "Scale chance-based guard, rush and repeat-guard decisions.\n100% uses normal chances; 50% halves them; 0% stops new decisions.");
                dropdown("Parry Colors",()->mobColorsOpen,"Mob tracers and their impact share these three colors.");
                if(mobColorsOpen){
                    colorField("Mob Tracer Start Color",()->mobTracerStartColor,v->mobTracerStartColor=v,true,"Color at spawn. Also colors the start of the mob impact.\nDefault: #77433A.");
                    colorField("Mob Tracer Middle Color",()->mobTracerMiddleColor,v->mobTracerMiddleColor=v,true,"Color halfway through fading. Also colors the middle of the impact.\nDefault: #9A342D.");
                    colorField("Mob Tracer End Color",()->mobTracerEndColor,v->mobTracerEndColor=v,true,"Color near disappearance. Also colors the end of the impact.\nDefault: #CFCFCF.");
                }
                dropdown("Spawn Equipment",()->mobEquipmentOpen,"Assign equipment for fresh whitelisted vanilla mobs.\nDifficulty does not affect gear chance.");
                if(mobEquipmentOpen){
                    sliderBound("Mob gear chance",()->mobGearChance,"%",1,true,v->{mobGearChance=v;dirtyMobs=true;},"Chance to generate assigned gear on a fresh mob.\nExisting melee gear and occupied armor slots are preserved. Default: 50%.");
                    mobGearAssignments();
                }
                dropdown("Mob Eligibility",()->mobEligibilityOpen,"Choose which mobs may guard or receive generated gear. Bosses remain excluded.");
                if(mobEligibilityOpen)editBox("Mob Guard Whitelist",mobWhitelist,v->{mobWhitelist=v;dirtyMobs=true;},"Exact entity IDs, separated by commas; empty allows none.\nOnly listed mobs can guard or receive gear. Bosses remain excluded.");
            }
            if (page == 11) {
                dropdown("Shield Timing", () -> shieldTimingOpen, "Show shield parry timing and guard break settings.");
                if (shieldTimingOpen) {
                sliderBound("Shield perfect window", () -> shieldPerfect, " ticks", 1, true, v -> {
                    shieldPerfect = Math.min(v, shieldWindow); dirtyShield = true;
                }, "Perfect parry timing for shields. It must fit within the total shield parry window. 0 disables shield perfect parries.");
                sliderBound("Shield parry window", () -> shieldWindow, " ticks", 1, true, v -> {
                    shieldWindow = v; shieldPerfect = Math.min(shieldPerfect, v); dirtyShield = true;
                }, "Total shield parry time, including its perfect window. Lowering this also limits perfect parry timing. Shield use continues into blocking until you release guard or guard breaks.");
                sliderBound("Shield parry recharge", () -> shieldRechargeTicks, " ticks", 1, true, v -> { shieldRechargeTicks = v; dirtyShield = true; }, "Time before another shield parry after releasing guard. Regular shield parries halve it; perfect shield parries reset it. Separate from the guard break penalty.");
                blockCountAssignments();
                sliderBound("Shield blocks before break", () -> shieldMaxBlocks, "", 1, true, v -> { shieldMaxBlocks = v; dirtyShield = true; }, "Successful shield blocks allowed before the guard ends. 0 allows unlimited blocks. A disabled shield still follows vanilla's cooldown.");
                }
                dropdown("Shield Perfect Parry", () -> shieldPerfectOpen, "Show shield perfect parry cone effects.");
                if (shieldPerfectOpen) {
                sliderBound("Cone half-angle", () -> shieldCone, "°", 1, true, v -> { shieldCone = v; dirtyShield = true; }, "Angle to each side of where you look. 90° covers the whole front half (180° total); 0° disables cone retaliation, stun and pushback.");
                sliderBound("Cone reach", () -> shieldReach, " blocks", 1, true, v -> { shieldReach = v; dirtyShield = true; }, "Maximum distance between your eyes and a target's eyes for perfect shield parry cone effects.");
                sliderBound("Shield retaliation", () -> shieldReturn, "%", 1, true, v -> { shieldReturn = v; dirtyShield = true; }, "Percentage of the parried hit dealt to each target in the cone. 0% disables damage but allows configured stun and pushback.");
                sliderBound("Stun duration", () -> shieldStun, " ticks", 1, true, v -> { shieldStun = v; dirtyShield = true; }, "Stop voluntary movement and attacks after cone pushback. 20 ticks = 1 second; 0 disables stun. Unlisted bosses are immune.");
                sliderBound("Cone pushback", () -> shieldPerfectPush, "%", 1, true, v -> { shieldPerfectPush = v; dirtyShield = true; }, "Pushes targets in the perfect parry cone away. 0% disables pushback.");
                }
                dropdown("Additional Shields and Bosses", () -> shieldIdsOpen, "Show extra shield item IDs and bosses eligible for stun.");
                if (shieldIdsOpen) {
                itemRuleAssignment("Shield Item Whitelist", shieldItems, v -> { shieldItems = v; invalidItemRules = false; dirtyShield = true; },
                    "Assign item IDs, #tags, @mod namespaces, or ID/tag keywords to recognize extra shields. Default: Empty.");
                itemRuleAssignment("Shield Item Blacklist", shieldBlacklist, v -> {shieldBlacklist=v;invalidItemRules=false;dirtyShield=true;}, "Assigned items and matching rules excluded from shield recognition. Overrides tags and whitelist. Default: Empty.");
                editBox("Stunnable bosses", stunnableBosses, v -> { stunnableBosses = v; invalidItemRules = false; dirtyShield = true; },
                    "Comma-separated entity IDs or #entity tags. Listed bosses may be stunned by a shield perfect parry. Other bosses stay immune. Default: Empty.");
                }
            }
            if (page == 0) {
            dropdown("Fall Damage", () -> fallOpen, "Configure fall parries and launch behavior.");
            if(fallOpen) {
            toggle("Perfect fall parry", () -> fallPerfectParry, v -> fallPerfectParry = v, true,
                "On: falls and wall collisions caught during the perfect window count as perfect parries. Off: they count as regular parries even in that window.");
            sliderBound("Fall blast", () -> fallBlastStrength, "%", 1, true, v -> fallBlastStrength = v, "Size of the blast after parrying a fall of more than ten blocks. 0% disables the blast.");
            sliderBound("Fall launch", () -> fallLaunchPower, "%", 1, true, v -> fallLaunchPower = v, "Fall parry launch strength. With Look down enabled, carries current movement; otherwise launches where you look. Perfect parries launch farther. 0% prevents damage without launching.");
            toggle("Fall blast breaks blocks", () -> fallBreakBlocks, v -> fallBreakBlocks = v, true,
                "Allow the blast after a long fall parry to break terrain.");
            toggle("Look down to parry", () -> fallLookDown, v -> fallLookDown = v, true,
                "On requires looking at least 40° down and carries movement momentum. Off allows any aim and launches toward your view.");
            }
            }
            if (page == 1) {
            dropdown("Guard Stance", () -> guardOpen, "Show or hide blocking stance and movement settings.");
            if (guardOpen) {
            toggle("Blocking", () -> block, v -> { block = v; rebuildWidgets(); }, true,
                "Keep weapons and shields guarding past the parry window. Off also lowers shields after successful parries. Guard defaults to Right Click and can be rebound in Controls.");
            if (block) sliderBound("Damage reduction", () -> reductionPercent, "%", 1, true, v -> reductionPercent = v, "Damage prevented by held block. 50% halves the damage; 100% prevents it.");
            if(block)blockCountAssignments();
            if (block) sliderBound("Weapon blocks before break", () -> toolMaxBlocks, "", 1, true, v -> { toolMaxBlocks = v; dirtyShield = true; }, "Successful weapon or empty-hand blocks before guard ends. 0 allows unlimited blocks. Attacking with your weapon remains available after guard breaks.");
            if (block) sliderBound("Guard break recharge", () -> shieldBreakTicks, " ticks", 1, true, v -> { shieldBreakTicks = v; dirtyShield = true; }, "Wait after the last allowed shield or weapon block before guarding again. A broken shield also receives an item cooldown. Minimum matches normal recharge; max 10 seconds.");
            sliderBound("Guard movement", () -> guardMovementPercent, "%", 1, true, v -> guardMovementPercent = v, "Movement speed while guarding, including parry and block. 100% means normal movement speed.");
                        }
            }
            if (page == 1) {
            if (block) {
            dropdown("Projectiles", () -> blockProjectileOpen, "Show or hide projectiles settings.");
            if (blockProjectileOpen) {
            if (block) sliderBound("Block deflect chance", () -> blockDeflectChance, "%", 1, true, v -> blockDeflectChance = v, "Chance that held block sends any incoming projectile in a random direction. 0% disables deflection.");
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
            dropdown("Valid Parry Conditions", () -> eligibilityOpen, "Choose eligible items and guard restrictions.");
            if (eligibilityOpen) {
                toggle("Empty Hand", () -> allowEmptyHand, v -> {allowEmptyHand=v;dirtyShield=true;}, true,
                    "Allow empty-hand guarding only when BOTH hands are completely empty. Held items must qualify separately. Default: Off.");
                choice("Tool Requirement", new String[]{"Default", "Any", "Off"}, () -> allowAnyItem?2:allowUsableItems?1:0,
                    v -> {allowAnyItem=v==2;allowUsableItems=v==1;}, "Default uses tools without a use action; Any includes usable tools. Off allows any held item. Shields/whitelist also qualify; empty hands use their own toggle.");
                itemRuleAssignment("Item Whitelist", includedItems, v -> {includedItems=v;invalidItemRules=false;}, "Assign individual items, #tags, @mod namespaces, or ID/tag keywords. Default: Empty.");
                itemRuleAssignment("Item Blacklist", excludedItems, v -> {excludedItems=v;invalidItemRules=false;}, "Exclude assigned items and matching rules in every mode. Overrides the whitelist. Default: Empty.");
                toggle("Respect Item Cooldown", () -> cooldownPreventsGuard, v -> {cooldownPreventsGuard=v;dirtyShield=true;}, true,
                    "Items on cooldown cannot guard. Disabled shields always obey cooldown. Default: On.");
                toggle("Consumables Take Priority", () -> consumablePriority, v -> {consumablePriority=v;dirtyShield=true;}, true,
                    "Eat or drink before guarding when either hand holds a consumable. Default: On.");
            }
            dropdown("Priority", () -> priorityOpen, "Choose which eligible hand guards.");
            if (priorityOpen) {
                choice("Parry Hand Priority", new String[]{"Random", "Off-Hand", "Main-Hand"}, () -> parryHandPriority,
                    v -> {parryHandPriority=v;dirtyShield=true;}, "Choose the guarding item when both hands qualify. Random never repeats a hand more than twice. Default: Random.");
                choice("Shield Parry Priority", new String[]{"Random", "Shield", "Default"}, () -> shieldParryPriority,
                    v -> {shieldParryPriority=v;dirtyShield=true;}, "Random chooses either eligible hand; Shield prefers shields. Default prefers the main hand. Crouch override takes priority.");
                toggle("Force Crouch Off-Hand", () -> forceCrouchOffhand, v -> {forceCrouchOffhand=v;dirtyShield=true;}, true,
                    "Crouching selects an eligible offhand, overriding both priorities. Otherwise normal selection applies. Default: On.");
            }
            dropdown("Item Durability Wear", () -> durabilityOpen, "Durability consumed by guarded hits.");
            if (durabilityOpen) {
                sliderBound("Parry durability", () -> parryWear, "%", 10, true, v->parryWear=v, "Maximum item durability consumed per regular parry, rounded up. 0% disables wear.");
                sliderBound("Perfect durability", () -> perfectWear, "%", 10, true, v->perfectWear=v, "Maximum item durability consumed per perfect parry, rounded up. 0% disables wear.");
                sliderBound("Block durability", () -> blockWear, "%", 10, true, v->blockWear=v, "Maximum item durability consumed per block, rounded up. Shields use vanilla wear; items without durability are unaffected.");
            }
            }
        } else {
            if (page == 7) {
            featureDivider("Flying Sparks", () -> animatedOpen, () -> extraClient[GuardParticleColors.FLYING_ENABLED]!=0, value -> extraClient[GuardParticleColors.FLYING_ENABLED]=value?1:0, false, "Uneven bursts with animated firework sparks and occasional electric sparks. On/Off preserves your intensity settings.");
            if(animatedOpen) {
                sliderBound("Base Intensity", () -> extraClient[16], "%", 1, false, v -> extraClient[16]=v, "Regular-parry amount. 100% starts with 100 sparks; 0% turns Flying Sparks off completely. Both appearances share the total.");
                sliderBound("Perfect Intensity", () -> extraClient[17], "%", 1, false, v -> extraClient[17]=v, "Additional percentage of Base Intensity on perfect parries. 0% adds nothing; 100% doubles the base. Base Intensity 0 turns this effect off completely.");
                sliderBound("Damage Intensity", () -> extraClient[18], "%", 1, false, v -> extraClient[18]=v, "Increase the amount per damage point: 10% × 5 damage adds 50%. 0% adds nothing. All particles share a 512-head burst cap.");
                particleAppearance(false,GuardParticleColors.BASE,GuardParticleColors.FLYING_SIZE_VARIANCE+1);
            }

            featureDivider("Spark Debris", () -> dotStreaksOpen, () -> extraClient[GuardParticleColors.DEBRIS_ENABLED]!=0, value -> extraClient[GuardParticleColors.DEBRIS_ENABLED]=value?1:0, false, "Small debris sparks with immediate gravity, brief dotted tails, and diminishing bounces. On/Off preserves your settings.");
            if(dotStreaksOpen) {
                sliderBound("Debris Base Intensity", () -> extraClient[19], "%", 1, false, v -> extraClient[19]=v, "Regular-parry amount. 100% starts with 100 debris sparks; 0% turns Spark Debris off completely.");
                sliderBound("Debris Perfect Intensity", () -> extraClient[20], "%", 1, false, v -> extraClient[20]=v, "Additional percentage of Base Intensity on perfect parries. 0% adds nothing; 100% doubles the base. Base Intensity 0 turns this effect off completely.");
                sliderBound("Debris Damage Intensity", () -> extraClient[21], "%", 1, false, v -> extraClient[21]=v, "Increase debris amount per damage point: 10% × 5 damage adds 50%. 0% adds nothing. All three effects share the head budget.");
                particleAppearance(false,GuardParticleColors.DEBRIS_COLOR,GuardParticleColors.DEBRIS_SIZE+1);
            }

            int tracerOffset=GuardSparkTracerConfig.OFFSET;
            featureDivider("Spark Tracers", () -> sparkTracersOpen, () -> extraClient[GuardParticleColors.TRACER_ENABLED]!=0, value -> extraClient[GuardParticleColors.TRACER_ENABLED]=value?1:0, false, "Upright tick-by-tick square trails. On/Off preserves your settings.");
            if(sparkTracersOpen) {
                for(int k:GuardSparkTracerConfig.DISPLAY_ORDER) {
                    final int index=tracerOffset+k;
                    slider(GuardSparkTracerConfig.LABELS[k],()->extraClient[index],0,100,"%",1,false,v->extraClient[index]=v,GuardSparkTracerConfig.TIPS[k]);
                }
                particleAppearance(false,GuardParticleColors.TRACER_START_COLOR,GuardParticleColors.PREVIOUS_LENGTH);
            }
            featureDivider("Center Impact", () -> centerImpactOpen, () -> extraClient[GuardParticleColors.IMPACT_ENABLED]!=0, value -> extraClient[GuardParticleColors.IMPACT_ENABLED]=value?1:0, false, "Six sharp frames disperse into fading arcs. Uses tracer colors. On/Off preserves your settings.");
            if(centerImpactOpen)particleAppearance(false,GuardParticleColors.IMPACT_SIZE,GuardParticleColors.IMPACT_DURATION+2);

            }
            if (page == 17) {
            featureDivider("Shield Icon", () -> shieldOpen, () -> hud, value -> hud=value, false, "Show or hide shield icon settings.");
            if (shieldOpen && hud) {
                if (hud) toggle("Shield reactions", () -> shieldEffects, v -> shieldEffects = v, false,
                    "Enable the individual shield reactions below. Each reaction has its own controls. Default: On.");
                if(hud && shieldEffects) buildShieldReactions(false);
                        }
            }
            if (page == 2) {
            dropdown("Screen Flash", () -> screenFlashOpen, "Show or hide screen flash settings.");
            if (screenFlashOpen) {
            sliderBound("Screen flash", () -> flashStrength, "%", 1, false, v -> flashStrength = v, "Strength of the ordinary white screen flash. 0% disables it; meme flash still uses a full white flash. Blocks never trigger it. ");
            toggle("Perfect only flash", () -> perfectOnlyFlash, v -> perfectOnlyFlash = v, false,
                "Show the ordinary white screen flash only on perfect parries. Meme flash still flashes on either parry. Default: On.");
            toggle("Meme flash", () -> memeFlash, v -> memeFlash = v, false,
                "Show a random image with a full white flash after hitlag. Holds 0.5 seconds, then fades for 0.2 seconds. Default: Off.");
                        }
            }
            if (page == 2) {
            dropdown("Camera Feedback", () -> cameraFeedbackOpen, "Show or hide camera feedback settings.");
            if (cameraFeedbackOpen) {
                sliderBound("Camera shake", () -> shakeStrength, "%", 1, false, v -> shakeStrength = v, "Strength of camera shake after a hit. Blocking shakes the screen the most. 0% disables it.");
                sliderBound("Perfect parry hitlag", () -> hitlagFrames, " frames", 1, false, v -> hitlagFrames = v, "Freeze duration: each frame is 1/60 second. Impact holds for the same duration (minimum 1 frame). Feedback and queued damage follow completion.");
                slider("Regular parry hitlag", () -> extraClient[62], 0, 10, " frames", 1, false, v -> extraClient[62]=v, "Freeze duration for regular parries; each frame is 1/60 second.\n0 disables regular hitlag. Default: 0.");
                toggle("Impact frame", () -> impactFrames > 0, v -> { impactFrames = v ? 1 : 0; rebuildWidgets(); }, false,
                    "Show one freshly captured grayscale image after hitlag. It lasts as long as the configured hitlag, or one frame if hitlag is zero. Default: On.");
                if (impactFrames > 0) {
                    toggle("Perfect only impact", () -> impactPerfectOnly, v -> impactPerfectOnly = v, false,
                        "Show the impact image only on perfect parries. Off lets regular parries show it too. Default: On.");
                    int previewRow = controlIndex++;
                    if (visible(previewRow)) {
                        settingLabel(previewRow, "Live preview", "Tune brightness, contrast, edges, and grain over a captured view. Return to settings and press Apply to save.", true);
                        Button preview = addRenderableWidget(GuardUi.builder(Component.literal("Preview"), b ->
                            openImpactPreview(false)).bounds(settingX(), row(previewRow), settingWidth(), 20).build());
                        tip(preview, "Open the impact frame preview. Changes return here and save only when you press Apply.");
                    }
                }
                        }
            }
            if (page == 15) {
                dropdown("Hand Animation", () -> animationOpen, "Show or hide first-person and third-person guard pose settings.");
                if (animationOpen) {
                    toggle("Third-person guard pose", () -> thirdPersonAnimation, v -> thirdPersonAnimation = v, false,
                        "Show the guarding pose on players and supported humanoid mobs. Off hides it only on your client.");
                    toggle("First-person guard pose", () -> simplePoseDraftEnabled(),
                    v -> firstPersonAnimation = v, false,
                    "Animate weapon guarding with the simple pose. Used as fallback when Punchy is unavailable; compatible Punchy takes priority. Shields animate separately.");
                }
            }
            if (page == 3) {
            sliderBound("Master volume", () -> masterVolume, "%", 1, false, v -> masterVolume = v, "Scale all Mallard Guard sounds you hear. 0% mutes them; above 100% uses louder mixes where available.");
            sliderBound("Perfect volume", () -> perfectVolume, "%", 1, false, v -> perfectVolume = v, "Perfect-parry and associated meme sound volume, multiplied by Master Volume.");
            sliderBound("Parry volume", () -> parryVolume, "%", 1, false, v -> parryVolume = v, "Regular-parry and associated meme sound volume, multiplied by Master Volume.");
            sliderBound("Block volume", () -> blockVolume, "%", 1, false, v -> blockVolume = v, "Block and shield-parry sound volume, multiplied by Master Volume.");
            }
        }

        if (clientSection && page == 2) {
            dropdown("Chromatic Aberration", () -> chromaticOpen, "Animated color separation after hitlag and impact finish. Both parry types use it by default; the impact preview controls a separate static effect.");
            if (chromaticOpen) {
                sliderBound("Chromatic intensity", () -> extraClient[58], "%", 1, false, v -> extraClient[58] = v, "Strength of animated RGB separation. 0 disables it. Default: 200%.");
                sliderBound("Chromatic duration", () -> extraClient[59], " ticks", 1, false, v -> extraClient[59] = v, "How long color separation expands and settles. Duration also controls animation speed. Default: 6 ticks.");
                toggle("Perfect only chromatic", () -> extraClient[60] != 0, v -> extraClient[60] = v ? 1 : 0, false, "On: animate only perfect parries. Off: regular and perfect parries both trigger it. Default: Off.");
                toggle("Chromatic affects HUD", () -> extraClient[61] != 0, v -> extraClient[61] = v ? 1 : 0, false, "On processes the HUD too. Off processes the world before HUD drawing. Config screens stay unaffected. Default: On.");
            }

        }

        if (experimentalSection && page == 14) {
            if (PunchyGuardCompat.installed()) {

            toggle("Punchy Compatibility", () -> extraClient[65] != 0, v -> {extraClient[65]=v?1:0;poseDraft.enabled=v;rebuildWidgets();}, false, "Use Punchy for guard animations when its animation API is compatible, including newer versions. Automatically fall back to the simple pose if integration is unavailable. Default: On.");
            sliderBound("Release delay", () -> extraClient[GuardPoseSettings.RELEASE_DELAY_SLOT], " ms", 1, false, v -> extraClient[GuardPoseSettings.RELEASE_DELAY_SLOT]=v, "Extra hold after hitlag finishes. No extra delay when hitlag does not occur. Applies to every Punchy preset. 0 removes the delay. Default: 50 ms.");
            poseEditor(false);
            }
        }



        {
            String[] tabs = debugSection ? DEBUG_TABS : enforceSection ? ENFORCE_TABS : clientSection ? CLIENT_TABS : experimentalSection ? EXPERIMENTAL_TABS : SERVER_TABS;
            int[] pages = debugSection ? DEBUG_PAGES : enforceSection ? ENFORCE_PAGES : clientSection ? CLIENT_PAGES : experimentalSection ? EXPERIMENTAL_PAGES : SERVER_PAGES;
            int shown = tabs.length;
            sidebarOffset = 0;
            for (int i = sidebarOffset; i < Math.min(tabs.length, sidebarOffset + shown); i++) {
                final int selected = pages[i];
                int buttonY = sidebarTop() + (i - sidebarOffset) * sidebarStep(tabs.length);
                String shortName = tabs[i].equals("Multiplayer") ? "Multiplayer"
                    : tabs[i].equals("Damage Types") ? "Damage" : tabs[i];
                Button tab = addRenderableWidget(GuardUi.builder(Component.literal(shortName), b -> {
                    settingsSearch = "";
                    rememberLocation();
                    page = selected;
                    firstRow = 0;
                    selectRememberedGroup();
                    rememberLocation();
                    rebuildWidgets();
                }).bounds(sidebarLeft + 8, buttonY, sidebarWidth - 16, sidebarStep(tabs.length)-3).build());
                tab.active = page != selected;
                tip(tab, "Open " + tabs[i] + " settings.");
            }

        }

        int iconSize=26;
        Button modPage=addRenderableWidget(new Button(sidebarLeft+1,layoutTop+4,iconSize,iconSize,Component.literal("Mallard Guard on CurseForge"),
            b->GuardUi.openLink(this,"Mallard Guard", "https://www.curseforge.com/minecraft/mc-mods/mallard-guard"),message->message.get()){
            @Override public void renderWidget(net.minecraft.client.gui.GuiGraphics graphics,int mouseX,int mouseY,float delta){
                GuardUi.logo(graphics,getX(),getY(),getWidth());
                if(isHoveredOrFocused())graphics.renderOutline(getX()-1,getY()-1,getWidth()+2,getHeight()+2,GuardUi.WARM);
            }
        });
        logoButton=modPage;
        tip(modPage,"Open Mallard Guard's CurseForge page.");
        Button report=addRenderableWidget(GuardUi.iconButton("github","Issue Tracker",sidebarLeft+sidebarWidth-21,layoutTop+7,18,()->GuardUi.openLink(this,"Issue Tracker","https://github.com/zeli-024/Mallard-Guard/issues")));
        reportButton=report;tip(report,"Open the GitHub issue tracker.");
        Button help=addRenderableWidget(GuardUi.button("?",sidebarLeft+sidebarWidth-45,layoutTop+7,18,18,()->GuardUi.choices(this,"Config Help","EDITING\nClick a category, then a divider to open its controls.\nScroll within the settings list. Hover controls for details.\n\nVALUES\nScroll a slider for precise changes; Shift-scroll moves ten steps.\nYellow marks changed values. Undo restores the saved value.\n\nSAVING\nApply saves your edits. Reset restores defaults in the draft.\nServer settings require operator permission.\nLocked client settings are controlled by the server.",new GuardUi.Choice("Back",()->Minecraft.getInstance().setScreen(this)))));
        tip(help,"Help with navigation, editing and saving.");
        int actionWidth=(sidebarWidth-22)/2, actionHeight=height<220?16:20, actionTop=height-(height<220?43:56), actionBottom=height-(height<220?23:32);
        Button reset=addRenderableWidget(GuardUi.button("Reset",sidebarLeft+8,actionTop,sidebarWidth-16,actionHeight,()->confirmReset(true)));
        reset.active=!(experimentalSection&&page==14&&!PunchyGuardCompat.installed())&&(clientSection||debugSection&&(page==18||operator)||experimentalSection&&!GuardClient.clientCategoryLocked(GuardClientPreset.PUNCHY)||operator);tip(reset,"Reset this page or all editable settings to defaults.");redButtons.add(reset);
        Button apply=addRenderableWidget(GuardUi.button("Apply",sidebarLeft+8,actionBottom,actionWidth,actionHeight,this::applyPrompt));tip(apply,"Save settings, then choose Stay or Close.");
        Button close=addRenderableWidget(GuardUi.button("Close",sidebarLeft+14+actionWidth,actionBottom,actionWidth,actionHeight,this::onClose));tip(close,"Close settings. Unsaved changes require confirmation.");
        if(appliedState!=null){SavedState current=captureState();for(var entry:appliedState.fields().entrySet())restoreField(entry.getKey(),entry.getValue());
            for(var entry:rowValues.entrySet())originalRows.putIfAbsent(entry.getKey(),entry.getValue().get());
            for(var entry:current.fields().entrySet())restoreField(entry.getKey(),entry.getValue());}
        for(var entry:rowRestores.entrySet()){
            int position=entry.getKey();
            boolean header=dropdownLabels.stream().anyMatch(d->d.y()==row(position));
            for(var rendered:renderables)if(rendered instanceof AbstractWidget control
                &&control.getX()>=panelLeft&&control.getY()==row(position)){
                rowControls.put(position,new RowControl(control,control.getX(),control.getWidth(),header));break;
            }
            int undoX=contentRight()-GuardUiLayout.UNDO_SIZE-(header?GuardUiLayout.ARROW_SLOT+GuardUiLayout.CONTROL_GAP:0);
            Button undo=addRenderableWidget(GuardUi.iconButton("undo","Undo",undoX,row(position),GuardUiLayout.UNDO_SIZE,entry.getValue()));
            undo.visible=rowChanged(position);undo.active=undo.visible;
            rowUndoButtons.put(position,undo);tip(undo,"Restore this setting to its last saved value.");
        }
        updateRowLayouts();
        totalRows=controlIndex;
        int clamped=Math.clamp(firstRow,0,maxScroll());
        if(clamped!=firstRow){firstRow=clamped;rebuildWidgets();}




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

    private int pageRows() { return Math.max(1,totalRows); }

    private record SearchEntry(int section, int page, String category, String name) {}

    private void addSearchEntries(List<SearchEntry> entries, int section, int page, String category, String names) {
        for (String name : names.split("\\|")) entries.add(new SearchEntry(section, page, category, name));
    }

    private List<SearchEntry> searchResults() {
        List<SearchEntry> all = new ArrayList<>();
        addSearchEntries(all,3,12,"Mob Guard","Mob guarding|Mob guard difficulty|Parry Colors|Mob Tracer Start Color|Mob Tracer Middle Color|Mob Tracer End Color|Spawn Equipment|Mob gear chance|Mob Gear Assignments|Mob Eligibility|Mob Guard Whitelist");
        addSearchEntries(all, 0, 7, "World Particles", "Shield Forward Bias|Flying Sparks|Base Intensity|Perfect Intensity|Damage Intensity|"+String.join("|",new String[]{"Flying Size","Flying Size Variance","Flying Lifetime","Flying Base Color"})
            +"|Spark Debris|Debris Base Intensity|Debris Perfect Intensity|Debris Damage Intensity|"+String.join("|",new String[]{"Debris Size","Debris Lifetime","Debris Base Color"})
            +"|Spark Tracers|Tracer Base Intensity|Tracer Perfect Intensity|Tracer Damage Intensity|"+String.join("|",new String[]{"Tracer Size","Tracer Lifetime","Tracer Tail Length","Tracer Start Color","Tracer Middle Color","Tracer End Color"})
            +"|Center Impact|Impact Size|Impact Spin Speed|Impact Opacity|Impact Duration");
        addSearchEntries(all, 0, 2, "Screen Effects", "Screen Flash|Screen flash|Perfect only flash|Meme flash|Camera Feedback|Camera shake|Hitlag frames|Perfect only hitlag|Impact frame|Perfect only impact|Live preview|Chromatic Aberration|Chromatic intensity|Chromatic duration|Perfect only chromatic|Chromatic affects HUD");
        addSearchEntries(all,0,17,"HUD Icons","Shield Icon|Shield icon|Shield reactions|Parry|Perfect Parry|Block Hit|Guard Break|Recharge");
        addSearchEntries(all,4,6,"Damage Sources","Damage Types|Recent Hits|Runtime Damage Types");
        addSearchEntries(all,4,18,"Diagnostics","Diagnostic Logging|Guard Events|Damage & Projectiles|Animations|Config & Sync|Mob Guard|Particle Collision Profiling");
        addSearchEntries(all, 0, 15, "Animations", "Hand Animation|Third-person guard pose|First-person guard pose");
        addSearchEntries(all, 0, 3, "Audio Sliders", "Master volume|Perfect volume|Parry volume|Block volume");
        addSearchEntries(all, 1, 0, "Parrying", "Timing|Parrying|Healing|Parry Healing|Parry healing amount|Perfect window|Parry window|Recharge|Facing angle|Follow-up parries|Damage Returned|Parry retaliation|Perfect retaliation|Retaliation damage cap|Parry Pushback|Defender pushback|Shield attacker pushback|Weapon attacker pushback|Projectiles|Projectile parry|Explosions|Explosion parry|Explosions: perfect only");
        addSearchEntries(all, 1, 0, "Parrying", "Fall Damage|Perfect fall parry|Fall blast|Fall launch|Fall blast breaks blocks|Look down to parry");
        addSearchEntries(all, 1, 1, "Blocking", "Guard Stance|Blocking|Damage reduction|Weapon blocks before break|Guard break recharge|Guard movement|Projectiles|Block deflect chance|Block projectiles|Explosions|Block explosions");
        addSearchEntries(all, 1, 11, "Shields", "Shield Timing|Shield perfect window|Shield parry window|Shield parry recharge|Shield blocks before break|Shield Perfect Parry|Cone half-angle|Cone reach|Shield retaliation|Stun duration|Cone pushback|Additional Shields and Bosses|Shield Item Eligibility|Item Block Counts|Stunnable bosses");
        addSearchEntries(all, 4, 6, "Damage Sources", "Common Damage Types|Drowning|Standing in fire|Burning|Lava|Hot floor|Campfire|Starvation|Wither|Poison / Instant Damage|Indirect magic|Fall damage|Wall collision|Vanilla /kill|Damage Types");
        addSearchEntries(all, 1, 2, "Multiplayer", "Sparks and flashes|Hit sounds");
        addSearchEntries(all, 1, 4, "Eligibility", "Valid Parry Conditions|Empty Hand|Tool Requirement|Item Eligibility|Respect Item Cooldown|Consumables Take Priority|Priority|Parry Hand Priority|Shield Parry Priority|Force Crouch Off-Hand|Item Durability Wear|Parry durability|Perfect durability|Block durability");
        addSearchEntries(all, 3, 14, "Punchy", "Punchy Compatibility|Release delay|Parry Animation Stance");
        addSearchEntries(all, 2, 8, "Enforce", "Enforce client settings|World Particles|Screen Effects|Shield Icon|Audio Sliders|Hand Animation|Experimental Punchy|Punchy guard poses|Release delay|Parry Animation Stance|Don't Enforce");
        for (SearchEntry entry : List.copyOf(all)) if (entry.section() == 0)
            all.add(new SearchEntry(2,entry.page()==7?8:entry.page()==2?19:entry.page()==17?20:entry.page()==3?21:22,"Enforce "+entry.category(),entry.name()));
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
            String scopeName = entry.section() == 0 ? "Client" : entry.section() == 1 ? "Server" : entry.section() == 3 ? "Experimental" : entry.section()==4?"Debug":"Enforce";
            Button result = addRenderableWidget(GuardUi.builder(Component.literal(entry.name()), b -> jumpToSetting(entry))
                .bounds(panelLeft + 4, row(position), contentRight() - panelLeft - 8, 20).build());
            tip(result, scopeName + " > " + entry.category() + " > " + entry.name());
        }
    }

    private void jumpToSetting(SearchEntry entry) {
        settingsSearch = "";
        settingsSearchChanged = false;
        debugSection=entry.section()==4;
        experimentalSection = entry.section() == 3;
        enforceSection = entry.section() == 2;
        clientSection = entry.section() == 0;
        page = entry.page();
        if (!enforceSection) {
            int[] categories = debugSection ? DEBUG_PAGES : enforceSection ? ENFORCE_PAGES : clientSection ? CLIENT_PAGES : experimentalSection ? EXPERIMENTAL_PAGES : SERVER_PAGES;
            for (int i = 0; i < categories.length; i++) if (categories[i] == page) {
                sidebarOffset = Math.max(0, i - visibleSidebarTabs(categories.length) + 1);
                break;
            }
        }
        firstRow = 0;
        String[] sections = enforceSection ? new String[] {"World Particles","Screen Effects","Shield Icon","Audio Sliders","Hand Animation","Experimental Punchy"}
            : clientSection ? page == 7 ? new String[] {"Flying Sparks", "Spark Debris", "Spark Tracers", "Center Impact"}
                : page == 17 ? new String[]{"Shield Icon"} : page == 2 ? new String[] {"Screen Flash", "Camera Feedback", "Chromatic Aberration"} : page == 15 ? new String[]{"Hand Animation"} : new String[0]
            : switch (page) {
                case 0 -> new String[] {"Timing", "Healing", "Damage Returned", "Parry Pushback", "Projectiles", "Explosions", "Fall Damage"};
                case 1 -> new String[] {"Guard Stance", "Projectiles", "Explosions"};
                case 4 -> new String[] {"Valid Parry Conditions", "Priority", "Item Durability Wear"};
                case 6 -> new String[] {"Common Damage Types"};
                case 18 -> new String[]{"Diagnostic Logging"};
                case 12 -> new String[] {"Parry Colors", "Spawn Equipment", "Mob Eligibility"};
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
    private int sidebarTop() { return layoutTop + (height < 220 ? 54 : height < 260 ? 58 : 70); }
    private int visibleSidebarTabs(int count) { return count; }
    private int sidebarStep(int count){return Math.min(23,Math.max(10,(height-(height<220?47:60)-sidebarTop())/Math.max(1,count)));}

    private int contentRight() { return panelLeft + panelWidth - 22; }
    private int settingX() { return panelLeft + ((contentRight() - panelLeft) * 53 / 100); }
    private int settingWidth() { return Math.max(12,contentRight() - settingX() - GuardUiLayout.CONTROL_GAP); }
    private boolean visible(int index) { return index >= firstRow && index < firstRow + visibleRows; }
    private int row(int index) { return rowTop() + (index - firstRow) * ROW_STEP; }

    public void updateDamageState(GuardPackets.DamageState state) {
        SavedState before=captureState();
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
        acceptReceivedValues(before);
        if (!clientSection && page == 6) rebuildWidgets();
    }

    public void updateDamageHit(String id) {
        if (!GuardDamageRules.validId(id)) return;
        recentHits.remove(id);
        recentHits.add(0, id);
        while (recentHits.size() > 10) recentHits.remove(recentHits.size() - 1);
        if (!clientSection && page == 6) rebuildWidgets();
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

    private String sourceTip(String id, String description) {
        return description + " Mod: " + modName(id) + ". Damage type: " + id + ".";
    }

    private void buildDamageTypes() {
        buildDamageSources();
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
    }

    private void buildDamageSources() {
        int position=controlIndex++;
        if(visible(position)){
            sectionLabels.add(new SettingLabel("Damage Sources","Review recent hits or search all runtime damage types.",row(position),operator));
            Button open=addRenderableWidget(GuardUi.builder(Component.literal("Damage Types"),b->{

                Minecraft.getInstance().setScreen(new GuardDamageSourcesScreen(this,recentHits,damageCatalog,damageRules,operator,this::modName,rules->{
                    if(!damageRules.equals(rules)){damageRules.clear();damageRules.putAll(rules);dirty=dirtyServer=dirtyDamageRules=true;}
                }));
            }).bounds(settingX(),row(position),settingWidth(),20).build());
            tip(open,"Open ten recent damage sources and the searchable runtime list. Toggles share these server parry rules.");
        }
    }

    private void trackValue(int position,java.util.function.Supplier<String> value){rowValues.put(position,value);}
    private boolean rowChanged(int position){String saved=originalRows.get(position);var current=rowValues.get(position);return saved!=null&&current!=null&&!saved.equals(current.get());}
    private void rowUndo(int position,boolean server,Runnable restore){
        rowRestores.put(position,()->{
            restore.run();dirty=true;
            if(server){dirtyServer=true;if(enforceSection)dirtyPolicy=true;else if(page==12)dirtyMobs=true;else if(page==11)dirtyShield=true;else if(page==6)dirtyDamageRules=true;else dirtyServerRules=true;}else dirtyClient=true;
            rebuildWidgets();
        });
    }
    private void updateRowLayouts(){
        for(var entry:rowUndoButtons.entrySet()){
            boolean changed=rowChanged(entry.getKey());Button undo=entry.getValue();
            undo.visible=changed;undo.active=changed;
            RowControl layout=rowControls.get(entry.getKey());if(layout==null)continue;
            int reserve=changed?GuardUiLayout.UNDO_SIZE+GuardUiLayout.CONTROL_GAP:0;
            layout.widget().setX(layout.x()-(layout.header()?reserve:0));
            layout.widget().setWidth(Math.max(12,layout.width()-(layout.header()?0:reserve)));
        }
        for(ColorPreview preview:colorPreviews){
            int reserve=rowChanged(preview.row())?GuardUiLayout.UNDO_SIZE+GuardUiLayout.CONTROL_GAP:0;
            preview.button().setX(preview.x()-reserve);
        }
    }
    private String originalText(String name,String fallback){
        if(appliedState!=null)for(int i=GuardParticleColors.OFFSET;i<GuardParticleColors.LENGTH;i++)if(GuardParticleColors.colorSlot(i)&&name.equals(GuardParticleColors.LABELS[i-GuardParticleColors.OFFSET]))for(var entry:appliedState.fields().entrySet())if(entry.getKey().getName().equals(enforceSection?"enforcedDefaults":"extraClient"))return String.format("#%06X",((int[])entry.getValue())[i]);
        if(appliedState!=null&&name.startsWith("Mob Tracer ")){
            String colorField=name.equals("Mob Tracer Start Color")?"mobTracerStartColor":name.equals("Mob Tracer Middle Color")?"mobTracerMiddleColor":"mobTracerEndColor";
            for(var entry:appliedState.fields().entrySet())if(entry.getKey().getName().equals(colorField))return String.format("#%06X",((Number)entry.getValue()).intValue());
        }
        String field=switch(name){case "Item Whitelist"->"includedItems";case "Item Blacklist"->"excludedItems";case "Shield Item Whitelist","Extra shields and tags"->"shieldItems";case "Shield Item Blacklist"->"shieldBlacklist";case "Item Block Counts"->"itemBlockCounts";case "Stunnable bosses"->"stunnableBosses";case "Mob Guard Whitelist"->"mobWhitelist";case "Mob Gear Whitelist"->"mobGearWhitelist";case "Mob Gear Blacklist"->"mobGearBlacklist";default->"";};if(appliedState!=null)for(var entry:appliedState.fields().entrySet())if(entry.getKey().getName().equals(field))return String.valueOf(entry.getValue());return fallback;}
    private static boolean sameSetting(java.lang.reflect.Field field,Object current,Object saved){
        if(field.getName().equals("damageRules")&&current instanceof Map<?,?> a&&saved instanceof Map<?,?> b){
            java.util.Set<Object> keys=new java.util.HashSet<>();keys.addAll(a.keySet());keys.addAll(b.keySet());
            for(Object key:keys){Object av=a.containsKey(key)?a.get(key):GuardDamageRules.defaultAllowed((String)key);Object bv=b.containsKey(key)?b.get(key):GuardDamageRules.defaultAllowed((String)key);if(!java.util.Objects.equals(av,bv))return false;}
            return true;
        }
        return java.util.Objects.deepEquals(current,saved);
    }
    private boolean hasChanges(){
        if(appliedState==null)return false;
        SavedState current=captureState();
        for(var entry:current.fields().entrySet())if(!sameSetting(entry.getKey(),entry.getValue(),appliedState.fields().get(entry.getKey())))return true;
        return current.poses()==null ? appliedState.poses()!=null : !current.poses().same(appliedState.poses());
    }
    private void featureDivider(String name,BooleanSupplier expanded,BooleanSupplier enabled,Consumer<Boolean> change,boolean server,String description){
        int position=controlIndex;dropdown(name,expanded,description);
        if(!visible(position))return;
        trackValue(position,()->enabled.getAsBoolean()?"On":"Off");
        if(editable(server))rowUndo(position,server,()->{
            change.accept("On".equals(originalRows.get(position)));
            if(name.equals("Diagnostic Logging")&&appliedState!=null)for(var entry:appliedState.fields().entrySet())if(entry.getKey().getName().equals("debugFlags"))debugFlags=(Integer)entry.getValue();
        });
        int w=Math.min(58,settingWidth());
        Button control=addRenderableWidget(GuardUi.button(enabled.getAsBoolean()?"On":"Off",contentRight()-w-GuardUiLayout.ARROW_SLOT-GuardUiLayout.CONTROL_GAP,row(position),w,18,()->{
            boolean value=!enabled.getAsBoolean();change.accept(value);dirty=true;if(server){dirtyServer=dirtyServerRules=true;}else dirtyClient=true;
            if(java.util.Arrays.asList(GuardShieldReactions.NAMES).contains(name))reactionGroup=value?name:"";else rememberedGroups.put(groupKey(),value?name:"");selectRememberedGroup();rebuildWidgets();
        }));control.active=editable(server);tip(control,scope(server,description));
    }
    private void buildDiagnostics(){
        featureDivider("Diagnostic Logging",()->diagnosticOpen,()->debugFlags!=0,value->debugFlags=value?GuardDiagnostics.DEFAULT_EVENT_GROUPS:0,false,"Write diagnostic events to latest.log. Default: Off. Identical repeated messages are suppressed.");
        if(!diagnosticOpen||debugFlags==0)return;
        String[] names={"Guard Events","Damage & Projectiles","Animations","Config & Sync","Mob Guard","Punchy Reactions","Punchy Hand Transitions","Punchy Item Priority","Particle Collision Profiling"};
        String[] descriptions={"Guard input and server-confirmed stance changes.","Damage results and projectile feedback.","Guard pose selection, timing, and easing.","Config saves and synchronization.","Mob guarding events.","Reaction triggers, peak holds, rendered offsets, and hitlag recovery.","Pose release, empty-offhand lowering, and visibility changes.","When native item use or another Punchy clip takes priority.","Time collision queries for all three particle types. Summaries reach latest.log every 100 game ticks. Profiling adds overhead."};
        for(int i=0;i<names.length;i++){final int mask=1<<i;toggle(names[i],()->(debugFlags&mask)!=0,value->debugFlags=value?debugFlags|mask:debugFlags&~mask,false,descriptions[i]+(i==8?" Default: Off; enable explicitly for a test.":" Event logging only; no continuous frame logs. Default: On when diagnostic logging is enabled."));}
    }
    private void buildShieldReactions(boolean enforced){
        int[] target=enforced?enforcedDefaults:extraClient;
        for(int r=0;r<GuardShieldReactions.NAMES.length;r++){
            int base=GuardShieldReactions.OFFSET+r*GuardShieldReactions.STRIDE;
            String name=GuardShieldReactions.NAMES[r];
            featureDivider(name,()->reactionGroup.equals(name),()->target[base]!=0,value->{target[base]=value?1:0;if(enforced)dirtyServer=dirtyPolicy=true;},enforced,"Animate the "+name.toLowerCase(Locale.ROOT)+" reaction. Default: On.");
            if(!reactionGroup.equals(name)||target[base]==0)continue;
            for(int c=1;c<6;c++){final int index=base+c;if((r==0&&c==4)||(r==4&&(c==2||c==4)))continue;slider(name+" "+GuardShieldReactions.CONTROLS[c],()->target[index],c==5?1:0,GuardShieldReactions.max(r*6+c),c==5?" ticks":"%",1,enforced,value->{target[index]=value;if(enforced)dirtyServer=dirtyPolicy=true;},c==5?"How long the reaction lasts. Default: "+GuardShieldReactions.defaultValue(r*6+c)+" ticks.":"Scale this part of the reaction. 100% preserves its default appearance; 0 disables it. Default: 100%.");}
        }
    }

    private void dropdown(String name, BooleanSupplier expanded, String description) {
        int position = controlIndex++;
        visibleSettingRows.putIfAbsent(name, position);
        if (!visible(position)) return;
        dropdownLabels.add(new DropdownLabel(name, row(position), expanded, tooltipLines(description)));
    }

    private String groupKey() {
        return (debugSection ? "debug" : enforceSection ? "enforce" : experimentalSection ? "experimental" : clientSection ? "client" : "server") + ":" + page;
    }

    private String initialGroup() {
        if(enforceSection)return enforceEnabled?switch(page){case 19->"Screen Effects";case 20->"Shield Icon";case 21->"Audio Sliders";case 22->"Hand Animation";case 23->"Experimental Punchy";default->"World Particles";}:null;
        return switch (page) {
            case 7 -> "Flying Sparks";
            case 2 -> clientSection ? "Screen Flash" : null;
            case 17 -> "Shield Icon";
            case 18 -> "Diagnostic Logging";
            case 15 -> "Hand Animation";
            case 0 -> "Timing";
            case 1 -> "Guard Stance";
            case 4 -> clientSection ? null : "Valid Parry Conditions";
            case 6 -> "Common Damage Types";
            case 12 -> "Parry Colors";
            case 11 -> "Shield Timing";
            default -> null;
        };
    }

    private void selectRememberedGroup() {
        String selected = rememberedGroups.getOrDefault(groupKey(), initialGroup());
        if(page==7 && selected!=null && !selected.isEmpty() && !java.util.Set.of("Spark Tracers","Flying Sparks","Spark Debris","Center Impact").contains(selected)){selected="Spark Tracers";rememberedGroups.put(groupKey(),selected);}
        centerImpactOpen = "Center Impact".equals(selected) && page == 7;
        sparkTracersOpen = "Spark Tracers".equals(selected) && page == 7;
        animatedOpen = "Flying Sparks".equals(selected) && page == 7;
        dotStreaksOpen = "Spark Debris".equals(selected) && page == 7;
        shieldOpen = "Shield Icon".equals(selected) && clientSection && page == 17;
        fallOpen="Fall Damage".equals(selected)&&page==0;
        diagnosticOpen="Diagnostic Logging".equals(selected)&&page==18;
        screenFlashOpen = "Screen Flash".equals(selected) && clientSection && page == 2;
        cameraFeedbackOpen = "Camera Feedback".equals(selected) && clientSection && page == 2;
        chromaticOpen = "Chromatic Aberration".equals(selected) && clientSection && page == 2;
        animationOpen = "Hand Animation".equals(selected) && clientSection && page == 15;
        healingOpen = "Healing".equals(selected) && !clientSection && page == 0;
        timingOpen = "Timing".equals(selected) && !clientSection && page == 0;
        retaliationOpen = "Damage Returned".equals(selected) && page == 0;
        pushbackOpen = "Parry Pushback".equals(selected) && page == 0;
        parryProjectileOpen = "Projectiles".equals(selected) && page == 0;
        parryExplosionOpen = "Explosions".equals(selected) && page == 0;
        guardOpen = "Guard Stance".equals(selected) && page == 1;
        blockProjectileOpen = "Projectiles".equals(selected) && page == 1;
        blockExplosionOpen = "Explosions".equals(selected) && page == 1;
        eligibilityOpen = "Valid Parry Conditions".equals(selected) && !clientSection && page == 4;
        priorityOpen = "Priority".equals(selected) && !clientSection && page == 4;
        durabilityOpen = "Item Durability Wear".equals(selected) && page == 4;
        mobColorsOpen = "Parry Colors".equals(selected) && page == 12;
        mobEquipmentOpen = "Spawn Equipment".equals(selected) && page == 12;
        mobEligibilityOpen = "Mob Eligibility".equals(selected) && page == 12;
        shieldTimingOpen = "Shield Timing".equals(selected) && page == 11;
        shieldPerfectOpen = "Shield Perfect Parry".equals(selected) && page == 11;
        shieldIdsOpen = "Additional Shields and Bosses".equals(selected) && page == 11;
        commonDamageOpen = "Common Damage Types".equals(selected) && page == 6;
        enforceParticlesOpen = "World Particles".equals(selected) && enforceEnabled;
        enforceHudOpen = ("Screen Effects".equals(selected)||"Shield Icon".equals(selected)||"Hand Animation".equals(selected)) && enforceEnabled;
        enforceAudioOpen = "Audio Sliders".equals(selected) && enforceEnabled;
        enforcePunchyOpen = "Experimental Punchy".equals(selected) && enforceEnabled;
    }

    private void rememberLocation() {
        rememberedSection = debugSection ? 4 : experimentalSection ? 3 : enforceSection ? 2 : clientSection ? 0 : 1;
        if (experimentalSection) { rememberedExperimentalPage = page; rememberedExperimentalRow = firstRow; }
        else if (enforceSection) {rememberedEnforceRow=firstRow;rememberedEnforcePage=page;}
        else if (clientSection) { rememberedClientPage = page; rememberedClientRow = firstRow; }
        else if(debugSection){rememberedDebugPage=page;}
        else {rememberedServerPage = page; rememberedServerRow = firstRow;}
    }

    private int[] particleAppearanceOrder(int first,int end){
        if(first==GuardParticleColors.OFFSET&&end==GuardParticleColors.LENGTH)return java.util.stream.Stream.of(particleAppearanceOrder(GuardParticleColors.BASE,GuardParticleColors.FLYING_SIZE_VARIANCE+1),particleAppearanceOrder(GuardParticleColors.DEBRIS_COLOR,GuardParticleColors.DEBRIS_SIZE+1),particleAppearanceOrder(GuardParticleColors.TRACER_START_COLOR,GuardParticleColors.PREVIOUS_LENGTH),particleAppearanceOrder(GuardParticleColors.IMPACT_SIZE,GuardParticleColors.IMPACT_DURATION+2)).flatMapToInt(java.util.Arrays::stream).toArray();
        if(first==GuardParticleColors.BASE&&end==GuardParticleColors.FLYING_SIZE_VARIANCE+1)return new int[]{GuardParticleColors.FLYING_ENABLED,GuardParticleColors.FLYING_SIZE,GuardParticleColors.FLYING_SIZE_VARIANCE,GuardParticleColors.FLYING_LIFETIME,GuardParticleColors.BASE};
        if(first==GuardParticleColors.DEBRIS_COLOR)return new int[]{GuardParticleColors.DEBRIS_ENABLED,GuardParticleColors.DEBRIS_SIZE,GuardParticleColors.DEBRIS_LIFETIME,GuardParticleColors.DEBRIS_COLOR};
        if(first==GuardParticleColors.TRACER_START_COLOR)return new int[]{GuardParticleColors.TRACER_ENABLED,GuardParticleColors.SHIELD_FORWARD_BIAS,GuardParticleColors.TRACER_SIZE,GuardParticleColors.TRACER_LIFETIME,GuardParticleColors.TRACER_TRAIL_TICKS,GuardParticleColors.TRACER_START_COLOR,GuardParticleColors.TRACER_MIDDLE_COLOR,GuardParticleColors.TRACER_END_COLOR};
        if(first==GuardParticleColors.IMPACT_SIZE)return new int[]{GuardParticleColors.IMPACT_ENABLED,GuardParticleColors.IMPACT_SIZE,GuardParticleColors.IMPACT_SPIN,GuardParticleColors.IMPACT_OPACITY,GuardParticleColors.IMPACT_DURATION};
        return java.util.stream.IntStream.range(first,end).toArray();
    }
    private void particleAppearance(boolean server,int first,int end) {
        int[] settings=server?enforcedDefaults:extraClient;
        for(int i:particleAppearanceOrder(first,end)) {
            if(GuardParticleColors.retired(i)||!server&&i>=GuardParticleColors.FLYING_ENABLED&&i<=GuardParticleColors.IMPACT_ENABLED)continue;
            final int index=i;String label=GuardParticleColors.LABELS[i-GuardParticleColors.OFFSET];
            if(GuardParticleColors.toggleSlot(i))toggle(label,()->settings[index]!=0,v->settings[index]=v?1:0,server,GuardParticleColors.tip(i));
            else if(GuardParticleColors.colorSlot(i))colorField(label,()->settings[index],v->settings[index]=v,server,GuardParticleColors.tip(i));
            else slider(label,()->settings[index],GuardParticleColors.min(i),GuardParticleColors.max(i),GuardParticleColors.unit(i),1,server,v->settings[index]=v,GuardParticleColors.tip(i));
        }
    }

    private void buildEnforcedDefaults() {
        if(page==8)dropdown("World Particles", () -> enforceParticlesOpen, "Set starting appearance and intensity for the sparks and center impact.");
        if (page==8 && enforceEnabled && enforceParticlesOpen) {
            dontEnforce(GuardClientPreset.PARTICLES);
            presetSlider("Base Intensity",16,0,100,"%");
            presetSlider("Perfect Intensity",17,0,100,"%");
            presetSlider("Damage Intensity",18,0,100,"%");
            presetSlider("Debris Base Intensity",19,0,100,"%");
            presetSlider("Debris Perfect Intensity",20,0,100,"%");
            presetSlider("Debris Damage Intensity",21,0,100,"%");
            for(int i:GuardSparkTracerConfig.DISPLAY_ORDER)presetSlider(GuardSparkTracerConfig.LABELS[i],GuardSparkTracerConfig.OFFSET+i,0,100,"%");
            particleAppearance(true,GuardParticleColors.OFFSET,GuardParticleColors.LENGTH);
        }
        if(page==19||page==20||page==22)dropdown(page==20?"Shield Icon":page==22?"Hand Animation":"Screen Effects", () -> enforceHudOpen, "Set the server's starting HUD and screen feedback values.");
        if ((page==19||page==20||page==22) && enforceEnabled && enforceHudOpen) {
            dontEnforce(page==20?GuardClientPreset.HUD:page==22?GuardClientPreset.ANIMATIONS:GuardClientPreset.SCREEN);
            if(page==20){presetToggle("Shield icon",0);presetToggle("Shield reactions",1);buildShieldReactions(true);}
            if(page==19){
            presetToggle("Screen flash", 2);
            presetToggle("Meme flash", 24);
            presetToggle("Perfect only flash", 27);
            presetSlider("Regular parry hitlag", 62, 0, 10, " frames");
            presetSlider("Screen flash strength", 4, 0, 100, "%");
            presetSlider("Camera shake", 5, 0, 100, "%");
            presetSlider("Perfect parry hitlag", 3, 0, 10, " frames");
            presetToggle("Impact frame", 34);
            presetToggle("Perfect only impact", 35);
            int previewRow = controlIndex++;
            if (visible(previewRow)) {
                settingLabel(previewRow, "Impact preview", "Tune the enforced impact image with a captured scene. Changes save when you press Apply.", operator);
                Button preview = addRenderableWidget(GuardUi.builder(Component.literal("Preview"), b ->
                    openImpactPreview(true)).bounds(settingX(), row(previewRow), settingWidth(), 20).build());
                preview.active = operator;
                tip(preview, "Open the preview to tune enforced impact values.");
            }
            }
            if(page==22){presetToggle("Third-person guard pose",49);presetToggle("First-person guard pose",11);}
            if(page==19){
            presetSlider("Chromatic intensity", 58, 0, 200, "%");
            presetSlider("Chromatic duration", 59, 1, 40, " ticks");
            presetToggle("Perfect only chromatic", 60);
            presetToggle("Chromatic affects HUD", 61);
            }

        }

        if(page==23)dropdown("Experimental Punchy", () -> enforcePunchyOpen, "Set the five Parrying presets used by locked clients. Default: All five presets enabled; Preset 5 is empty-hand only.");
        if (page==23 && enforceEnabled && enforcePunchyOpen) {
            dontEnforce(GuardClientPreset.PUNCHY);
            slider("Release delay", () -> enforcedDefaults[GuardPoseSettings.RELEASE_DELAY_SLOT], 0, 1000, " ms", 1, true, v -> enforcedDefaults[GuardPoseSettings.RELEASE_DELAY_SLOT]=v, "Extra hold after hitlag finishes. No extra delay when hitlag does not occur. Applies to every Punchy preset. Default: 50 ms.");
            poseEditor(true);

        }
        if(page==21)dropdown("Audio Sliders", () -> enforceAudioOpen, "Set the server's starting local playback volumes.");
        if (page==21 && enforceEnabled && enforceAudioOpen) {
            dontEnforce(GuardClientPreset.AUDIO);
            presetSlider("Master volume", 12, 0, 200, "%");
            presetSlider("Perfect volume", 14, 0, 200, "%");
            presetSlider("Parry volume", 13, 0, 200, "%");
            presetSlider("Block volume", 15, 0, 200, "%");
        }
    }



    private void poseEditor(boolean server) {
        int position = controlIndex++;
        visibleSettingRows.putIfAbsent("Parry Animation Stance", position);
        if (!visible(position)) return;
        String description = "Edit built-in and custom Parrying presets, placement, motion and item assignments. Requires Punchy. Default: All five presets enabled; Preset 5 is empty-hand only.";
        settingLabel(position, "Parry Animation Stance", scope(server, description), editable(server));
        Button editor = addRenderableWidget(GuardUi.builder(Component.literal("Edit Presets"), b -> {
            int[] source = server ? enforcedDefaults : extraClient;
            GuardPoseManagerScreen.Draft draft = poseDraft.copy();
            draft.enabled=source[65]!=0;
            if (server) { draft.poses.clear(); for (int i=0;i < GuardPoseSettings.COUNT;i++) { var pose=dev.zeli.mallardguard.GuardPoseLibrary.preset(i); pose.values=dev.zeli.mallardguard.GuardPoseSettings.pose(source,i);pose.enabled=pose.values[0]!=0;draft.poses.add(pose); } var enforced=dev.zeli.mallardguard.GuardPoseLibrary.parseAnimations(enforcedPoseData);if(enforced!=null)draft.poses=new java.util.ArrayList<>(enforced); }
            Minecraft.getInstance().setScreen(new GuardPoseManagerScreen(this, draft, editable(server), result -> {

                for (int i=0;i < GuardPoseSettings.COUNT;i++) { var pose=result.poses.get(i); int[] values=pose.values.clone();values[0]=pose.enabled?1:0;System.arraycopy(values,0,source,dev.zeli.mallardguard.GuardPoseSettings.OFFSET+i*23,23); }
                source[65]=result.enabled?1:0;
                if(server)enforcedPoseData=dev.zeli.mallardguard.GuardPoseLibrary.encodeAnimations(result.poses);
                if (!server) poseDraft = result.copy();
                dirty = true; if (server) dirtyServer = dirtyPolicy = true; else dirtyClient = true;

            }));
        }).bounds(settingX(), row(position), settingWidth(), 20).build());
        editor.active = editable(server) && PunchyGuardCompat.supportedVersion(); tip(editor, scope(server, description));
    }

    private void dontEnforce(int category) {
        toggle("Don't Enforce", () -> (dontEnforceMask & category) != 0, value -> {
            dontEnforceMask = value ? dontEnforceMask | category : dontEnforceMask & ~category;
        }, true, "On allows client edits after receiving server defaults. Off locks this category.");
    }

    private void presetToggle(String name, int index) {
        toggle(name, () -> enforcedDefaults[index] != 0, value -> enforcedDefaults[index] = value ? 1 : 0, true,
            "Starting client value. Locked unless Don't Enforce is enabled.");
    }

    private void presetSlider(String name, int index, int min, int max, String suffix) {
        slider(name, () -> enforcedDefaults[index], dev.zeli.mallardguard.GuardClientSettings.minimum(index), dev.zeli.mallardguard.GuardClientSettings.maximum(index), suffix, dev.zeli.mallardguard.GuardClientSettings.scale(index), true, value -> enforcedDefaults[index] = value,
            "Starting client value. Locked unless Don't Enforce is enabled."
                + (index == 48 ? " Downward acceleration after the drop delay; 0% disables gravity." : "")
                + (index == 3 ? " When Screen Effects is locked, this hitlag duration also controls whether perfect retaliation waits for feedback completion." : ""));
    }

    private boolean editable(boolean server) {
        return server ? operator : !GuardClient.clientCategoryLocked(activeClientCategory);
    }

    private String scope(boolean server, String description) {
        return description + (server ? operator ? "" : " OP required." : editable(false) ? "" : " Locked by server.");
    }

    private void settingLabel(int index, String name, String description, boolean enabled) {
        settingLabels.add(new SettingLabel(name, tooltipLines(description), row(index), enabled));
    }

    private String declaredDefault(String name) {
        var binding = dev.zeli.mallardguard.GuardControlSettings.binding(name, !clientSection && !enforceSection);
        Object value = binding == null ? null : binding.defaultValue();
        if(value==null)return null;
        if(name.startsWith("Mob Tracer "))return String.format("#%06X",((Number)value).intValue());
        int appearance=GuardParticleColors.indexOfLabel(name);
        if(appearance>=0){
            int number=((Number)value).intValue();
            if(GuardParticleColors.toggleSlot(appearance))return number==0?"Off":"On";
            if(GuardParticleColors.colorSlot(appearance))return String.format("#%06X",number);
            return number+GuardParticleColors.unit(appearance);
        }
        if(value instanceof Boolean b)return b ? "On" : "Off";
        String suffix=switch(name) {
            case "Release delay" -> " ms";
            case "Perfect window" -> " ticks";
            case "Parry window" -> " ticks";
            case "Recharge" -> " ticks";
            case "Facing angle" -> "°";
            case "Follow-up parries" -> " ticks";
            case "Parry healing amount" -> " hearts";
            case "Shield perfect window" -> " ticks";
            case "Shield parry window" -> " ticks";
            case "Shield parry recharge" -> " ticks";
            case "Shield blocks before break" -> "";
            case "Weapon blocks before break" -> "";
            case "Guard break recharge" -> " ticks";
            case "Cone reach" -> " blocks";
            case "Shield retaliation" -> "%";
            case "Retaliation damage cap" -> " damage";
            case "Stun duration" -> " ticks";
            case "Base Intensity", "Perfect Intensity", "Damage Intensity", "Debris Base Intensity", "Debris Perfect Intensity", "Debris Damage Intensity" -> "%";
            case "Screen flash" -> "%";
            case "Camera shake" -> "%";
            case "Hitlag frames" -> " frames";
            case "Mob gear chance" -> "%";
            case "Mob guard difficulty" -> "%";
            case "Any held item" -> "";
            case "Empty Hand" -> "";
            case "First-person guard pose" -> "";
            case "Master volume" -> "%";
            case "Perfect volume" -> "%";
            case "Parry volume" -> "%";
            case "Block volume" -> "%";
            default -> "";
        };
        if(binding!=null && binding.scale()>1 && value instanceof Number number)return String.format(Locale.ROOT,binding.scale()==100?"%.2f":"%.1f",number.doubleValue()/binding.scale())+suffix;
        return value.toString()+suffix;
    }

    private String defaultText(String name, boolean server) {
        String declared = declaredDefault(name);
        if (declared != null) return declared;
        if (name.equals("Don't Enforce") || name.equals("Enforce client settings")) return "Off";
        if (name.equals("Fall parry")) return "On";
        return "Off";
    }



    private void sliderBound(String name, java.util.function.IntSupplier current, String suffix,
                             int divisor, boolean server, Change change, String description) {
        var binding = dev.zeli.mallardguard.GuardControlSettings.binding(name, server && !enforceSection);
        if (binding == null) throw new IllegalArgumentException("Unknown config control: " + name);
        int minimum = name.equals("Guard break recharge") ? Math.max(recharge, binding.minimum()) : binding.minimum();
        slider(name, current, minimum, binding.maximum(), suffix, divisor, server, change, description);
    }

    private void choice(String name, String[] options, java.util.function.IntSupplier current, java.util.function.IntConsumer change, String description) {
        int position=controlIndex++;visibleSettingRows.putIfAbsent(name,position);if(!visible(position))return;
        trackValue(position,()->options[current.getAsInt()]);
        if(operator)rowUndo(position,true,()->{String old=originalRows.get(position);for(int i=0;i<options.length;i++)if(options[i].equals(old)){change.accept(i);break;}});
        settingLabel(position,name,scope(true,description),operator);
        Button button=addRenderableWidget(GuardUi.builder(Component.literal(options[current.getAsInt()]),b->{
            dirty=true;dirtyServer=true;dirtyServerRules=true;change.accept((current.getAsInt()+1)%options.length);b.setMessage(Component.literal(options[current.getAsInt()]));
        }).bounds(settingX(),row(position),settingWidth(),20).build());button.active=operator;tip(button,scope(true,description));
    }

    private void toggle(String name, BooleanSupplier current,
                        Consumer<Boolean> change, boolean server, String description) {
        int position = controlIndex++;
        visibleSettingRows.putIfAbsent(name, position);
        if (!visible(position)) return;
        trackValue(position,()->current.getAsBoolean()?"On":"Off");
        var binding = dev.zeli.mallardguard.GuardControlSettings.binding(name, server && !enforceSection);
        String tooltip = binding == null
            ? scope(server, description + (description.contains("Default:") ? "" : " Default: " + defaultText(name, server) + "."))
            : scope(server, description.replaceAll("\\s*Default:[^.]*(?:\\.(?!\\d)|$)", "").trim() + " Default: " + defaultText(name, server) + ".");
        settingLabel(position, name, tooltip, editable(server));
        if(editable(server))rowUndo(position,server,()->change.accept("On".equals(originalRows.get(position))));
        boolean damageToggle = server && !enforceSection && page == 6;
        Button button = addRenderableWidget(GuardUi.builder(damageToggle ? damageStatus(current.getAsBoolean()) : label(current.getAsBoolean()), b -> {
            dirty = true;
            if (server) {
                dirtyServer = true;
                if (enforceSection) dirtyPolicy = true; else if (page == 12) dirtyMobs = true; else dirtyServerRules = true;
            } else dirtyClient = true;
            change.accept(!current.getAsBoolean());
            b.setMessage(damageToggle ? damageStatus(current.getAsBoolean()) : label(current.getAsBoolean()));
        }).bounds(settingX(), row(position), settingWidth(), 20).build());
        boolean automaticPose = !server && name.equals("First-person guard pose")
            && (!PunchyGuardCompat.installed() || extraClient[65] != 0);
        button.active = editable(server) && !automaticPose;
        if (automaticPose) tooltip += " Selected automatically for the available animation renderer.";
        tip(button, tooltip);
    }

    private boolean simplePoseDraftEnabled() {
        if (!PunchyGuardCompat.installed()) return true;
        if (extraClient[65] != 0) return !PunchyGuardCompat.supportedVersion();
        return firstPersonAnimation;
    }

    private void slider(String name, java.util.function.IntSupplier current, int min, int max, String suffix,
                        int divisor, boolean server, Change change, String description) {
        int position = controlIndex++;
        visibleSettingRows.putIfAbsent(name, position);
        if (!visible(position)) return;
        trackValue(position,()->Integer.toString(current.getAsInt()));
        var binding = dev.zeli.mallardguard.GuardControlSettings.binding(name, server && !enforceSection);
        String defaultValue = defaultText(name, server);
        if (binding != null && binding.defaultValue() instanceof Number number) {
            defaultValue = (divisor == 1 ? Integer.toString(number.intValue()) : String.format(java.util.Locale.ROOT, divisor==1000?"%.3f":"%.1f", number.doubleValue() / divisor)) + suffix;
        }
        String tooltip = scope(server, description.replaceAll("\\s*Default:[^.]*(?:\\.(?!\\d)|$)", "").trim() + " Default: " + defaultValue + ".");
        settingLabel(position, name, tooltip, editable(server));
        if(editable(server))rowUndo(position,server,()->change.accept(Integer.parseInt(originalRows.get(position))));
        Slider control = addRenderableWidget(new Slider(settingX(), row(position), settingWidth(),
            name, current.getAsInt(), min, max, suffix, value -> {
                dirty = true;
                if (server) {
                    dirtyServer = true;
                    if (enforceSection) dirtyPolicy = true; else if (page == 12) dirtyMobs = true; else dirtyServerRules = true;
                } else dirtyClient = true;
                change.accept(value);
            }, divisor));
        control.active = editable(server);
        tip(control, tooltip);
    }

    private void itemRuleAssignment(String name,String current,Consumer<String> update,String description){
        // One editor owns both rule lists; keep the persisted server fields compatible.
        if(name.endsWith("Blacklist"))return;
        boolean shield=name.startsWith("Shield");String title=shield?"Shield Item Eligibility":"Item Eligibility";
        int position=controlIndex++;visibleSettingRows.putIfAbsent(title,position);if(!visible(position))return;
        String help="Left-click items to allow; right-click to block.\nUse ! exceptions for overlapping mod/tag rules.";
        settingLabel(position,title,scope(true,help),editable(true));
        java.util.function.Supplier<String> allowed=()->shield?shieldItems:includedItems,blocked=()->shield?shieldBlacklist:excludedItems;
        String oldAllowed=originalText(name,current),oldBlocked=originalText(shield?"Shield Item Blacklist":"Item Blacklist",blocked.get());
        rowValues.put(position,()->"Allowed: "+allowed.get()+" | Blocked: "+blocked.get());originalRows.put(position,"Allowed: "+oldAllowed+" | Blocked: "+oldBlocked);
        if(editable(true))rowUndo(position,true,()->{if(shield){shieldItems=oldAllowed;shieldBlacklist=oldBlocked;dirtyShield=dirtyServerRules=true;}else{includedItems=oldAllowed;excludedItems=oldBlocked;dirtyServerRules=true;}});
        Button assign=addRenderableWidget(GuardUi.button("Assign Items",settingX(),row(position),settingWidth(),20,()->Minecraft.getInstance().setScreen(new GuardPoseItemScreen(this,title,allowed.get(),blocked.get(),(whitelist,blacklist)->{
            if(whitelist.equals(allowed.get())&&blacklist.equals(blocked.get()))return;
            if(shield){shieldItems=whitelist;shieldBlacklist=blacklist;dirtyShield=dirtyServerRules=true;}else{includedItems=whitelist;excludedItems=blacklist;dirtyServerRules=true;}
            invalidItemRules=false;dirty=dirtyServer=true;
        }))));assign.active=editable(true);tip(assign,help);
    }

    private void mobGearAssignments(){
        String name="Mob Gear Assignments",help="Left: allow; right: block; either click removes an assignment.\nDefault: vanilla armor/tools, without axes, netherite, elytra, turtle helmets or maces. Generated gear only.";
        int position=controlIndex++;visibleSettingRows.putIfAbsent(name,position);if(!visible(position))return;
        settingLabel(position,name,scope(true,help),editable(true));
        String oldAllowed=originalText("Mob Gear Whitelist",mobGearWhitelist),oldBlocked=originalText("Mob Gear Blacklist",mobGearBlacklist);
        rowValues.put(position,()->mobGearWhitelist+" | "+mobGearBlacklist);originalRows.put(position,oldAllowed+" | "+oldBlocked);
        if(editable(true))rowUndo(position,true,()->{mobGearWhitelist=oldAllowed;mobGearBlacklist=oldBlocked;});
        Button button=addRenderableWidget(GuardUi.button("Assign Gear",settingX(),row(position),settingWidth(),20,()->Minecraft.getInstance().setScreen(
            new GuardPoseItemScreen(this,"Mob Gear",mobGearWhitelist,mobGearBlacklist,(allowed,blocked)->{
                if(!allowed.equals(mobGearWhitelist)||!blocked.equals(mobGearBlacklist)){mobGearWhitelist=allowed;mobGearBlacklist=blocked;dirty=dirtyServer=dirtyMobs=true;}
            }).restrictItems(item->dev.zeli.mallardguard.GuardMobState.equipmentSlot(item)!=null)
                .assignmentDefaults(GuardConfig.MOB_GEAR_WHITELIST.getDefault(),GuardConfig.MOB_GEAR_BLACKLIST.getDefault()))));
        button.active=editable(true);tip(button,help);
    }

    private void blockCountAssignments(){
        String name="Item Block Counts",description="Assign a block limit to individual items.\nOverrides compatibility and ordinary limits; 0 is unlimited.";
        int position=controlIndex++;visibleSettingRows.putIfAbsent(name,position);if(!visible(position))return;
        settingLabel(position,name,scope(true,description),editable(true));
        rowValues.put(position,()->itemBlockCounts);originalRows.put(position,originalText(name,itemBlockCounts));
        if(editable(true))rowUndo(position,true,()->itemBlockCounts=originalRows.get(position));
        Button button=addRenderableWidget(GuardUi.button("Assign Items",settingX(),row(position),settingWidth(),20,()->Minecraft.getInstance().setScreen(new GuardPoseItemScreen(this,"Item Assignments",includedItems,excludedItems,itemBlockCounts,result->{
            if(!result.whitelist().equals(includedItems)||!result.blacklist().equals(excludedItems)){includedItems=result.whitelist();excludedItems=result.blacklist();dirtyServerRules=true;}
            if(!result.blockCounts().equals(itemBlockCounts)){itemBlockCounts=result.blockCounts();dirtyShield=true;}
            invalidItemRules=false;dirty=dirtyServer=true;
        }))));
        button.active=editable(true);tip(button,description);
    }

    private static Integer parseColor(String value) {
        String hex = value.startsWith("#") ? value.substring(1) : value;
        if (!hex.matches("[0-9a-fA-F]{6}")) return null;
        return Integer.parseInt(hex, 16);
    }
    private record ColorPreview(Button button,java.util.function.IntSupplier color,int row,int x) {}
    private final List<ColorPreview> colorPreviews=new ArrayList<>();
    private void colorField(String name,java.util.function.IntSupplier current,java.util.function.IntConsumer change,boolean server,String description){
        int position=controlIndex;
        editBox(name,String.format("#%06X",current.getAsInt()),text->{Integer rgb=parseColor(text);if(rgb!=null)change.accept(rgb);},description,server);
        if(!visible(position))return;
        EditBox box=null;for(var child:children())if(child instanceof EditBox e&&e.getY()==row(position)&&e.getX()==settingX()){box=e;break;}
        if(box==null)return;
        box.setMaxLength(7);box.setWidth(Math.max(20,settingWidth()-24));
        Button preview=addRenderableWidget(GuardUi.button("",settingX()+settingWidth()-20,row(position),20,20,()->Minecraft.getInstance().setScreen(new GuardColorPickerScreen(this,name,current.getAsInt(),rgb->{change.accept(rgb);markChanged(server);}))));
        preview.active=editable(server);tip(preview,"Choose a color using the hue wheel and triangle.");colorPreviews.add(new ColorPreview(preview,current,position,preview.getX()));
    }
    private void markChanged(boolean server) {
        dirty = true;
        if (!server) { dirtyClient = true; return; }
        dirtyServer = true;
        if (enforceSection) dirtyPolicy = true;
        else if (page == 12) dirtyMobs = true;
        else if (page == 11) dirtyShield = true;
        else if (page == 6) dirtyDamageRules = true;
        else dirtyServerRules = true;
    }
    private void editBox(String name, String initial, Consumer<String> update, String description) {
        editBox(name,initial,update,description,true);
    }
    private void editBox(String name, String initial, Consumer<String> update, String description,boolean server) {
        int position = controlIndex++;
        visibleSettingRows.putIfAbsent(name, position);
        if (!visible(position)) return;
        String tooltip = scope(server, description + (description.contains("Default:") ? "" : " Default: " + defaultText(name, server) + "."));
        settingLabel(position, name, tooltip, editable(server));
        EditBox box = addRenderableWidget(GuardUi.editBox(font,settingX(),row(position),settingWidth(),name));
        rowValues.put(position,()->box.getValue());
        originalRows.put(position,originalText(name,initial));
        if(editable(server))rowUndo(position,server,()->update.accept(originalRows.get(position)));
        box.setMaxLength(1024);
        box.setValue(initial);
        box.setResponder(value -> { update.accept(value);markChanged(server); });
        box.setEditable(editable(server));
        box.active = editable(server);
        tip(box, tooltip);
    }

    private static Component damageStatus(boolean enabled) {
        return Component.literal("Parry: " + (enabled ? "Enabled" : "Disabled"))
            .withStyle(style -> style.withColor(enabled ? 0xB6E8B6 : 0xA7A7A7));
    }

    private static String tooltipLines(String description) {
        return description.strip().replaceAll("(?<=[.!?]) +(?=[A-Z0-9])", "\n");
    }

    private void tip(AbstractWidget widget, String description) {
        description=tooltipLines(description);
        widget.setTooltip(null);
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
        SavedState before=captureState();
        if (GuardClient.clientCategoryLocked(GuardClientPreset.PARTICLES)) {
            for(int i=16;i<=21;i++)extraClient[i]=dev.zeli.mallardguard.GuardClientSettings.local(i);
        }
        int[] effectiveClient = GuardClientPreset.readLocal();
        for (int i = 50; i < extraClient.length; i++) if (GuardClient.clientCategoryLocked(GuardClientPreset.categoryOf(i))) extraClient[i] = effectiveClient[i];
        if(GuardClient.clientCategoryLocked(GuardClientPreset.HUD)){hud=GuardConfig.HUD.get();shieldEffects=GuardConfig.SHIELD_EFFECTS.get();}
        if(GuardClient.clientCategoryLocked(GuardClientPreset.ANIMATIONS)){firstPersonAnimation=GuardConfig.FIRST_PERSON_ANIMATION.get();thirdPersonAnimation=GuardConfig.THIRD_PERSON_ANIMATION.get();}
        if (GuardClient.clientCategoryLocked(GuardClientPreset.SCREEN)) {
            memeFlash = GuardConfig.MEME_FLASH.get();
            flashStrength = GuardConfig.SCREEN_FLASH.get() ? GuardConfig.FLASH_STRENGTH.get() : 0;
            perfectOnlyFlash = GuardConfig.PERFECT_ONLY_FLASH.get();
            shakeStrength = GuardConfig.SCREEN_SHAKE.get() ? GuardConfig.SHAKE_STRENGTH.get() : 0;
            hitlagFrames = GuardConfig.HITLAG_FRAMES.get();
            impactFrames = GuardConfig.IMPACT_FRAMES.get();
            impactPerfectOnly = GuardConfig.IMPACT_PERFECT_ONLY.get();
            impactBrightness = GuardConfig.IMPACT_BRIGHTNESS.get(); impactContrast = GuardConfig.IMPACT_CONTRAST.get();
            impactEdges = GuardConfig.IMPACT_EDGES.get(); impactGrain = GuardConfig.IMPACT_GRAIN.get();
        }
        if (GuardClient.clientCategoryLocked(GuardClientPreset.AUDIO)) {
            masterVolume = GuardConfig.LOCAL_MASTER_VOLUME.get(); perfectVolume = GuardConfig.LOCAL_PERFECT_VOLUME.get();
            parryVolume = GuardConfig.LOCAL_PARRY_VOLUME.get(); blockVolume = GuardConfig.LOCAL_BLOCK_VOLUME.get();
        }
        if (GuardClient.clientCategoryLocked(GuardClientPreset.PUNCHY)) {
            int[] current = GuardClientPreset.readLocal();
            System.arraycopy(current, 65, extraClient, 65, GuardShieldReactions.OFFSET - 65);
            System.arraycopy(current,GuardPoseSettings.RELEASE_DELAY_SLOT,extraClient,GuardPoseSettings.RELEASE_DELAY_SLOT,current.length-GuardPoseSettings.RELEASE_DELAY_SLOT);
        }
        acceptReceivedValues(before);
        rebuildWidgets();
    }

    /** Reset values in memory; Apply is still required to commit. */
    private void reset(boolean wholeSection) {
        if(debugSection&&page==18){debugFlags=0;dirty=dirtyClient=true;rebuildWidgets();return;}
        if ((enforceSection || !clientSection && !experimentalSection) && !operator) return;
        if (enforceSection) {
            enforceEnabled = false;
            dontEnforceMask = 0;
            enforcedPoseData="";
            System.arraycopy(GuardClientPreset.DEFAULTS, 0, enforcedDefaults, 0, enforcedDefaults.length);
            dirtyServer = dirtyPolicy = dirty = true;
        } else if (clientSection) {
            if (wholeSection) {
                for (int category : CLIENT_PAGES) if (!GuardClient.clientCategoryLocked(categoryMask(category))) resetClientPage(category);
            } else if (!GuardClient.clientCategoryLocked(categoryMask(page))) resetClientPage(page);
        } else if (experimentalSection) {
            if ((wholeSection || page == 14) && !GuardClient.clientCategoryLocked(GuardClientPreset.PUNCHY)) resetClientPage(14);
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
            case 2->GuardClientPreset.SCREEN;
            case 15->GuardClientPreset.ANIMATIONS;
            case 17->GuardClientPreset.HUD;
            case 14 -> GuardClientPreset.PUNCHY;
            case 3 -> GuardClientPreset.AUDIO;
            default -> 0;
        };
    }

    private void resetClientPage(int category) {
        int[] values = GuardClientPreset.DEFAULTS;
        for(int i=50;i<extraClient.length;i++)if(category==17?GuardClientPreset.categoryOf(i)==GuardClientPreset.HUD:category==2?i<65&&GuardClientPreset.categoryOf(i)==GuardClientPreset.SCREEN:category==7?GuardClientPreset.categoryOf(i)==GuardClientPreset.PARTICLES:false)extraClient[i]=values[i];
        switch (category) {
            case 7 -> {
                for(int i=0;i<extraClient.length;i++)if(GuardClientPreset.categoryOf(i)==GuardClientPreset.PARTICLES)extraClient[i]=values[i];
                }
            case 2 -> {
                memeFlash = values[24] != 0;
                flashStrength = values[4]; shakeStrength = values[5];
                hitlagFrames = values[3];
                perfectOnlyFlash = values[27] != 0;
                impactFrames = values[34]; impactPerfectOnly = values[35] != 0;
                impactBrightness = values[40]; impactContrast = values[41];
                impactEdges = values[42]; impactGrain = values[43];
            }
            case 17 -> {hud=values[0]!=0;shieldEffects=values[1]!=0;}
            case 15 -> { firstPersonAnimation = values[11] != 0; thirdPersonAnimation = values[49] != 0; }
            case 3 -> { masterVolume = values[12]; parryVolume = values[13];
                perfectVolume = values[14]; blockVolume = values[15]; }
            case 14 -> {
                for(int i=0;i<extraClient.length;i++)if(GuardClientPreset.categoryOf(i)==GuardClientPreset.PUNCHY)extraClient[i]=values[i];
                if(poseDraft!=null){
                    poseDraft.enabled=values[65]!=0;poseDraft.poses.clear();
                    for(int i=0;i<GuardPoseSettings.COUNT;i++)poseDraft.poses.add(dev.zeli.mallardguard.GuardPoseLibrary.preset(i));
                }
            }
            case 4 -> {}
            default -> { return; }
        }
        dirty = dirtyClient = true;
    }

    private void resetServerPage(int category, GuardPackets.Settings d) {
        switch (category) {
            case 12 -> { setMobSettings(GuardConfig.mobSnapshot(true)); dirtyMobs = true; }
            case 0 -> {
                resetServerPage(5,d);
                parryHealing = d.parryHealing(); parryHealingHearts = d.parryHealingHearts();
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
                var hands=GuardConfig.defaultShieldSnapshot();
                parryHandPriority=hands.parryHandPriority();shieldParryPriority=hands.shieldParryPriority();forceCrouchOffhand=hands.forceCrouchOffhand();
                allowAnyItem = d.allowAnyItem(); allowUsableItems = d.allowUsableItems();
                consumablePriority = d.consumablePriority();
                cooldownPreventsGuard = GuardConfig.defaultShieldSnapshot().cooldownPreventsGuard();
                allowEmptyHand = GuardConfig.defaultShieldSnapshot().allowEmptyHand();
                dirtyShield = true;
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
                int handRule=parryHandPriority, shieldRule=shieldParryPriority; boolean crouchRule=forceCrouchOffhand, cooldownRule=cooldownPreventsGuard, emptyRule=allowEmptyHand, foodRule=consumablePriority;
                setShieldSettings(GuardConfig.defaultShieldSnapshot());
                shieldRegularPush = regularPush; toolPush = weaponPush; toolMaxBlocks = weaponBlocks; shieldBreakTicks = sharedBreak; retaliationCap = sharedCap;
                parryHandPriority=handRule;shieldParryPriority=shieldRule;forceCrouchOffhand=crouchRule;cooldownPreventsGuard=cooldownRule; allowEmptyHand = emptyRule; consumablePriority = foodRule;
                shieldItems = d.shieldItems(); shieldBlacklist=GuardConfig.SHIELD_BLACKLIST.getDefault(); dirtyShield = true;
            }
            default -> { return; }
        }
        dirty = dirtyServer = true;
        if (category != 6 && category != 12) dirtyServerRules = true;
    }

    private void save(boolean close) {
        if (pendingSave != null) return;
        if (!hasChanges()) { dirty = false; return; }

        if (operator && (!GuardItemRules.valid(includedItems) || !GuardItemRules.valid(excludedItems) || !GuardItemRules.valid(shieldItems) || !GuardItemRules.valid(shieldBlacklist) || !dev.zeli.mallardguard.GuardItemBlockCounts.valid(itemBlockCounts)
            || !GuardItemRules.validEntityRules(stunnableBosses))) {
            invalidItemRules = true;
            experimentalSection = false;
            enforceSection = false;
            clientSection = false;
            page = !GuardItemRules.valid(shieldItems) || !GuardItemRules.valid(shieldBlacklist) || !dev.zeli.mallardguard.GuardItemBlockCounts.valid(itemBlockCounts) || !GuardItemRules.validEntityRules(stunnableBosses) ? 11 : 4;
            eligibilityOpen = true;
            firstRow = 0;
            rebuildWidgets();
            if (page == 4) scrollTo(2);
            else {shieldIdsOpen=true;rememberedGroups.put(groupKey(),"Additional Shields and Bosses");selectRememberedGroup();rebuildWidgets();}
            return;
        }
        if (operator && (!GuardItemRules.validEntityIds(mobWhitelist)||!GuardItemRules.valid(mobGearWhitelist)||!GuardItemRules.valid(mobGearBlacklist))) {
            experimentalSection = true; enforceSection = clientSection = false; page = 12;
            rememberedGroups.put(groupKey(), "Mob Eligibility"); selectRememberedGroup(); firstRow = 0; rebuildWidgets();
            GuardClient.configMessage(Component.literal("Use exact entity IDs in Mob Eligibility and valid item rules in Mob Gear."));
            return;
        }
        if(operator&&dirtyPolicy&&!enforcedPoseData.isEmpty()&&dev.zeli.mallardguard.GuardPoseLibrary.parseAnimations(enforcedPoseData)==null){GuardClient.configMessage(Component.literal("Mallard Guard: The enforced animation assignments are too large to save."));return;}
        try(GuardConfig.ClientEdit edit=new GuardConfig.ClientEdit()) {
            GuardConfig.DEBUG_CLIENT_FLAGS.set(debugFlags);
            GuardDiagnostics.event(GuardDiagnostics.CONFIG,"Applying config changes: client="+dirtyClient+", server="+dirtyServer+", enforce="+dirtyPolicy);
            int[] clientValues = extraClient.clone();
            clientValues[0] = (hud) ? 1 : 0;
            clientValues[1] = (shieldEffects) ? 1 : 0;
            clientValues[2] = (flashStrength > 0) ? 1 : 0;
            clientValues[3] = hitlagFrames;
            clientValues[4] = flashStrength;
            clientValues[5] = shakeStrength;
            clientValues[11] = firstPersonAnimation ? 1 : 0;
            clientValues[12] = masterVolume;
            clientValues[13] = parryVolume;
            clientValues[14] = perfectVolume;
            clientValues[15] = blockVolume;
            clientValues[24] = (memeFlash) ? 1 : 0;
            clientValues[27] = (perfectOnlyFlash) ? 1 : 0;
            clientValues[34] = impactFrames;
            clientValues[35] = (impactPerfectOnly) ? 1 : 0;
            clientValues[40] = impactBrightness;
            clientValues[41] = impactContrast;
            clientValues[42] = impactEdges;
            clientValues[43] = impactGrain;
            clientValues[49] = (thirdPersonAnimation) ? 1 : 0;
            clientValues[50] = extraClient[50];
            clientValues[51] = extraClient[51];
            clientValues[52] = extraClient[52];
            clientValues[53] = extraClient[53];
            clientValues[54] = extraClient[54];
            clientValues[55] = extraClient[55];
            clientValues[56] = (extraClient[56] != 0) ? 1 : 0;
            clientValues[57] = (extraClient[57] != 0) ? 1 : 0;
            clientValues[58] = extraClient[58];
            clientValues[59] = extraClient[59];
            clientValues[60] = (extraClient[60] != 0) ? 1 : 0;
            clientValues[61] = (extraClient[61] != 0) ? 1 : 0;
            clientValues[62] = extraClient[62];
            clientValues[6] = extraClient[6];
            clientValues[32] = extraClient[32];
            clientValues[63] = extraClient[63];
            clientValues[64] = extraClient[64];
            clientValues[47] = GuardParticleConfig.PRESET_REVISION;
            for (int category : GuardClientPreset.CATEGORIES) {
                if (category != GuardClientPreset.PUNCHY) GuardClientPreset.apply(clientValues, category);
            }
            if (!GuardClient.clientCategoryLocked(GuardClientPreset.PUNCHY)) { GuardPoseSettings.apply(clientValues); }
            if (dirtyClient) {
                if (!GuardClient.clientCategoryLocked(GuardClientPreset.PUNCHY)) {
                    if (!dev.zeli.mallardguard.GuardPoseLibrary.save(poseDraft.poses)) { GuardClient.configMessage(Component.literal("Mallard Guard poses could not be saved. Please check the game log.")); return; }
                }
                GuardClient.saveClientConfig();
            }
            edit.commit();
        }catch(RuntimeException error){
            GuardClient.configMessage(Component.literal("Mallard Guard: Client settings could not be saved. Previous values were restored. Check the game log."));
            org.slf4j.LoggerFactory.getLogger("MallardGuard").warn("Client config save failed",error);
            return;
        }
        GuardPackets.Save rulesSave = operator && dirtyServerRules ? new GuardPackets.Save(parry, block, parryDrowningFire, parryStarvation, parryGenericKill, perfect, window, recharge, angle, reductionPercent, followUp, parryReturnPercent, perfectReturnPercent, parryWear, perfectWear, blockWear, serverHitSounds, hitParticles, serverMasterVolume, serverPerfectVolume, serverParryVolume, serverBlockVolume, fallParry, fallPerfectParry, fallLookDown, fallBreakBlocks, fallBlastStrength, fallLaunchPower, parryExplosions, perfectExplosionsOnly, blockExplosions, parryProjectiles, blockProjectiles, knockbackStrength > 0, knockbackStrength, guardMovementPercent, blockDeflectChance, allowAnyItem, allowUsableItems, includedItems, excludedItems, shieldItems, consumablePriority, parryHealing, parryHealingHearts) : null;
        GuardPackets.SaveMobs mobsSave = operator && dirtyMobs ? new GuardPackets.SaveMobs(new GuardPackets.MobSettings(
            mobGuard,mobDifficulty,mobWhitelist,mobGearChance,mobGearWhitelist,mobGearBlacklist,mobTracerStartColor,mobTracerMiddleColor,mobTracerEndColor)) : null;
        if (operator && dirtyShield) shieldBreakTicks = Math.max(recharge, shieldBreakTicks);
        GuardPackets.SaveShield shieldSave = operator && dirtyShield ? new GuardPackets.SaveShield(new GuardPackets.ShieldSettings(
            shieldPerfect, shieldWindow, shieldRechargeTicks, shieldMaxBlocks, toolMaxBlocks, Math.max(recharge, shieldBreakTicks), shieldCone, shieldReach, shieldReturn,
            shieldStun, shieldPerfectPush, shieldRegularPush, toolPush, stunnableBosses, consumablePriority, cooldownPreventsGuard, allowEmptyHand, retaliationCap, shieldParryPriority, parryHandPriority, forceCrouchOffhand, shieldBlacklist, itemBlockCounts, GuardClient.currentShieldSettings().shieldExpansionActive())) : null;
        GuardPackets.SaveDamageRules damageSave = operator && dirtyDamageRules ? new GuardPackets.SaveDamageRules(
            GuardDamageRules.encodeRules(damageRules)) : null;
        GuardPackets.SaveClientPolicy policySave = operator && dirtyPolicy ? new GuardPackets.SaveClientPolicy(enforceEnabled, dontEnforceMask,
            GuardClientPreset.encode(enforcedDefaults),enforcedPoseData) : null;
        GuardPackets.SaveConfig request = new GuardPackets.SaveConfig(++nextSaveRequest, rulesSave, mobsSave, shieldSave, damageSave, policySave);
        SavedState submitted = captureState();
        boolean clientSaved = dirtyClient;
        if (request.mask() != 0) {
            if (Minecraft.getInstance().getConnection() == null) {
                acceptSavedValues(submitted, 0, 0, clientSaved);
                dirtyClient = false;
                dirty = hasChanges();
                GuardClient.configMessage(Component.literal("Mallard Guard: Connect to a server or world to save server settings."));
                return;
            }
            pendingSave = new PendingSave(request.request(), request.mask(), submitted, System.nanoTime(), clientSaved, close);
            GuardPackets.sendConfig(request,Minecraft.getInstance().getConnection().registryAccess());
            return;
        }
        if (clientSaved) GuardClient.configMessage(Component.literal("Mallard Guard configs are saved for the Client."));
        acceptSavedValues(submitted, 0, 0, clientSaved);
        dirtyClient = false;
        dirty = hasChanges();
        if (close && !dirty) onClose();
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
        updateRowLayouts();
        // Blur the game world before drawing either surface or any controls.
        renderBackground(graphics, mouseX, mouseY, delta);
        graphics.fill(0, 0, width, height, 0x3219141E);
        GuardUi.panel(graphics,sidebarLeft-6,layoutTop-5,sidebarWidth+13,height-layoutTop,true);
        GuardUi.panel(graphics,panelLeft-8,layoutTop-5,panelWidth+22,height-layoutTop,false);
        graphics.fill(contentRight() + 4, rowTop(), contentRight() + 5,
            rowTop() + visibleRows * ROW_STEP - 7, 0xFF76647F);
        for (DropdownLabel dropdown : dropdownLabels) {
            boolean hover = mouseX >= panelLeft && mouseX < contentRight()
                && mouseY >= dropdown.y() && mouseY < dropdown.y() + 22;
            if (hover || dropdown.expanded().getAsBoolean()) graphics.fill(panelLeft, dropdown.y(), contentRight(), dropdown.y() + 22,
                hover ? 0x88493E55 : 0x49493E55);
            if (dropdown.expanded().getAsBoolean()) graphics.fill(panelLeft, dropdown.y(), panelLeft + 2, dropdown.y() + 22, WARM_EDGE);
            int dropdownColor = enforceSection && !enforceEnabled ? 0xFF888888 : WARM_EDGE;
            int titleRight=contentRight()-18;for(Button control:themedButtons)if(control.visible&&control.getX()>=panelLeft&&control.getY()==dropdown.y())titleRight=Math.min(titleRight,control.getX()-4);
            graphics.drawString(font,font.plainSubstrByWidth(dropdown.name(),Math.max(0,titleRight-panelLeft-4)),panelLeft+4,dropdown.y()+3,dropdownColor);
            int lineEnd=contentRight()-4;for(Button control:themedButtons)if(control.visible&&control.getX()>=panelLeft&&control.getY()==dropdown.y())lineEnd=Math.min(lineEnd,control.getX()-4);
            graphics.fill(panelLeft + 4, dropdown.y() + 18, lineEnd,
                dropdown.y() + 19, enforceSection && !enforceEnabled ? 0x88777777 : GuardUi.WARM);
            GuardUi.chevron(graphics,contentRight()-GuardUiLayout.ARROW_SIZE-GuardUiLayout.CONTROL_GAP,
                dropdown.y()+(22-GuardUiLayout.ARROW_SIZE)/2,dropdown.expanded().getAsBoolean()?1:0,dropdownColor);
        }
        for (SettingLabel section : sectionLabels) {
            int lineEnd=contentRight()-4;for(Button control:themedButtons)if(control.visible&&control.getX()>=panelLeft&&control.getY()==section.y())lineEnd=Math.min(lineEnd,control.getX()-4);
            graphics.drawString(font,font.plainSubstrByWidth(section.name(),Math.max(0,lineEnd-panelLeft-4)),panelLeft+4,section.y()+4,WARM_EDGE);
            graphics.fill(panelLeft + 4, section.y() + 18, lineEnd, section.y() + 19, WARM_EDGE);
        }
        // Keep the panel and every widget on the same render layer.
        for (var widget : renderables) if (!(widget instanceof Button)) widget.render(graphics, mouseX, mouseY, delta);
        for (Button button : themedButtons) paintButton(graphics, button, mouseX, mouseY);
        for(ColorPreview preview:colorPreviews){Button swatch=preview.button();if(swatch.visible)graphics.fill(swatch.getX()+4,swatch.getY()+4,swatch.getX()+swatch.getWidth()-4,swatch.getY()+16,0xFF000000|preview.color().getAsInt());}
        // Shared geometry and colors also own hover/drag behavior.
        GuardUi.scrollbar(graphics,scrollbar(),mouseX,mouseY,draggingScroll);
        if (experimentalSection && page == 14 && !PunchyGuardCompat.installed()) {
            graphics.drawWordWrap(font, Component.literal("Punchy is required to access these settings. Install Punchy to create or edit guard poses."), panelLeft + 8, rowTop() + 12, panelWidth - 30, GuardUi.TEXT);
        }
        graphics.drawString(font,font.plainSubstrByWidth(title.getString(),sidebarWidth-84),sidebarLeft+33,layoutTop+8,TEXT);
        graphics.drawString(font, "v" + GuardConfig.version(), sidebarLeft+33, layoutTop + 21, 0xFFC3B4CA);
        int dividerX=(sidebarLeft+sidebarWidth+7+panelLeft-8)/2;
        graphics.fill(dividerX,layoutTop-5+GuardUiLayout.DIVIDER_INSET,dividerX+1,
            height-5-GuardUiLayout.DIVIDER_INSET,GuardUiLayout.DIVIDER_COLOR);
        if (settingsSearch.isBlank()) {
            int[] categories = debugSection ? DEBUG_PAGES : enforceSection ? ENFORCE_PAGES : clientSection ? CLIENT_PAGES : experimentalSection ? EXPERIMENTAL_PAGES : SERVER_PAGES;
            for (int i = sidebarOffset; i < Math.min(categories.length, sidebarOffset + visibleSidebarTabs(categories.length)); i++) {
                if (page == categories[i]) graphics.fill(sidebarLeft + 2, sidebarTop() + (i - sidebarOffset) * sidebarStep(categories.length) + 3,
                    sidebarLeft + 5, sidebarTop() + (i - sidebarOffset) * sidebarStep(categories.length) + sidebarStep(categories.length)-4, GuardUi.WARM);
            }
        }
        for(var entry:rowValues.entrySet()){String old=originalRows.get(entry.getKey()),value=entry.getValue().get();if(old!=null&&!old.equals(value)){
            changedRows.put(entry.getKey(),"Saved: "+old+" → Current: "+value);
            graphics.fill(panelLeft-5,row(entry.getKey())+7,panelLeft-2,row(entry.getKey())+10,GuardUi.HIGHLIGHT);
        }else changedRows.remove(entry.getKey());}
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
            int rowIndex=firstRow+(setting.y()-rowTop())/ROW_STEP;
            boolean changed=changedRows.containsKey(rowIndex);
            graphics.drawString(font,name,panelLeft,setting.y()+(changed?1:6),changed?GuardUi.HIGHLIGHT:textColor);
            if(changed){String savedValue="Saved: "+originalRows.get(rowIndex);graphics.drawString(font,font.plainSubstrByWidth(savedValue,limit),panelLeft,setting.y()+11,0xFFAFA1B6);}

        }
        if (!clientSection && invalidItemRules) {
            graphics.drawString(font, "Check the item IDs and commas.", panelLeft, rowTop() - 9, 0xFF6666);
        }
        if (mouseX >= panelLeft && mouseX < settingX() - 5) {
            for (SettingLabel setting : settingLabels) {
                if (mouseY >= setting.y() && mouseY < setting.y() + 20) {
                    graphics.renderTooltip(font, font.split(Component.literal(setting.description()+(changedRows.containsKey(firstRow+(setting.y()-rowTop())/ROW_STEP)?"\n"+changedRows.get(firstRow+(setting.y()-rowTop())/ROW_STEP):"")), Math.min(320, width - 24)), mouseX, mouseY);
                    break;
                }
            }
        }
        for (DropdownLabel dropdown : dropdownLabels) {
            if (mouseX >= panelLeft && mouseX < contentRight()
                && mouseY >= dropdown.y() && mouseY < dropdown.y() + 22
                && themedButtons.stream().noneMatch(b->b.visible&&b.isMouseOver(mouseX,mouseY))) {
                graphics.renderTooltip(font, font.split(Component.literal(dropdown.description()), Math.min(320, width - 24)), mouseX, mouseY);
                break;
            }
        }
        for (Button button : themedButtons) {
            if (button.visible && button.isMouseOver(mouseX, mouseY)) {
                graphics.renderTooltip(font, font.split(Component.literal(buttonTips.get(button)), Math.min(320, width - 24)), mouseX, mouseY);
                break;
            }
        }
        for (var entry : widgetTips.entrySet()) if (!(entry.getKey() instanceof Button)
            && entry.getKey().isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(font, font.split(Component.literal(entry.getValue()), Math.min(320, width - 24)), mouseX, mouseY);
            break;
        }
        if (pendingSave != null) {
            graphics.fill(0, 0, width, height, 0x991A1520);
            graphics.drawCenteredString(font, Component.literal("Waiting for the server to save settings…"), width / 2, height / 2, 0xFFFFFF);
        }
    }

    private void paintButton(GuiGraphics graphics,Button button,int mouseX,int mouseY){
        if(button==logoButton||button==reportButton||rowUndoButtons.containsValue(button)){button.render(graphics,mouseX,mouseY,0);return;}
        boolean selected=!button.active&&buttonTips.getOrDefault(button,"").startsWith("Open ");
        GuardUi.paint(graphics,button,mouseX,mouseY,selected,redButtons.contains(button));
    }

    private void scrollTo(int row) {
        int next = Math.clamp(row, 0, maxScroll());
        if (next != firstRow) {
            firstRow = next;
            rememberLocation();
            rebuildWidgets();
        }
    }

    private GuardUiLayout.Scrollbar scrollbar(){return new GuardUiLayout.Scrollbar(contentRight()+8,rowTop(),visibleRows*ROW_STEP-7,visibleRows,pageRows(),firstRow);}
    private void scrollAt(double mouseY){scrollTo(scrollbar().scrollAt(mouseY,scrollGrab));}

    private void selectSection(int section) {
        rememberLocation();
        settingsSearch = "";
        settingsSearchChanged = false;
        if (experimentalSection) { experimentalPage = page; experimentalRow = firstRow; }
        else if (enforceSection) {enforceRow=firstRow;enforcePage=page;}
        else if (clientSection) { clientPage = page; clientRow = firstRow; }
        else if(debugSection){debugPage=page;}
        else { serverPage = page; serverRow = firstRow; }
        debugSection = section == 4;
        experimentalSection = section == 3;
        enforceSection = section == 2;
        clientSection = section == 0;
        page = debugSection ? debugPage : experimentalSection ? experimentalPage : enforceSection ? enforcePage : clientSection ? clientPage : serverPage;
        if (!enforceSection) {
            int[] categories = debugSection ? DEBUG_PAGES : enforceSection ? ENFORCE_PAGES : clientSection ? CLIENT_PAGES : experimentalSection ? EXPERIMENTAL_PAGES : SERVER_PAGES;
            sidebarOffset = 0;
            for (int i = 0; i < categories.length; i++) if (categories[i] == page) {
                sidebarOffset = Math.max(0, i - visibleSidebarTabs(categories.length) + 1);
                break;
            }
        }
        firstRow = experimentalSection ? experimentalRow : enforceSection ? enforceRow : clientSection ? clientRow : serverRow;
        selectRememberedGroup();
        rememberLocation();
        if(Minecraft.getInstance().screen==this)rebuildWidgets();
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (pendingSave != null) return true;
        for(var child:children())if(child instanceof Slider slider && slider.active && slider.visible && slider.isMouseOver(mouseX,mouseY))
            return slider.mouseScrolled(mouseX,mouseY,scrollX,scrollY);
        if (mouseX >= panelLeft && mouseX <= panelLeft + panelWidth + 18
            && mouseY >= rowTop() && mouseY < rowTop() + visibleRows * ROW_STEP && maxScroll() > 0) {
            scrollTo(firstRow - (int) Math.signum(scrollY) * Math.max(1, (int) Math.abs(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (pendingSave != null) return true;

        if (button == 0 && scrollbar().contains(mouseX,mouseY)) {
            draggingScroll = true;scrollGrab=scrollbar().grab(mouseY);
            scrollAt(mouseY);
            return true;
        }
        if (button == 0 && enforceSection && !enforceEnabled && mouseX >= panelLeft && mouseX < contentRight())
            return super.mouseClicked(mouseX, mouseY, button);
        if (button == 0 && mouseX >= panelLeft && mouseX < contentRight()) {
            for (DropdownLabel dropdown : dropdownLabels) {
                if (mouseY >= dropdown.y() && mouseY < dropdown.y() + 22) {
                    for(Button control:themedButtons)if(control.active&&control.isMouseOver(mouseX,mouseY))return super.mouseClicked(mouseX,mouseY,button);
                    if(java.util.Arrays.asList(GuardShieldReactions.NAMES).contains(dropdown.name()))reactionGroup=dropdown.expanded().getAsBoolean()?"":dropdown.name();
                    else rememberedGroups.put(groupKey(), dropdown.expanded().getAsBoolean() ? "" : dropdown.name());
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

    @Override public boolean charTyped(char character, int modifiers) {
        return pendingSave != null || super.charTyped(character, modifiers);
    }

    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if (pendingSave != null) return true;
        if(getFocused() instanceof AbstractSliderButton){boolean handled=super.keyPressed(key,scan,modifiers);return handled;}
        return super.keyPressed(key,scan,modifiers);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (pendingSave != null) return true;
        if (draggingScroll && button == 0) { scrollAt(mouseY); return true; }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (pendingSave != null) return true;

        if (draggingScroll && button == 0) { draggingScroll = false; return true; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public void onClose() {
        if (pendingSave != null) return;
        rememberLocation();
        if (!hasChanges()) {
            Minecraft.getInstance().setScreen(parent);
            return;
        }
        GuardUi.choices(this,"Unsaved changes","Leave without saving your Mallard Guard settings?",new GuardUi.Choice("Discard Changes",()->Minecraft.getInstance().setScreen(parent)),new GuardUi.Choice("Go Back",()->Minecraft.getInstance().setScreen(this)));
    }

    private interface Change { void accept(int value); }

    private record SettingLabel(String name, String description, int y, boolean enabled) {}
    private record DropdownLabel(String name, int y, BooleanSupplier expanded, String description) {}

    private static final class Slider extends AbstractSliderButton {
        private final String name, suffix;
        private final int min, max;
        private final int divisor;
        private final Change change;
        private int dragWidth;

        private Slider(int x, int y, int width, String name, int current, int min, int max, String suffix, Change change) {
            this(x, y, width, name, current, min, max, suffix, change, 1);
        }

        private Slider(int x, int y, int width, String name, int current, int min, int max, String suffix, Change change, int divisor) {
            super(x, y, width, 20, Component.empty(), (double) (current - min) / (max - min));
            this.name = name; this.min = min; this.max = max; this.suffix = suffix; this.change = change; this.divisor = divisor;
            updateMessage();
        }

        @Override public void onClick(double mouseX,double mouseY){dragWidth=getWidth();super.onClick(mouseX,mouseY);}
        @Override protected void onDrag(double mouseX,double mouseY,double dragX,double dragY){
            int displayWidth=getWidth();if(dragWidth>0)setWidth(dragWidth);
            try{super.onDrag(mouseX,mouseY,dragX,dragY);}finally{setWidth(displayWidth);}
        }
        @Override public void onRelease(double mouseX,double mouseY){dragWidth=0;super.onRelease(mouseX,mouseY);}

        private static boolean particleControl(String name) { return name.equals("Base Intensity") || name.equals("Perfect Intensity") || name.equals("Damage Intensity") || name.startsWith("Debris ") || name.startsWith("Tracer ") || name.startsWith("Flying "); }
        @Override public boolean keyPressed(int key,int scan,int modifiers) {
            if(active && (key==263 || key==262)) {
                int current=min+(int)Math.round(value*(max-min));
                int next=Math.clamp(current+(key==263?-1:1)*(Screen.hasShiftDown()?10:1),min,max);
                value=(next-min)/(double)(max-min); if(next!=current)change.accept(next); updateMessage(); return true;
            }
            return super.keyPressed(key,scan,modifiers);
        }
        @Override public boolean mouseScrolled(double x,double y,double sx,double sy){
            if(!active || !isMouseOver(x,y) || sy==0)return false;
            int current=min+(int)Math.round(value*(max-min));
            int next=Math.clamp(current+(int)Math.signum(sy)*(Screen.hasShiftDown()?10:1),min,max);
            value=(next-min)/(double)(max-min);
            if(next!=current)change.accept(next);
            updateMessage();return true;
        }

        @Override protected void updateMessage() {
            int shown = min + (int) Math.round(value * (max - min));
            setMessage(Component.literal((divisor == 1 ? Integer.toString(shown) : String.format(java.util.Locale.ROOT, divisor == 10 ? "%.1f" : divisor == 1000 ? "%.3f" : "%.2f", shown / (double) divisor)) + suffix));
        }

        @Override protected void applyValue() {
            int selected = min + (int) Math.round(value * (max - min));
            int step = GuardUi.dragStep(min,max,divisor)>1 ? GuardUi.dragStep(min,max,divisor) : particleControl(name) ? 1 : suffix.equals("%") && divisor == 1
                ? name.contains("volume") || name.contains("chance") || name.contains("retaliation")
                    || name.contains("pushback") || name.contains("movement") ? 5 : 10 : 1;
            selected = Math.clamp((int) Math.round(selected / (double) step) * step, min, max);
            value = (selected - min) / (double) (max - min);
            change.accept(selected);
        }

        @Override public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            GuardUi.slider(graphics,this,value,false);
        }
    }
    @Override public void removed(){draggingScroll=false;super.removed();}

}
