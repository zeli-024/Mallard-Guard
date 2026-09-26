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
    public static final ModConfigSpec.IntValue FOLLOW_UP_TICKS;
    public static final ModConfigSpec.DoubleValue PARRY_RETALIATION;
    public static final ModConfigSpec.DoubleValue PERFECT_RETALIATION;
    public static final ModConfigSpec.IntValue PARRY_WEAR;
    public static final ModConfigSpec.IntValue PERFECT_WEAR;
    public static final ModConfigSpec.IntValue BLOCK_WEAR;
    public static final ModConfigSpec.IntValue ADDED_DURABILITY;
    public static final ModConfigSpec.BooleanValue HIT_SOUNDS;
    public static final ModConfigSpec.BooleanValue HIT_PARTICLES;
    public static final ModConfigSpec.IntValue MASTER_VOLUME;
    public static final ModConfigSpec.IntValue PERFECT_VOLUME;
    public static final ModConfigSpec.IntValue PARRY_VOLUME;
    public static final ModConfigSpec.IntValue BLOCK_VOLUME;
    public static final ModConfigSpec.BooleanValue FALL_PARRY;
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
    public static final ModConfigSpec.BooleanValue ALLOW_USABLE_ITEMS;
    public static final ModConfigSpec.ConfigValue<String> INCLUDED_ITEMS;
    public static final ModConfigSpec.ConfigValue<String> EXCLUDED_ITEMS;
    public static final ModConfigSpec.BooleanValue HUD;
    public static final ModConfigSpec.BooleanValue SHIELD_EFFECTS;
    public static final ModConfigSpec.BooleanValue SCREEN_FLASH;
    public static final ModConfigSpec.IntValue FLASH_STRENGTH;
    public static final ModConfigSpec.BooleanValue SCREEN_SHAKE;
    public static final ModConfigSpec.IntValue SHAKE_STRENGTH;

    static {
        ModConfigSpec.Builder server = new ModConfigSpec.Builder();
        server.push("stance");
        PARRY = server.comment("Allow a parry attempt at the start of a guard.").define("parry", true);
        BLOCK = server.comment("Remain in a guarding stance while the guard key is held.").define("block", true);
        server.pop();
        server.push("timing");
        PERFECT_TICKS = server.comment("Perfect parry duration; zero disables perfect parries.").defineInRange("perfectTicks", 3, 0, 5);
        PARRY_TICKS = server.comment("Total parry duration, including perfect ticks.").defineInRange("parryTicks", 7, 1, 10);
        RECHARGE_TICKS = server.comment("Time before another attempt; continues during held guard.").defineInRange("rechargeTicks", 12, 1, 60);
        server.pop();
        server.push("defense");
        FACING_ANGLE = server.comment("Maximum angle from the player's view direction; 180 allows the front half, 360 allows attacks from any direction.").defineInRange("facingAngle", 180, 0, 360);
        BLOCK_REDUCTION = server.comment("Fraction of incoming entity damage prevented while holding guard.").defineInRange("blockReduction", 0.5D, 0.0D, 1.0D);
        FOLLOW_UP_TICKS = server.comment("Ticks after a successful parry that catch additional valid hits; zero disables it. Follow-up hits never extend this window.").defineInRange("followUpParryTicks", 3, 0, 20);
        PARRY_RETALIATION = server.comment("Regular parry damage returned as a fraction of the incoming damage.").defineInRange("parryRetaliation", 0.8D, 0.0D, 3.0D);
        PERFECT_RETALIATION = server.comment("Perfect parry damage returned as a fraction of the incoming damage.").defineInRange("perfectRetaliation", 1.2D, 0.0D, 3.0D);
        PARRY_WEAR = server.comment("Item durability lost per regular parry, including follow-up parries.").defineInRange("parryWear", 2, 0, 100);
        PERFECT_WEAR = server.comment("Item durability lost per perfect parry.").defineInRange("perfectWear", 1, 0, 100);
        BLOCK_WEAR = server.comment("Item durability lost per blocked hit.").defineInRange("blockWear", 5, 0, 100);
        ADDED_DURABILITY = server.comment("Maximum durability granted to eligible damage items that have none; zero disables this.").defineInRange("addedDurability", 250, 0, 10000);
        server.pop();
        server.push("effects");
        HIT_SOUNDS = server.comment("Play the supplied parry and block sounds to nearby players.").define("hitSounds", true);
        HIT_PARTICLES = server.comment("Spawn flying sparks and a firework flash on parries; perfect parries also emit star-shaped sparks. Blocks emit none.").define("hitParticles", true);
        MASTER_VOLUME = server.comment("Master volume for guard hit sounds, percent; 0 mutes them.").defineInRange("masterVolume", 100, 0, 200);
        PERFECT_VOLUME = server.comment("Perfect parry sound volume, multiplied by master volume.").defineInRange("perfectVolume", 100, 0, 200);
        PARRY_VOLUME = server.comment("Regular parry sound volume, multiplied by master volume.").defineInRange("parryVolume", 100, 0, 200);
        BLOCK_VOLUME = server.comment("Held block sound volume, multiplied by master volume.").defineInRange("blockVolume", 100, 0, 200);
        server.pop();
        server.push("sources");
        FALL_PARRY = server.comment("Parry fall damage and launch in the direction you look, like Parry It. Held blocking does not stop falls.").define("fallParry", true);
        FALL_BREAK_BLOCKS = server.comment("Allow the blast from a long fall parry to break terrain. Disable to prevent terrain damage.").define("fallBlastBreaksBlocks", true);
        FALL_BLAST_STRENGTH = server.comment("Fall parry blast scaling as a percentage of Parry It's default 0.1 radius per damage; 0 disables the blast.").defineInRange("fallBlastStrength", 100, 0, 500);
        FALL_LAUNCH_POWER = server.comment("Fall parry launch strength as a percentage of Parry It's default. Perfect falls launch twice as far. 0 disables launch.").defineInRange("fallLaunchPower", 100, 0, 300);
        PARRY_EXPLOSIONS = server.comment("Allow parry timing to negate explosion damage.").define("parryExplosions", true);
        PERFECT_EXPLOSIONS_ONLY = server.comment("When explosion parrying is enabled, require a perfect parry; regular parries do not negate explosions.").define("perfectExplosionsOnly", false);
        BLOCK_EXPLOSIONS = server.comment("Allow held block to reduce explosion damage by the normal block reduction.").define("blockExplosions", true);
        PARRY_PROJECTILES = server.comment("Parry projectile entities. Regular parries deflect them randomly; perfect parries send them toward the original owner.").define("parryProjectiles", true);
        BLOCK_PROJECTILES = server.comment("Allow held block to reduce projectile damage by the normal block reduction.").define("blockProjectiles", true);
        DEFENDER_KNOCKBACK = server.comment("Push the defender away from the impact on successful melee, projectile, and explosion parries.").define("defenderKnockback", true);
        KNOCKBACK_STRENGTH = server.comment("Defender push strength in percent; 35% is a light shove and 0 disables it.").defineInRange("knockbackStrength", 35, 0, 200);
        server.pop();
        server.push("items");
        ALLOW_USABLE_ITEMS = server.comment("Allow eligible items with their own hold-to-use action to start a guard. Enabled by default for compatibility. Explicit inclusions override this switch.").define("allowUsableItems", true);
        INCLUDED_ITEMS = server.comment("Comma-separated item IDs or #item tags that can guard even without an attack-damage modifier. Exclusions always take priority.").define("includedItems", "", value -> value instanceof String rules && GuardItemRules.valid(rules));
        EXCLUDED_ITEMS = server.comment("Comma-separated item IDs or #item tags that can never guard. Exclusions take priority over inclusions.").define("excludedItems", "", value -> value instanceof String rules && GuardItemRules.valid(rules));
        server.pop();
        SERVER_SPEC = server.build();

        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        client.push("display");
        HUD = client.comment("Show the parry shield by the crosshair.").define("hud", true);
        SHIELD_EFFECTS = client.comment("Flash, shake, and briefly grow the shield after a parry or block.").define("shieldEffects", true);
        SCREEN_FLASH = client.comment("Brief white screen flash after regular and perfect parries. Blocks do not flash. Client-side only.").define("screenFlash", true);
        FLASH_STRENGTH = client.comment("Strength of the brief screen flash, as a percentage. 0 disables it.").defineInRange("flashStrength", 50, 0, 100);
        SCREEN_SHAKE = client.comment("Briefly shake the camera on regular parries, perfect parries, and blocks. Blocks shake most strongly. Client-side only.").define("screenShake", true);
        SHAKE_STRENGTH = client.comment("Camera shake strength as a percentage. 0 disables it; blocks shake most strongly.").defineInRange("shakeStrength", 60, 0, 100);
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
        FOLLOW_UP_TICKS.set(data.followUp());
        PARRY_RETALIATION.set(data.parryReturnPercent() / 100.0D);
        PERFECT_RETALIATION.set(data.perfectReturnPercent() / 100.0D);
        PARRY_WEAR.set(data.parryWear());
        PERFECT_WEAR.set(data.perfectWear());
        BLOCK_WEAR.set(data.blockWear());
        ADDED_DURABILITY.set(data.addedDurability());
        HIT_SOUNDS.set(data.hitSounds());
        HIT_PARTICLES.set(data.hitParticles());
        MASTER_VOLUME.set(data.masterVolume());
        PERFECT_VOLUME.set(data.perfectVolume());
        PARRY_VOLUME.set(data.parryVolume());
        BLOCK_VOLUME.set(data.blockVolume());
        FALL_PARRY.set(data.fallParry());
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
        ALLOW_USABLE_ITEMS.set(data.allowUsableItems());
        INCLUDED_ITEMS.set(data.includedItems());
        EXCLUDED_ITEMS.set(data.excludedItems());
        SERVER_SPEC.save();
    }

    public static GuardPackets.Settings snapshot(boolean operator) {
        return new GuardPackets.Settings(PARRY.get(), BLOCK.get(), PERFECT_TICKS.get(), PARRY_TICKS.get(), RECHARGE_TICKS.get(), FACING_ANGLE.get(), (int) Math.round(BLOCK_REDUCTION.get() * 100.0D), FOLLOW_UP_TICKS.get(), (int) Math.round(PARRY_RETALIATION.get() * 100), (int) Math.round(PERFECT_RETALIATION.get() * 100), PARRY_WEAR.get(), PERFECT_WEAR.get(), BLOCK_WEAR.get(), ADDED_DURABILITY.get(), HIT_SOUNDS.get(), HIT_PARTICLES.get(), MASTER_VOLUME.get(), PERFECT_VOLUME.get(), PARRY_VOLUME.get(), BLOCK_VOLUME.get(), FALL_PARRY.get(), FALL_BREAK_BLOCKS.get(), FALL_BLAST_STRENGTH.get(), FALL_LAUNCH_POWER.get(), PARRY_EXPLOSIONS.get(), PERFECT_EXPLOSIONS_ONLY.get(), BLOCK_EXPLOSIONS.get(), PARRY_PROJECTILES.get(), BLOCK_PROJECTILES.get(), DEFENDER_KNOCKBACK.get(), KNOCKBACK_STRENGTH.get(), ALLOW_USABLE_ITEMS.get(), INCLUDED_ITEMS.get(), EXCLUDED_ITEMS.get(), operator);
    }
}
