package dev.zeli.mallardguard;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.DirectoryStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class GuardConfig {
    public static final ModConfigSpec.IntValue DEBUG_CLIENT_FLAGS;
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec PUNCHY_SPEC;
    public static final ModConfigSpec.ConfigValue<String> MOB_BLACKLIST;
    public static final ModConfigSpec.BooleanValue PARRY;
    public static final ModConfigSpec.ConfigValue<String> DAMAGE_RULES;
    public static final ModConfigSpec.ConfigValue<String> OBSERVED_PROJECTILE_SOURCES;
    public static final ModConfigSpec.BooleanValue BLOCK;
    public static final ModConfigSpec.IntValue PERFECT_TICKS;
    public static final ModConfigSpec.IntValue PARRY_TICKS;
    public static final ModConfigSpec.IntValue RECHARGE_TICKS;
    public static final ModConfigSpec.IntValue FACING_ANGLE;
    public static final ModConfigSpec.DoubleValue BLOCK_REDUCTION;
    public static final ModConfigSpec.IntValue FOLLOW_UP_TICKS;
    public static final ModConfigSpec.DoubleValue PARRY_RETALIATION;
    public static final ModConfigSpec.DoubleValue PERFECT_RETALIATION;
    public static final ModConfigSpec.IntValue RETALIATION_CAP;
    public static final ModConfigSpec.IntValue PARRY_WEAR;
    public static final ModConfigSpec.IntValue PERFECT_WEAR;
    public static final ModConfigSpec.IntValue BLOCK_WEAR;
    public static final ModConfigSpec.BooleanValue HIT_SOUNDS;
    public static final ModConfigSpec.BooleanValue HIT_PARTICLES;
    public static final ModConfigSpec.BooleanValue ENFORCE_CLIENT;
    public static final ModConfigSpec.IntValue CLIENT_EXEMPT_MASK;
    public static final ModConfigSpec.ConfigValue<String> CLIENT_PRESET;
    public static final ModConfigSpec.IntValue MASTER_VOLUME;
    public static final ModConfigSpec.IntValue PERFECT_VOLUME;
    public static final ModConfigSpec.IntValue PARRY_VOLUME;
    public static final ModConfigSpec.IntValue BLOCK_VOLUME;
    public static final ModConfigSpec.BooleanValue FALL_LOOK_DOWN;
    public static final ModConfigSpec.BooleanValue FALL_PERFECT_PARRY;
    public static final ModConfigSpec.BooleanValue FALL_BREAK_BLOCKS;
    public static final ModConfigSpec.IntValue FALL_BLAST_STRENGTH;
    public static final ModConfigSpec.IntValue FALL_LAUNCH_POWER;
    public static final ModConfigSpec.BooleanValue PARRY_EXPLOSIONS;
    public static final ModConfigSpec.BooleanValue PERFECT_EXPLOSIONS_ONLY;
    public static final ModConfigSpec.BooleanValue BLOCK_EXPLOSIONS;
    public static final ModConfigSpec.BooleanValue PARRY_PROJECTILES;
    public static final ModConfigSpec.BooleanValue BLOCK_PROJECTILES;
    public static final ModConfigSpec.BooleanValue DEFENDER_KNOCKBACK;
    public static final ModConfigSpec.IntValue KNOCKBACK_STRENGTH;
    public static final ModConfigSpec.IntValue GUARD_MOVEMENT_PERCENT;
    public static final ModConfigSpec.IntValue BLOCK_DEFLECT_CHANCE;
    public static final ModConfigSpec.IntValue SHIELD_PERFECT_TICKS, SHIELD_PARRY_TICKS, SHIELD_RECHARGE_TICKS, SHIELD_MAX_BLOCKS, TOOL_MAX_BLOCKS, SHIELD_BREAK_TICKS;
    public static final ModConfigSpec.BooleanValue COOLDOWN_PREVENTS_GUARD, FORCE_CROUCH_OFFHAND, SHIELD_SPARK_CONE;
    public static final ModConfigSpec.IntValue SHIELD_CONE_DEGREES, SHIELD_CONE_REACH, SHIELD_RETALIATION_PERCENT, SHIELD_STUN_TICKS, SHIELD_PUSHBACK_PERCENT, TOOL_PUSHBACK_PERCENT, SHIELD_PARRY_PUSHBACK_PERCENT;
    public static final ModConfigSpec.ConfigValue<String> SHIELD_STUN_BOSSES;
    public static final ModConfigSpec.BooleanValue ALLOW_USABLE_ITEMS;
    public static final ModConfigSpec.BooleanValue ALLOW_ANY_ITEM, ALLOW_EMPTY_HAND;
    public static final ModConfigSpec.BooleanValue CONSUMABLE_PRIORITY;
    public static final ModConfigSpec.ConfigValue<String> INCLUDED_ITEMS;
    public static final ModConfigSpec.ConfigValue<String> EXCLUDED_ITEMS;
    public static final ModConfigSpec.ConfigValue<String> SHIELD_ITEMS;
    public static final ModConfigSpec.BooleanValue HUD;
    public static final ModConfigSpec.BooleanValue SHIELD_EFFECTS;
    public static final ModConfigSpec.BooleanValue SPARKS_ENABLED, SPARK_RING, PARTICLE_FLASHES_ENABLED;
    public static final ModConfigSpec.BooleanValue SCREEN_FLASH, PERFECT_ONLY_FLASH, PERFECT_ONLY_HITLAG, STREAKS_ENABLED, SIMPLY_SWORDS_NOTICE_SHOWN;
    public static final ModConfigSpec.BooleanValue MEME_FLASH, IMPACT_PERFECT_ONLY;
    public static final ModConfigSpec.IntValue HITLAG_FRAMES, IMPACT_FRAMES;
    public static final ModConfigSpec.IntValue IMPACT_BRIGHTNESS, IMPACT_CONTRAST, IMPACT_EDGES, IMPACT_GRAIN;
    public static final ModConfigSpec.IntValue IMPACT_CHROMATIC, MOB_IMPACT_BRIGHTNESS, MOB_IMPACT_CONTRAST, MOB_IMPACT_EDGES, MOB_IMPACT_GRAIN, MOB_IMPACT_CHROMATIC;
    public static final ModConfigSpec.BooleanValue MOB_IMPACT_ADAPT, MOB_IMPACT_INVERT, CHROMATIC_PERFECT_ONLY, CHROMATIC_HUD, STREAK_REVERSE;
    public static final ModConfigSpec.IntValue CHROMATIC_INTENSITY, CHROMATIC_TICKS;
    public static final ModConfigSpec.IntValue FLASH_STRENGTH;
    public static final ModConfigSpec.BooleanValue SCREEN_SHAKE;
    public static final ModConfigSpec.IntValue SHAKE_STRENGTH;
    public static final ModConfigSpec.IntValue PARRY_HAND_PRIORITY, SHIELD_PARRY_PRIORITY;
    public static final ModConfigSpec.ConfigValue<String> SHIELD_BLACKLIST;
    public static final ModConfigSpec.IntValue REGULAR_ORB_SIZE;
    public static final ModConfigSpec.IntValue PERFECT_ORB_SIZE;
    public static final ModConfigSpec.IntValue REGULAR_ORB_OPACITY;
    public static final ModConfigSpec.IntValue PERFECT_ORB_OPACITY;
    public static final ModConfigSpec.BooleanValue PUNCHY_COMPAT;
    public static final ModConfigSpec.IntValue PUNCHY_RELEASE_DELAY;
    public static final java.util.List<ModConfigSpec.ConfigValue<String>> PUNCHY_POSES;
    public static final ModConfigSpec.BooleanValue FIRST_PERSON_ANIMATION, THIRD_PERSON_ANIMATION, MOB_GUARD;
    public static final ModConfigSpec.BooleanValue MOB_BLOCKING, MOB_RETALIATION;
    public static final ModConfigSpec.ConfigValue<String> MOB_HUMANOID_IDS;
    public static final ModConfigSpec.IntValue MOB_APPROACH_CHANCE, MOB_TACTICAL_CHANCE, MOB_TACTICAL_SECONDS, MOB_TACTICAL_COOLDOWN, MOB_MOVEMENT, MOB_APPROACH_DISTANCE, MOB_APPROACH_SECONDS, MOB_RUSH_CHANCE, MOB_GEAR_CHANCE;
    public static final ModConfigSpec.IntValue MOB_COUNTER_TICKS, MOB_DIFFICULTY, MOB_HITS_TO_GUARD, MOB_PERFECT_TICKS, MOB_PARRY_TICKS, MOB_PARRY_COLOR, MOB_PERFECT_COLOR;
    public static final ModConfigSpec.IntValue LOCAL_MASTER_VOLUME, LOCAL_PARRY_VOLUME, LOCAL_PERFECT_VOLUME, LOCAL_BLOCK_VOLUME;
    public static final ModConfigSpec.IntValue REGULAR_SPARK_COUNT, PERFECT_SPARK_COUNT, STAR_COUNT;
    public static final ModConfigSpec.IntValue SPARK_LIFETIME, SPARK_LENGTH, SPARK_EXPLOSIVENESS, SPARK_REACH;
    public static final ModConfigSpec.IntValue REGULAR_ORB_LIFETIME, PERFECT_ORB_LIFETIME, STREAK_AMOUNT, REGULAR_STREAK_AMOUNT, STREAK_LENGTH, STREAK_LIFETIME, STREAK_DOT_SIZE, STREAK_STRETCH_SPEED, STREAK_DOT_SPACING, REGULAR_STREAK_DOT_SPACING, STREAK_BALLISTIC_CURVE;
    // This changes only when the config schema or a default changes, never with the mod version.
    static final int CONFIG_REVISION = 41;
    private static final ModConfigSpec.IntValue SERVER_REVISION;
    private static final ModConfigSpec.IntValue CLIENT_REVISION;
    private static final ModConfigSpec.IntValue PUNCHY_REVISION;
    private static String currentVersion;
    private static Path serverConfigFolder;
    private static final List<PendingUpdate> PENDING_UPDATES = new ArrayList<>();
    private static final Map<Path, byte[]> ORIGINAL_FILES = new ConcurrentHashMap<>();

    // Record default changes here whenever CONFIG_REVISION is raised. A user's saved value
    // cannot tell us what an older default was, so this history must be explicit.
    private record DefaultChange(int revision, String path, String previous, String current) {}
    private static final List<DefaultChange> DEFAULT_CHANGES = List.of(
        new DefaultChange(4, "display.impactFrames", "3", "1"),
        new DefaultChange(4, "display.hitlagFrames", "0", "8"),
        new DefaultChange(5, "sources.fallPerfectParry", "false", "true"),
        new DefaultChange(5, "sparks.regularAmount", "100", "90"),
        new DefaultChange(5, "sparks.perfectAmount", "100", "70"),
        new DefaultChange(5, "sparks.length", "50", "100"),
        new DefaultChange(5, "display.regularOrbSize", "20", "10"),
        new DefaultChange(5, "display.regularOrbOpacity", "35", "20"),
        new DefaultChange(5, "display.regularOrbLifetime", "4", "5"),
        new DefaultChange(5, "display.perfectOrbLifetime", "4", "5"),
        new DefaultChange(6, "sources.fallPerfectParry", "true", "false"),
        new DefaultChange(6, "sources.knockbackStrength", "150", "120"),
        new DefaultChange(6, "items.allowAnyItem", "false", "true"),
        new DefaultChange(6, "shields.weaponPushback", "35", "80"),
        new DefaultChange(6, "display.regularOrbOpacity", "20", "35"),
        new DefaultChange(6, "display.perfectOrbOpacity", "35", "60"),
        new DefaultChange(6, "display.screenFlash", "false", "true"),
        new DefaultChange(6, "display.flashStrength", "0", "100"),
        new DefaultChange(6, "display.hitlagFrames", "8", "6"),
        new DefaultChange(6, "display.regularOrbLifetime", "5", "4"),
        new DefaultChange(6, "sparks.regularAmount", "90", "100% (same strength as 70% before rescaling)"),
        new DefaultChange(6, "sparks.perfectAmount", "70", "100% (same strength as 70% before rescaling)"),
        new DefaultChange(6, "sparks.lifetime", "70", "100% (same duration as 70% before rescaling)"),
        new DefaultChange(6, "sparks.stars", "2", "4"),
        new DefaultChange(6, "sparks.length", "100", "100% (same length as 200% before rescaling)"),
        new DefaultChange(6, "sparks.explosiveness", "200", "100% (same burst as 200% before rescaling)"),
        new DefaultChange(6, "sparks.streakAmount", "100", "100% (same count as 75% before rescaling)"),
        new DefaultChange(6, "sparks.streakLength", "100% of old scale", "100% of new scale"),
        new DefaultChange(6, "sparks.streakLifetime", "8", "20"),
        new DefaultChange(6, "sparks.streakDotSize", "100% of old scale", "100% of new scale"),
        new DefaultChange(6, "sparks.streakStretchSpeed", "250% of old scale", "100% of new scale"),
        new DefaultChange(8, "sources.fallPerfectParry", "false", "true"),
        new DefaultChange(8, "display.regularOrbOpacity", "35", "60"),
        new DefaultChange(8, "display.impactBrightness", "100", "186"),
        new DefaultChange(8, "display.impactContrast", "100", "110"),
        new DefaultChange(8, "display.impactEdges", "100", "200"),
        new DefaultChange(8, "display.impactGrain", "100", "101"),
        new DefaultChange(8, "sparks.perfectAmount", "100", "100% of 130% previous strength"),
        new DefaultChange(8, "sparks.streakAmount", "100", "100% of 110% previous count"),
        new DefaultChange(8, "sparks.explosiveness", "200", "100% of 200% previous speed"),
        new DefaultChange(8, "sparks.regularStreakAmount", "100", "0"),
        new DefaultChange(8, "sparks.streakLength", "100", "100% of 200% previous length"),
        new DefaultChange(8, "sparks.streakDotSize", "100", "100% of 90% previous size"),
        new DefaultChange(8, "sparks.streakStretchSpeed", "100", "100% of 180% previous speed"),
        new DefaultChange(8, "sparks.streakLifetime", "20", "30"),
        new DefaultChange(10, "sparks.streakDotSpacing", null, "100"),
        new DefaultChange(10, "sparks.streakLength", "100", "80"),
        new DefaultChange(10, "sparks.reach", "100", "10"),
        new DefaultChange(10, "sparks.length", "100", "50"),
        new DefaultChange(10, "sparks.stars", "4", "2"),
        new DefaultChange(10, "sparks.perfectAmount", "100", "130"),
        new DefaultChange(10, "sparks.regularStreakAmount", "0", "80"),
        new DefaultChange(10, "sparks.streakAmount", "100", "80"),
        new DefaultChange(10, "sparks.explosiveness", "100", "20"),
        new DefaultChange(10, "sparks.streakDotSize", "100", "80"),
        new DefaultChange(11, "sparks.streakLength", "80", "100"),
        new DefaultChange(11, "sparks.streakLifetime", "30", "40"),
        new DefaultChange(11, "sparks.streakDotSpacing", "100", "55"),
        new DefaultChange(11, "sparks.regularStreakDotSpacing", null, "55"),
        new DefaultChange(11, "defense.retaliationCap", null, "25"),
        new DefaultChange(11, "shields.maxBlocks", "3", "5"),
        new DefaultChange(11, "shields.perfectDamage", "10", "0"),
        new DefaultChange(11, "shields.rechargeTicks", "30", "20"),
        new DefaultChange(11, "sources.fallPerfectParry", "true", "false"),
        new DefaultChange(11, "display.regularOrbOpacity", "60", "100"),
        new DefaultChange(11, "display.perfectOrbOpacity", "60", "100"),
        new DefaultChange(11, "display.shakeStrength", "60", "100"),
        new DefaultChange(12, "timing.parryTicks", "6", "4"),
        new DefaultChange(12, "sources.fallPerfectParry", "false", "true"),
        new DefaultChange(12, "sources.knockbackStrength", "120", "80"),
        new DefaultChange(12, "shields.parryTicks", "8", "6"),
        new DefaultChange(12, "display.regularOrbLifetime", "4", "5"),
        new DefaultChange(12, "display.perfectOrbLifetime", "5", "6"),
        new DefaultChange(12, "sparks.perfectAmount", "previous scale", "100% (same tuned strength, new scale)"),
        new DefaultChange(12, "sparks.length", "previous scale", "100% (same tuned strength, new scale)"),
        new DefaultChange(12, "sparks.explosiveness", "previous scale", "100% (same tuned strength, new scale)"),
        new DefaultChange(12, "sparks.reach", "previous scale", "100% (same tuned strength, new scale)"),
        new DefaultChange(12, "sparks.streakLength", "previous scale", "100% (same tuned strength, new scale)"),
        new DefaultChange(12, "sparks.streakDotSize", "previous scale", "100% (same tuned strength, new scale)"),
        new DefaultChange(12, "sparks.streakDotSpacing", "previous scale", "100% (same tuned strength, new scale)"),
        new DefaultChange(12, "sparks.regularStreakDotSpacing", "previous scale", "100% (same tuned strength, new scale)"),
        new DefaultChange(12, "display.regularOrbSize", "previous scale", "100% (same tuned strength, new scale)"),
        new DefaultChange(12, "display.perfectOrbSize", "previous scale", "100% (same tuned strength, new scale)"),
        new DefaultChange(12, "shields.perfectDamage", "0", "10"),
        new DefaultChange(15, "display.regularOrbSize", "100", "10"),
        new DefaultChange(15, "display.perfectOrbSize", "100", "30"),
        new DefaultChange(15, "sparks.perfectAmount", "100", "170"),
        new DefaultChange(15, "sparks.streakAmount", "80", "90"),
        new DefaultChange(15, "sparks.streakLength", "100", "80"),
        new DefaultChange(15, "sparks.streakLifetime", "40", "30"),
        new DefaultChange(15, "sparks.streakDotSize", "100", "30"),
        new DefaultChange(15, "sparks.streakStretchSpeed", "100", "200"),
        new DefaultChange(15, "sparks.streakDotSpacing", "100", "20"),
        new DefaultChange(15, "sparks.streakBallisticCurve", "0", "40"),
        new DefaultChange(15, "sparks.regularStreakDotSpacing", "100", "60"),
        new DefaultChange(15, "sparks.length", "100", "50"),
        new DefaultChange(15, "sparks.explosiveness", "100", "80"),
        new DefaultChange(15, "sparks.reach", "100", "10"),
        new DefaultChange(15, "sparks.regularAmount", "previous per-effect scale", "shared percentage scale"),
        new DefaultChange(17, "mobs.guardDifficulty", "40% periodic stance chance at maximum", "40% initial chance after landed hits, increasing with pressure"),
        new DefaultChange(18, "mobs.blocking", "true", "false"),
        new DefaultChange(18, "mobs.swordChance", "10%", "100%"),
        new DefaultChange(18, "mobs.axeChance", "10%", "100%"),
        new DefaultChange(18, "mobs.shieldChance", "10%", "100%"),
        new DefaultChange(18, "mobs.swordShieldChance", "10%", "100%"),
        new DefaultChange(18, "mobs.axeShieldChance", "10%", "100%"),
        new DefaultChange(21, "mobs.hitsBeforeGuard", "2", "1"),
        new DefaultChange(21, "mobs.tacticalChance", "15", "35"),
        new DefaultChange(21, "mobs.tacticalMaxSeconds", "10", "5"),
        new DefaultChange(22, "mobs.enabled", "true", "false"),
        new DefaultChange(22, "stance.damageRules", "empty", "minecraft:generic_kill=1"),
        new DefaultChange(22, "timing.parryTicks", "4", "6"),
        new DefaultChange(22, "mobs.perfectTicks", "8", "20"),
        new DefaultChange(22, "mobs.parryTicks", "20", "60"),
        new DefaultChange(22, "mobs.approachChance", "40", "100"),
        new DefaultChange(22, "mobs.tacticalChance", "35", "100"),
        new DefaultChange(22, "mobs.retaliationEnabled", "false", "true"),
        new DefaultChange(22, "mobs.guardMovement", "90", "100"),
        new DefaultChange(22, "mobs.approachDistance", "3", "2"),
        new DefaultChange(22, "mobs.approachMaxSeconds", "3", "2"),
        new DefaultChange(22, "mobs.rushChance", "60", "70"),
        new DefaultChange(22, "mobs.gearChance", "40", "50"),
        new DefaultChange(22, "display.impactBrightness", "186", "400"),
        new DefaultChange(22, "display.impactContrast", "110", "200"),
        new DefaultChange(22, "display.impactEdges", "200", "400"),
        new DefaultChange(22, "display.impactGrain", "101", "400"),
        new DefaultChange(22, "display.impactChromatic", "30", "400"),
        new DefaultChange(22, "display.chromaticIntensity", "35", "200"),
        new DefaultChange(22, "display.chromaticTicks", "8", "10"),
        new DefaultChange(22, "display.chromaticHud", "false", "true"),
        new DefaultChange(24, "display.streakReverse", "false", "true"),
        new DefaultChange(24, "display.regularOrbSize", "10", "20"),
        new DefaultChange(24, "display.perfectOrbSize", "30", "40"),
        new DefaultChange(24, "sparks.streakAmount", "90", "100"),
        new DefaultChange(24, "sparks.regularStreakAmount", "80", "100"),
        new DefaultChange(24, "sparks.streakLength", "80", "150"),
        new DefaultChange(24, "sparks.streakLifetime", "30", "25"),
        new DefaultChange(24, "sparks.streakDotSize", "30", "100"),
        new DefaultChange(24, "sparks.streakDotSpacing", "20", "35"),
        new DefaultChange(24, "sparks.streakBallisticCurve", "40", "20"),
        new DefaultChange(24, "sparks.regularStreakDotSpacing", "60", "50"),
        new DefaultChange(24, "stance.damageRules", "minecraft:generic_kill=1", "minecraft:generic_kill=1,minecraft:arrow=1,minecraft:lava=0,minecraft:hot_floor=0"),
                new DefaultChange(24, "items.allowAnyItem", "true", "false"),
        new DefaultChange(24, "shields.perfectTicks", "2", "1"),
        new DefaultChange(24, "shields.rechargeTicks", "20", "10"),
        new DefaultChange(24, "animation.firstPersonGuardPose", "true", "false"),
        new DefaultChange(28, "items.allowUsableItems", "true", "false"),
        new DefaultChange(29, "pose1", "200 ms transitions", "250 ms transitions"),
        new DefaultChange(29, "pose2", "200 ms transitions", "250 ms transitions"),
        new DefaultChange(29, "pose3", "200 ms transitions", "250 ms transitions"),
        new DefaultChange(29, "pose4", "300 ms transitions", "250 ms transitions"),
        new DefaultChange(29, "pose5", "200/180 ms transitions", "250 ms transitions"),
        new DefaultChange(33, "pose1", "250 ms transitions", "original pose; 200 ms transitions"),
        new DefaultChange(33, "pose2", "250 ms transitions", "200 ms transitions"),
        new DefaultChange(33, "pose3", "250 ms transitions", "200 ms transitions"),
        new DefaultChange(33, "pose4", "250 ms transitions", "300 ms transitions"),
        new DefaultChange(33, "pose5", "enabled; 250 ms transitions", "disabled; 200/180 ms transitions"),
        new DefaultChange(34, "pose1–pose5", "previous preset defaults", "uploaded presets; 180/240 ms transitions; Preset 5 enabled and empty-hand only"),
        new DefaultChange(36, "shields.shieldParryPriority", "1 (Shield)", "2 (Default: player main hand)"),
        new DefaultChange(37, "pose1", "180/240 ms transitions", "300 ms entry; 360 ms release"),
        new DefaultChange(37, "pose2", "180/240 ms transitions", "300 ms entry; 360 ms release"),
        new DefaultChange(37, "pose3", "180/240 ms transitions", "300 ms entry; 360 ms release"),
        new DefaultChange(37, "pose4", "180/240 ms transitions", "300 ms entry; 360 ms release"),
        new DefaultChange(37, "pose5", "180/240 ms transitions", "300 ms entry; 360 ms release")
    );
    private record PendingUpdate(ModConfig config, List<String> changes) {}

    static {
        ModConfigSpec.Builder server = new ModConfigSpec.Builder();
        SERVER_REVISION = GuardSettingRanges.integer(server.comment("Mallard Guard config format revision. This changes only when settings or their defaults change."),
            "configRevision", CONFIG_REVISION, 0, Integer.MAX_VALUE);
        server.push("stance");
        PARRY = server.comment("Allow parries when the guard key is pressed. Right Click by default; the key can be changed.").define("parry", true);
        DAMAGE_RULES = server.comment("Per-damage-type parry rules, stored as damage_type_id=1 or =0, separated by commas. Configure in game.")
            .define("damageRules", "minecraft:generic_kill=1,minecraft:arrow=1,minecraft:lava=0,minecraft:hot_floor=0", value -> value instanceof String rules && GuardDamageRules.validRules(rules));
        OBSERVED_PROJECTILE_SOURCES = server.comment("Automatically learned projectile entity ID to damage type ID associations. Updated after players are hit; used to honor modded projectile source rules at impact.")
            .define("observedProjectileSources", "", value -> value instanceof String mapping && GuardDamageRules.validProjectileSources(mapping));
        BLOCK = server.comment("Keep guarding after the parry window while the guard key is held.").define("block", true);
        TOOL_MAX_BLOCKS = GuardSettingRanges.integer(server.comment("Successful weapon or empty-hand blocks before guard breaks; 0 allows unlimited blocks. Guard break delays the next guard but does not disable attacking with the item."), "toolMaxBlocks", 3, 0, 5);
        server.pop();
        server.push("timing");
        PERFECT_TICKS = GuardSettingRanges.integer(server.comment("Perfect parry duration for tools; zero disables perfect parries."), "perfectTicks", 2, 0, 10);
        PARRY_TICKS = GuardSettingRanges.integer(server.comment("Total weapon parry duration in ticks, including perfect parry ticks."), "parryTicks", 6, 1, 10);
        RECHARGE_TICKS = GuardSettingRanges.integer(server.comment("Time before another attempt; continues during held guard."), "rechargeTicks", 10, 1, 60);
        server.pop();
        server.push("defense");
        FACING_ANGLE = GuardSettingRanges.integer(server.comment("Maximum angle from the player's view direction; 180 allows the front half, 360 allows attacks from any direction."), "facingAngle", 180, 0, 360);
        BLOCK_REDUCTION = GuardSettingRanges.decimal(server.comment("Fraction of incoming entity damage prevented while holding guard."), "blockReduction", 0.5, 0.0D, 1.0D);
        FOLLOW_UP_TICKS = GuardSettingRanges.integer(server.comment("Ticks after a successful parry that catch additional valid hits; zero disables it. Follow-up hits never extend this window."), "followUpParryTicks", 3, 0, 20);
        PARRY_RETALIATION = GuardSettingRanges.decimal(server.comment("Regular parry damage returned as a fraction of the incoming damage. This damage adds no knockback; attacker pushback is separate. 0 disables it; 2 doubles it."), "parryRetaliation", 0.25, 0.0D, 2.0D);
        PERFECT_RETALIATION = GuardSettingRanges.decimal(server.comment("Perfect parry damage returned as a fraction of the incoming damage. This damage adds no knockback; attacker pushback is separate. 0 disables it; 2 doubles it."), "perfectRetaliation", 0.5, 0.0D, 2.0D);
        RETALIATION_CAP = GuardSettingRanges.integer(server.comment("Maximum damage returned by one parry to each target after the retaliation multiplier. 0 removes the cap. Applies to weapon and shield retaliation."), "retaliationCap", 25, 0, 1000);
        PARRY_WEAR = GuardSettingRanges.integer(server.comment("Wear in tenths of a percent of maximum durability per regular or follow-up parry. 2 means 0.2%; rounded up. 0 disables wear."), "parryWear", 2, 0, 100);
        PERFECT_WEAR = GuardSettingRanges.integer(server.comment("Wear in tenths of a percent of maximum durability per perfect parry. 1 means 0.1%; rounded up. 0 disables wear."), "perfectWear", 1, 0, 100);
        BLOCK_WEAR = GuardSettingRanges.integer(server.comment("Wear in tenths of a percent of maximum durability per blocked hit. 4 means 0.4%; rounded up. Real shields keep their own block wear."), "blockWear", 4, 0, 100);
        server.pop();
        server.push("effects");
        HIT_SOUNDS = server.comment("Play the supplied parry and block sounds to nearby players.").define("hitSounds", true);
        HIT_PARTICLES = server.comment("Send parry sparks, Streak Lines, and spherical flashes to nearby players; perfect parries also show star-shaped sparks. Blocks create none.").define("hitParticles", true);
        MASTER_VOLUME = GuardSettingRanges.integer(server.comment("Server master volume for guard sounds sent to nearby clients. 100% plays the recorded level; each listener's client master and individual volume settings multiply it. 0% mutes the sounds."), "masterVolume", 100, 0, 200);
        PERFECT_VOLUME = GuardSettingRanges.integer(server.comment("Perfect parry sound volume, multiplied by master volume."), "perfectVolume", 130, 0, 200);
        PARRY_VOLUME = GuardSettingRanges.integer(server.comment("Regular parry sound volume, multiplied by master volume."), "parryVolume", 65, 0, 200);
        BLOCK_VOLUME = GuardSettingRanges.integer(server.comment("Held block sound volume, multiplied by master volume."), "blockVolume", 65, 0, 200);
        server.pop();
        server.push("clientEnforcement");
        ENFORCE_CLIENT = server.comment("Apply server chosen client defaults when players connect and lock categories without a Don't Enforce exception.").define("enabled", false);
        CLIENT_EXEMPT_MASK = GuardSettingRanges.integer(server.comment("Client categories editable after receiving server defaults: 1 world particles, 2 HUD icons, 4 audio, 8 screen effects, 16 animations, 32 experimental Punchy poses."), "dontEnforceMask", 0, 0, GuardClientPreset.ALL_CATEGORIES);
        CLIENT_PRESET = server.comment("Client defaults shared by the server; changed through the Enforce configuration screen.")
            .define("defaults", GuardClientPreset.encode(GuardClientPreset.DEFAULTS), value -> value instanceof String data && GuardClientPreset.valid(data));
        server.pop();
        server.push("sources");
        FALL_LOOK_DOWN = server.comment("Require looking at least 40 degrees below the horizon to parry a fall. On: preserve current horizontal momentum, bouncing in place when still. Off: fall parries work in any direction and launch where you look.").define("fallLookDown", true);
        FALL_PERFECT_PARRY = server.comment("Allow perfect parries on fall damage or wall collisions. Off: catching either impact in the perfect window performs a regular parry instead.").define("fallPerfectParry", true);
        FALL_BREAK_BLOCKS = server.comment("Allow the blast from a long fall parry to break terrain. Disable to prevent terrain damage.").define("fallBlastBreaksBlocks", true);
        FALL_BLAST_STRENGTH = GuardSettingRanges.integer(server.comment("Fall parry blast scaling as a percentage of Parry It's default 0.1 radius per damage; 0 disables the blast."), "fallBlastStrength", 100, 0, 200);
        FALL_LAUNCH_POWER = GuardSettingRanges.integer(server.comment("Fall parry launch strength as a percentage of Parry It's default. Perfect falls launch twice as far. 0 disables launch."), "fallLaunchPower", 10, 0, 200);
        PARRY_EXPLOSIONS = server.comment("Allow parry timing to negate explosion damage.").define("parryExplosions", true);
        PERFECT_EXPLOSIONS_ONLY = server.comment("When explosion parrying is enabled, require a perfect parry; regular parries do not negate explosions.").define("perfectExplosionsOnly", false);
        BLOCK_EXPLOSIONS = server.comment("Allow held block to reduce explosion damage by the normal block reduction.").define("blockExplosions", true);
        PARRY_PROJECTILES = server.comment("Parry projectile entities. Regular parries deflect them randomly; perfect parries send them toward the original owner.").define("parryProjectiles", true);
        BLOCK_PROJECTILES = server.comment("Allow held block to reduce projectile damage by the normal block reduction.").define("blockProjectiles", true);
        DEFENDER_KNOCKBACK = server.comment("Push the defender away from the impact on successful melee, projectile, and explosion parries. Shield parries use half strength.").define("defenderKnockback", true);
        KNOCKBACK_STRENGTH = GuardSettingRanges.integer(server.comment("Push strength for the defender in percent; shield parries use half the configured strength. 0 disables both."), "knockbackStrength", 80, 0, 200);
        GUARD_MOVEMENT_PERCENT = GuardSettingRanges.integer(server.comment("Movement speed while guarding, including parry and block. 100 keeps normal movement speed."), "guardMovementPercent", 55, 0, 100);
        BLOCK_DEFLECT_CHANCE = GuardSettingRanges.integer(server.comment("Chance to deflect a projectile during held block. 0 disables deflection; every projectile type uses the same roll."), "blockDeflectChance", 0, 0, 100);
        server.pop();
        server.push("items");
        ALLOW_ANY_ITEM = server.comment("Off: recognized weapons and shields plus extra allowed items may guard. On: all held items except excluded items may guard. Empty hands use a separate toggle.").define("allowAnyItem", false);
        ALLOW_EMPTY_HAND = server.comment("Allow guarding with an empty hand. Separate from allowing all held items; off by default.").define("allowEmptyHand", false);
        CONSUMABLE_PRIORITY = server.comment("When enabled, food and drinks in either hand take priority over guard, even if the other hand holds a weapon. Enabled by default.").define("consumablePriority", true);
        ALLOW_USABLE_ITEMS = server.comment("Allow eligible items with a hold-to-use action to guard. Whitelisted items and universal guard are also allowed.").define("allowUsableItems", false);
        INCLUDED_ITEMS = server.comment("Item IDs or #tags allowed beyond the tool requirement. The item blacklist still takes priority.").define("includedItems", "", value -> value instanceof String rules && GuardItemRules.valid(rules));
        EXCLUDED_ITEMS = server.comment("Item IDs or #tags that cannot guard in any mode. Overrides whitelist and shield recognition. Empty hands use a separate toggle.").define("excludedItems", "", value -> value instanceof String rules && GuardItemRules.valid(rules));
        SHIELD_ITEMS = server.comment("Extra item IDs or #tags treated as shields. Built-in shields and blocking items are detected automatically. A whitelist or blacklist can still restrict them.").define("shieldItems", "", value -> value instanceof String rules && GuardItemRules.valid(rules));
        COOLDOWN_PREVENTS_GUARD = server.comment("When on, an item with an active vanilla or modded cooldown cannot enter the guard stance. Another eligible hand can still guard.").define("cooldownPreventsGuard", true);
        PARRY_HAND_PRIORITY = GuardSettingRanges.integer(server.comment("Guarding hand: 0 Random, 1 Off-Hand, 2 Main-Hand. Falls back to the other eligible hand."), "parryHandPriority", 0, 0, 2);
        SHIELD_PARRY_PRIORITY = GuardSettingRanges.integer(server.comment("When either eligible hand holds a shield: 0 Random, 1 Shield, 2 Default (player main hand). Falls back to the other eligible hand. Force Crouch Off-Hand overrides this."), "shieldParryPriority", 2, 0, 2);
        FORCE_CROUCH_OFFHAND = server.comment("Crouching selects the eligible offhand before other hand priorities.").define("forceCrouchOffhand", true);
        SHIELD_BLACKLIST = server.comment("Item IDs or #tags excluded from shield recognition. Takes priority over shield tags and whitelist.").define("shieldBlacklist", "", value -> value instanceof String rules && GuardItemRules.valid(rules));
        server.pop();
        server.push("shields");
        SHIELD_SPARK_CONE = server.comment("Off: shield sparks spray in a sphere. On: sparks and stars fan toward the attack in a 90-degree cone. Streak Lines remain spherical.").define("coneSparks", false);
        SHIELD_PERFECT_TICKS = GuardSettingRanges.integer(server.comment("Shield perfect parry window, in ticks. 0 turns off shield perfect parries; cannot exceed total shield parry ticks."), "perfectTicks", 1, 0, 10);
        SHIELD_PARRY_TICKS = GuardSettingRanges.integer(server.comment("Total shield parry window in ticks, including perfect ticks; blocking follows without restarting shield use."), "parryTicks", 6, 1, 10);
        SHIELD_RECHARGE_TICKS = GuardSettingRanges.integer(server.comment("Shield parry recharge after leaving guard; separate from guard break recharge. Regular shield parries halve it and perfect shield parries reset it, like tool parries."), "rechargeTicks", 10, 1, 60);
        SHIELD_MAX_BLOCKS = GuardSettingRanges.integer(server.comment("Successful shield blocks before guard breaks; 0 allows unlimited blocks. Disabled shields still obey vanilla cooldown."), "maxBlocks", 5, 0, 10);
        SHIELD_BREAK_TICKS = GuardSettingRanges.integer(server.comment("Recharge after the last allowed shield or weapon block. Shields also receive an item cooldown; weapons remain usable for attacking. At least the normal parry recharge applies. Default 100 ticks = 5 seconds."), "breakTicks", 100, 1, 200);
        SHIELD_CONE_DEGREES = GuardSettingRanges.integer(server.comment("Angle to each side of your view for the shield perfect parry cone. 90 degrees covers the whole front half (180 degrees total). 0 disables cone effects."), "perfectCone", 90, 0, 90);
        SHIELD_CONE_REACH = GuardSettingRanges.integer(server.comment("Maximum eye-to-eye distance in blocks for shield perfect parry cone effects."), "perfectConeReach", 5, 1, 10);
        SHIELD_RETALIATION_PERCENT = GuardSettingRanges.integer(server.comment("Shield perfect parry damage to each target in its cone as a percentage of incoming damage. 0 disables damage; stun and pushback still apply."), "perfectDamage", 10, 0, 30);
        SHIELD_STUN_TICKS = GuardSettingRanges.integer(server.comment("Time in ticks that normal targets hit by a shield perfect parry cannot voluntarily move or attack; 0 disables the stun."), "stunTicks", 30, 0, 100);
        SHIELD_PUSHBACK_PERCENT = GuardSettingRanges.integer(server.comment("Shield perfect parry cone pushback strength; 0 disables pushback. Knockback applies before stun."), "perfectPushback", 35, 0, 200);
        SHIELD_PARRY_PUSHBACK_PERCENT = GuardSettingRanges.integer(server.comment("Shield regular parry pushback on the attacker; 0 disables pushback."), "regularPushback", 85, 0, 200);
        TOOL_PUSHBACK_PERCENT = GuardSettingRanges.integer(server.comment("Regular and perfect weapon parry pushback on the attacker; 0 disables pushback."), "weaponPushback", 80, 0, 200);
        SHIELD_STUN_BOSSES = server.comment("Boss entity IDs or #entity tags explicitly allowed to be stunned. By default, bosses are immune to stun.").define("stunnableBosses", "", value -> value instanceof String rules && GuardItemRules.valid(rules));
        server.pop();
        server.push("punchy");
        server.pop();
        server.push("mobs");
        MOB_BLACKLIST = server.comment("Entity IDs excluded from mob guarding and generated gear. Comma-separated. Default: vindicators, evokers, pillagers, and villagers.").define("blacklist", "minecraft:vindicator,minecraft:evoker,minecraft:pillager,minecraft:villager", value -> value instanceof String ids && GuardItemRules.valid(ids) && !ids.contains("#"));
        MOB_GUARD = server.comment("Allow supported non-boss, goal-driven humanoids with eligible weapons or shields to use approach, tactical, and damage-pressure guarding. Empty hands do not qualify.").define("enabled", false);
        MOB_DIFFICULTY = GuardSettingRanges.integer(server.comment("Scales approach, tactical, and damage-pressure chances. At 100%, pressure begins at 40% after the configured hit count and rises with further damaging hits. Pressure resets after entering a behavior or 10 seconds without damage. 0 disables new guarding decisions."), "guardDifficulty", 100, 0, 100);
        MOB_HITS_TO_GUARD = GuardSettingRanges.integer(server.comment("Landed damaging combat hits needed for a pressure-based guarding attempt. Does not limit approach or tactical guarding. Default: 1. The hit that triggers the stance still deals damage; the mob can parry subsequent attacks."), "hitsBeforeGuard", 1, 1, 3);
        MOB_PERFECT_TICKS = GuardSettingRanges.integer(server.comment("Perfect portion at the start of a mob parry stance. Applies to mob weapons and shields, independently of player timing. 0 disables perfect parries. Default: 20 ticks."), "perfectTicks", 20, 0, 20);
        MOB_PARRY_TICKS = GuardSettingRanges.integer(server.comment("Regular mob parry duration after its perfect portion, for weapons and shields. Default: 60 ticks."), "parryTicks", 60, 1, 60);
        MOB_BLOCKING = server.comment("Allow blocking for 20 ticks after each raised parry window, using full shield blocking or tool damage reduction. Default: false.").define("blocking", false);
        MOB_COUNTER_TICKS = GuardSettingRanges.integer(server.comment("Maximum duration of a fast counter approach after a mob parry. One melee attack at most, followed by normal AI. 0 disables this counter approach. Default: 12 ticks."), "counterTicks", 12, 0, 40);
        MOB_APPROACH_CHANCE = GuardSettingRanges.integer(server.comment("Chance to raise guard on the first close approach to a target, within the randomly chosen approach distance. Scaled by mob guard difficulty. 0 disables. Default: 100%."), "approachChance", 100, 0, 100);
        MOB_TACTICAL_CHANCE = GuardSettingRanges.integer(server.comment("Chance per scheduled combat decision (every 2-4 seconds) to begin tactical guarding. Scaled by difficulty and limited by its cooldown. 0 disables. Default: 100%."), "tacticalChance", 100, 0, 100);
        MOB_TACTICAL_SECONDS = GuardSettingRanges.integer(server.comment("Maximum tactical sequence duration. Each sequence randomly lasts 1 second through this maximum, choosing either stillness or strafing. Raised windows alternate with vulnerable 5-10 tick gaps. Default: 5 seconds."), "tacticalMaxSeconds", 5, 1, 10);
        MOB_TACTICAL_COOLDOWN = GuardSettingRanges.integer(server.comment("Ticks after a tactical sequence's reserved end before another may begin. Default: 200 ticks (10 seconds)."), "tacticalCooldownTicks", 200, 20, 1200);
        MOB_HUMANOID_IDS = server.comment("Additional entity IDs explicitly declared to be non-boss, goal-driven humanoids. Separate IDs with commas. Built-in zombie/skeleton families and vindicators are supported automatically. Brain-based mobs and custom attack/animation systems require integration. Known or tagged bosses are always excluded. Default: empty.").define("humanoidIds", "", value -> value instanceof String ids && GuardItemRules.valid(ids) && !ids.contains("#"));
        MOB_RETALIATION = server.comment("Return damage on mob parries using the server's regular/perfect retaliation multipliers and cap. Shield perfect parries use the shield damage percentage. Default: true.").define("retaliationEnabled", true);
        MOB_PARRY_COLOR = GuardSettingRanges.integer(server.comment("RGB color for mob regular-parry particles, stored as a decimal RGB integer. The red HUD reaction uses its own artwork."), "parryColor", 16733525, 0, 0xFFFFFF);
        MOB_PERFECT_COLOR = GuardSettingRanges.integer(server.comment("RGB color for mob perfect-parry particles, stored as a decimal RGB integer. The red HUD reaction uses its own artwork."), "perfectColor", 16724804, 0, 0xFFFFFF);
        MOB_MOVEMENT = GuardSettingRanges.integer(server.comment("Movement speed retained while a mob guards. Independent of player guarding speed. 100% causes no guard slowdown; 0% prevents deliberate movement. Default: 100%."), "guardMovement", 100, 0, 100);
        MOB_APPROACH_DISTANCE = GuardSettingRanges.integer(server.comment("Maximum distance for surprise approach guarding. Each encounter chooses a trigger distance between 1 block and this maximum. Default: 2 blocks."), "approachDistance", 2, 1, 5);
        MOB_APPROACH_SECONDS = GuardSettingRanges.integer(server.comment("Maximum approach stance duration. Each attempt lasts randomly from 1 second through this maximum. Parry windows repeat with vulnerable gaps if needed. Default: 2 seconds."), "approachMaxSeconds", 2, 1, 10);
        MOB_RUSH_CHANCE = GuardSettingRanges.integer(server.comment("Chance to immediately rush the target after a successful mob parry, instead of lowering guard or raising one follow-up stance. Default: 70%."), "rushChance", 70, 0, 100);
        MOB_GEAR_CHANCE = GuardSettingRanges.integer(server.comment("Chance to assign a fresh vanilla zombie, skeleton, pillager, or vindicator a balanced loadout. Existing modded gear is preserved. Tool tiers are stone, iron, gold, and rarely diamond. Melee loadouts may include a shield; ranged loadouts never do. Default: 50%."), "gearChance", 50, 0, 100);
        server.pop();


        SERVER_SPEC = server.build();

        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        CLIENT_REVISION = client.comment("Mallard Guard config format revision. This changes only when settings or their defaults change.")
            .defineInRange("configRevision", CONFIG_REVISION, 0, Integer.MAX_VALUE);
        client.push("display");
        HUD = client.comment("Show the parry shield by the crosshair.").define("hud", GuardClientSettings.initialFlag(0));
        SHIELD_EFFECTS = client.comment("Animate the guard icon: a ready outline and parry flashes. Tool blocks flash red, shield blocks emit a short white echo, and the darker icon trembles with repeating echoes while held.").define("shieldEffects", GuardClientSettings.initialFlag(1));
        SPARKS_ENABLED = client.comment("Show flying sparks and perfect parry stars on this client. Disable without changing your spark settings.").define("sparksEnabled", GuardClientSettings.initialFlag(22));
        STREAKS_ENABLED = client.comment("Show Streak Lines made from white square dots. The line stretches from the hit center and fades from center to tip; bouncing sparks are separate.").define("streaksEnabled", GuardClientSettings.initialFlag(26));
        STREAK_REVERSE = client.comment("Reverse the streak gradient while keeping its color proportions: warm/red at the center and white at the tip. Default: false.").define("streakReverse", GuardClientSettings.initialFlag(62));
        SPARK_RING = client.comment("Off: sparks spray in a rounded sphere. On: four shorter diagonal sprays form a compact X. Fall parries always use the compact X; shield parries have their own sphere/cone setting.").define("sparkRing", GuardClientSettings.initialFlag(25));
        PARTICLE_FLASHES_ENABLED = client.comment("Show spherical impact flash particles on this client. Disable without changing size, opacity, or lifetime settings.").define("particleFlashesEnabled", GuardClientSettings.initialFlag(23));
        SCREEN_FLASH = client.comment("Brief white screen flash after parries. Meme flash always uses a full white flash. Blocks never flash.").define("screenFlash", GuardClientSettings.initialFlag(2));
        PERFECT_ONLY_FLASH = client.comment("When on, the ordinary white screen flash occurs only on perfect parries. Meme flash still shows a white flash for either parry.").define("perfectOnlyFlash", GuardClientSettings.initialFlag(27));
        PERFECT_ONLY_HITLAG = client.comment("When on, only perfect parries freeze the client screen. 0 hitlag frames disables freezing regardless.").define("perfectOnlyHitlag", GuardClientSettings.initialFlag(28));
        MEME_FLASH = client.comment("Show one of four full-screen meme images alongside a local meme clip. Both start with the white parry flash after hitlag; the image holds 0.5 seconds and fades over 0.2 seconds. Off by default; blocks never trigger it.").define("memeFlash", GuardClientSettings.initialFlag(24));
        IMPACT_FRAMES = client.comment("Show one freshly captured rough grayscale impact image after hitlag. Its hold time matches the hitlag frame setting, with a one-frame minimum. 0 disables it, 1 enables it.").defineInRange("impactFrames", GuardClientSettings.initial(34), GuardClientSettings.minimum(34), GuardClientSettings.maximum(34));
        IMPACT_PERFECT_ONLY = client.comment("When enabled, only perfect parries show the grayscale impact image. Regular parries skip it. Enabled by default.").define("impactPerfectOnly", GuardClientSettings.initialFlag(35));
        IMPACT_BRIGHTNESS = client.comment("Brightness of the captured impact image; adjust inside the live preview.").defineInRange("impactBrightness", GuardClientSettings.initial(40), GuardClientSettings.minimum(40), GuardClientSettings.maximum(40));
        IMPACT_CONTRAST = client.comment("Contrast of the captured impact image; adjust inside the live preview.").defineInRange("impactContrast", GuardClientSettings.initial(41), GuardClientSettings.minimum(41), GuardClientSettings.maximum(41));
        IMPACT_EDGES = client.comment("Darkening of edges in the captured impact image; adjust inside the live preview.").defineInRange("impactEdges", GuardClientSettings.initial(42), GuardClientSettings.minimum(42), GuardClientSettings.maximum(42));
        IMPACT_GRAIN = client.comment("Fine roughness in the captured impact image; adjust inside the live preview.").defineInRange("impactGrain", GuardClientSettings.initial(43), GuardClientSettings.minimum(43), GuardClientSettings.maximum(43));
        IMPACT_CHROMATIC = client.comment("RGB channel separation in your still impact image. 0 disables it. Adjust in the preview. Default: 400%.").defineInRange("impactChromatic", GuardClientSettings.initial(50), GuardClientSettings.minimum(50), GuardClientSettings.maximum(50));
        MOB_IMPACT_BRIGHTNESS = client.comment("Independent brightness when a mob perfectly parries you; used with Adapt off.").defineInRange("mobImpactBrightness", GuardClientSettings.initial(51), GuardClientSettings.minimum(51), GuardClientSettings.maximum(51));
        MOB_IMPACT_CONTRAST = client.comment("Independent mob impact contrast, with Adapt off.").defineInRange("mobImpactContrast", GuardClientSettings.initial(52), GuardClientSettings.minimum(52), GuardClientSettings.maximum(52));
        MOB_IMPACT_EDGES = client.comment("Independent mob impact edge strength, with Adapt off.").defineInRange("mobImpactEdges", GuardClientSettings.initial(53), GuardClientSettings.minimum(53), GuardClientSettings.maximum(53));
        MOB_IMPACT_GRAIN = client.comment("Independent mob impact grain, with Adapt off.").defineInRange("mobImpactGrain", GuardClientSettings.initial(54), GuardClientSettings.minimum(54), GuardClientSettings.maximum(54));
        MOB_IMPACT_CHROMATIC = client.comment("Independent mob impact RGB separation, with Adapt off.").defineInRange("mobImpactChromatic", GuardClientSettings.initial(55), GuardClientSettings.minimum(55), GuardClientSettings.maximum(55));
        MOB_IMPACT_ADAPT = client.comment("Mob impact images follow your own impact settings. Turn off to customize them independently. Default: true.").define("mobImpactAdapt", GuardClientSettings.initialFlag(56));
        MOB_IMPACT_INVERT = client.comment("Invert the mob impact image, independently of Adapt. Default: true.").define("mobImpactInvert", GuardClientSettings.initialFlag(57));
        CHROMATIC_INTENSITY = client.comment("Animated RGB separation after parries. Smoothly expands and settles. 0 disables it. Default: 200%.").defineInRange("chromaticIntensity", GuardClientSettings.initial(58), GuardClientSettings.minimum(58), GuardClientSettings.maximum(58));
        CHROMATIC_TICKS = client.comment("Duration of animated RGB separation; changing duration also changes animation speed. Default: 10 ticks.").defineInRange("chromaticTicks", GuardClientSettings.initial(59), GuardClientSettings.minimum(59), GuardClientSettings.maximum(59));
        CHROMATIC_PERFECT_ONLY = client.comment("Only perfect parries trigger animated RGB separation when enabled. Off applies it to both parry types. Default: false.").define("chromaticPerfectOnly", GuardClientSettings.initialFlag(60));
        CHROMATIC_HUD = client.comment("Also separate HUD color channels. Off processes the world view before HUD drawing. Default: true.").define("chromaticHud", GuardClientSettings.initialFlag(61));

        HITLAG_FRAMES = client.comment("Hold the captured screen for this many 60 FPS equivalent frames. The impact image, then the local flash, particles and sound follow. Perfect parry retaliation waits for this feedback to finish; disabling hitlag returns damage immediately. 0 disables hitlag but allows a one-frame impact image.").defineInRange("hitlagFrames", GuardClientSettings.initial(3), GuardClientSettings.minimum(3), GuardClientSettings.maximum(3));
        FLASH_STRENGTH = client.comment("Strength of the brief screen flash, as a percentage. 0 disables it.").defineInRange("flashStrength", GuardClientSettings.initial(4), GuardClientSettings.minimum(4), GuardClientSettings.maximum(4));
        SCREEN_SHAKE = client.comment("Briefly shake the camera on regular parries, perfect parries, and blocks. Blocks shake most strongly. Client-side only.").define("screenShake", true);
        SHAKE_STRENGTH = client.comment("Camera shake strength as a percentage. 0 disables it; blocks shake most strongly.").defineInRange("shakeStrength", GuardClientSettings.initial(5), GuardClientSettings.minimum(5), GuardClientSettings.maximum(5));
        REGULAR_ORB_SIZE = client.comment("Regular spherical flash size as a percentage of the full-size flash. Uses the same scale as perfect flashes. 0 hides this flash.").defineInRange("regularOrbSize", GuardClientSettings.initial(7), GuardClientSettings.minimum(7), GuardClientSettings.maximum(7));
        PERFECT_ORB_SIZE = client.comment("Perfect spherical flash size as a percentage of the full-size flash. Uses the same scale as regular flashes. 0 hides this flash.").defineInRange("perfectOrbSize", GuardClientSettings.initial(8), GuardClientSettings.minimum(8), GuardClientSettings.maximum(8));
        REGULAR_ORB_OPACITY = client.comment("Opacity of the regular parry flash. 0 hides this flash.").defineInRange("regularOrbOpacity", GuardClientSettings.initial(9), GuardClientSettings.minimum(9), GuardClientSettings.maximum(9));
        PERFECT_ORB_OPACITY = client.comment("Opacity of the perfect parry flash. 0 hides this flash.").defineInRange("perfectOrbOpacity", GuardClientSettings.initial(10), GuardClientSettings.minimum(10), GuardClientSettings.maximum(10));
        REGULAR_ORB_LIFETIME = client.comment("Regular parry spherical flash lifetime in ticks.").defineInRange("regularOrbLifetime", GuardClientSettings.initial(33), GuardClientSettings.minimum(33), GuardClientSettings.maximum(33));
        PERFECT_ORB_LIFETIME = client.comment("Perfect parry spherical flash lifetime in ticks, separate from regular parry.").defineInRange("perfectOrbLifetime", GuardClientSettings.initial(36), GuardClientSettings.minimum(36), GuardClientSettings.maximum(36));
        client.pop();
        client.push("animation");
        SIMPLY_SWORDS_NOTICE_SHOWN = client.comment("Whether the Simply Swords keybind notice has already appeared in chat on this client.").define("simplySwordsNoticeShown", false);
        THIRD_PERSON_ANIMATION = client.comment("Show the simple blocking pose on yourself and other guarding players and humanoid mobs. Off hides this pose for everyone on your client; server guard mechanics do not change.").define("thirdPersonGuardPose", GuardClientSettings.initialFlag(49));
        FIRST_PERSON_ANIMATION = client.comment("Use Mallard Guard's first-person weapon pose. Disable when another mod animates your hands; shields always use vanilla animation.").define("firstPersonGuardPose", GuardClientSettings.initialFlag(11));
        client.pop();
        ModConfigSpec.Builder punchy = new ModConfigSpec.Builder();
        PUNCHY_REVISION=punchy.comment("Internal config schema revision.").defineInRange("configRevision",CONFIG_REVISION,0,Integer.MAX_VALUE);
        PUNCHY_COMPAT = punchy.comment("Render eligible guard items through Punchy 2.8b or 2.8d. While Punchy is installed and compatibility enabled, the simple first-person guard animation is disabled. Default: true.").define("enabled", true);
        PUNCHY_RELEASE_DELAY = GuardSettingRanges.integer(punchy.comment("Extra hold after client hitlag finishes. Releases without hitlag have no extra delay. Applies to every preset. 0 removes the delay. Default: 50 ms."), "releaseDelayMillis", 50, 0, 1000);
        java.util.List<ModConfigSpec.ConfigValue<String>> poses = new java.util.ArrayList<>();
        for (int i = 0; i < GuardPoseSettings.COUNT; i++) {
            poses.add(punchy.comment("Preset " + (i + 1) + ": enabled, active hand position/rotation, supporting hand position/rotation, weapon position/rotation, raise/lower milliseconds, easing curve, easing strength. Positions use tenths of a model pixel; rotations use degrees. Curves: 0 linear, 1 ease in, 2 ease out, 3 ease in/out.")
                .define("pose" + (i + 1), GuardPoseSettings.encodePose(GuardPoseSettings.defaultPose(i)), value -> value instanceof String text && GuardPoseSettings.parsePose(text) != null));
        }
        PUNCHY_POSES = java.util.List.copyOf(poses);
        PUNCHY_SPEC = punchy.build();
        client.push("audio");
        LOCAL_MASTER_VOLUME = client.comment("Your own master playback level for Mallard Guard sounds, including the meme clip. 100% is the recorded level before other volume settings; 0% mutes it. Levels above 100% use a louder recording where available.").defineInRange("masterVolume", GuardClientSettings.initial(12), GuardClientSettings.minimum(12), GuardClientSettings.maximum(12));
        LOCAL_PARRY_VOLUME = client.comment("Your playback volume for regular parries and regular fall parries and their meme clip; multiplied by the master level.").defineInRange("parryVolume", GuardClientSettings.initial(13), GuardClientSettings.minimum(13), GuardClientSettings.maximum(13));
        LOCAL_PERFECT_VOLUME = client.comment("Your playback volume for perfect parries, including shield and enabled perfect fall parries, and their meme clip; multiplied by the master level.").defineInRange("perfectVolume", GuardClientSettings.initial(14), GuardClientSettings.minimum(14), GuardClientSettings.maximum(14));
        LOCAL_BLOCK_VOLUME = client.comment("Your playback volume for blocks and regular shield parries; multiplied by the master level.").defineInRange("blockVolume", GuardClientSettings.initial(15), GuardClientSettings.minimum(15), GuardClientSettings.maximum(15));
        client.pop();
        client.push("sparks");
        REGULAR_SPARK_COUNT = client.comment("Regular parry spark count as a percentage of the base burst. 0 hides bouncing sparks.").defineInRange("regularAmount", GuardClientSettings.initial(16), GuardClientSettings.minimum(16), GuardClientSettings.maximum(16));
        PERFECT_SPARK_COUNT = client.comment("Perfect parry spark count on the same scale as regular sparks. 0 hides bouncing sparks.").defineInRange("perfectAmount", GuardClientSettings.initial(17), GuardClientSettings.minimum(17), GuardClientSettings.maximum(17));
        STAR_COUNT = client.comment("Number of star-shaped sparks on perfect parries; 0 disables stars.").defineInRange("stars", GuardClientSettings.initial(18), GuardClientSettings.minimum(18), GuardClientSettings.maximum(18));
        SPARK_LIFETIME = client.comment("Shared lifetime for bouncing sparks and star sparks. 100% is 30 ticks; 0 hides these sparks. Streak Lines have their own lifetime.").defineInRange("lifetime", GuardClientSettings.initial(19), GuardClientSettings.minimum(19), GuardClientSettings.maximum(19));
        STREAK_AMOUNT = client.comment("Perfect parry Streak Line count on the same scale as regular streaks. 0 hides them.").defineInRange("streakAmount", GuardClientSettings.initial(29), GuardClientSettings.minimum(29), GuardClientSettings.maximum(29));
        REGULAR_STREAK_AMOUNT = client.comment("Regular parry Streak Line count. 100% is the base line count; 0 hides them.").defineInRange("regularStreakAmount", GuardClientSettings.initial(39), GuardClientSettings.minimum(39), GuardClientSettings.maximum(39));
        STREAK_LENGTH = client.comment("Streak Line reach as a percentage of the base 3.5–6.75 block range. 0 hides Streak Lines.").defineInRange("streakLength", GuardClientSettings.initial(30), GuardClientSettings.minimum(30), GuardClientSettings.maximum(30));
        STREAK_LIFETIME = client.comment("Streak Lines lifetime in ticks; individual square dots shrink and fade from center to tip.").defineInRange("streakLifetime", GuardClientSettings.initial(31), GuardClientSettings.minimum(31), GuardClientSettings.maximum(31));
        STREAK_DOT_SIZE = client.comment("Square dot size as a percentage of the base dot size. 0 hides Streak Lines.").defineInRange("streakDotSize", GuardClientSettings.initial(37), GuardClientSettings.minimum(37), GuardClientSettings.maximum(37));
        STREAK_STRETCH_SPEED = client.comment("Streak Line reveal speed. 100% is the base speed; 200% is twice as fast. 0 hides Streak Lines.").defineInRange("streakStretchSpeed", GuardClientSettings.initial(38), GuardClientSettings.minimum(38), GuardClientSettings.maximum(38));
        STREAK_DOT_SPACING = client.comment("Perfect parry dot spacing on the same scale as regular parry spacing. Lower values pack dots closer.").defineInRange("streakDotSpacing", GuardClientSettings.initial(45), GuardClientSettings.minimum(45), GuardClientSettings.maximum(45));
        STREAK_BALLISTIC_CURVE = client.comment("Downward ballistic curvature of Streak Lines. 0% keeps them straight; higher values curve the tip down more while the center stays fixed. Default: 40%.").defineInRange("streakBallisticCurve", GuardClientSettings.initial(48), GuardClientSettings.minimum(48), GuardClientSettings.maximum(48));
        REGULAR_STREAK_DOT_SPACING = client.comment("Regular parry dot spacing. Lower values pack dots closer; 1% is the densest setting.").defineInRange("regularStreakDotSpacing", GuardClientSettings.initial(46), GuardClientSettings.minimum(46), GuardClientSettings.maximum(46));
        SPARK_LENGTH = client.comment("Moving spark trail length. 100% uses the full trail length; 0 hides the trails without hiding stars.").defineInRange("length", GuardClientSettings.initial(20), GuardClientSettings.minimum(20), GuardClientSettings.maximum(20));
        SPARK_EXPLOSIVENESS = client.comment("Outward spark launch speed. 100% uses the base speed; 0 stops the outward burst.").defineInRange("explosiveness", GuardClientSettings.initial(21), GuardClientSettings.minimum(21), GuardClientSettings.maximum(21));
        SPARK_REACH = client.comment("Distance before sparks and stars start falling, as a percentage of 1.5 blocks. 10% = 0.15 blocks; 0 drops immediately.").defineInRange("reach", GuardClientSettings.initial(44), GuardClientSettings.minimum(44), GuardClientSettings.maximum(44));
        client.pop();
        GuardShieldReactions.configure(client);
        client.push("debug");
        DEBUG_CLIENT_FLAGS=client.comment("Diagnostic event groups: 1 guard, 2 damage/projectiles, 4 animations, 8 config/sync, 16 mob guard, 32 Punchy reactions, 64 Punchy hand transitions, 128 Punchy item priority. Zero disables diagnostic logging.").defineInRange("logging",0,0,GuardDiagnostics.ALL);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    private GuardConfig() {}

    public static void setCurrentVersion(String version) {
        currentVersion = version;
    }

    public static void prepareConfigFolders() {
        Path folder = FMLPaths.CONFIGDIR.get().resolve("mallard_guard");
        GuardPoseLibrary.prepare();
        try {
            Files.createDirectories(folder.resolve("enforce"));
            clearBackups(folder);
            clearBackups(folder.resolve("enforce"));
            clearLegacyBackups(FMLPaths.CONFIGDIR.get());
            Path instructions = folder.resolve("enforce/README.txt");
            if (Files.notExists(instructions)) Files.writeString(instructions,
                "Mallard Guard client enforcement\n\n"
                + "To use your client settings as the server's enforced values, copy\n"
                + "config/mallard_guard/client.toml here as client.toml. On a dedicated server,\n"
                + "copy the client.toml from your Minecraft installation to this server's\n"
                + "config/mallard_guard/enforce/ folder. Enable enforcement in game as an OP.\n\n"
                + "When this template exists, it supplies the Enforce screen's values.\n"
                + "For Punchy presets and motion, also copy mg_punchy/config.toml into enforce/mg_punchy/config.toml.\n"
                + "Applying changes in that screen also updates the copied files.\n"
                + "The categories marked Don't Enforce still start with these values,\n"
                + "but players can edit them afterward. The original client file is not changed.\n");
            copyIfAbsent(FMLPaths.CONFIGDIR.get().resolve("mallardguard-client.toml"), folder.resolve("client.toml"));
            copyIfAbsent(FMLPaths.CONFIGDIR.get().resolve("mallardguard-server.toml"), folder.resolve("server.toml"));
            Path game = FMLPaths.GAMEDIR.get();
            Path properties = game.resolve("server.properties");
            if (Files.isRegularFile(properties)) {
                java.util.Properties settings = new java.util.Properties();
                try (var input = Files.newInputStream(properties)) { settings.load(input); }
                Path world = game.resolve(settings.getProperty("level-name", "world")).normalize();
                if (world.startsWith(game)) migrateWorldConfig(world);
            }
            Path saves = game.resolve("saves");
            if (Files.isDirectory(saves)) try (DirectoryStream<Path> worlds = Files.newDirectoryStream(saves)) {
                for (Path world : worlds) migrateWorldConfig(world);
            }
        } catch (IOException error) {
            System.err.println("Mallard Guard: could not prepare config folders: " + error.getMessage());
        }
    }

    private static void migrateWorldConfig(Path world) throws IOException {
        Path folder = world.resolve("serverconfig/mallard_guard");
        clearBackups(folder);
        clearLegacyBackups(world.resolve("serverconfig"));
        Path source = world.resolve("serverconfig/mallardguard-server.toml");
        if (Files.isRegularFile(source) && !Files.exists(folder.resolve("server.toml"))) {
            Files.createDirectories(folder);
            copyIfAbsent(source, folder.resolve("server.toml"));
        }
    }

    public static void clearConfigBackups() {
        Path folder = FMLPaths.CONFIGDIR.get().resolve("mallard_guard");
        clearBackups(folder);
        clearBackups(folder.resolve("enforce")); clearBackups(folder.resolve("mg_punchy"));
        if (serverConfigFolder != null) clearBackups(serverConfigFolder);
    }

    public static void clearBackups(Path folder) {
        deleteConfigBackups(folder, "(?:client|server|config)(?:\\.toml)?(?:[-.]\\d+)?(?:\\.toml)?\\.bak");
    }

    private static void clearLegacyBackups(Path folder) {
        deleteConfigBackups(folder, "mallardguard-(?:client|server|config)(?:\\.toml)?(?:[-.]\\d+)?(?:\\.toml)?\\.bak");
    }

    private static void deleteConfigBackups(Path folder, String pattern) {
        if (!Files.isDirectory(folder, java.nio.file.LinkOption.NOFOLLOW_LINKS)) return;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder)) {
            for (Path file : files) {
                if (file.getFileName().toString().matches(pattern)
                    && Files.isRegularFile(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)) Files.deleteIfExists(file);
            }
        } catch (IOException error) {
            System.err.println("Mallard Guard: could not remove config backups: " + error.getMessage());
        }
    }

    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == CLIENT_SPEC || event.getConfig().getSpec() == SERVER_SPEC || event.getConfig().getSpec() == PUNCHY_SPEC)
            clearBackups(event.getConfig().getFullPath().getParent());
    }

    private static void copyIfAbsent(Path oldFile, Path newFile) throws IOException {
        if (Files.isRegularFile(oldFile) && !Files.exists(newFile)) Files.copy(oldFile, newFile);
    }

    /** Capture existing files before NeoForge validates them, without making backup files. */
    public static void snapshotExistingConfigs() {
        remember(FMLPaths.CONFIGDIR.get().resolve("mallard_guard/client.toml"));
        remember(FMLPaths.CONFIGDIR.get().resolve("mallard_guard/mg_punchy/config.toml"));
        remember(FMLPaths.CONFIGDIR.get().resolve("mallard_guard/server.toml"));
        Path game = FMLPaths.GAMEDIR.get();
        Path dedicatedProperties = game.resolve("server.properties");
        if (Files.isRegularFile(dedicatedProperties)) {
            try {
                java.util.Properties properties = new java.util.Properties();
                try (var input = Files.newInputStream(dedicatedProperties)) { properties.load(input); }
                String level = properties.getProperty("level-name", "world");
                Path world = game.resolve(level).normalize();
                if (world.startsWith(game)) remember(world.resolve("serverconfig/mallard_guard/server.toml"));
            } catch (IOException error) {
                System.err.println("Mallard Guard: could not inspect server.properties: " + error.getMessage());
            }
        }
        Path saves = FMLPaths.GAMEDIR.get().resolve("saves");
        if (!Files.isDirectory(saves)) return;
        try (DirectoryStream<Path> worlds = Files.newDirectoryStream(saves)) {
            for (Path world : worlds) remember(world.resolve("serverconfig/mallard_guard/server.toml"));
        } catch (IOException error) {
            System.err.println("Mallard Guard: could not inspect saved world configs: " + error.getMessage());
        }
    }

    private static void remember(Path path) {
        try {
            if(Files.isRegularFile(path)&&Files.size(path)<=1_048_576){byte[] bytes=Files.readAllBytes(path);if(revisionIn(bytes)!=CONFIG_REVISION)ORIGINAL_FILES.put(path.toAbsolutePath().normalize(),bytes);}
        } catch (IOException error) {
            System.err.println("Mallard Guard: could not preserve config in memory: " + error.getMessage());
        }
    }

    public static String version() { return currentVersion == null ? "unknown" : currentVersion; }

    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == PUNCHY_SPEC) GuardPoseLibrary.loaded();
        ModConfigSpec.IntValue marker;
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            marker = SERVER_REVISION;
            serverConfigFolder = event.getConfig().getFullPath().getParent();
        } else if (event.getConfig().getSpec() == PUNCHY_SPEC) {
            marker = PUNCHY_REVISION;
        } else if (event.getConfig().getSpec() == CLIENT_SPEC) {
            marker = CLIENT_REVISION;
        } else return;
        Path path = event.getConfig().getFullPath().toAbsolutePath().normalize();
        clearBackups(path.getParent());
        byte[] original = ORIGINAL_FILES.get(path);
        // Fresh installations receive the current revision without a prompt.
        if (original == null) return;
        int oldRevision = revisionIn(original);
        if (oldRevision == CONFIG_REVISION) {
            ORIGINAL_FILES.remove(path);
            return;
        }
        Set<String> currentPaths = new HashSet<>();
        collectPaths(((ModConfigSpec) event.getConfig().getSpec()).getValues(), "", currentPaths);
        Set<String> oldPaths = pathsIn(original);
        oldPaths.remove("configVersion"); // Version marker used by 1.22 and older.
        oldPaths.remove("configRevision");
        currentPaths.remove("configRevision");
        List<String> changes = new ArrayList<>();
        for (DefaultChange change : DEFAULT_CHANGES) {
            if (change.revision() > oldRevision && change.revision() <= CONFIG_REVISION
                && currentPaths.contains(change.path()))
                changes.add("Default changed: " + label(change.path()) + " (" + change.previous() + " -> " + change.current() + ")");
        }
        for (String added : currentPaths.stream().filter(key -> !oldPaths.contains(key)).sorted().toList())
            changes.add("Added: " + label(added));
        for (String removed : oldPaths.stream().filter(key -> !currentPaths.contains(key)).sorted().toList())
            changes.add("Removed: " + label(removed));
        if (changes.isEmpty()) {
            // A revision change without actual setting changes needs no prompt.
            marker.set(CONFIG_REVISION);
            ((ModConfigSpec) event.getConfig().getSpec()).save();
            currentPaths.add("configRevision");
            cleanFile(event.getConfig(), currentPaths);
            ORIGINAL_FILES.remove(path);
            return;
        }
        synchronized (PENDING_UPDATES) {
            PENDING_UPDATES.removeIf(update -> update.config() == event.getConfig());
            PENDING_UPDATES.add(new PendingUpdate(event.getConfig(), List.copyOf(changes)));
        }
    }

    public static void serverClosed(){synchronized(PENDING_UPDATES){PENDING_UPDATES.removeIf(update->update.config().getSpec()==SERVER_SPEC);}serverConfigFolder=null;}

    public static int pendingUpdates() {
        synchronized (PENDING_UPDATES) {
            return PENDING_UPDATES.size();
        }
    }

    public static boolean pendingServerUpdate() {
        synchronized (PENDING_UPDATES) {
            return PENDING_UPDATES.stream().anyMatch(update -> update.config().getSpec() == SERVER_SPEC);
        }
    }

    public static List<String> pendingChanges() {
        synchronized (PENDING_UPDATES) {
            List<String> lines = new ArrayList<>();
            for (PendingUpdate update : PENDING_UPDATES) {
                lines.add(update.config().getSpec() == PUNCHY_SPEC ? "Client / Punchy settings" : update.config().getSpec() == CLIENT_SPEC ? "Client settings" : "World/server settings");
                if (update.changes().isEmpty()) lines.add("Config format changed; settings remain available.");
                else lines.addAll(update.changes());
            }
            return List.copyOf(lines);
        }
    }

    public static List<String> pendingServerChanges() {
        synchronized (PENDING_UPDATES) {
            return PENDING_UPDATES.stream().filter(update -> update.config().getSpec() == SERVER_SPEC)
                .flatMap(update -> update.changes().stream()).toList();
        }
    }

    /** Return false on a write failure so the user can retry without losing the prompt. */
    public static boolean respondToUpdate(boolean keepOldValues) {
        return respondToUpdate(null, keepOldValues);
    }

    public static boolean respondToServerUpdate(boolean keepOldValues) {
        return respondToUpdate(SERVER_SPEC, keepOldValues);
    }

    private static boolean respondToUpdate(ModConfigSpec onlySpec, boolean keepOldValues) {
        synchronized (PENDING_UPDATES) {
            for (PendingUpdate update : List.copyOf(PENDING_UPDATES)) {
                ModConfig entry = update.config();
                if (onlySpec != null && entry.getSpec() != onlySpec) continue;
                ModConfigSpec spec = (ModConfigSpec) entry.getSpec();
                Set<String> paths = new HashSet<>();
                collectPaths(spec.getValues(), "", paths);
                try {
                    if (!keepOldValues) {
                        resetValues(spec.getValues());
                        if(spec==CLIENT_SPEC||spec==PUNCHY_SPEC){if(spec==CLIENT_SPEC){resetValues(PUNCHY_SPEC.getValues());PUNCHY_SPEC.save();}var presets=new java.util.ArrayList<>(GuardPoseLibrary.snapshot());if(presets.size()>=GuardPoseSettings.COUNT){for(int i=0;i<GuardPoseSettings.COUNT;i++)presets.set(i,GuardPoseLibrary.preset(i));GuardPoseLibrary.save(presets);}}
                    }
                    if (keepOldValues && spec == SERVER_SPEC) {
                        byte[] previous=ORIGINAL_FILES.get(entry.getFullPath().toAbsolutePath().normalize());
                        if(previous!=null&&revisionIn(previous)<28){
                            String text=new String(previous,java.nio.charset.StandardCharsets.UTF_8);
                            var dual=java.util.regex.Pattern.compile("(?m)^\\s*randomDualTools\\s*=\\s*(true|false)\\s*$").matcher(text);
                            if(dual.find())PARRY_HAND_PRIORITY.set(Boolean.parseBoolean(dual.group(1))?0:2);
                            var shield=java.util.regex.Pattern.compile("(?m)^\\s*randomShieldWeapon\\s*=\\s*(true|false)\\s*$").matcher(text);
                            if(shield.find())SHIELD_PARRY_PRIORITY.set(Boolean.parseBoolean(shield.group(1))?0:1);
                        }
                        if(previous!=null&&revisionIn(previous)<35&&(CLIENT_EXEMPT_MASK.get()&2)!=0)CLIENT_EXEMPT_MASK.set(CLIENT_EXEMPT_MASK.get()|24);
                        CLIENT_EXEMPT_MASK.set(CLIENT_EXEMPT_MASK.get() & GuardClientPreset.ALL_CATEGORIES);
                        int[] preset = GuardClientPreset.parse(CLIENT_PRESET.get());
                        if (preset != null) CLIENT_PRESET.set(GuardClientPreset.encode(preset));
                    }
                    if (keepOldValues && spec == CLIENT_SPEC) {
                        byte[] previous = ORIGINAL_FILES.get(entry.getFullPath().toAbsolutePath().normalize());
                        if (previous != null && revisionIn(previous) < 6) {
                            migrateScale(previous, "streakLength", STREAK_LENGTH, 5.0F);
                            migrateScale(previous, "streakDotSize", STREAK_DOT_SIZE, 1.8F);
                            migrateScale(previous, "streakStretchSpeed", STREAK_STRETCH_SPEED, 7.2F);
                        }
                        if (previous != null && revisionIn(previous) < 6) {
                            migrateScale(previous, "regularAmount", REGULAR_SPARK_COUNT, 0.7F);
                            migrateScale(previous, "perfectAmount", PERFECT_SPARK_COUNT, 0.91F);
                            migrateScale(previous, "lifetime", SPARK_LIFETIME, 0.7F);
                            migrateScale(previous, "length", SPARK_LENGTH, 2.0F);
                            migrateScale(previous, "explosiveness", SPARK_EXPLOSIVENESS, 4.0F);
                            migrateScale(previous, "streakAmount", STREAK_AMOUNT, 0.825F);
                        }
                        if (previous != null && revisionIn(previous) >= 6 && revisionIn(previous) < 8) {
                            migrateScale(previous, "perfectAmount", PERFECT_SPARK_COUNT, 1.3F);
                            migrateScale(previous, "streakAmount", STREAK_AMOUNT, 1.1F);
                            migrateScale(previous, "streakLength", STREAK_LENGTH, 2.0F);
                            migrateScale(previous, "streakDotSize", STREAK_DOT_SIZE, 0.9F);
                            migrateScale(previous, "streakStretchSpeed", STREAK_STRETCH_SPEED, 1.8F);
                            migrateScale(previous, "explosiveness", SPARK_EXPLOSIVENESS, 2.0F);
                        }
                    }
                    if (keepOldValues && spec == CLIENT_SPEC) {
                        byte[] previous = ORIGINAL_FILES.get(entry.getFullPath().toAbsolutePath().normalize());
                        if (previous != null && revisionIn(previous) < 12) {
                            PERFECT_SPARK_COUNT.set(Math.clamp(Math.round(PERFECT_SPARK_COUNT.get() / 1.3F), 0, 200));
                            SPARK_LENGTH.set(Math.clamp(Math.round(SPARK_LENGTH.get() / 0.5F), 0, 200));
                            SPARK_EXPLOSIVENESS.set(Math.clamp(Math.round(SPARK_EXPLOSIVENESS.get() / 0.2F), 0, 200));
                            SPARK_REACH.set(Math.clamp(Math.round(SPARK_REACH.get() / 0.1F), 0, 200));
                            STREAK_LENGTH.set(Math.clamp(Math.round(STREAK_LENGTH.get() / 1.4F), 10, 200));
                            STREAK_DOT_SIZE.set(Math.clamp(Math.round(STREAK_DOT_SIZE.get() / 0.6F), 10, 200));
                            STREAK_DOT_SPACING.set(Math.clamp(Math.round(STREAK_DOT_SPACING.get() / 0.4F), 10, 200));
                            REGULAR_STREAK_DOT_SPACING.set(Math.clamp(Math.round(REGULAR_STREAK_DOT_SPACING.get() / 0.6F), 10, 200));
                            REGULAR_ORB_SIZE.set(Math.clamp(Math.round(REGULAR_ORB_SIZE.get() / 0.1F), 0, 200));
                            PERFECT_ORB_SIZE.set(Math.clamp(Math.round(PERFECT_ORB_SIZE.get() / 0.3F), 0, 200));
                        }
                    }
                    if (keepOldValues && spec == CLIENT_SPEC) {
                        byte[] previous = ORIGINAL_FILES.get(entry.getFullPath().toAbsolutePath().normalize());
                        if (previous != null && revisionIn(previous) < 15) {
                            PERFECT_SPARK_COUNT.set(Math.clamp(Math.round(PERFECT_SPARK_COUNT.get() * 1.69F), 0, 200));
                            SPARK_LENGTH.set(Math.clamp(Math.round(SPARK_LENGTH.get() * 0.5F), 0, 200));
                            SPARK_EXPLOSIVENESS.set(Math.clamp(Math.round(SPARK_EXPLOSIVENESS.get() * 0.8F), 0, 200));
                            SPARK_REACH.set(Math.clamp(Math.round(SPARK_REACH.get() * 0.1F), 0, 200));
                            STREAK_AMOUNT.set(Math.clamp(Math.round(STREAK_AMOUNT.get() * 1.1F), 0, 200));
                            STREAK_LENGTH.set(Math.clamp(Math.round(STREAK_LENGTH.get() * 1.4F), 0, 200));
                            STREAK_DOT_SIZE.set(Math.clamp(Math.round(STREAK_DOT_SIZE.get() * 0.6F), 0, 200));
                            STREAK_DOT_SPACING.set(Math.clamp(Math.round(STREAK_DOT_SPACING.get() * 0.4F), 1, 200));
                            REGULAR_STREAK_DOT_SPACING.set(Math.clamp(Math.round(REGULAR_STREAK_DOT_SPACING.get() * 0.6F), 1, 200));
                            REGULAR_ORB_SIZE.set(Math.clamp(Math.round(REGULAR_ORB_SIZE.get() * 0.1F), 0, 200));
                            PERFECT_ORB_SIZE.set(Math.clamp(Math.round(PERFECT_ORB_SIZE.get() * 0.3F), 0, 200));
                        }
                    }
                    (spec == CLIENT_SPEC ? CLIENT_REVISION : spec == PUNCHY_SPEC ? PUNCHY_REVISION : SERVER_REVISION).set(CONFIG_REVISION);
                    spec.save();
                    cleanFile(entry, paths);
                    clearBackups(entry.getFullPath().getParent());
                    ORIGINAL_FILES.remove(entry.getFullPath().toAbsolutePath().normalize());
                    PENDING_UPDATES.remove(update);
                } catch (RuntimeException error) {
                    System.err.println("Mallard Guard: could not update config " + entry.getFullPath() + ": " + error.getMessage());
                    return false;
                }
            }
            return true;
        }
    }

    private static void cleanFile(ModConfig entry, Set<String> paths) {
        try (CommentedFileConfig file = CommentedFileConfig.builder(entry.getFullPath()).sync().build()) {
            file.load();
            removeStale(file, "", paths);
            file.save();
        }
    }

    private static void resetValues(UnmodifiableConfig entries) {
        for (var entry : entries.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof UnmodifiableConfig group) resetValues(group);
            else if (value instanceof ModConfigSpec.ConfigValue<?> setting) resetValue(setting);
        }
    }

    private static void migrateScale(byte[] original, String key, ModConfigSpec.IntValue setting, float previousScale) {
        boolean sparks = false;
        for (String line : new String(original, java.nio.charset.StandardCharsets.UTF_8).split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) sparks = trimmed.equals("[sparks]");
            else if (sparks && (trimmed.startsWith(key + " ") || trimmed.startsWith(key + "="))) {
                int equal = trimmed.indexOf('=');
                try {
                    int old = Integer.parseInt(trimmed.substring(equal + 1).trim());
                    int minimum = setting == STREAK_LENGTH || setting == STREAK_DOT_SIZE || setting == STREAK_STRETCH_SPEED ? 10 : 0;
                    setting.set(Math.clamp(Math.round(old / previousScale), minimum, 200));
                } catch (NumberFormatException ignored) { }
                return;
            }
        }
    }

    private static <T> void resetValue(ModConfigSpec.ConfigValue<T> setting) {
        setting.set(setting.getDefault());
    }

    private static int revisionIn(byte[] bytes) {
        String section = "";
        for (String line : new String(bytes, java.nio.charset.StandardCharsets.UTF_8).split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) section = trimmed.substring(1, trimmed.length() - 1);
            else if (section.isEmpty() && trimmed.matches("configRevision\\s*=.*")) {
                try { return Integer.parseInt(trimmed.substring(trimmed.indexOf('=') + 1).trim()); }
                catch (NumberFormatException ignored) { return 0; }
            }
        }
        return -1; // Legacy file with no config revision.
    }

    private static Set<String> pathsIn(byte[] bytes) {
        Set<String> paths = new LinkedHashSet<>();
        String section = "";
        for (String line : new String(bytes, java.nio.charset.StandardCharsets.UTF_8).split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) section = trimmed.substring(1, trimmed.length() - 1) + ".";
            else if (!trimmed.startsWith("#") && !trimmed.isEmpty()) {
                int equals = trimmed.indexOf('=');
                if (equals > 0) paths.add(section + trimmed.substring(0, equals).trim());
            }
        }
        return paths;
    }

    private static String label(String path) {
        String spaced = path.replace('.', ' ').replaceAll("([a-z])([A-Z])", "$1 $2");
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private static void collectPaths(UnmodifiableConfig entries, String prefix, Set<String> paths) {
        for (var entry : entries.entrySet()) {
            String path = prefix + entry.getKey();
            if (entry.getValue() instanceof UnmodifiableConfig group) collectPaths(group, path + ".", paths);
            else if (entry.getValue() instanceof ModConfigSpec.ConfigValue<?>) paths.add(path);
        }
    }

    private static void removeStale(Config config, String prefix, Set<String> paths) {
        for (var entry : new ArrayList<>(config.entrySet())) {
            String key = entry.getKey();
            String path = prefix + key;
            Object value = entry.getValue();
            if (value instanceof Config group) {
                removeStale(group, path + ".", paths);
                if (group.isEmpty()) config.remove(key);
            } else if (!paths.contains(path)) config.remove(key);
        }
    }

    public static void apply(GuardPackets.Settings data) {
        PARRY.set(data.parry());
        BLOCK.set(data.block());
        PERFECT_TICKS.set(data.perfect());
        PARRY_TICKS.set(data.window());
        RECHARGE_TICKS.set(data.recharge());
        FACING_ANGLE.set(data.angle());
        BLOCK_REDUCTION.set(data.reductionPercent() / 100.0D);
        FOLLOW_UP_TICKS.set(data.followUp());
        PARRY_RETALIATION.set(data.parryReturnPercent() / 100.0D);
        PERFECT_RETALIATION.set(data.perfectReturnPercent() / 100.0D);
        PARRY_WEAR.set(data.parryWear());
        PERFECT_WEAR.set(data.perfectWear());
        BLOCK_WEAR.set(data.blockWear());
        ALLOW_ANY_ITEM.set(data.allowAnyItem());
        CONSUMABLE_PRIORITY.set(data.consumablePriority());
        HIT_SOUNDS.set(data.hitSounds());
        HIT_PARTICLES.set(data.hitParticles());
        MASTER_VOLUME.set(data.masterVolume());
        PERFECT_VOLUME.set(data.perfectVolume());
        PARRY_VOLUME.set(data.parryVolume());
        BLOCK_VOLUME.set(data.blockVolume());
        FALL_PERFECT_PARRY.set(data.fallPerfectParry());
        FALL_LOOK_DOWN.set(data.fallLookDown());
        FALL_BREAK_BLOCKS.set(data.fallBreakBlocks());
        FALL_BLAST_STRENGTH.set(data.fallBlastStrength());
        FALL_LAUNCH_POWER.set(data.fallLaunchPower());
        PARRY_EXPLOSIONS.set(data.parryExplosions());
        PERFECT_EXPLOSIONS_ONLY.set(data.perfectExplosionsOnly());
        BLOCK_EXPLOSIONS.set(data.blockExplosions());
        PARRY_PROJECTILES.set(data.parryProjectiles());
        BLOCK_PROJECTILES.set(data.blockProjectiles());
        DEFENDER_KNOCKBACK.set(data.defenderKnockback());
        KNOCKBACK_STRENGTH.set(data.knockbackStrength());
        GUARD_MOVEMENT_PERCENT.set(data.guardMovementPercent());
        BLOCK_DEFLECT_CHANCE.set(data.blockDeflectChance());
        ALLOW_USABLE_ITEMS.set(data.allowUsableItems());
        INCLUDED_ITEMS.set(data.includedItems());
        EXCLUDED_ITEMS.set(data.excludedItems());
        SHIELD_ITEMS.set(data.shieldItems());
        SERVER_SPEC.save();
        clearConfigBackups();
    }

    private static <T> T read(ModConfigSpec.ConfigValue<T> value, boolean defaults) {
        return defaults ? value.getDefault() : value.get();
    }

    public static GuardPackets.Settings snapshot(boolean operator) { return settingsSnapshot(operator, false); }
    public static GuardPackets.Settings defaultSnapshot(boolean operator) { return settingsSnapshot(operator, true); }
    private static GuardPackets.Settings settingsSnapshot(boolean operator, boolean defaults) {
        return new GuardPackets.Settings(read(PARRY, defaults), read(BLOCK, defaults), false, false, false, read(PERFECT_TICKS, defaults), read(PARRY_TICKS, defaults), read(RECHARGE_TICKS, defaults), read(FACING_ANGLE, defaults), (int) Math.round(read(BLOCK_REDUCTION, defaults) * 100.0D), read(FOLLOW_UP_TICKS, defaults), (int) Math.round(read(PARRY_RETALIATION, defaults) * 100), (int) Math.round(read(PERFECT_RETALIATION, defaults) * 100), read(PARRY_WEAR, defaults), read(PERFECT_WEAR, defaults), read(BLOCK_WEAR, defaults), read(HIT_SOUNDS, defaults), read(HIT_PARTICLES, defaults), read(MASTER_VOLUME, defaults), read(PERFECT_VOLUME, defaults), read(PARRY_VOLUME, defaults), read(BLOCK_VOLUME, defaults), true, read(FALL_PERFECT_PARRY, defaults), read(FALL_LOOK_DOWN, defaults), read(FALL_BREAK_BLOCKS, defaults), read(FALL_BLAST_STRENGTH, defaults), read(FALL_LAUNCH_POWER, defaults), read(PARRY_EXPLOSIONS, defaults), read(PERFECT_EXPLOSIONS_ONLY, defaults), read(BLOCK_EXPLOSIONS, defaults), read(PARRY_PROJECTILES, defaults), read(BLOCK_PROJECTILES, defaults), read(DEFENDER_KNOCKBACK, defaults), read(KNOCKBACK_STRENGTH, defaults), read(GUARD_MOVEMENT_PERCENT, defaults), read(BLOCK_DEFLECT_CHANCE, defaults), read(ALLOW_ANY_ITEM, defaults), read(ALLOW_USABLE_ITEMS, defaults), read(INCLUDED_ITEMS, defaults), read(EXCLUDED_ITEMS, defaults), read(SHIELD_ITEMS, defaults), read(CONSUMABLE_PRIORITY, defaults), operator);
    }



    public static GuardPackets.MobSettings mobSnapshot(boolean defaults) {
        return new GuardPackets.MobSettings(defaults ? MOB_GUARD.getDefault() : MOB_GUARD.get(),
            defaults ? MOB_DIFFICULTY.getDefault() : MOB_DIFFICULTY.get(),
            defaults ? MOB_BLOCKING.getDefault() : MOB_BLOCKING.get(),
            defaults ? MOB_RETALIATION.getDefault() : MOB_RETALIATION.get(),
            defaults ? MOB_PARRY_COLOR.getDefault() : MOB_PARRY_COLOR.get(),
            defaults ? MOB_PERFECT_COLOR.getDefault() : MOB_PERFECT_COLOR.get(),
            defaults ? MOB_HITS_TO_GUARD.getDefault() : MOB_HITS_TO_GUARD.get(),
            defaults ? MOB_PERFECT_TICKS.getDefault() : MOB_PERFECT_TICKS.get(),
            defaults ? MOB_PARRY_TICKS.getDefault() : MOB_PARRY_TICKS.get(),
            defaults ? MOB_COUNTER_TICKS.getDefault() : MOB_COUNTER_TICKS.get(),
            defaults ? MOB_APPROACH_CHANCE.getDefault() : MOB_APPROACH_CHANCE.get(),
            defaults ? MOB_TACTICAL_CHANCE.getDefault() : MOB_TACTICAL_CHANCE.get(),
            defaults ? MOB_TACTICAL_SECONDS.getDefault() : MOB_TACTICAL_SECONDS.get(),
            defaults ? MOB_TACTICAL_COOLDOWN.getDefault() : MOB_TACTICAL_COOLDOWN.get(),
            defaults ? MOB_HUMANOID_IDS.getDefault() : MOB_HUMANOID_IDS.get(),
            defaults ? MOB_BLACKLIST.getDefault() : MOB_BLACKLIST.get(),
            defaults ? MOB_MOVEMENT.getDefault() : MOB_MOVEMENT.get(),
            defaults ? MOB_APPROACH_DISTANCE.getDefault() : MOB_APPROACH_DISTANCE.get(),
            defaults ? MOB_APPROACH_SECONDS.getDefault() : MOB_APPROACH_SECONDS.get(),
            defaults ? MOB_RUSH_CHANCE.getDefault() : MOB_RUSH_CHANCE.get(),
            defaults ? MOB_GEAR_CHANCE.getDefault() : MOB_GEAR_CHANCE.get());
    }

    public static GuardPackets.ShieldSettings shieldSnapshot() { return shieldSnapshotValues(false); }
    public static GuardPackets.ShieldSettings defaultShieldSnapshot() { return shieldSnapshotValues(true); }
    private static GuardPackets.ShieldSettings shieldSnapshotValues(boolean defaults) {
        return new GuardPackets.ShieldSettings(read(SHIELD_PERFECT_TICKS, defaults), read(SHIELD_PARRY_TICKS, defaults),
            read(SHIELD_RECHARGE_TICKS, defaults), read(SHIELD_MAX_BLOCKS, defaults), read(TOOL_MAX_BLOCKS, defaults), read(SHIELD_BREAK_TICKS, defaults), read(SHIELD_CONE_DEGREES, defaults), read(SHIELD_CONE_REACH, defaults),
            read(SHIELD_RETALIATION_PERCENT, defaults), read(SHIELD_STUN_TICKS, defaults), read(SHIELD_PUSHBACK_PERCENT, defaults),
            read(SHIELD_PARRY_PUSHBACK_PERCENT, defaults), read(TOOL_PUSHBACK_PERCENT, defaults), read(SHIELD_STUN_BOSSES, defaults), read(CONSUMABLE_PRIORITY, defaults), read(COOLDOWN_PREVENTS_GUARD, defaults), read(ALLOW_EMPTY_HAND, defaults), read(RETALIATION_CAP, defaults), read(SHIELD_PARRY_PRIORITY, defaults), read(SHIELD_SPARK_CONE, defaults), read(PARRY_HAND_PRIORITY, defaults), read(FORCE_CROUCH_OFFHAND, defaults), read(SHIELD_BLACKLIST, defaults));
    }

    public static void applyShield(GuardPackets.ShieldSettings data) {
        PARRY_HAND_PRIORITY.set(data.parryHandPriority()); FORCE_CROUCH_OFFHAND.set(data.forceCrouchOffhand()); SHIELD_BLACKLIST.set(data.shieldBlacklist());
        SHIELD_PERFECT_TICKS.set(data.perfect()); SHIELD_PARRY_TICKS.set(data.window());
        SHIELD_RECHARGE_TICKS.set(data.rechargeTicks()); SHIELD_MAX_BLOCKS.set(data.maxBlocks()); TOOL_MAX_BLOCKS.set(data.toolMaxBlocks()); SHIELD_BREAK_TICKS.set(data.breakTicks());
        SHIELD_CONE_DEGREES.set(data.cone()); SHIELD_CONE_REACH.set(data.reach()); SHIELD_RETALIATION_PERCENT.set(data.retaliation());
        SHIELD_STUN_TICKS.set(data.stunTicks()); SHIELD_PUSHBACK_PERCENT.set(data.perfectPushback());
        SHIELD_PARRY_PUSHBACK_PERCENT.set(data.regularPushback()); TOOL_PUSHBACK_PERCENT.set(data.weaponPushback());
        SHIELD_STUN_BOSSES.set(data.stunnableBosses()); CONSUMABLE_PRIORITY.set(data.consumablePriority()); COOLDOWN_PREVENTS_GUARD.set(data.cooldownPreventsGuard()); ALLOW_EMPTY_HAND.set(data.allowEmptyHand()); RETALIATION_CAP.set(data.retaliationCap());
        SHIELD_PARRY_PRIORITY.set(data.shieldParryPriority()); SHIELD_SPARK_CONE.set(data.coneSparks());
        SERVER_SPEC.save();
        clearConfigBackups();
    }
}
