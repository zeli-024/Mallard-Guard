package dev.zeli.mallardguard;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class GuardEffects {
    private GuardEffects() {}

    public static void onHit(ServerPlayer player, DamageSource source, GuardState.Result result) {
        ServerLevel level = player.serverLevel();
        if (GuardConfig.HIT_SOUNDS.get()) {
            SoundEvent sound = switch (result) {
                case PERFECT -> GuardSounds.PERFECT.get();
                case PARRY -> GuardSounds.PARRY.get();
                case BLOCK -> GuardSounds.BLOCK.get();
                default -> null;
            };
            int individual = switch (result) {
                case PERFECT -> GuardConfig.PERFECT_VOLUME.get();
                case PARRY -> GuardConfig.PARRY_VOLUME.get();
                case BLOCK -> GuardConfig.BLOCK_VOLUME.get();
                default -> 0;
            };
            float volume = GuardConfig.MASTER_VOLUME.get() / 100.0F * individual / 100.0F;
            if (sound != null && volume > 0) level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, 1.0F);
        }
        if (!GuardConfig.HIT_PARTICLES.get()) return;

        Entity attacker = source.getEntity();
        Vec3 direction = attacker == null ? player.getLookAngle() : attacker.position().subtract(player.position());
        if (direction.lengthSqr() < 1.0E-6D) direction = player.getLookAngle();
        Vec3 center = player.position().add(direction.normalize().scale(0.55D)).add(0, player.getBbHeight() * 0.67D, 0);
        int count = result == GuardState.Result.PERFECT ? 12 : result == GuardState.Result.PARRY ? 8 : 5;
        level.sendParticles(GuardParticles.FLYING_SPARK.get(), center.x, center.y, center.z, count, 0.18D, 0.14D, 0.18D, 0.13D);
        level.sendParticles(GuardParticles.SPARK_FLASH.get(), center.x, center.y, center.z, 1, 0, 0, 0, 0);
    }
}
