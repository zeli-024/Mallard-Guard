package dev.zeli.mallardguard;

import java.util.function.Supplier;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Stable client setting slots shared by config files, presets, enforcement and the UI. */
public final class GuardClientSettings {
    private record Slot(String key, int category, int initial, int min, int max,
                        boolean flag, Supplier<ModConfigSpec.ConfigValue<?>> value) {}
    // Reserved slots retain their old positions so saved presets stay compatible.
    private static final Slot[] SLOTS = {
        new Slot("display.hud", 2, 1, 0, 1, true, () -> GuardConfig.HUD),
        new Slot("display.shieldEffects", 2, 1, 0, 1, true, () -> GuardConfig.SHIELD_EFFECTS),
        new Slot("display.screenFlash", 8, 1, 0, 1, true, () -> GuardConfig.SCREEN_FLASH),
        new Slot("display.hitlagFrames", 8, 6, 0, 10, false, () -> GuardConfig.HITLAG_FRAMES),
        new Slot("display.flashStrength", 8, 100, 0, 100, false, () -> GuardConfig.FLASH_STRENGTH),
        new Slot("display.shakeStrength", 8, 100, 0, 100, false, () -> GuardConfig.SHAKE_STRENGTH),
        new Slot(null, 0, 0, 0, 1, true, () -> null),
        new Slot("display.regularOrbSize", 1, 20, 0, 200, false, () -> GuardConfig.REGULAR_ORB_SIZE),
        new Slot("display.perfectOrbSize", 1, 40, 0, 200, false, () -> GuardConfig.PERFECT_ORB_SIZE),
        new Slot("display.regularOrbOpacity", 1, 100, 0, 100, false, () -> GuardConfig.REGULAR_ORB_OPACITY),
        new Slot("display.perfectOrbOpacity", 1, 100, 0, 100, false, () -> GuardConfig.PERFECT_ORB_OPACITY),
        new Slot("animation.firstPersonGuardPose", 16, 0, 0, 1, true, () -> GuardConfig.FIRST_PERSON_ANIMATION),
        new Slot("audio.masterVolume", 4, 100, 0, 200, false, () -> GuardConfig.LOCAL_MASTER_VOLUME),
        new Slot("audio.parryVolume", 4, 65, 0, 200, false, () -> GuardConfig.LOCAL_PARRY_VOLUME),
        new Slot("audio.perfectVolume", 4, 130, 0, 200, false, () -> GuardConfig.LOCAL_PERFECT_VOLUME),
        new Slot("audio.blockVolume", 4, 65, 0, 200, false, () -> GuardConfig.LOCAL_BLOCK_VOLUME),
        new Slot("sparks.regularAmount", 1, 100, 0, 200, false, () -> GuardConfig.REGULAR_SPARK_COUNT),
        new Slot("sparks.perfectAmount", 1, 170, 0, 200, false, () -> GuardConfig.PERFECT_SPARK_COUNT),
        new Slot("sparks.stars", 1, 2, 0, 16, false, () -> GuardConfig.STAR_COUNT),
        new Slot("sparks.lifetime", 1, 100, 0, 200, false, () -> GuardConfig.SPARK_LIFETIME),
        new Slot("sparks.length", 1, 50, 0, 200, false, () -> GuardConfig.SPARK_LENGTH),
        new Slot("sparks.explosiveness", 1, 80, 0, 200, false, () -> GuardConfig.SPARK_EXPLOSIVENESS),
        new Slot("display.sparksEnabled", 1, 1, 0, 1, true, () -> GuardConfig.SPARKS_ENABLED),
        new Slot("display.particleFlashesEnabled", 1, 1, 0, 1, true, () -> GuardConfig.PARTICLE_FLASHES_ENABLED),
        new Slot("display.memeFlash", 8, 0, 0, 1, true, () -> GuardConfig.MEME_FLASH),
        new Slot("display.sparkRing", 1, 0, 0, 1, true, () -> GuardConfig.SPARK_RING),
        new Slot("display.streaksEnabled", 1, 1, 0, 1, true, () -> GuardConfig.STREAKS_ENABLED),
        new Slot("display.perfectOnlyFlash", 8, 1, 0, 1, true, () -> GuardConfig.PERFECT_ONLY_FLASH),
        new Slot("display.perfectOnlyHitlag", 8, 1, 0, 1, true, () -> GuardConfig.PERFECT_ONLY_HITLAG),
        new Slot("sparks.streakAmount", 1, 100, 0, 200, false, () -> GuardConfig.STREAK_AMOUNT),
        new Slot("sparks.streakLength", 1, 150, 0, 200, false, () -> GuardConfig.STREAK_LENGTH),
        new Slot("sparks.streakLifetime", 1, 25, 2, 40, false, () -> GuardConfig.STREAK_LIFETIME),
        new Slot(null, 1, 30, 1, 30, false, () -> null),
        new Slot("display.regularOrbLifetime", 1, 5, 1, 12, false, () -> GuardConfig.REGULAR_ORB_LIFETIME),
        new Slot("display.impactFrames", 8, 1, 0, 1, false, () -> GuardConfig.IMPACT_FRAMES),
        new Slot("display.impactPerfectOnly", 8, 1, 0, 1, true, () -> GuardConfig.IMPACT_PERFECT_ONLY),
        new Slot("display.perfectOrbLifetime", 1, 6, 1, 12, false, () -> GuardConfig.PERFECT_ORB_LIFETIME),
        new Slot("sparks.streakDotSize", 1, 100, 0, 200, false, () -> GuardConfig.STREAK_DOT_SIZE),
        new Slot("sparks.streakStretchSpeed", 1, 200, 0, 200, false, () -> GuardConfig.STREAK_STRETCH_SPEED),
        new Slot("sparks.regularStreakAmount", 1, 100, 0, 200, false, () -> GuardConfig.REGULAR_STREAK_AMOUNT),
        new Slot("display.impactBrightness", 8, 400, 0, 400, false, () -> GuardConfig.IMPACT_BRIGHTNESS),
        new Slot("display.impactContrast", 8, 200, 0, 400, false, () -> GuardConfig.IMPACT_CONTRAST),
        new Slot("display.impactEdges", 8, 400, 0, 400, false, () -> GuardConfig.IMPACT_EDGES),
        new Slot("display.impactGrain", 8, 400, 0, 400, false, () -> GuardConfig.IMPACT_GRAIN),
        new Slot("sparks.reach", 1, 10, 0, 200, false, () -> GuardConfig.SPARK_REACH),
        new Slot("sparks.streakDotSpacing", 1, 35, 1, 200, false, () -> GuardConfig.STREAK_DOT_SPACING),
        new Slot("sparks.regularStreakDotSpacing", 1, 50, 1, 200, false, () -> GuardConfig.REGULAR_STREAK_DOT_SPACING),
        new Slot(null, 1, 15, 15, 15, false, () -> null),
        new Slot("sparks.streakBallisticCurve", 1, 20, 0, 200, false, () -> GuardConfig.STREAK_BALLISTIC_CURVE),
        new Slot("animation.thirdPersonGuardPose", 16, 1, 0, 1, true, () -> GuardConfig.THIRD_PERSON_ANIMATION),
        new Slot("display.impactChromatic", 8, 400, 0, 400, false, () -> GuardConfig.IMPACT_CHROMATIC),
        new Slot("display.mobImpactBrightness", 8, 186, 0, 400, false, () -> GuardConfig.MOB_IMPACT_BRIGHTNESS),
        new Slot("display.mobImpactContrast", 8, 110, 0, 400, false, () -> GuardConfig.MOB_IMPACT_CONTRAST),
        new Slot("display.mobImpactEdges", 8, 200, 0, 400, false, () -> GuardConfig.MOB_IMPACT_EDGES),
        new Slot("display.mobImpactGrain", 8, 101, 0, 400, false, () -> GuardConfig.MOB_IMPACT_GRAIN),
        new Slot("display.mobImpactChromatic", 8, 30, 0, 400, false, () -> GuardConfig.MOB_IMPACT_CHROMATIC),
        new Slot("display.mobImpactAdapt", 8, 1, 0, 1, true, () -> GuardConfig.MOB_IMPACT_ADAPT),
        new Slot("display.mobImpactInvert", 8, 1, 0, 1, true, () -> GuardConfig.MOB_IMPACT_INVERT),
        new Slot("display.chromaticIntensity", 8, 200, 0, 200, false, () -> GuardConfig.CHROMATIC_INTENSITY),
        new Slot("display.chromaticTicks", 8, 10, 1, 40, false, () -> GuardConfig.CHROMATIC_TICKS),
        new Slot("display.chromaticPerfectOnly", 8, 0, 0, 1, true, () -> GuardConfig.CHROMATIC_PERFECT_ONLY),
        new Slot("display.chromaticHud", 8, 1, 0, 1, true, () -> GuardConfig.CHROMATIC_HUD),
        new Slot("display.streakReverse", 1, 1, 0, 1, true, () -> GuardConfig.STREAK_REVERSE),
        new Slot(null, 0, 0, 0, 1, true, () -> null),
        new Slot(null, 0, 0, 0, 100, false, () -> null),
    };
    private GuardClientSettings() {}
    public static int count() { return SLOTS.length; }
    public static String key(int index) { return SLOTS[index].key(); }
    public static int category(int index) { return SLOTS[index].category(); }
    public static boolean flag(int index) { return SLOTS[index].flag(); }
    public static int initial(int index) { return SLOTS[index].initial(); }
    public static boolean initialFlag(int index) { return initial(index) != 0; }
    public static int minimum(int index) { return SLOTS[index].min(); }
    public static int maximum(int index) { return SLOTS[index].max(); }
    public static int[] defaults() {
        int[] values = new int[count()];
        for (int i = 0; i < values.length; i++) values[i] = SLOTS[i].initial();
        return values;
    }
    public static int[] minimums() { int[] out = new int[count()]; for(int i=0;i<out.length;i++)out[i]=minimum(i); return out; }
    public static int[] maximums() { int[] out = new int[count()]; for(int i=0;i<out.length;i++)out[i]=maximum(i); return out; }
    public static String[] keys() { String[] out = new String[count()]; for(int i=0;i<out.length;i++)out[i]=key(i); return out; }
    public static int indexOf(ModConfigSpec.ConfigValue<?> value) {
        for (int i = 0; i < SLOTS.length; i++) if (SLOTS[i].value().get() == value) return i;
        return -1;
    }
    public static int[] readLocal() {
        int[] values = defaults();
        for (int i = 0; i < values.length; i++) {
            var setting = SLOTS[i].value().get();
            if (setting == null) continue;
            Object value = setting.get();
            values[i] = value instanceof Boolean enabled ? enabled ? 1 : 0 : ((Number)value).intValue();
        }
        if (!GuardConfig.SCREEN_SHAKE.get()) values[5] = 0;
        return values;
    }
    @SuppressWarnings("unchecked")
    public static void apply(int[] values, int category) {
        for (int i = 0; i < SLOTS.length; i++) {
            Slot slot = SLOTS[i];
            if (slot.category() != category) continue;
            var setting = slot.value().get();
            if (setting == null) continue;
            if (slot.flag()) ((ModConfigSpec.ConfigValue<Boolean>) setting).set(values[i] != 0);
            else ((ModConfigSpec.ConfigValue<Integer>) setting).set(values[i]);
        }
        if (category == GuardClientPreset.SCREEN) GuardConfig.SCREEN_SHAKE.set(values[5] > 0);
    }
}
