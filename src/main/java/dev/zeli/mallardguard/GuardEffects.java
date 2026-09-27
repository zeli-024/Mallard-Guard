package dev.zeli.mallardguard;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class GuardEffects {
    private static final Map<ServerPlayer, int[]> LAST_SOUNDS = new WeakHashMap<>();
    private GuardEffects() {}

    public static void onHit(ServerPlayer player, DamageSource source, GuardState.Result result) {
        ServerLevel level = player.serverLevel();
        if (GuardConfig.HIT_SOUNDS.get()) {
            List<DeferredHolder<SoundEvent, SoundEvent>> sounds = switch (result) {
                case PERFECT -> GuardSounds.PERFECT;
                case PARRY -> GuardSounds.PARRY;
                case BLOCK -> GuardSounds.BLOCK;
                default -> List.of();
            };
            int individual = switch (result) {
                case PERFECT -> GuardConfig.PERFECT_VOLUME.get();
                case PARRY -> GuardConfig.PARRY_VOLUME.get();
                case BLOCK -> GuardConfig.BLOCK_VOLUME.get();
                default -> 0;
            };
            // Minecraft caps audible gain at 1; reserve usable headroom above the default settings.
            float volume = Math.min(1.0F, GuardConfig.MASTER_VOLUME.get() / 200.0F * individual / 100.0F);
            if (!sounds.isEmpty() && volume > 0) {
                int[] previous = LAST_SOUNDS.computeIfAbsent(player, ignored -> new int[]{-1, -1});
                // Regular and perfect parries share clips and therefore also share their last selection.
                int slot = result == GuardState.Result.BLOCK ? 1 : 0;
                // Pick uniformly from the other variants; never ask Minecraft to roll again.
                int choice = ThreadLocalRandom.current().nextInt(sounds.size() - (previous[slot] < 0 ? 0 : 1));
                if (previous[slot] >= 0 && choice >= previous[slot]) choice++;
                previous[slot] = choice;
                level.playSound(null, player.getX(), player.getY(), player.getZ(), sounds.get(choice).get(), SoundSource.PLAYERS, volume, 1.0F);
            }
        }
        if (!GuardConfig.HIT_PARTICLES.get() || result == GuardState.Result.BLOCK) return;

        Entity attacker = source.getEntity();
        Vec3 direction = attacker == null ? player.getLookAngle() : attacker.position().subtract(player.position());
        if (direction.lengthSqr() < 1.0E-6D) direction = player.getLookAngle();
        Vec3 center = player.position().add(direction.normalize().scale(0.55D)).add(0, player.getBbHeight() * 0.67D, 0);
        int emitters = result == GuardState.Result.PERFECT ? 23 : 10;
        GuardPackets.Sparks packet = new GuardPackets.Sparks(center.x, center.y, center.z, emitters, result == GuardState.Result.PERFECT);
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(center) <= 32.0D * 32.0D) PacketDistributor.sendToPlayer(viewer, packet);
        }
    }
}
