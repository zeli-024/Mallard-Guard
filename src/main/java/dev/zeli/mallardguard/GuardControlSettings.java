package dev.zeli.mallardguard;

import net.neoforged.neoforge.common.ModConfigSpec;

/** UI labels bind to the same values used by config reset and packet validation. */
public final class GuardControlSettings {
    public record Binding(ModConfigSpec.ConfigValue<?> setting, int scale) {
        public int minimum() {
            var range = GuardSettingRanges.range(setting);
            if (range != null) return (int)Math.round(range.min() * scale);
            int slot = GuardClientSettings.indexOf(setting);
            if (slot >= 0) return GuardClientSettings.minimum(slot);
            throw new IllegalArgumentException("No numeric bounds for config control");
        }
        public int maximum() {
            var range = GuardSettingRanges.range(setting);
            if (range != null) return (int)Math.round(range.max() * scale);
            int slot = GuardClientSettings.indexOf(setting);
            if (slot >= 0) return GuardClientSettings.maximum(slot);
            throw new IllegalArgumentException("No numeric bounds for config control");
        }
        public Object defaultValue() {
            Object value = setting.getDefault();
            return value instanceof Double number ? (int)Math.round(number * scale) : value;
        }
    }
    private GuardControlSettings() {}
    public static Binding binding(String name, boolean server) {
        return switch (name) {
            case "Release delay" -> new Binding(GuardConfig.PUNCHY_RELEASE_DELAY, 1);
            case "Perfect window" -> new Binding(GuardConfig.PERFECT_TICKS, 1);
            case "Parry window" -> new Binding(GuardConfig.PARRY_TICKS, 1);
            case "Recharge" -> new Binding(GuardConfig.RECHARGE_TICKS, 1);
            case "Facing angle" -> new Binding(GuardConfig.FACING_ANGLE, 1);
            case "Follow-up parries" -> new Binding(GuardConfig.FOLLOW_UP_TICKS, 1);
            case "Parry retaliation" -> new Binding(GuardConfig.PARRY_RETALIATION, 100);
            case "Perfect retaliation" -> new Binding(GuardConfig.PERFECT_RETALIATION, 100);
            case "Retaliation damage cap" -> new Binding(GuardConfig.RETALIATION_CAP, 1);
            case "Defender pushback" -> new Binding(GuardConfig.KNOCKBACK_STRENGTH, 1);
            case "Shield attacker pushback" -> new Binding(GuardConfig.SHIELD_PARRY_PUSHBACK_PERCENT, 1);
            case "Weapon attacker pushback" -> new Binding(GuardConfig.TOOL_PUSHBACK_PERCENT, 1);
            case "Projectile parry" -> new Binding(GuardConfig.PARRY_PROJECTILES, 1);
            case "Explosion parry" -> new Binding(GuardConfig.PARRY_EXPLOSIONS, 1);
            case "Explosions: perfect only" -> new Binding(GuardConfig.PERFECT_EXPLOSIONS_ONLY, 1);
            case "Mob guarding" -> new Binding(GuardConfig.MOB_GUARD, 1);
            case "Mob guard difficulty" -> new Binding(GuardConfig.MOB_DIFFICULTY, 1);
            case "Mob blocking" -> new Binding(GuardConfig.MOB_BLOCKING, 1);
            case "Mob retaliation" -> new Binding(GuardConfig.MOB_RETALIATION, 1);
            case "Hits before guard" -> new Binding(GuardConfig.MOB_HITS_TO_GUARD, 1);
            case "Counter approach duration" -> new Binding(GuardConfig.MOB_COUNTER_TICKS, 1);
            case "Approach guard chance" -> new Binding(GuardConfig.MOB_APPROACH_CHANCE, 1);
            case "Tactical guard chance" -> new Binding(GuardConfig.MOB_TACTICAL_CHANCE, 1);
            case "Tactical stance maximum" -> new Binding(GuardConfig.MOB_TACTICAL_SECONDS, 1);
            case "Tactical guard cooldown" -> new Binding(GuardConfig.MOB_TACTICAL_COOLDOWN, 1);
            case "Mob guard movement" -> new Binding(GuardConfig.MOB_MOVEMENT, 1);
            case "Approach guard distance" -> new Binding(GuardConfig.MOB_APPROACH_DISTANCE, 1);
            case "Approach stance maximum" -> new Binding(GuardConfig.MOB_APPROACH_SECONDS, 1);
            case "Rush after parry chance" -> new Binding(GuardConfig.MOB_RUSH_CHANCE, 1);
            case "Mob perfect parry window" -> new Binding(GuardConfig.MOB_PERFECT_TICKS, 1);
            case "Mob regular parry window" -> new Binding(GuardConfig.MOB_PARRY_TICKS, 1);
            case "Mob gear chance" -> new Binding(GuardConfig.MOB_GEAR_CHANCE, 1);
            case "Shield cone sparks" -> new Binding(GuardConfig.SHIELD_SPARK_CONE, 1);
            case "Shield perfect window" -> new Binding(GuardConfig.SHIELD_PERFECT_TICKS, 1);
            case "Shield parry window" -> new Binding(GuardConfig.SHIELD_PARRY_TICKS, 1);
            case "Shield parry recharge" -> new Binding(GuardConfig.SHIELD_RECHARGE_TICKS, 1);
            case "Shield blocks before break" -> new Binding(GuardConfig.SHIELD_MAX_BLOCKS, 1);
            case "Cone half-angle" -> new Binding(GuardConfig.SHIELD_CONE_DEGREES, 1);
            case "Cone reach" -> new Binding(GuardConfig.SHIELD_CONE_REACH, 1);
            case "Shield retaliation" -> new Binding(GuardConfig.SHIELD_RETALIATION_PERCENT, 1);
            case "Stun duration" -> new Binding(GuardConfig.SHIELD_STUN_TICKS, 1);
            case "Cone pushback" -> new Binding(GuardConfig.SHIELD_PUSHBACK_PERCENT, 1);
            case "Perfect fall parry" -> new Binding(GuardConfig.FALL_PERFECT_PARRY, 1);
            case "Fall blast" -> new Binding(GuardConfig.FALL_BLAST_STRENGTH, 1);
            case "Fall launch" -> new Binding(GuardConfig.FALL_LAUNCH_POWER, 1);
            case "Fall blast breaks blocks" -> new Binding(GuardConfig.FALL_BREAK_BLOCKS, 1);
            case "Look down to parry" -> new Binding(GuardConfig.FALL_LOOK_DOWN, 1);
            case "Blocking" -> new Binding(GuardConfig.BLOCK, 1);
            case "Damage reduction" -> new Binding(GuardConfig.BLOCK_REDUCTION, 100);
            case "Weapon blocks before break" -> new Binding(GuardConfig.TOOL_MAX_BLOCKS, 1);
            case "Guard break recharge" -> new Binding(GuardConfig.SHIELD_BREAK_TICKS, 1);
            case "Guard movement" -> new Binding(GuardConfig.GUARD_MOVEMENT_PERCENT, 1);
            case "Block deflect chance" -> new Binding(GuardConfig.BLOCK_DEFLECT_CHANCE, 1);
            case "Block projectiles" -> new Binding(GuardConfig.BLOCK_PROJECTILES, 1);
            case "Block explosions" -> new Binding(GuardConfig.BLOCK_EXPLOSIONS, 1);
            case "Sparks and flashes" -> new Binding(GuardConfig.HIT_PARTICLES, 1);
            case "Hit sounds" -> new Binding(GuardConfig.HIT_SOUNDS, 1);
            case "Empty Hand" -> new Binding(GuardConfig.ALLOW_EMPTY_HAND, 1);
            case "Respect Item Cooldown" -> new Binding(GuardConfig.COOLDOWN_PREVENTS_GUARD, 1);
            case "Consumables Take Priority" -> new Binding(GuardConfig.CONSUMABLE_PRIORITY, 1);
            case "Force Crouch Off-Hand" -> new Binding(GuardConfig.FORCE_CROUCH_OFFHAND, 1);
            case "Parry durability" -> new Binding(GuardConfig.PARRY_WEAR, 1);
            case "Perfect durability" -> new Binding(GuardConfig.PERFECT_WEAR, 1);
            case "Block durability" -> new Binding(GuardConfig.BLOCK_WEAR, 1);
            case "Ring sparks" -> new Binding(GuardConfig.SPARK_RING, 1);
            case "Parry sparks" -> new Binding(GuardConfig.REGULAR_SPARK_COUNT, 1);
            case "Perfect sparks" -> new Binding(GuardConfig.PERFECT_SPARK_COUNT, 1);
            case "Perfect stars" -> new Binding(GuardConfig.STAR_COUNT, 1);
            case "Spark lifetime" -> new Binding(GuardConfig.SPARK_LIFETIME, 1);
            case "Spark length" -> new Binding(GuardConfig.SPARK_LENGTH, 1);
            case "Spark speed" -> new Binding(GuardConfig.SPARK_EXPLOSIVENESS, 1);
            case "Spark reach" -> new Binding(GuardConfig.SPARK_REACH, 1);
            case "Reverse streak gradient" -> new Binding(GuardConfig.STREAK_REVERSE, 1);
            case "Regular streak count" -> new Binding(GuardConfig.REGULAR_STREAK_AMOUNT, 1);
            case "Perfect streak count" -> new Binding(GuardConfig.STREAK_AMOUNT, 1);
            case "Streak length" -> new Binding(GuardConfig.STREAK_LENGTH, 1);
            case "Streak lifetime" -> new Binding(GuardConfig.STREAK_LIFETIME, 1);
            case "Streak dot size" -> new Binding(GuardConfig.STREAK_DOT_SIZE, 1);
            case "Regular dot spacing" -> new Binding(GuardConfig.REGULAR_STREAK_DOT_SPACING, 1);
            case "Perfect dot spacing" -> new Binding(GuardConfig.STREAK_DOT_SPACING, 1);
            case "Streak ballistic curve" -> new Binding(GuardConfig.STREAK_BALLISTIC_CURVE, 1);
            case "Streak stretch speed" -> new Binding(GuardConfig.STREAK_STRETCH_SPEED, 1);
            case "Parry flash size" -> new Binding(GuardConfig.REGULAR_ORB_SIZE, 1);
            case "Perfect flash size" -> new Binding(GuardConfig.PERFECT_ORB_SIZE, 1);
            case "Parry flash opacity" -> new Binding(GuardConfig.REGULAR_ORB_OPACITY, 1);
            case "Perfect flash opacity" -> new Binding(GuardConfig.PERFECT_ORB_OPACITY, 1);
            case "Parry sphere lifetime" -> new Binding(GuardConfig.REGULAR_ORB_LIFETIME, 1);
            case "Perfect sphere lifetime" -> new Binding(GuardConfig.PERFECT_ORB_LIFETIME, 1);
            case "Shield reactions" -> new Binding(GuardConfig.SHIELD_EFFECTS, 1);
            case "Screen flash" -> new Binding(GuardConfig.FLASH_STRENGTH, 1);
            case "Perfect only flash" -> new Binding(GuardConfig.PERFECT_ONLY_FLASH, 1);
            case "Meme flash" -> new Binding(GuardConfig.MEME_FLASH, 1);
            case "Camera shake" -> new Binding(GuardConfig.SHAKE_STRENGTH, 1);
            case "Hitlag frames" -> new Binding(GuardConfig.HITLAG_FRAMES, 1);
            case "Perfect only hitlag" -> new Binding(GuardConfig.PERFECT_ONLY_HITLAG, 1);
            case "Impact frame" -> new Binding(GuardConfig.IMPACT_FRAMES, 1);
            case "Perfect only impact" -> new Binding(GuardConfig.IMPACT_PERFECT_ONLY, 1);
            case "Third-person guard pose" -> new Binding(GuardConfig.THIRD_PERSON_ANIMATION, 1);
            case "First-person guard pose" -> new Binding(GuardConfig.FIRST_PERSON_ANIMATION, 1);
            case "Master volume" -> new Binding(server ? GuardConfig.MASTER_VOLUME : GuardConfig.LOCAL_MASTER_VOLUME, 1);
            case "Perfect volume" -> new Binding(server ? GuardConfig.PERFECT_VOLUME : GuardConfig.LOCAL_PERFECT_VOLUME, 1);
            case "Parry volume" -> new Binding(server ? GuardConfig.PARRY_VOLUME : GuardConfig.LOCAL_PARRY_VOLUME, 1);
            case "Block volume" -> new Binding(server ? GuardConfig.BLOCK_VOLUME : GuardConfig.LOCAL_BLOCK_VOLUME, 1);
            case "Chromatic intensity" -> new Binding(GuardConfig.CHROMATIC_INTENSITY, 1);
            case "Chromatic duration" -> new Binding(GuardConfig.CHROMATIC_TICKS, 1);
            case "Perfect only chromatic" -> new Binding(GuardConfig.CHROMATIC_PERFECT_ONLY, 1);
            case "Chromatic affects HUD" -> new Binding(GuardConfig.CHROMATIC_HUD, 1);
            case "Mob guard blacklist" -> new Binding(GuardConfig.MOB_BLACKLIST, 1);
            case "Any held item" -> new Binding(GuardConfig.ALLOW_ANY_ITEM, 1);
            case "Parry Hand Priority" -> new Binding(GuardConfig.PARRY_HAND_PRIORITY, 1);
            case "Shield Parry Priority" -> new Binding(GuardConfig.SHIELD_PARRY_PRIORITY, 1);
            case "Item Whitelist" -> new Binding(GuardConfig.INCLUDED_ITEMS, 1);
            case "Item Blacklist" -> new Binding(GuardConfig.EXCLUDED_ITEMS, 1);
            case "Shield Item Whitelist" -> new Binding(GuardConfig.SHIELD_ITEMS, 1);
            case "Shield Item Blacklist" -> new Binding(GuardConfig.SHIELD_BLACKLIST, 1);
            case "Stunnable bosses" -> new Binding(GuardConfig.SHIELD_STUN_BOSSES, 1);
            case "Additional humanoid mob IDs" -> new Binding(GuardConfig.MOB_HUMANOID_IDS, 1);
            default -> null;
        };
    }
}
