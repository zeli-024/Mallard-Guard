package dev.zeli.mallardguard;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import java.util.Collection;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class GuardConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec.BooleanValue PARRY;
    public static final ModConfigSpec.BooleanValue PARRY_DROWNING_FIRE;
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
    public static final ModConfigSpec.IntValue GUARD_MOVEMENT_PERCENT;
    public static final ModConfigSpec.IntValue BLOCK_DEFLECT_CHANCE;
    public static final ModConfigSpec.BooleanValue ALLOW_USABLE_ITEMS;
    public static final ModConfigSpec.ConfigValue<String> INCLUDED_ITEMS;
    public static final ModConfigSpec.ConfigValue<String> EXCLUDED_ITEMS;
    public static final ModConfigSpec.ConfigValue<String> SHIELD_ITEMS;
    public static final ModConfigSpec.BooleanValue HUD;
    public static final ModConfigSpec.BooleanValue SHIELD_EFFECTS;
    public static final ModConfigSpec.BooleanValue SCREEN_FLASH;
    public static final ModConfigSpec.IntValue FLASH_STRENGTH;
    public static final ModConfigSpec.BooleanValue SCREEN_SHAKE;
    public static final ModConfigSpec.IntValue SHAKE_STRENGTH;
    public static final ModConfigSpec.BooleanValue PREFER_OFFHAND;
    public static final ModConfigSpec.IntValue REGULAR_ORB_SIZE;
    public static final ModConfigSpec.IntValue PERFECT_ORB_SIZE;
    public static final ModConfigSpec.IntValue REGULAR_ORB_OPACITY;
    public static final ModConfigSpec.IntValue PERFECT_ORB_OPACITY;
    private static final ModConfigSpec.ConfigValue<String> SERVER_VERSION;
    private static final ModConfigSpec.ConfigValue<String> CLIENT_VERSION;
    private static String currentVersion;

    static {
        ModConfigSpec.Builder server = new ModConfigSpec.Builder();
        SERVER_VERSION = server.comment("Mallard Guard version for automatic setting reset. Changing this resets this file to its defaults.").define("configVersion", "unversioned");
        server.push("stance");
        PARRY = server.comment("Allow parries when the guard key is pressed. Right Click by default; the key can be changed.").define("parry", true);
        PARRY_DROWNING_FIRE = server.comment("Allow parrying drowning and fire damage, including lava. Other damage types remain eligible when this is disabled.").define("parryDrowningAndFire", true);
        BLOCK = server.comment("Keep guarding after the parry window while the guard key is held.").define("block", true);
        server.pop();
        server.push("timing");
        PERFECT_TICKS = server.comment("Perfect parry duration; zero disables perfect parries.").defineInRange("perfectTicks", 3, 0, 5);
        PARRY_TICKS = server.comment("Total parry duration, including perfect ticks.").defineInRange("parryTicks", 10, 1, 10);
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
        ADDED_DURABILITY = server.comment("Maximum durability granted to eligible damage items that have none.").defineInRange("addedDurability", 600, 10, 1000);
        server.pop();
        server.push("effects");
        HIT_SOUNDS = server.comment("Play the supplied parry and block sounds to nearby players.").define("hitSounds", true);
        HIT_PARTICLES = server.comment("Show sparks and flashes on parries; perfect parries also show star-shaped sparks. Blocking creates none.").define("hitParticles", true);
        MASTER_VOLUME = server.comment("Master volume for guard hit sounds, percent; 0 mutes them.").defineInRange("masterVolume", 130, 0, 200);
        PERFECT_VOLUME = server.comment("Perfect parry sound volume, multiplied by master volume.").defineInRange("perfectVolume", 100, 0, 200);
        PARRY_VOLUME = server.comment("Regular parry sound volume, multiplied by master volume.").defineInRange("parryVolume", 100, 0, 200);
        BLOCK_VOLUME = server.comment("Held block sound volume, multiplied by master volume.").defineInRange("blockVolume", 100, 0, 200);
        server.pop();
        server.push("sources");
        FALL_PARRY = server.comment("Parry fall damage while looking at least 40 degrees below the horizon and launch forward. Held blocking does not stop falls.").define("fallParry", true);
        FALL_BREAK_BLOCKS = server.comment("Allow the blast from a long fall parry to break terrain. Disable to prevent terrain damage.").define("fallBlastBreaksBlocks", true);
        FALL_BLAST_STRENGTH = server.comment("Fall parry blast scaling as a percentage of Parry It's default 0.1 radius per damage; 0 disables the blast.").defineInRange("fallBlastStrength", 100, 0, 500);
        FALL_LAUNCH_POWER = server.comment("Fall parry launch strength as a percentage of Parry It's default. Perfect falls launch twice as far. 0 disables launch.").defineInRange("fallLaunchPower", 50, 0, 300);
        PARRY_EXPLOSIONS = server.comment("Allow parry timing to negate explosion damage.").define("parryExplosions", true);
        PERFECT_EXPLOSIONS_ONLY = server.comment("When explosion parrying is enabled, require a perfect parry; regular parries do not negate explosions.").define("perfectExplosionsOnly", false);
        BLOCK_EXPLOSIONS = server.comment("Allow held block to reduce explosion damage by the normal block reduction.").define("blockExplosions", true);
        PARRY_PROJECTILES = server.comment("Parry projectile entities. Regular parries deflect them randomly; perfect parries send them toward the original owner.").define("parryProjectiles", true);
        BLOCK_PROJECTILES = server.comment("Allow held block to reduce projectile damage by the normal block reduction.").define("blockProjectiles", true);
        DEFENDER_KNOCKBACK = server.comment("Push the defender away from the impact on successful melee, projectile, and explosion parries.").define("defenderKnockback", true);
        KNOCKBACK_STRENGTH = server.comment("Defender push strength in percent; 0 disables it.").defineInRange("knockbackStrength", 150, 0, 200);
        GUARD_MOVEMENT_PERCENT = server.comment("Movement speed during all guard phases, percentage of normal speed. Guard slowdown does not change camera FOV. 100 removes the slowdown.").defineInRange("guardMovementPercent", 55, 0, 100);
        BLOCK_DEFLECT_CHANCE = server.comment("Chance to deflect a projectile during held block. 0 disables deflection; every projectile type uses the same roll.").defineInRange("blockDeflectChance", 0, 0, 100);
        server.pop();
        server.push("items");
        ALLOW_USABLE_ITEMS = server.comment("Allow eligible items with their own hold-to-use action to start a guard. Enabled by default for compatibility. Explicit inclusions override this switch.").define("allowUsableItems", true);
        INCLUDED_ITEMS = server.comment("Comma-separated item IDs or #item tags that can guard even without an attack-damage modifier. Exclusions always take priority.").define("includedItems", "", value -> value instanceof String rules && GuardItemRules.valid(rules));
        EXCLUDED_ITEMS = server.comment("Comma-separated item IDs or #item tags that can never guard. Exclusions take priority over inclusions.").define("excludedItems", "", value -> value instanceof String rules && GuardItemRules.valid(rules));
        SHIELD_ITEMS = server.comment("Extra item IDs or #tags treated as shields. Shields and items with a blocking use animation are detected automatically. After the parry window, these items use their own blocking mechanics. Excluded items take priority.").define("shieldItems", "", value -> value instanceof String rules && GuardItemRules.valid(rules));
        server.pop();
        SERVER_SPEC = server.build();

        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        CLIENT_VERSION = client.comment("Mallard Guard version for automatic setting reset. Changing this resets this file to its defaults.").define("configVersion", "unversioned");
        client.push("display");
        HUD = client.comment("Show the parry shield by the crosshair.").define("hud", true);
        SHIELD_EFFECTS = client.comment("Animate the shield: a ready outline and parry flashes. Blocking hits flash red; afterward the darker shield trembles with repeating small echoes while held.").define("shieldEffects", true);
        SCREEN_FLASH = client.comment("Brief white screen flash after regular and perfect parries. Off by default; blocks never flash. Client-side only.").define("screenFlash", false);
        FLASH_STRENGTH = client.comment("Strength of the brief screen flash, as a percentage. 0 disables it.").defineInRange("flashStrength", 0, 0, 100);
        SCREEN_SHAKE = client.comment("Briefly shake the camera on regular parries, perfect parries, and blocks. Blocks shake most strongly. Client-side only.").define("screenShake", true);
        SHAKE_STRENGTH = client.comment("Camera shake strength as a percentage. 0 disables it; blocks shake most strongly.").defineInRange("shakeStrength", 60, 0, 100);
        PREFER_OFFHAND = client.comment("When both hands hold eligible weapons, guard with the offhand. When only one hand is eligible, guard with that hand.").define("preferOffhand", false);
        REGULAR_ORB_SIZE = client.comment("Size of the regular parry flash. 0 hides this flash without hiding sparks.").defineInRange("regularOrbSize", 25, 0, 150);
        PERFECT_ORB_SIZE = client.comment("Size of the perfect parry flash. 0 hides this flash without hiding sparks.").defineInRange("perfectOrbSize", 40, 0, 150);
        REGULAR_ORB_OPACITY = client.comment("Opacity of the regular parry flash. 0 hides this flash.").defineInRange("regularOrbOpacity", 70, 0, 100);
        PERFECT_ORB_OPACITY = client.comment("Opacity of the perfect parry flash. 0 hides this flash.").defineInRange("perfectOrbOpacity", 70, 0, 100);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    private GuardConfig() {}

    public static void setCurrentVersion(String version) {
        currentVersion = version;
    }

    public static void onLoad(ModConfigEvent.Loading event) {
        ModConfigSpec spec;
        ModConfigSpec.ConfigValue<String> marker;
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            spec = SERVER_SPEC;
            marker = SERVER_VERSION;
        } else if (event.getConfig().getSpec() == CLIENT_SPEC) {
            spec = CLIENT_SPEC;
            marker = CLIENT_VERSION;
        } else return;
        if (currentVersion == null || currentVersion.equals(marker.get())) return;
        resetValues(spec.getValues().valueMap().values());
        marker.set(currentVersion);
        spec.save();
    }

    private static void resetValues(Collection<?> entries) {
        for (Object entry : entries) {
            if (entry instanceof UnmodifiableConfig group) resetValues(group.valueMap().values());
            else if (entry instanceof ModConfigSpec.ConfigValue<?> value) resetValue(value);
        }
    }

    private static <T> void resetValue(ModConfigSpec.ConfigValue<T> value) {
        value.set(value.getDefault());
    }

    public static void apply(GuardPackets.Settings data) {
        PARRY.set(data.parry());
        PARRY_DROWNING_FIRE.set(data.parryDrowningFire());
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
        GUARD_MOVEMENT_PERCENT.set(data.guardMovementPercent());
        BLOCK_DEFLECT_CHANCE.set(data.blockDeflectChance());
        ALLOW_USABLE_ITEMS.set(data.allowUsableItems());
        INCLUDED_ITEMS.set(data.includedItems());
        EXCLUDED_ITEMS.set(data.excludedItems());
        SHIELD_ITEMS.set(data.shieldItems());
        SERVER_SPEC.save();
    }

    public static GuardPackets.Settings snapshot(boolean operator) {
        return new GuardPackets.Settings(PARRY.get(), BLOCK.get(), PARRY_DROWNING_FIRE.get(), PERFECT_TICKS.get(), PARRY_TICKS.get(), RECHARGE_TICKS.get(), FACING_ANGLE.get(), (int) Math.round(BLOCK_REDUCTION.get() * 100.0D), FOLLOW_UP_TICKS.get(), (int) Math.round(PARRY_RETALIATION.get() * 100), (int) Math.round(PERFECT_RETALIATION.get() * 100), PARRY_WEAR.get(), PERFECT_WEAR.get(), BLOCK_WEAR.get(), ADDED_DURABILITY.get(), HIT_SOUNDS.get(), HIT_PARTICLES.get(), MASTER_VOLUME.get(), PERFECT_VOLUME.get(), PARRY_VOLUME.get(), BLOCK_VOLUME.get(), FALL_PARRY.get(), FALL_BREAK_BLOCKS.get(), FALL_BLAST_STRENGTH.get(), FALL_LAUNCH_POWER.get(), PARRY_EXPLOSIONS.get(), PERFECT_EXPLOSIONS_ONLY.get(), BLOCK_EXPLOSIONS.get(), PARRY_PROJECTILES.get(), BLOCK_PROJECTILES.get(), DEFENDER_KNOCKBACK.get(), KNOCKBACK_STRENGTH.get(), GUARD_MOVEMENT_PERCENT.get(), BLOCK_DEFLECT_CHANCE.get(), ALLOW_USABLE_ITEMS.get(), INCLUDED_ITEMS.get(), EXCLUDED_ITEMS.get(), SHIELD_ITEMS.get(), operator);
    }

    public static GuardPackets.Settings defaultSnapshot(boolean operator) {
        return new GuardPackets.Settings(PARRY.getDefault(), BLOCK.getDefault(), PARRY_DROWNING_FIRE.getDefault(), PERFECT_TICKS.getDefault(), PARRY_TICKS.getDefault(), RECHARGE_TICKS.getDefault(), FACING_ANGLE.getDefault(), (int) Math.round(BLOCK_REDUCTION.getDefault() * 100.0D), FOLLOW_UP_TICKS.getDefault(), (int) Math.round(PARRY_RETALIATION.getDefault() * 100), (int) Math.round(PERFECT_RETALIATION.getDefault() * 100), PARRY_WEAR.getDefault(), PERFECT_WEAR.getDefault(), BLOCK_WEAR.getDefault(), ADDED_DURABILITY.getDefault(), HIT_SOUNDS.getDefault(), HIT_PARTICLES.getDefault(), MASTER_VOLUME.getDefault(), PERFECT_VOLUME.getDefault(), PARRY_VOLUME.getDefault(), BLOCK_VOLUME.getDefault(), FALL_PARRY.getDefault(), FALL_BREAK_BLOCKS.getDefault(), FALL_BLAST_STRENGTH.getDefault(), FALL_LAUNCH_POWER.getDefault(), PARRY_EXPLOSIONS.getDefault(), PERFECT_EXPLOSIONS_ONLY.getDefault(), BLOCK_EXPLOSIONS.getDefault(), PARRY_PROJECTILES.getDefault(), BLOCK_PROJECTILES.getDefault(), DEFENDER_KNOCKBACK.getDefault(), KNOCKBACK_STRENGTH.getDefault(), GUARD_MOVEMENT_PERCENT.getDefault(), BLOCK_DEFLECT_CHANCE.getDefault(), ALLOW_USABLE_ITEMS.getDefault(), INCLUDED_ITEMS.getDefault(), EXCLUDED_ITEMS.getDefault(), SHIELD_ITEMS.getDefault(), operator);
    }
}
