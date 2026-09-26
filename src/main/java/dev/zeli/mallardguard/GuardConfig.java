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
        SERVER_SPEC.save();
    }

    public static GuardPackets.Settings snapshot(boolean operator) {
        return new GuardPackets.Settings(PARRY.get(), BLOCK.get(), PERFECT_TICKS.get(), PARRY_TICKS.get(), RECHARGE_TICKS.get(), operator);
    }
}
