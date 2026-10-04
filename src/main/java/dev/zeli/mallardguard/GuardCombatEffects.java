package dev.zeli.mallardguard;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

public final class GuardCombatEffects {
    private static final Set<UUID> FALL_BLASTS = new HashSet<>();
    private static final Map<LivingEntity, Integer> STUNNED = new java.util.WeakHashMap<>();
    private static final ResourceLocation STUN_SPEED = ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "shield_stun");

    public static void clearSession(){for(var entity:STUNNED.keySet()){var speed=entity.getAttribute(Attributes.MOVEMENT_SPEED);if(speed!=null)speed.removeModifier(STUN_SPEED);}STUNNED.clear();FALL_BLASTS.clear();}

    private GuardCombatEffects() {}

    public static boolean isOwnFallBlast(ServerPlayer player, DamageSource source) {
        return FALL_BLASTS.contains(player.getUUID()) && source.is(DamageTypeTags.IS_EXPLOSION);
    }

    public static void fall(ServerPlayer player, float damage, boolean perfect) {
        ServerLevel level = player.serverLevel();
        float distance = player.fallDistance > 0 ? player.fallDistance : damage + 3.0F;
        if (distance > 10.0F && GuardConfig.FALL_BLAST_STRENGTH.get() > 0) {
            float radius = Math.min(8.0F, 0.2F + damage * 0.1F * GuardConfig.FALL_BLAST_STRENGTH.get() / 100.0F);
            FALL_BLASTS.add(player.getUUID());
            try {
                level.explode(player, player.getX(), player.getY(), player.getZ(), radius, false,
                    GuardConfig.FALL_BREAK_BLOCKS.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
            } finally {
                FALL_BLASTS.remove(player.getUUID());
            }
        }
        double power = GuardConfig.FALL_LAUNCH_POWER.get() / 100.0D * (perfect ? 2.0D : 1.0D);
        player.fallDistance = 0;
        if (power == 0) return;
        if (!GuardConfig.FALL_LOOK_DOWN.get()) {
            // The unrestricted option retains the original view-directed launch.
            Vec3 look = player.getLookAngle();
            double facing = Math.hypot(look.x, look.z);
            double boost = (1.0D + damage * 0.25D) * power;
            player.setDeltaMovement(facing > 0.01D ? look.x / facing * boost : 0.0D,
                (0.6D + Math.max(0.0D, look.y) * 0.6D) * power,
                facing > 0.01D ? look.z / facing * boost : 0.0D);
            player.hurtMarked = true;
            return;
        }
        // Preserve the player's actual horizontal momentum at impact. Looking
        // down is still required, but looking direction must not create a lunge.
        Vec3 motion = player.getDeltaMovement();
        double horizontal = Math.hypot(motion.x, motion.z);
        double boost = (1.0D + damage * 0.25D) * power;
        double carried = horizontal < 0.02D ? 0.0D : Math.min(boost, horizontal * (1.0D + power));
        player.setDeltaMovement(horizontal < 0.02D ? 0.0D : motion.x / horizontal * carried,
            0.6D * power,
            horizontal < 0.02D ? 0.0D : motion.z / horizontal * carried);
        player.hurtMarked = true;
    }

    public static void pushDefender(LivingEntity player, Vec3 origin, boolean shield) {
        if (!GuardConfig.DEFENDER_KNOCKBACK.get() || GuardConfig.KNOCKBACK_STRENGTH.get() == 0) return;
        Vec3 away = origin == null ? player.getLookAngle().scale(-1.0D) : player.position().subtract(origin);
        away = new Vec3(away.x, 0, away.z);
        if (away.lengthSqr() < 1.0E-6D) away = new Vec3(-player.getLookAngle().x, 0, -player.getLookAngle().z);
        if (away.lengthSqr() < 1.0E-6D) return;
        double strength = GuardConfig.KNOCKBACK_STRENGTH.get() / 100.0D * (shield ? 0.5D : 1.0D);
        player.setDeltaMovement(player.getDeltaMovement().add(away.normalize().scale(strength)).add(0, strength * 0.25D, 0));
        player.hurtMarked = true;
    }

    public static void pushAttacker(LivingEntity player, LivingEntity attacker, boolean shield) {
        if (player.distanceToSqr(attacker) > 16.0D) return;
        int percent = shield ? GuardConfig.SHIELD_PARRY_PUSHBACK_PERCENT.get() : GuardConfig.TOOL_PUSHBACK_PERCENT.get();
        if (percent == 0) return;
        pushLikeHit(player, attacker, percent);
    }

    public static void shieldPerfect(ServerPlayer player, float incomingDamage) {
        int cone = GuardConfig.SHIELD_CONE_DEGREES.get();
        if (cone == 0) return;
        double reach = GuardConfig.SHIELD_CONE_REACH.get();
        Vec3 eye = player.getEyePosition();
        double minDot = Math.cos(Math.toRadians(cone));
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
            player.getBoundingBox().inflate(reach), entity -> entity != player && entity.isAlive())) {
            Vec3 toTarget = target.getEyePosition().subtract(eye);
            if (toTarget.lengthSqr() < 1.0E-6D || toTarget.lengthSqr() > reach * reach ||
                player.getLookAngle().dot(toTarget.normalize()) < minDot) continue;
            pushLikeHit(player, target, GuardConfig.SHIELD_PUSHBACK_PERCENT.get());
            if (GuardConfig.SHIELD_STUN_TICKS.get() > 0 && (!boss(target) || allowedBoss(target)))
                STUNNED.put(target, GuardConfig.SHIELD_STUN_TICKS.get());
            float damage = incomingDamage * GuardConfig.SHIELD_RETALIATION_PERCENT.get() / 100.0F;
            if (damage > 0) GuardRetaliation.damage(player, target, damage);
        }
    }

    private static void pushLikeHit(LivingEntity defender, LivingEntity target, int percent) {
        if (percent <= 0) return;
        double dx = defender.getX() - target.getX();
        double dz = defender.getZ() - target.getZ();
        if (dx * dx + dz * dz < 1.0E-6D) {
            Vec3 look = defender.getLookAngle();
            dx = -look.x; dz = -look.z;
        }
        target.knockback(percent / 200.0D, dx, dz);
    }

    private static boolean boss(LivingEntity target) {
        return target instanceof WitherBoss || target instanceof EnderDragon
            || target.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses")))
            || target.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("forge", "bosses")));
    }

    private static boolean allowedBoss(LivingEntity target) {
        String rules = GuardConfig.SHIELD_STUN_BOSSES.get();
        for (String entry : rules.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;
            ResourceLocation id = ResourceLocation.tryParse(trimmed.startsWith("#") ? trimmed.substring(1) : trimmed);
            if (id == null) continue;
            if (trimmed.startsWith("#") && target.getType().is(TagKey.create(Registries.ENTITY_TYPE, id))
                || !trimmed.startsWith("#") && id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()))) return true;
        }
        return false;
    }

    public static void leave(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity entity) {
            STUNNED.remove(entity);
            AttributeInstance movement = entity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movement != null) movement.removeModifier(STUN_SPEED);
        }
    }

    public static boolean stunned(Entity entity) {
        return entity instanceof LivingEntity living && STUNNED.containsKey(living);
    }

    public static void tickStun(EntityTickEvent.Post event) {
        if (STUNNED.isEmpty()) return;
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()) return;
        Integer ticks = STUNNED.get(entity);
        if (ticks == null) return;
        AttributeInstance movement = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null && movement.getModifier(STUN_SPEED) == null)
            movement.addTransientModifier(new AttributeModifier(STUN_SPEED, -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if (ticks < GuardConfig.SHIELD_STUN_TICKS.get()) {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(0.0D, Math.min(0.0D, motion.y), 0.0D);
            entity.hurtMarked = true;
        }
        if (ticks <= 1 || !entity.isAlive()) {
            STUNNED.remove(entity);
            if (movement != null && movement.getModifier(STUN_SPEED) != null) movement.removeModifier(STUN_SPEED);
        } else STUNNED.put(entity, ticks - 1);
    }

    public static void projectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile().level() instanceof ServerLevel) || !(event.getRayTraceResult() instanceof EntityHitResult hit)) return;
        if (hit.getEntity() instanceof net.minecraft.world.entity.Mob mob) {
            mobProjectile(event, mob, hit);
            return;
        }
        if (!(hit.getEntity() instanceof ServerPlayer player) || event.getProjectile().getOwner() == player) return;
        Projectile projectile = event.getProjectile();
        GuardState.Result result = GuardState.handleProjectile(player, projectile);
        if (result == GuardState.Result.NONE) return;
        boolean shield = GuardState.shieldGuard(player);
        event.setCanceled(true);
        Entity owner = projectile.getOwner();
        Vec3 sparkDirection = owner != null && owner != player ? owner.position().subtract(player.position())
            : projectile.getDeltaMovement().scale(-1.0D);
        reflectProjectile(player, projectile, result == GuardState.Result.PERFECT);
        PacketDistributor.sendToPlayer(player, new GuardPackets.HitResult(result == GuardState.Result.PERFECT ? 2 : result == GuardState.Result.BLOCK ? 3 : 1, GuardState.guardBreakPending(player), result == GuardState.Result.PERFECT ? GuardRetaliation.begin(player) : 0));
        GuardEffects.onHit(player, owner instanceof net.minecraft.world.entity.player.Player attackingPlayer ? player.damageSources().playerAttack(attackingPlayer)
            : owner instanceof LivingEntity attackingMob ? player.damageSources().mobAttack(attackingMob) : player.damageSources().playerAttack(player), result, sparkDirection);
        GuardState.wear(player, result);
        if (result != GuardState.Result.BLOCK) pushDefender(player, hit.getLocation(), shield);
        if (shield && result == GuardState.Result.PERFECT) {
            // Projectile impact fires before Minecraft calculates its actual damage.
            // Arrow base damage is known; other modded projectiles use a modest estimate.
            float estimate = projectile instanceof AbstractArrow arrow ? (float) arrow.getBaseDamage()
                : (float) Math.max(2.0D, projectile.getDeltaMovement().length() * 2.0D);
            shieldPerfect(player, estimate);
        } else if ((result == GuardState.Result.PARRY || !shield && result == GuardState.Result.PERFECT)
            && owner instanceof LivingEntity attacker)
            pushAttacker(player, attacker, shield);
    }

    public static void reflectProjectile(LivingEntity player, Projectile projectile, boolean perfect) {
        Entity owner = projectile.getOwner();
        Vec3 direction;
        if (perfect && owner != null && owner != player) {
            direction = owner.getEyePosition().subtract(player.getEyePosition());
        } else {
            // Keep a firm horizontal component; downward deflections should be as common as upward ones.
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
            direction = new Vec3(Math.cos(angle), player.getRandom().nextDouble() * 0.32D - 0.20D,
                Math.sin(angle));
        }
        if (direction.lengthSqr() < 1.0E-6D) direction = player.getLookAngle();
        direction = direction.normalize();
        double speed = Math.max(1.4D, projectile.getDeltaMovement().length() * 1.1D);
        projectile.setOwner(player);
        projectile.setPos(player.getEyePosition().add(direction.scale(0.85D)));
        projectile.shoot(direction.x, direction.y, direction.z, (float) speed, 0.0F);
        projectile.hasImpulse = true;
    }

    private static void mobProjectile(ProjectileImpactEvent event, net.minecraft.world.entity.Mob mob, EntityHitResult hit) {
        Projectile projectile = event.getProjectile();
        Entity owner = projectile.getOwner();
        if (owner == mob) return;
        GuardState.Result result = GuardMobState.react(mob, projectile.position(), GuardDamageRules.canParryProjectile(projectile), true, false);
        // Cache the chosen outcome so the damage event cannot process the same projectile twice.
        if (result == GuardState.Result.NONE || result == GuardState.Result.BLOCK &&
            mob.getRandom().nextDouble() * 100.0D >= GuardConfig.BLOCK_DEFLECT_CHANCE.get()) {
            GuardMobState.projectileResult(projectile, mob, result);
            return;
        }
        boolean shield = GuardItemRules.shieldLike(mob.getItemInHand(GuardMobState.defenseHand(mob)));
        event.setCanceled(true);
        reflectProjectile(mob, projectile, result == GuardState.Result.PERFECT);
        DamageSource source = mob.damageSources().mobAttack(owner instanceof LivingEntity living ? living : mob);
        GuardEffects.onMobHit(mob, source, result, shield);
        float amount = projectile instanceof AbstractArrow arrow ? (float) arrow.getBaseDamage() : 2.0F;
        GuardMobState.wear(mob, result, amount);
        if (result != GuardState.Result.BLOCK) {
            pushDefender(mob, hit.getLocation(), shield);
            if (owner instanceof LivingEntity attacker) pushAttacker(mob, attacker, shield);
        }
    }
}
