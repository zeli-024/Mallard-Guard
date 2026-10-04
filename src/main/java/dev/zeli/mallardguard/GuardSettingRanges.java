package dev.zeli.mallardguard;

import java.util.IdentityHashMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Numeric bounds declared once with the server config and shared with validation and controls. */
public final class GuardSettingRanges {
    public record Range(double min, double max) {
        public boolean accepts(double value) { return Double.isFinite(value) && value >= min && value <= max; }
    }
    private static final Map<ModConfigSpec.ConfigValue<?>, Range> RANGES = new IdentityHashMap<>();
    private GuardSettingRanges() {}
    public static ModConfigSpec.IntValue integer(ModConfigSpec.Builder builder, String key, int initial, int min, int max) {
        var value = builder.defineInRange(key, initial, min, max);
        RANGES.put(value, new Range(min, max));
        return value;
    }
    public static ModConfigSpec.DoubleValue decimal(ModConfigSpec.Builder builder, String key, double initial, double min, double max) {
        var value = builder.defineInRange(key, initial, min, max);
        RANGES.put(value, new Range(min, max));
        return value;
    }
    public static Range range(ModConfigSpec.ConfigValue<?> value) { return RANGES.get(value); }
    public static boolean accepts(ModConfigSpec.ConfigValue<?> setting, double value) {
        Range bounds = range(setting);
        return bounds != null && bounds.accepts(value);
    }
}
