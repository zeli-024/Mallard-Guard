package dev.zeli.mallardguard;

import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.neoforged.fml.loading.FMLPaths;
import java.util.stream.Collectors;

/** Values used when a server supplies initial or enforced client settings. */
public final class GuardClientPreset {
    public static final int PARTICLES = 1, HUD = 2, AUDIO = 4, ITEMS = 8, STAGGER = 16;
    public static final int[] CATEGORIES = {PARTICLES, HUD, AUDIO, ITEMS, STAGGER};
    public static final int ALL_CATEGORIES = 31;
    public static final int[] DEFAULTS = {
        1, 1, 1, 6, 100, 100, 0, 10, 30, 100, 100, 1,
        100, 65, 130, 65, 100, 170, 2, 100, 50, 80, 1, 1,
        0, 0, 1, 1, 1, 90, 80, 30, 30, 5, 1, 1,
        6, 30, 200, 80, 400, 200, 400, 400, 10, 20, 60, 15,
        40, 1, 400, 186, 110, 200, 101, 30, 1, 1, 200, 10,
        0, 1, 0, 1, 1
    };
    private static final int[] MIN = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 2, 1, 1, 0, 0,
        1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 15,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
        0, 0, 0, 0, 0
    };
    private static final int[] MAX = {
        1, 1, 1, 10, 100, 100, 1, 200, 200, 100, 100, 1,
        200, 200, 200, 200, 200, 200, 16, 200, 200, 200, 1, 1,
        1, 1, 1, 1, 1, 200, 200, 40, 30, 12, 1, 1,
        12, 200, 200, 200, 400, 400, 400, 400, 200, 200, 200, 15,
        200, 1, 400, 400, 400, 400, 400, 400, 1, 1, 200, 40,
        1, 1, 1, 1, 100
    };

    // The copied client config is an optional, editable template on the server.
    private static final String[] TEMPLATE_KEYS = {
        "display.hud", "display.shieldEffects", "display.screenFlash", "display.hitlagFrames",
        "display.flashStrength", "display.shakeStrength", "display.offhandGuardPriority",
        "display.regularOrbSize", "display.perfectOrbSize", "display.regularOrbOpacity", "display.perfectOrbOpacity",
        "animation.firstPersonGuardPose", "audio.masterVolume", "audio.parryVolume", "audio.perfectVolume", "audio.blockVolume",
        "sparks.regularAmount", "sparks.perfectAmount", "sparks.stars", "sparks.lifetime", "sparks.length", "sparks.explosiveness",
        "display.sparksEnabled", "display.particleFlashesEnabled", "display.memeFlash", "display.sparkRing",
        "display.streaksEnabled", "display.perfectOnlyFlash", "display.perfectOnlyHitlag",
        "sparks.streakAmount", "sparks.streakLength", "sparks.streakLifetime", null,
        "display.regularOrbLifetime", "display.impactFrames", "display.impactPerfectOnly", "display.perfectOrbLifetime",
        "sparks.streakDotSize", "sparks.streakStretchSpeed", "sparks.regularStreakAmount",
        "display.impactBrightness", "display.impactContrast", "display.impactEdges", "display.impactGrain",
        "sparks.reach", "sparks.streakDotSpacing", "sparks.regularStreakDotSpacing", null, "sparks.streakBallisticCurve", "animation.thirdPersonGuardPose",
        "display.impactChromatic", "display.mobImpactBrightness", "display.mobImpactContrast", "display.mobImpactEdges", "display.mobImpactGrain", "display.mobImpactChromatic", "display.mobImpactAdapt", "display.mobImpactInvert", "display.chromaticIntensity", "display.chromaticTicks", "display.chromaticPerfectOnly", "display.chromaticHud", "display.streakReverse", "display.staggerCompact", "display.staggerHudOffset"
    };

    public static Path enforceTemplatePath() {
        return FMLPaths.CONFIGDIR.get().resolve("mallard_guard/enforce/client.toml");
    }

    private static boolean booleanField(int index) {
        return switch (index) {
            case 0, 1, 2, 6, 11, 22, 23, 24, 25, 26, 27, 28, 35, 49, 56, 57, 60, 61, 62, 63 -> true;
            default -> false;
        };
    }

    public static int[] readEnforceTemplate(int[] fallback) {
        Path path = enforceTemplatePath();
        if (!Files.isRegularFile(path)) return fallback;
        int[] values = fallback.clone();
        try (CommentedFileConfig config = CommentedFileConfig.builder(path).sync().build()) {
            config.load();
            for (int i = 0; i < TEMPLATE_KEYS.length; i++) {
                if (TEMPLATE_KEYS[i] == null) continue;
                Object raw = config.get(TEMPLATE_KEYS[i]);
                if (raw instanceof Boolean booleanValue && booleanField(i)) values[i] = booleanValue ? 1 : 0;
                else if (raw instanceof Number number && !booleanField(i)) {
                    int value = number.intValue();
                    if (value >= MIN[i] && value <= MAX[i]) values[i] = value;
                }
            }
            Number revision = config.get("configRevision");
            if (revision == null || revision.intValue() < 12) {
                for (int i = 0; i < TEMPLATE_KEYS.length; i++) {
                    if (TEMPLATE_KEYS[i] != null && config.contains(TEMPLATE_KEYS[i])) values[i] = normalizeOldValue(i, values[i]);
                }
            }
            if (revision == null || revision.intValue() < 15) {
                for (int i = 0; i < TEMPLATE_KEYS.length; i++) {
                    if (TEMPLATE_KEYS[i] != null && config.contains(TEMPLATE_KEYS[i])) values[i] = sharedScale(i, values[i]);
                }
            }
            if (Boolean.FALSE.equals(config.get("display.screenShake"))) values[5] = 0;
            return values;
        } catch (RuntimeException error) {
            System.err.println("Mallard Guard: could not load enforce template: " + error.getMessage());
            return fallback;
        }
    }

    public static boolean writeEnforceTemplate(int[] values) {
        Path path = enforceTemplatePath();
        if (!Files.isRegularFile(path)) return true;
        try (CommentedFileConfig config = CommentedFileConfig.builder(path).sync().build()) {
            config.load();
            for (int i = 0; i < TEMPLATE_KEYS.length; i++) {
                if (TEMPLATE_KEYS[i] != null) {
                    Object value = booleanField(i) ? Boolean.valueOf(values[i] != 0) : Integer.valueOf(values[i]);
                    config.set(TEMPLATE_KEYS[i], value);
                }
            }
            config.set("display.screenShake", values[5] > 0);
            config.set("configRevision", GuardConfig.CONFIG_REVISION);
            config.save();
            GuardConfig.clearBackups(path.getParent());
            return true;
        } catch (RuntimeException error) {
            System.err.println("Mallard Guard: could not save enforce template: " + error.getMessage());
            return false;
        }
    }

    private static int normalizeOldValue(int index, int value) {
        float factor = switch (index) {
            case 7, 44 -> 0.1F;
            case 8 -> 0.3F;
            case 17 -> 1.3F;
            case 20 -> 0.5F;
            case 21 -> 0.2F;
            case 30 -> 1.4F;
            case 37, 46 -> 0.6F;
            case 45 -> 0.4F;
            default -> 1.0F;
        };
        return Math.clamp(Math.round(value / factor), MIN[index], MAX[index]);
    }

    private static int sharedScale(int index, int value) {
        float factor = switch (index) {
            case 7, 44 -> 0.1F;
            case 8 -> 0.3F;
            case 17 -> 1.69F;
            case 20 -> 0.5F;
            case 21 -> 0.8F;
            case 29 -> 1.1F;
            case 30 -> 1.4F;
            case 37, 46 -> 0.6F;
            case 45 -> 0.4F;
            default -> 1.0F;
        };
        return Math.clamp(Math.round(value * factor), MIN[index], MAX[index]);
    }

    private GuardClientPreset() {}

    public static String encode(int[] values) {
        return Arrays.stream(values).mapToObj(Integer::toString).collect(Collectors.joining(","));
    }

    public static int[] parse(String text) {
        if (text == null || text.length() > 512) return null;
        String[] parts = text.split(",", -1);
        // Accept saved server presets from earlier config revisions, filling new slots with defaults.
        if (parts.length != DEFAULTS.length && parts.length != 50 && parts.length != 49 && parts.length != 48 && parts.length != 47 && parts.length != 46 && parts.length != 45 && parts.length != 44 && parts.length != 40 && parts.length != 39 && parts.length != 35 && parts.length != 29 && parts.length != 26 && parts.length != 25) return null;
        int[] values = DEFAULTS.clone();
        try {
            boolean oldScale = parts.length < 48 || Integer.parseInt(parts[47]) < 15;
            for (int i = 0; i < parts.length; i++) {
                values[i] = Integer.parseInt(parts[i]);
                if (parts.length < 40) {
                    if (i == 16 || i == 17 || i == 19) values[i] = Math.clamp(Math.round(values[i] / 0.7F), MIN[i], MAX[i]);
                    if (i == 20 || i == 21) values[i] = Math.clamp(Math.round(values[i] / 2.0F), MIN[i], MAX[i]);
                    if (i == 29) values[i] = Math.clamp(Math.round(values[i] / 0.75F), MIN[i], MAX[i]);
                    if (i == 30) values[i] = Math.clamp(Math.round(values[i] / 2.5F), MIN[i], MAX[i]);
                    if (i == 37) values[i] = Math.clamp(Math.round(values[i] / 2.0F), MIN[i], MAX[i]);
                    if (i == 38) values[i] = Math.clamp(Math.round(values[i] / 4.0F), MIN[i], MAX[i]);
                }
                if (parts.length < 45 && parts.length >= 40) {
                    if (i == 21) values[i] = Math.clamp(Math.round(values[i] / 2.0F), MIN[i], MAX[i]);
                    if (i == 17) values[i] = Math.clamp(Math.round(values[i] / 1.3F), MIN[i], MAX[i]);
                    if (i == 29) values[i] = Math.clamp(Math.round(values[i] / 1.1F), MIN[i], MAX[i]);
                    if (i == 30) values[i] = Math.clamp(Math.round(values[i] / 2.0F), MIN[i], MAX[i]);
                    if (i == 37) values[i] = Math.clamp(Math.round(values[i] / 0.9F), MIN[i], MAX[i]);
                    if (i == 38) values[i] = Math.clamp(Math.round(values[i] / 1.8F), MIN[i], MAX[i]);
                }
                if (i == 3) values[i] = Math.min(10, values[i]);
                if (i == 12) values[i] = Math.min(200, values[i]);
                if (i == 25 || i == 34) values[i] = Math.min(1, values[i]);
                if (parts.length < 48) values[i] = normalizeOldValue(i, values[i]);
                if (oldScale) values[i] = i == 47 ? 15 : sharedScale(i, values[i]);
                if (values[i] < MIN[i] || values[i] > MAX[i]) return null;
            }
            if (parts.length == 35) values[36] = values[33];
            if (parts.length > 29 && parts.length < 40) values[39] = values[29];
        } catch (NumberFormatException error) { return null; }
        return values;
    }

    public static boolean valid(String text) { return parse(text) != null; }

    public static int categoryOf(int index) {
        if (index == 6) return ITEMS;
        if (index == 62) return PARTICLES;
        if (index == 63 || index == 64) return STAGGER;
        if (index == 63 || index == 64) return STAGGER;
        if (index >= 50) return HUD;
        if (index == 49 || index == 11 || index == 24 || index == 27 || index == 28 || index == 34 || index == 35) return HUD;
        if (index >= 12 && index <= 15) return AUDIO;
        if (index <= 5 || index >= 40 && index <= 43) return HUD;
        return PARTICLES;
    }

    public static int[] readLocal() {
        return new int[] {
            GuardConfig.HUD.get() ? 1 : 0, GuardConfig.SHIELD_EFFECTS.get() ? 1 : 0,
            GuardConfig.SCREEN_FLASH.get() ? 1 : 0, GuardConfig.HITLAG_FRAMES.get(),
            GuardConfig.FLASH_STRENGTH.get(), GuardConfig.SCREEN_SHAKE.get() ? GuardConfig.SHAKE_STRENGTH.get() : 0,
            GuardConfig.PREFER_OFFHAND.get() ? 1 : 0,
            GuardConfig.REGULAR_ORB_SIZE.get(), GuardConfig.PERFECT_ORB_SIZE.get(),
            GuardConfig.REGULAR_ORB_OPACITY.get(), GuardConfig.PERFECT_ORB_OPACITY.get(),
            GuardConfig.FIRST_PERSON_ANIMATION.get() ? 1 : 0,
            GuardConfig.LOCAL_MASTER_VOLUME.get(), GuardConfig.LOCAL_PARRY_VOLUME.get(),
            GuardConfig.LOCAL_PERFECT_VOLUME.get(), GuardConfig.LOCAL_BLOCK_VOLUME.get(),
            GuardConfig.REGULAR_SPARK_COUNT.get(), GuardConfig.PERFECT_SPARK_COUNT.get(),
            GuardConfig.STAR_COUNT.get(), GuardConfig.SPARK_LIFETIME.get(),
            GuardConfig.SPARK_LENGTH.get(), GuardConfig.SPARK_EXPLOSIVENESS.get(),
            GuardConfig.SPARKS_ENABLED.get() ? 1 : 0, GuardConfig.PARTICLE_FLASHES_ENABLED.get() ? 1 : 0,
            GuardConfig.MEME_FLASH.get() ? 1 : 0, GuardConfig.SPARK_RING.get() ? 1 : 0,
            GuardConfig.STREAKS_ENABLED.get() ? 1 : 0, GuardConfig.PERFECT_ONLY_FLASH.get() ? 1 : 0,
            GuardConfig.PERFECT_ONLY_HITLAG.get() ? 1 : 0,
            GuardConfig.STREAK_AMOUNT.get(), GuardConfig.STREAK_LENGTH.get(), GuardConfig.STREAK_LIFETIME.get(),
            30, GuardConfig.REGULAR_ORB_LIFETIME.get(), GuardConfig.IMPACT_FRAMES.get(),
            GuardConfig.IMPACT_PERFECT_ONLY.get() ? 1 : 0, GuardConfig.PERFECT_ORB_LIFETIME.get(),
            GuardConfig.STREAK_DOT_SIZE.get(), GuardConfig.STREAK_STRETCH_SPEED.get(), GuardConfig.REGULAR_STREAK_AMOUNT.get(),
            GuardConfig.IMPACT_BRIGHTNESS.get(), GuardConfig.IMPACT_CONTRAST.get(), GuardConfig.IMPACT_EDGES.get(), GuardConfig.IMPACT_GRAIN.get(), GuardConfig.SPARK_REACH.get(), GuardConfig.STREAK_DOT_SPACING.get(), GuardConfig.REGULAR_STREAK_DOT_SPACING.get(), 15, GuardConfig.STREAK_BALLISTIC_CURVE.get(), GuardConfig.THIRD_PERSON_ANIMATION.get() ? 1 : 0,
            GuardConfig.IMPACT_CHROMATIC.get(), GuardConfig.MOB_IMPACT_BRIGHTNESS.get(), GuardConfig.MOB_IMPACT_CONTRAST.get(), GuardConfig.MOB_IMPACT_EDGES.get(), GuardConfig.MOB_IMPACT_GRAIN.get(), GuardConfig.MOB_IMPACT_CHROMATIC.get(), GuardConfig.MOB_IMPACT_ADAPT.get() ? 1 : 0, GuardConfig.MOB_IMPACT_INVERT.get() ? 1 : 0, GuardConfig.CHROMATIC_INTENSITY.get(), GuardConfig.CHROMATIC_TICKS.get(), GuardConfig.CHROMATIC_PERFECT_ONLY.get() ? 1 : 0, GuardConfig.CHROMATIC_HUD.get() ? 1 : 0, GuardConfig.STREAK_REVERSE.get() ? 1 : 0, GuardConfig.STAGGER_COMPACT.get() ? 1 : 0, GuardConfig.STAGGER_HUD_OFFSET.get()
        };
    }

    public static void apply(int[] values, int category) {
        if (category == PARTICLES) {
            GuardConfig.REGULAR_ORB_SIZE.set(values[7]); GuardConfig.PERFECT_ORB_SIZE.set(values[8]);
            GuardConfig.REGULAR_ORB_OPACITY.set(values[9]); GuardConfig.PERFECT_ORB_OPACITY.set(values[10]);
            GuardConfig.REGULAR_SPARK_COUNT.set(values[16]); GuardConfig.PERFECT_SPARK_COUNT.set(values[17]);
            GuardConfig.STAR_COUNT.set(values[18]); GuardConfig.SPARK_LIFETIME.set(values[19]);
            GuardConfig.SPARK_LENGTH.set(values[20]); GuardConfig.SPARK_EXPLOSIVENESS.set(values[21]);
            GuardConfig.SPARKS_ENABLED.set(values[22] != 0); GuardConfig.PARTICLE_FLASHES_ENABLED.set(values[23] != 0);
            GuardConfig.SPARK_RING.set(values[25] != 0); GuardConfig.STREAKS_ENABLED.set(values[26] != 0);
            GuardConfig.STREAK_AMOUNT.set(values[29]); GuardConfig.REGULAR_STREAK_AMOUNT.set(values[39]); GuardConfig.STREAK_LENGTH.set(values[30]);
            GuardConfig.STREAK_LIFETIME.set(values[31]); // Slot 32 is retained for old server presets.
            GuardConfig.REGULAR_ORB_LIFETIME.set(values[33]); GuardConfig.PERFECT_ORB_LIFETIME.set(values[36]);
            GuardConfig.SPARK_REACH.set(values[44]); GuardConfig.STREAK_DOT_SIZE.set(values[37]); GuardConfig.STREAK_STRETCH_SPEED.set(values[38]); GuardConfig.STREAK_DOT_SPACING.set(values[45]); GuardConfig.REGULAR_STREAK_DOT_SPACING.set(values[46]); GuardConfig.STREAK_BALLISTIC_CURVE.set(values[48]); GuardConfig.STREAK_REVERSE.set(values[62] != 0);
        } else if (category == HUD) {
            GuardConfig.THIRD_PERSON_ANIMATION.set(values[49] != 0); GuardConfig.HUD.set(values[0] != 0); GuardConfig.SHIELD_EFFECTS.set(values[1] != 0);
            GuardConfig.SCREEN_FLASH.set(values[2] != 0); GuardConfig.HITLAG_FRAMES.set(values[3]);
            GuardConfig.FLASH_STRENGTH.set(values[4]); GuardConfig.SCREEN_SHAKE.set(values[5] > 0);
            GuardConfig.SHAKE_STRENGTH.set(values[5]);
            GuardConfig.FIRST_PERSON_ANIMATION.set(values[11] != 0);
            GuardConfig.MEME_FLASH.set(values[24] != 0);
            GuardConfig.PERFECT_ONLY_FLASH.set(values[27] != 0); GuardConfig.PERFECT_ONLY_HITLAG.set(values[28] != 0); GuardConfig.IMPACT_FRAMES.set(values[34]);
            GuardConfig.IMPACT_PERFECT_ONLY.set(values[35] != 0);
            GuardConfig.IMPACT_BRIGHTNESS.set(values[40]); GuardConfig.IMPACT_CONTRAST.set(values[41]);
            GuardConfig.IMPACT_EDGES.set(values[42]); GuardConfig.IMPACT_GRAIN.set(values[43]);
            GuardConfig.IMPACT_CHROMATIC.set(values[50]);
            GuardConfig.MOB_IMPACT_BRIGHTNESS.set(values[51]);
            GuardConfig.MOB_IMPACT_CONTRAST.set(values[52]);
            GuardConfig.MOB_IMPACT_EDGES.set(values[53]);
            GuardConfig.MOB_IMPACT_GRAIN.set(values[54]);
            GuardConfig.MOB_IMPACT_CHROMATIC.set(values[55]);
            GuardConfig.MOB_IMPACT_ADAPT.set(values[56] != 0);
            GuardConfig.MOB_IMPACT_INVERT.set(values[57] != 0);
            GuardConfig.CHROMATIC_INTENSITY.set(values[58]);
            GuardConfig.CHROMATIC_TICKS.set(values[59]);
            GuardConfig.CHROMATIC_PERFECT_ONLY.set(values[60] != 0);
            GuardConfig.CHROMATIC_HUD.set(values[61] != 0);
        } else if (category == STAGGER) {
        } else if (category == STAGGER) {
            GuardConfig.STAGGER_COMPACT.set(values[63] != 0);
            GuardConfig.STAGGER_HUD_OFFSET.set(values[64]);
        } else if (category == AUDIO) {
            GuardConfig.LOCAL_MASTER_VOLUME.set(values[12]); GuardConfig.LOCAL_PARRY_VOLUME.set(values[13]);
            GuardConfig.LOCAL_PERFECT_VOLUME.set(values[14]); GuardConfig.LOCAL_BLOCK_VOLUME.set(values[15]);
        } else if (category == ITEMS) {
            GuardConfig.PREFER_OFFHAND.set(values[6] != 0);
        }
    }
}
