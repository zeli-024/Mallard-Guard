package dev.zeli.mallardguard;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class GuardConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec.BooleanValue PARRY;
    public static final ModConfigSpec.BooleanValue BLOCK;
    public static final ModConfigSpec.IntValue PERFECT_TICKS;
    public static final ModConfigSpec.IntValue PARRY_TICKS;
    public static final ModConfigSpec.IntValue RECHARGE_TICKS;
    public static final ModConfigSpec.IntValue FACING_ANGLE;
    public static final ModConfigSpec.DoubleValue BLOCK_REDUCTION;
    public static final ModConfigSpec.BooleanValue HUD;

    static {
        ModConfigSpec.Builder server = new ModConfigSpec.Builder();
        server.push("stance");
        PARRY = server.comment("Allow a parry attempt at the start of a guard.").define("parry", true);
        BLOCK = server.comment("Remain in a guarding stance while Use Item is held.").define("block", true);
        server.pop();
        server.push("timing");
        PERFECT_TICKS = server.comment("Perfect parry duration; zero disables perfect parries.").defineInRange("perfectTicks", 3, 0, 5);
        PARRY_TICKS = server.comment("Total parry duration, including perfect ticks.").defineInRange("parryTicks", 7, 1, 10);
        RECHARGE_TICKS = server.comment("Time before another attempt; continues during held guard.").defineInRange("rechargeTicks", 12, 1, 60);
        server.pop();
        server.push("defense");
        FACING_ANGLE = server.comment("Maximum angle from the player's view direction; 180 allows the front half, 360 allows attacks from any direction.").defineInRange("facingAngle", 180, 0, 360);
        BLOCK_REDUCTION = server.comment("Fraction of incoming entity damage prevented while holding guard.").defineInRange("blockReduction", 0.5D, 0.0D, 1.0D);
        server.pop();
        SERVER_SPEC = server.build();

        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        client.push("display");
        HUD = client.comment("Show the parry shield by the crosshair.").define("hud", true);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    private GuardConfig() {}

    public static void apply(GuardPackets.Settings data) {
        PARRY.set(data.parry());
        BLOCK.set(data.block());
        PERFECT_TICKS.set(data.perfect());
        PARRY_TICKS.set(data.window());
        RECHARGE_TICKS.set(data.recharge());
        FACING_ANGLE.set(data.angle());
        BLOCK_REDUCTION.set(data.reductionPercent() / 100.0D);
        SERVER_SPEC.save();
    }

    public static GuardPackets.Settings snapshot(boolean operator) {
        return new GuardPackets.Settings(PARRY.get(), BLOCK.get(), PERFECT_TICKS.get(), PARRY_TICKS.get(), RECHARGE_TICKS.get(), FACING_ANGLE.get(), (int) Math.round(BLOCK_REDUCTION.get() * 100.0D), operator);
    }
}
