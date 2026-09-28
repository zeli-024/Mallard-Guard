package dev.zeli.mallardguard;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Values used when a server supplies initial or enforced client settings. */
public final class GuardClientPreset {
    public static final int PARTICLES = 1, HUD = 2, AUDIO = 4, ITEMS = 8;
    public static final int[] DEFAULTS = {
        1, 1, 0, 0, 0, 60, 0, 20, 30, 35, 35, 1,
        100, 65, 130, 65, 100, 100, 2, 70, 50, 200, 1, 1, 0, 0
    };
    private static final int[] MIN = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 25, 50, 0, 0, 0, 0, 0
    };
    private static final int[] MAX = {
        1, 1, 1, 8, 100, 100, 1, 150, 150, 100, 100, 1,
        200, 200, 200, 200, 200, 200, 16, 200, 200, 200, 1, 1, 1, 1
    };

    private GuardClientPreset() {}

    public static String encode(int[] values) {
        return Arrays.stream(values).mapToObj(Integer::toString).collect(Collectors.joining(","));
    }

    public static int[] parse(String text) {
        if (text == null || text.length() > 256) return null;
        String[] parts = text.split(",", -1);
        // 1.16 presets had no spark shape value. Older 26-value drafts ended in an impact-frame value.
        if (parts.length != DEFAULTS.length && parts.length != DEFAULTS.length - 1) return null;
        int[] values = DEFAULTS.clone();
        try {
            for (int i = 0; i < parts.length; i++) {
                values[i] = Integer.parseInt(parts[i]);
                if (i == 3) values[i] = Math.min(8, values[i]);
                if (i == 12) values[i] = Math.min(200, values[i]);
                if (i == 25) values[i] = Math.min(1, values[i]);
                if (values[i] < MIN[i] || values[i] > MAX[i]) return null;
            }
        } catch (NumberFormatException error) { return null; }
        return values;
    }

    public static boolean valid(String text) { return parse(text) != null; }

    public static int categoryOf(int index) {
        if (index == 6) return ITEMS;
        if (index == 11 || index == 24) return HUD;
        if (index >= 12 && index <= 15) return AUDIO;
        if (index <= 5) return HUD;
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
            GuardConfig.MEME_FLASH.get() ? 1 : 0, GuardConfig.SPARK_RING.get() ? 1 : 0
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
            GuardConfig.SPARK_RING.set(values[25] != 0);
        } else if (category == HUD) {
            GuardConfig.HUD.set(values[0] != 0); GuardConfig.SHIELD_EFFECTS.set(values[1] != 0);
            GuardConfig.SCREEN_FLASH.set(values[2] != 0); GuardConfig.HITLAG_FRAMES.set(values[3]);
            GuardConfig.FLASH_STRENGTH.set(values[4]); GuardConfig.SCREEN_SHAKE.set(values[5] > 0);
            GuardConfig.SHAKE_STRENGTH.set(values[5]);
            GuardConfig.FIRST_PERSON_ANIMATION.set(values[11] != 0);
            GuardConfig.MEME_FLASH.set(values[24] != 0);
        } else if (category == AUDIO) {
            GuardConfig.LOCAL_MASTER_VOLUME.set(values[12]); GuardConfig.LOCAL_PARRY_VOLUME.set(values[13]);
            GuardConfig.LOCAL_PERFECT_VOLUME.set(values[14]); GuardConfig.LOCAL_BLOCK_VOLUME.set(values[15]);
        } else if (category == ITEMS) {
            GuardConfig.PREFER_OFFHAND.set(values[6] != 0);
        }
    }
}
