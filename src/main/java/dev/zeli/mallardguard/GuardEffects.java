package dev.zeli.mallardguard;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

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
            if (sound != null) level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, result == GuardState.Result.PERFECT ? 0.95F : 0.8F, 1.0F);
        }
        if (!GuardConfig.HIT_PARTICLES.get()) return;

        Entity attacker = source.getEntity();
        Vec3 direction = attacker == null ? player.getLookAngle() : attacker.position().subtract(player.position());
        if (direction.lengthSqr() < 1.0E-6D) direction = player.getLookAngle();
        Vec3 center = player.position().add(direction.normalize().scale(0.55D)).add(0, player.getBbHeight() * 0.67D, 0);
        int count = result == GuardState.Result.PERFECT ? 12 : result == GuardState.Result.PARRY ? 8 : 5;
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, count, 0.22, 0.27, 0.22, 0.12);
        Vector3f color = switch (result) {
            case PERFECT -> new Vector3f(1.0F, 0.82F, 0.18F);
            case PARRY -> new Vector3f(1.0F, 1.0F, 1.0F);
            default -> new Vector3f(1.0F, 0.25F, 0.30F);
        };
        level.sendParticles(new DustParticleOptions(color, 1.1F), center.x, center.y, center.z, count / 2, 0.20, 0.22, 0.20, 0.035);
    }
}
