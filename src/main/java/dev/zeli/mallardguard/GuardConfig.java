package dev.zeli.mallardguard;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.util.ArrayList;
import java.util.HashSet;
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
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;
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
    public static final ModConfigSpec.BooleanValue COOLDOWN_PREVENTS_GUARD;
    public static final ModConfigSpec.IntValue SHIELD_CONE_DEGREES, SHIELD_CONE_REACH, SHIELD_RETALIATION_PERCENT, SHIELD_STUN_TICKS, SHIELD_PUSHBACK_PERCENT, TOOL_PUSHBACK_PERCENT, SHIELD_PARRY_PUSHBACK_PERCENT;
    public static final ModConfigSpec.ConfigValue<String> SHIELD_STUN_BOSSES;
    public static final ModConfigSpec.BooleanValue ALLOW_USABLE_ITEMS;
    public static final ModConfigSpec.BooleanValue ALLOW_ANY_ITEM;
    public static final ModConfigSpec.BooleanValue CONSUMABLE_PRIORITY;
    public static final ModConfigSpec.ConfigValue<String> INCLUDED_ITEMS;
    public static final ModConfigSpec.ConfigValue<String> EXCLUDED_ITEMS;
    public static final ModConfigSpec.ConfigValue<String> SHIELD_ITEMS;
    public static final ModConfigSpec.BooleanValue HUD;
    public static final ModConfigSpec.BooleanValue SHIELD_EFFECTS;
    public static final ModConfigSpec.BooleanValue SPARKS_ENABLED, SPARK_RING, PARTICLE_FLASHES_ENABLED;
    public static final ModConfigSpec.BooleanValue SCREEN_FLASH;
    public static final ModConfigSpec.BooleanValue MEME_FLASH;
    public static final ModConfigSpec.IntValue HITLAG_FRAMES;
    public static final ModConfigSpec.IntValue FLASH_STRENGTH;
    public static final ModConfigSpec.BooleanValue SCREEN_SHAKE;
    public static final ModConfigSpec.IntValue SHAKE_STRENGTH;
    public static final ModConfigSpec.BooleanValue PREFER_OFFHAND;
    public static final ModConfigSpec.IntValue REGULAR_ORB_SIZE;
    public static final ModConfigSpec.IntValue PERFECT_ORB_SIZE;
    public static final ModConfigSpec.IntValue REGULAR_ORB_OPACITY;
    public static final ModConfigSpec.IntValue PERFECT_ORB_OPACITY;
    public static final ModConfigSpec.BooleanValue FIRST_PERSON_ANIMATION;
    public static final ModConfigSpec.IntValue LOCAL_MASTER_VOLUME, LOCAL_PARRY_VOLUME, LOCAL_PERFECT_VOLUME, LOCAL_BLOCK_VOLUME;
    public static final ModConfigSpec.IntValue REGULAR_SPARK_COUNT, PERFECT_SPARK_COUNT, STAR_COUNT;
    public static final ModConfigSpec.IntValue SPARK_LIFETIME, SPARK_LENGTH, SPARK_EXPLOSIVENESS;
    private static final ModConfigSpec.ConfigValue<String> SERVER_VERSION;
    private static final ModConfigSpec.ConfigValue<String> CLIENT_VERSION;
    private static String currentVersion;
    private static final List<ModConfig> PENDING_UPDATES = new ArrayList<>();
    private static final Map<Path, byte[]> ORIGINAL_FILES = new ConcurrentHashMap<>();
    private static boolean promptDismissed;

    static {
        ModConfigSpec.Builder server = new ModConfigSpec.Builder();
        SERVER_VERSION = server.comment("Last approved Mallard Guard config update. Your values are kept when updating.").define("configVersion", "unversioned");
        server.push("stance");
        PARRY = server.comment("Allow parries when the guard key is pressed. Right Click by default; the key can be changed.").define("parry", true);
        DAMAGE_RULES = server.comment("Per-damage-type parry rules, stored as damage_type_id=1 or =0, separated by commas. Configure in game.")
            .define("damageRules", "", value -> value instanceof String rules && GuardDamageRules.validRules(rules));
        OBSERVED_PROJECTILE_SOURCES = server.comment("Automatically learned projectile entity ID to damage type ID associations. Updated after players are hit; used to honor modded projectile source rules at impact.")
            .define("observedProjectileSources", "", value -> value instanceof String mapping && GuardDamageRules.validProjectileSources(mapping));
        BLOCK = server.comment("Keep guarding after the parry window while the guard key is held.").define("block", true);
        TOOL_MAX_BLOCKS = server.comment("Successful weapon or empty-hand blocks before guard breaks; 0 allows unlimited blocks. Guard break delays the next guard but does not disable attacking with the item.").defineInRange("toolMaxBlocks", 3, 0, 5);
        server.pop();
        server.push("timing");
        PERFECT_TICKS = server.comment("Perfect parry duration for tools; zero disables perfect parries.").defineInRange("perfectTicks", 2, 0, 5);
        PARRY_TICKS = server.comment("Total weapon parry duration in ticks, including perfect parry ticks.").defineInRange("parryTicks", 6, 1, 10);
        RECHARGE_TICKS = server.comment("Time before another attempt; continues during held guard.").defineInRange("rechargeTicks", 10, 1, 60);
        server.pop();
        server.push("defense");
        FACING_ANGLE = server.comment("Maximum angle from the player's view direction; 180 allows the front half, 360 allows attacks from any direction.").defineInRange("facingAngle", 180, 0, 360);
        BLOCK_REDUCTION = server.comment("Fraction of incoming entity damage prevented while holding guard.").defineInRange("blockReduction", 0.5D, 0.0D, 1.0D);
        FOLLOW_UP_TICKS = server.comment("Ticks after a successful parry that catch additional valid hits; zero disables it. Follow-up hits never extend this window.").defineInRange("followUpParryTicks", 3, 0, 20);
        PARRY_RETALIATION = server.comment("Regular parry damage returned as a fraction of the incoming damage. 0 disables it; 2 doubles it.").defineInRange("parryRetaliation", 0.25D, 0.0D, 2.0D);
        PERFECT_RETALIATION = server.comment("Perfect parry damage returned as a fraction of the incoming damage. 0 disables it; 2 doubles it.").defineInRange("perfectRetaliation", 0.50D, 0.0D, 2.0D);
        PARRY_WEAR = server.comment("Wear in tenths of a percent of maximum durability per regular or follow-up parry. 2 means 0.2%; rounded up. 0 disables wear.").defineInRange("parryWear", 2, 0, 100);
        PERFECT_WEAR = server.comment("Wear in tenths of a percent of maximum durability per perfect parry. 1 means 0.1%; rounded up. 0 disables wear.").defineInRange("perfectWear", 1, 0, 100);
        BLOCK_WEAR = server.comment("Wear in tenths of a percent of maximum durability per blocked hit. 4 means 0.4%; rounded up. Real shields keep their own block wear.").defineInRange("blockWear", 4, 0, 100);
        server.pop();
        server.push("effects");
        HIT_SOUNDS = server.comment("Play the supplied parry and block sounds to nearby players.").define("hitSounds", true);
        HIT_PARTICLES = server.comment("Show sparks and flashes on parries; perfect parries also show star-shaped sparks. Blocking creates none.").define("hitParticles", true);
        MASTER_VOLUME = server.comment("Server master volume for guard sounds sent to nearby clients. 100% plays the recorded level; each listener's client master and individual volume settings multiply it. 0% mutes the sounds.").defineInRange("masterVolume", 100, 0, 200);
        PERFECT_VOLUME = server.comment("Perfect parry sound volume, multiplied by master volume.").defineInRange("perfectVolume", 130, 0, 200);
        PARRY_VOLUME = server.comment("Regular parry sound volume, multiplied by master volume.").defineInRange("parryVolume", 65, 0, 200);
        BLOCK_VOLUME = server.comment("Held block sound volume, multiplied by master volume.").defineInRange("blockVolume", 65, 0, 200);
        server.pop();
        server.push("clientEnforcement");
        ENFORCE_CLIENT = server.comment("Apply server chosen client defaults when players connect and lock categories without a Don't Enforce exception.").define("enabled", false);
        CLIENT_EXEMPT_MASK = server.comment("Client categories editable after receiving server defaults: 1 particles, 2 visuals, 4 audio, 8 hand selection.").defineInRange("dontEnforceMask", 0, 0, 15);
        CLIENT_PRESET = server.comment("Client defaults shared by the server; changed through the Enforce configuration screen.")
            .define("defaults", GuardClientPreset.encode(GuardClientPreset.DEFAULTS), value -> value instanceof String data && GuardClientPreset.valid(data));
        server.pop();
        server.push("sources");
        FALL_LOOK_DOWN = server.comment("Require looking at least 40 degrees below the horizon to parry a fall. On: preserve current horizontal momentum, bouncing in place when still. Off: fall parries work in any direction and launch where you look.").define("fallLookDown", true);
        FALL_PERFECT_PARRY = server.comment("Allow perfect parries on fall damage or wall collisions. Off: catching either impact in the perfect window performs a regular parry instead.").define("fallPerfectParry", false);
        FALL_BREAK_BLOCKS = server.comment("Allow the blast from a long fall parry to break terrain. Disable to prevent terrain damage.").define("fallBlastBreaksBlocks", true);
        FALL_BLAST_STRENGTH = server.comment("Fall parry blast scaling as a percentage of Parry It's default 0.1 radius per damage; 0 disables the blast.").defineInRange("fallBlastStrength", 100, 0, 500);
        FALL_LAUNCH_POWER = server.comment("Fall parry launch strength as a percentage of Parry It's default. Perfect falls launch twice as far. 0 disables launch.").defineInRange("fallLaunchPower", 10, 0, 300);
        PARRY_EXPLOSIONS = server.comment("Allow parry timing to negate explosion damage.").define("parryExplosions", true);
        PERFECT_EXPLOSIONS_ONLY = server.comment("When explosion parrying is enabled, require a perfect parry; regular parries do not negate explosions.").define("perfectExplosionsOnly", false);
        BLOCK_EXPLOSIONS = server.comment("Allow held block to reduce explosion damage by the normal block reduction.").define("blockExplosions", true);
        PARRY_PROJECTILES = server.comment("Parry projectile entities. Regular parries deflect them randomly; perfect parries send them toward the original owner.").define("parryProjectiles", true);
        BLOCK_PROJECTILES = server.comment("Allow held block to reduce projectile damage by the normal block reduction.").define("blockProjectiles", true);
        DEFENDER_KNOCKBACK = server.comment("Push the defender away from the impact on successful melee, projectile, and explosion parries. Shield parries use half strength.").define("defenderKnockback", true);
        KNOCKBACK_STRENGTH = server.comment("Push strength for the defender in percent; shield parries use half the configured strength. 0 disables both.").defineInRange("knockbackStrength", 150, 0, 200);
        GUARD_MOVEMENT_PERCENT = server.comment("Movement speed while guarding, including parry and block. 100 keeps normal movement speed.").defineInRange("guardMovementPercent", 55, 0, 100);
        BLOCK_DEFLECT_CHANCE = server.comment("Chance to deflect a projectile during held block. 0 disables deflection; every projectile type uses the same roll.").defineInRange("blockDeflectChance", 0, 0, 100);
        server.pop();
        server.push("items");
        ALLOW_ANY_ITEM = server.comment("Off: recognized weapons and shields plus extra allowed items may guard. On: empty hands and all items except excluded items may guard. The inactive list remains saved.").define("allowAnyItem", false);
        CONSUMABLE_PRIORITY = server.comment("When enabled, food and drinks in either hand take priority over guard, even if the other hand holds a weapon. Enabled by default.").define("consumablePriority", true);
        ALLOW_USABLE_ITEMS = server.comment("Allow eligible items with a hold-to-use action to guard. Whitelisted items and universal guard are also allowed.").define("allowUsableItems", true);
        INCLUDED_ITEMS = server.comment("Extra allowed item IDs or #tags when universal guard is off; recognized weapons and shields are already allowed.").define("includedItems", "", value -> value instanceof String rules && GuardItemRules.valid(rules));
        EXCLUDED_ITEMS = server.comment("Item IDs or #tags excluded when universal guard is on; empty hands remain eligible.").define("excludedItems", "", value -> value instanceof String rules && GuardItemRules.valid(rules));
        SHIELD_ITEMS = server.comment("Extra item IDs or #tags treated as shields. Built-in shields and blocking items are detected automatically. A whitelist or blacklist can still restrict them.").define("shieldItems", "", value -> value instanceof String rules && GuardItemRules.valid(rules));
        COOLDOWN_PREVENTS_GUARD = server.comment("When on, an item with an active vanilla or modded cooldown cannot enter the guard stance. Another eligible hand can still guard.").define("cooldownPreventsGuard", true);
        server.pop();
        server.push("shields");
        SHIELD_PERFECT_TICKS = server.comment("Shield perfect parry window, in ticks. 0 turns off shield perfect parries; cannot exceed total shield parry ticks.").defineInRange("perfectTicks", 2, 0, 3);
        SHIELD_PARRY_TICKS = server.comment("Total shield parry window in ticks, including perfect ticks; blocking follows without restarting shield use.").defineInRange("parryTicks", 8, 1, 10);
        SHIELD_RECHARGE_TICKS = server.comment("Shield parry recharge after leaving guard; separate from guard break recharge. Regular shield parries halve it and perfect shield parries reset it, like tool parries.").defineInRange("rechargeTicks", 30, 1, 60);
        SHIELD_MAX_BLOCKS = server.comment("Successful shield blocks before guard breaks; 0 allows unlimited blocks. Disabled shields still obey vanilla cooldown.").defineInRange("maxBlocks", 3, 0, 5);
        SHIELD_BREAK_TICKS = server.comment("Recharge after the last allowed shield or weapon block. Shields also receive an item cooldown; weapons remain usable for attacking. At least the normal parry recharge applies. Default 100 ticks = 5 seconds.").defineInRange("breakTicks", 100, 1, 200);
        SHIELD_CONE_DEGREES = server.comment("Angle to each side of your view for the shield perfect parry cone. 90 degrees covers the whole front half (180 degrees total). 0 disables cone effects.").defineInRange("perfectCone", 90, 0, 90);
        SHIELD_CONE_REACH = server.comment("Maximum eye-to-eye distance in blocks for shield perfect parry cone effects.").defineInRange("perfectConeReach", 5, 1, 10);
        SHIELD_RETALIATION_PERCENT = server.comment("Shield perfect parry damage to each target in its cone as a percentage of incoming damage. 0 disables damage; stun and pushback still apply.").defineInRange("perfectDamage", 10, 0, 30);
        SHIELD_STUN_TICKS = server.comment("Time in ticks that normal targets hit by a shield perfect parry cannot voluntarily move or attack; 0 disables the stun.").defineInRange("stunTicks", 30, 0, 100);
        SHIELD_PUSHBACK_PERCENT = server.comment("Shield perfect parry cone pushback strength; 0 disables pushback. Knockback applies before stun.").defineInRange("perfectPushback", 35, 0, 200);
        SHIELD_PARRY_PUSHBACK_PERCENT = server.comment("Shield regular parry pushback on the attacker; 0 disables pushback.").defineInRange("regularPushback", 85, 0, 200);
        TOOL_PUSHBACK_PERCENT = server.comment("Regular and perfect weapon parry pushback on the attacker; 0 disables pushback.").defineInRange("weaponPushback", 35, 0, 200);
        SHIELD_STUN_BOSSES = server.comment("Boss entity IDs or #entity tags explicitly allowed to be stunned. By default, bosses are immune to stun.").define("stunnableBosses", "", value -> value instanceof String rules && GuardItemRules.valid(rules));
        server.pop();
        SERVER_SPEC = server.build();

        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        CLIENT_VERSION = client.comment("Last approved Mallard Guard config update. Your values are kept when updating.").define("configVersion", "unversioned");
        client.push("display");
        HUD = client.comment("Show the parry shield by the crosshair.").define("hud", true);
        SHIELD_EFFECTS = client.comment("Animate the guard icon: a ready outline and parry flashes. Tool blocks flash red, shield blocks emit a short white echo, and the darker icon trembles with repeating echoes while held.").define("shieldEffects", true);
        SPARKS_ENABLED = client.comment("Show flying sparks and perfect parry stars on this client. Disable without changing your spark settings.").define("sparksEnabled", true);
        SPARK_RING = client.comment("Off: sparks spray in a rounded sphere. On: four shorter diagonal sprays form a compact X. Fall parries always use the compact X; shield parries fan toward the attacker in a 90-degree arc.").define("sparkRing", false);
        PARTICLE_FLASHES_ENABLED = client.comment("Show the white impact particles on this client. Disable without changing their size or opacity settings.").define("particleFlashesEnabled", true);
        SCREEN_FLASH = client.comment("Brief white screen flash after regular and perfect parries. Off by default; blocks never flash. Client-side only.").define("screenFlash", false);
        MEME_FLASH = client.comment("Show one of four full-screen meme images alongside a local meme clip. Both start with the white parry flash after hitlag; the image holds 0.5 seconds and fades over 0.2 seconds. Off by default; blocks never trigger it.").define("memeFlash", false);
        HITLAG_FRAMES = client.comment("Hold the captured screen for this many 60 FPS equivalent frames after a parry. Your screen flash and sound play when the freeze ends. 0 disables hitlag.").defineInRange("hitlagFrames", 0, 0, 8);
        FLASH_STRENGTH = client.comment("Strength of the brief screen flash, as a percentage. 0 disables it.").defineInRange("flashStrength", 0, 0, 100);
        SCREEN_SHAKE = client.comment("Briefly shake the camera on regular parries, perfect parries, and blocks. Blocks shake most strongly. Client-side only.").define("screenShake", true);
        SHAKE_STRENGTH = client.comment("Camera shake strength as a percentage. 0 disables it; blocks shake most strongly.").defineInRange("shakeStrength", 60, 0, 100);
        PREFER_OFFHAND = client.comment("When both hands can guard, use the offhand item for parrying, blocking and durability. The first-person pose follows the chosen hand; shields keep their vanilla pose.").define("offhandGuardPriority", false);
        REGULAR_ORB_SIZE = client.comment("Size of the regular parry flash. 0 hides this flash without hiding sparks.").defineInRange("regularOrbSize", 20, 0, 150);
        PERFECT_ORB_SIZE = client.comment("Size of the perfect parry flash. 0 hides this flash without hiding sparks.").defineInRange("perfectOrbSize", 30, 0, 150);
        REGULAR_ORB_OPACITY = client.comment("Opacity of the regular parry flash. 0 hides this flash.").defineInRange("regularOrbOpacity", 35, 0, 100);
        PERFECT_ORB_OPACITY = client.comment("Opacity of the perfect parry flash. 0 hides this flash.").defineInRange("perfectOrbOpacity", 35, 0, 100);
        client.pop();
        client.push("animation");
        FIRST_PERSON_ANIMATION = client.comment("Use Mallard Guard's first-person weapon pose. Disable when another mod animates your hands; shields always use vanilla animation.").define("firstPersonGuardPose", true);
        client.pop();
        client.push("audio");
        LOCAL_MASTER_VOLUME = client.comment("Your own master playback level for Mallard Guard sounds, including the meme clip. 100% is the recorded level before other volume settings; 0% mutes it. Levels above 100% use a louder recording where available.").defineInRange("masterVolume", 100, 0, 200);
        LOCAL_PARRY_VOLUME = client.comment("Your playback volume for regular parries and their meme clip; multiplied by the master level. Fall parries use their own recording.").defineInRange("parryVolume", 65, 0, 200);
        LOCAL_PERFECT_VOLUME = client.comment("Your playback volume for perfect parries, including shield and enabled perfect fall parries, and their meme clip; multiplied by the master level.").defineInRange("perfectVolume", 130, 0, 200);
        LOCAL_BLOCK_VOLUME = client.comment("Your playback volume for blocks and regular shield parries; multiplied by the master level.").defineInRange("blockVolume", 65, 0, 200);
        client.pop();
        client.push("sparks");
        REGULAR_SPARK_COUNT = client.comment("Regular parry spark quantity as a percentage of the default.").defineInRange("regularAmount", 100, 0, 200);
        PERFECT_SPARK_COUNT = client.comment("Perfect parry spark quantity as a percentage of the default.").defineInRange("perfectAmount", 100, 0, 200);
        STAR_COUNT = client.comment("Number of star-shaped sparks on perfect parries; 0 disables stars.").defineInRange("stars", 2, 0, 16);
        SPARK_LIFETIME = client.comment("Spark flight lifetime as a percentage of the default.").defineInRange("lifetime", 70, 25, 200);
        SPARK_LENGTH = client.comment("Flying spark streak length as a percentage of the default.").defineInRange("length", 50, 50, 200);
        SPARK_EXPLOSIVENESS = client.comment("How fast sparks burst outward from the impact point.").defineInRange("explosiveness", 200, 0, 200);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    private GuardConfig() {}

    public static void setCurrentVersion(String version) {
        currentVersion = version;
    }

    /** Capture existing files before NeoForge validates them, without making backup files. */
    public static void snapshotExistingConfigs() {
        remember(FMLPaths.CONFIGDIR.get().resolve("mallardguard-client.toml"));
        Path saves = FMLPaths.GAMEDIR.get().resolve("saves");
        if (!Files.isDirectory(saves)) return;
        try (DirectoryStream<Path> worlds = Files.newDirectoryStream(saves)) {
            for (Path world : worlds) remember(world.resolve("serverconfig/mallardguard-server.toml"));
        } catch (IOException error) {
            System.err.println("Mallard Guard: could not inspect saved world configs: " + error.getMessage());
        }
    }

    private static void remember(Path path) {
        try {
            if (Files.isRegularFile(path)) ORIGINAL_FILES.put(path.toAbsolutePath().normalize(), Files.readAllBytes(path));
        } catch (IOException error) {
            System.err.println("Mallard Guard: could not preserve config in memory: " + error.getMessage());
        }
    }

    public static String version() { return currentVersion == null ? "unknown" : currentVersion; }

    public static void onLoad(ModConfigEvent.Loading event) {
        ModConfigSpec.ConfigValue<String> marker;
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            marker = SERVER_VERSION;
        } else if (event.getConfig().getSpec() == CLIENT_SPEC) {
            marker = CLIENT_VERSION;
        } else return;
        byte[] original = ORIGINAL_FILES.get(event.getConfig().getFullPath().toAbsolutePath().normalize());
        if (currentVersion == null) return;
        if (currentVersion.equals(marker.get())) {
            if (original == null) return;
            Set<String> paths = new HashSet<>();
            collectPaths(((ModConfigSpec) event.getConfig().getSpec()).getValues(), "", paths);
            if (obsoleteInSnapshot(original, paths) == 0) return;
        }
        // A newly created config has no older values to migrate.
        if ("unversioned".equals(marker.get())
            && !ORIGINAL_FILES.containsKey(event.getConfig().getFullPath().toAbsolutePath().normalize())) {
            marker.set(currentVersion);
            return;
        }
        synchronized (PENDING_UPDATES) {
            if (!PENDING_UPDATES.contains(event.getConfig())) PENDING_UPDATES.add(event.getConfig());
        }
    }

    /** Called only from the client startup prompt. Dedicated servers keep old values until edited. */
    public static int pendingUpdates() {
        synchronized (PENDING_UPDATES) {
            return promptDismissed ? 0 : PENDING_UPDATES.size();
        }
    }

    public static void respondToUpdate() {
        synchronized (PENDING_UPDATES) {
            promptDismissed = true;
            for (ModConfig entry : PENDING_UPDATES) {
                ModConfigSpec spec = (ModConfigSpec) entry.getSpec();
                Set<String> paths = new HashSet<>();
                collectPaths(spec.getValues(), "", paths);
                Path path = entry.getFullPath().toAbsolutePath().normalize();
                byte[] original = ORIGINAL_FILES.get(path);
                if (original != null) try { Files.write(path, original); }
                catch (IOException error) {
                    System.err.println("Mallard Guard: could not read original config " + path + ": " + error.getMessage());
                    continue;
                }
                try (CommentedFileConfig file = CommentedFileConfig.builder(entry.getFullPath()).sync().build()) {
                    file.load();
                    removeStale(file, "", paths);
                    file.save();
                } catch (RuntimeException error) {
                    System.err.println("Mallard Guard: could not clean old config keys: " + error.getMessage());
                    continue;
                }
                if (spec == SERVER_SPEC) {
                    int[] preset = GuardClientPreset.parse(CLIENT_PRESET.get());
                    if (preset != null) CLIENT_PRESET.set(GuardClientPreset.encode(preset));
                }
                (spec == CLIENT_SPEC ? CLIENT_VERSION : SERVER_VERSION).set(currentVersion);
                spec.save();
                ORIGINAL_FILES.remove(path);
            }
            PENDING_UPDATES.clear();
        }
    }

    private static void collectPaths(UnmodifiableConfig entries, String prefix, Set<String> paths) {
        for (var entry : entries.valueMap().entrySet()) {
            String path = prefix + entry.getKey();
            if (entry.getValue() instanceof UnmodifiableConfig group) collectPaths(group, path + ".", paths);
            else if (entry.getValue() instanceof ModConfigSpec.ConfigValue<?>) paths.add(path);
        }
    }

    private static int obsoleteInSnapshot(byte[] bytes, Set<String> paths) {
        int count = 0;
        String section = "";
        for (String line : new String(bytes, java.nio.charset.StandardCharsets.UTF_8).split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) section = trimmed.substring(1, trimmed.length() - 1) + ".";
            else if (!trimmed.startsWith("#") && !trimmed.isEmpty()) {
                int equals = trimmed.indexOf('=');
                if (equals > 0 && !paths.contains(section + trimmed.substring(0, equals).trim())) count++;
            }
        }
        return count;
    }

    private static void removeStale(Config config, String prefix, Set<String> paths) {
        for (String key : new ArrayList<>(config.valueMap().keySet())) {
            String path = prefix + key;
            Object value = config.valueMap().get(key);
            if (value instanceof Config group) {
                removeStale(group, path + ".", paths);
                if (group.valueMap().isEmpty()) config.remove(key);
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
    }

    public static GuardPackets.Settings snapshot(boolean operator) {
        return new GuardPackets.Settings(PARRY.get(), BLOCK.get(), false, false, false, PERFECT_TICKS.get(), PARRY_TICKS.get(), RECHARGE_TICKS.get(), FACING_ANGLE.get(), (int) Math.round(BLOCK_REDUCTION.get() * 100.0D), FOLLOW_UP_TICKS.get(), (int) Math.round(PARRY_RETALIATION.get() * 100), (int) Math.round(PERFECT_RETALIATION.get() * 100), PARRY_WEAR.get(), PERFECT_WEAR.get(), BLOCK_WEAR.get(), HIT_SOUNDS.get(), HIT_PARTICLES.get(), MASTER_VOLUME.get(), PERFECT_VOLUME.get(), PARRY_VOLUME.get(), BLOCK_VOLUME.get(), true, FALL_PERFECT_PARRY.get(), FALL_LOOK_DOWN.get(), FALL_BREAK_BLOCKS.get(), FALL_BLAST_STRENGTH.get(), FALL_LAUNCH_POWER.get(), PARRY_EXPLOSIONS.get(), PERFECT_EXPLOSIONS_ONLY.get(), BLOCK_EXPLOSIONS.get(), PARRY_PROJECTILES.get(), BLOCK_PROJECTILES.get(), DEFENDER_KNOCKBACK.get(), KNOCKBACK_STRENGTH.get(), GUARD_MOVEMENT_PERCENT.get(), BLOCK_DEFLECT_CHANCE.get(), ALLOW_ANY_ITEM.get(), ALLOW_USABLE_ITEMS.get(), INCLUDED_ITEMS.get(), EXCLUDED_ITEMS.get(), SHIELD_ITEMS.get(), CONSUMABLE_PRIORITY.get(), operator);
    }

    public static GuardPackets.Settings defaultSnapshot(boolean operator) {
        return new GuardPackets.Settings(PARRY.getDefault(), BLOCK.getDefault(), false, false, false, PERFECT_TICKS.getDefault(), PARRY_TICKS.getDefault(), RECHARGE_TICKS.getDefault(), FACING_ANGLE.getDefault(), (int) Math.round(BLOCK_REDUCTION.getDefault() * 100.0D), FOLLOW_UP_TICKS.getDefault(), (int) Math.round(PARRY_RETALIATION.getDefault() * 100), (int) Math.round(PERFECT_RETALIATION.getDefault() * 100), PARRY_WEAR.getDefault(), PERFECT_WEAR.getDefault(), BLOCK_WEAR.getDefault(), HIT_SOUNDS.getDefault(), HIT_PARTICLES.getDefault(), MASTER_VOLUME.getDefault(), PERFECT_VOLUME.getDefault(), PARRY_VOLUME.getDefault(), BLOCK_VOLUME.getDefault(), true, FALL_PERFECT_PARRY.getDefault(), FALL_LOOK_DOWN.getDefault(), FALL_BREAK_BLOCKS.getDefault(), FALL_BLAST_STRENGTH.getDefault(), FALL_LAUNCH_POWER.getDefault(), PARRY_EXPLOSIONS.getDefault(), PERFECT_EXPLOSIONS_ONLY.getDefault(), BLOCK_EXPLOSIONS.getDefault(), PARRY_PROJECTILES.getDefault(), BLOCK_PROJECTILES.getDefault(), DEFENDER_KNOCKBACK.getDefault(), KNOCKBACK_STRENGTH.getDefault(), GUARD_MOVEMENT_PERCENT.getDefault(), BLOCK_DEFLECT_CHANCE.getDefault(), ALLOW_ANY_ITEM.getDefault(), ALLOW_USABLE_ITEMS.getDefault(), INCLUDED_ITEMS.getDefault(), EXCLUDED_ITEMS.getDefault(), SHIELD_ITEMS.getDefault(), CONSUMABLE_PRIORITY.getDefault(), operator);
    }

    public static GuardPackets.ShieldSettings shieldSnapshot() {
        return new GuardPackets.ShieldSettings(SHIELD_PERFECT_TICKS.get(), SHIELD_PARRY_TICKS.get(),
            SHIELD_RECHARGE_TICKS.get(), SHIELD_MAX_BLOCKS.get(), TOOL_MAX_BLOCKS.get(), SHIELD_BREAK_TICKS.get(), SHIELD_CONE_DEGREES.get(), SHIELD_CONE_REACH.get(),
            SHIELD_RETALIATION_PERCENT.get(), SHIELD_STUN_TICKS.get(), SHIELD_PUSHBACK_PERCENT.get(),
            SHIELD_PARRY_PUSHBACK_PERCENT.get(), TOOL_PUSHBACK_PERCENT.get(), SHIELD_STUN_BOSSES.get(), CONSUMABLE_PRIORITY.get(), COOLDOWN_PREVENTS_GUARD.get());
    }

    public static GuardPackets.ShieldSettings defaultShieldSnapshot() {
        return new GuardPackets.ShieldSettings(SHIELD_PERFECT_TICKS.getDefault(), SHIELD_PARRY_TICKS.getDefault(),
            SHIELD_RECHARGE_TICKS.getDefault(), SHIELD_MAX_BLOCKS.getDefault(), TOOL_MAX_BLOCKS.getDefault(), SHIELD_BREAK_TICKS.getDefault(), SHIELD_CONE_DEGREES.getDefault(), SHIELD_CONE_REACH.getDefault(),
            SHIELD_RETALIATION_PERCENT.getDefault(), SHIELD_STUN_TICKS.getDefault(), SHIELD_PUSHBACK_PERCENT.getDefault(),
            SHIELD_PARRY_PUSHBACK_PERCENT.getDefault(), TOOL_PUSHBACK_PERCENT.getDefault(), SHIELD_STUN_BOSSES.getDefault(), CONSUMABLE_PRIORITY.getDefault(), COOLDOWN_PREVENTS_GUARD.getDefault());
    }

    public static void applyShield(GuardPackets.ShieldSettings data) {
        SHIELD_PERFECT_TICKS.set(data.perfect()); SHIELD_PARRY_TICKS.set(data.window());
        SHIELD_RECHARGE_TICKS.set(data.rechargeTicks()); SHIELD_MAX_BLOCKS.set(data.maxBlocks()); TOOL_MAX_BLOCKS.set(data.toolMaxBlocks()); SHIELD_BREAK_TICKS.set(data.breakTicks());
        SHIELD_CONE_DEGREES.set(data.cone()); SHIELD_CONE_REACH.set(data.reach()); SHIELD_RETALIATION_PERCENT.set(data.retaliation());
        SHIELD_STUN_TICKS.set(data.stunTicks()); SHIELD_PUSHBACK_PERCENT.set(data.perfectPushback());
        SHIELD_PARRY_PUSHBACK_PERCENT.set(data.regularPushback()); TOOL_PUSHBACK_PERCENT.set(data.weaponPushback());
        SHIELD_STUN_BOSSES.set(data.stunnableBosses()); CONSUMABLE_PRIORITY.set(data.consumablePriority()); COOLDOWN_PREVENTS_GUARD.set(data.cooldownPreventsGuard());
        SERVER_SPEC.save();
    }
}
