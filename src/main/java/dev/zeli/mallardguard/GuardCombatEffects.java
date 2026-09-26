package dev.zeli.mallardguard;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

public final class GuardCombatEffects {
    private static final Set<UUID> FALL_BLASTS = new HashSet<>();

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
        Vec3 look = player.getLookAngle();
        double horizontal = Math.hypot(look.x, look.z);
        double boost = (1.0D + damage * 0.25D) * power;
        player.setDeltaMovement(horizontal > 0.01D ? look.x / horizontal * boost : 0.0D,
            (0.6D + Math.max(0.0D, look.y) * 0.6D) * power,
            horizontal > 0.01D ? look.z / horizontal * boost : 0.0D);
        player.hurtMarked = true;
    }

    public static void pushDefender(ServerPlayer player, Vec3 origin) {
        if (!GuardConfig.DEFENDER_KNOCKBACK.get() || GuardConfig.KNOCKBACK_STRENGTH.get() == 0) return;
        Vec3 away = origin == null ? player.getLookAngle().scale(-1.0D) : player.position().subtract(origin);
        away = new Vec3(away.x, 0, away.z);
        if (away.lengthSqr() < 1.0E-6D) away = new Vec3(-player.getLookAngle().x, 0, -player.getLookAngle().z);
        if (away.lengthSqr() < 1.0E-6D) return;
        double strength = GuardConfig.KNOCKBACK_STRENGTH.get() / 100.0D;
        player.setDeltaMovement(player.getDeltaMovement().add(away.normalize().scale(strength)).add(0, strength * 0.25D, 0));
        player.hurtMarked = true;
    }

    public static void projectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile().level() instanceof ServerLevel) || !(event.getRayTraceResult() instanceof EntityHitResult hit)
            || !(hit.getEntity() instanceof ServerPlayer player) || event.getProjectile().getOwner() == player) return;
        Projectile projectile = event.getProjectile();
        GuardState.Result result = GuardState.handleProjectile(player, projectile);
        if (result != GuardState.Result.PERFECT && result != GuardState.Result.PARRY) return;
        event.setCanceled(true);
        Entity owner = projectile.getOwner();
        Vec3 direction;
        if (result == GuardState.Result.PERFECT && owner != null && owner != player) {
            direction = owner.getEyePosition().subtract(player.getEyePosition());
        } else {
            direction = new Vec3(player.getRandom().nextDouble() - 0.5D,
                player.getRandom().nextDouble() * 0.65D - 0.1D,
                player.getRandom().nextDouble() - 0.5D);
        }
        if (direction.lengthSqr() < 1.0E-6D) direction = player.getLookAngle();
        direction = direction.normalize();
        double speed = Math.max(1.4D, projectile.getDeltaMovement().length() * 1.1D);
        projectile.setOwner(player);
        projectile.setPos(player.getEyePosition().add(direction.scale(0.85D)));
        projectile.shoot(direction.x, direction.y, direction.z, (float) speed, 0.0F);
        projectile.hasImpulse = true;
        GuardEffects.onHit(player, player.damageSources().playerAttack(player), result);
        PacketDistributor.sendToPlayer(player, new GuardPackets.HitResult(result == GuardState.Result.PERFECT ? 2 : 1));
        GuardState.wear(player, result);
        pushDefender(player, hit.getLocation());
    }
}
