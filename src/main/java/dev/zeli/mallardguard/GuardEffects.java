package dev.zeli.mallardguard;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class GuardEffects {
    // Track the last two actual clips for each player, across all guard outcomes.
    private static final Map<net.minecraft.world.entity.LivingEntity, int[]> LAST_SOUNDS = new WeakHashMap<>();
    public static void clearSession(){LAST_SOUNDS.clear();}

    private GuardEffects() {}

    public static void forget(ServerPlayer player) {
        LAST_SOUNDS.remove(player);
    }

    public static void onHit(ServerPlayer player, DamageSource source, GuardState.Result result) {
        onHit(player, source, result, null);
    }

    public static void onHit(ServerPlayer player, DamageSource source, GuardState.Result result, Vec3 impactDirection) {
        if(result!=GuardState.Result.BLOCK && source.getEntity() instanceof ServerPlayer attacker && attacker!=player)
            PacketDistributor.sendToPlayer(attacker,new GuardPackets.MobCounter(result==GuardState.Result.PERFECT));
        emit(player, source, result, impactDirection, GuardState.shieldGuard(player), GuardState.emptyHandGuard(player));
    }

    public static void onMobHit(net.minecraft.world.entity.Mob mob, DamageSource source, GuardState.Result result, boolean shield) {
        if (result != GuardState.Result.BLOCK && source.getEntity() instanceof ServerPlayer attacker) {
            PacketDistributor.sendToPlayer(attacker, new GuardPackets.MobCounter(result == GuardState.Result.PERFECT));
        }
        emit(mob, source, result, null, shield, false);
    }

    private static void emit(net.minecraft.world.entity.LivingEntity player, DamageSource source, GuardState.Result result,
        Vec3 impactDirection, boolean shield, boolean empty) {
        ServerLevel level = (ServerLevel) player.level();
        if (GuardConfig.HIT_SOUNDS.get()) {
            boolean shieldSound = shield;
            boolean fallSound = result != GuardState.Result.BLOCK
                && (source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL));
            int kind = fallSound ? result == GuardState.Result.PERFECT ? 5 : 6 : shieldSound && result == GuardState.Result.PERFECT ? 4
                : shieldSound ? 3 : result == GuardState.Result.PERFECT ? 2
                : result == GuardState.Result.PARRY ? 1 : 3;
            List<DeferredHolder<SoundEvent, SoundEvent>> sounds = fallSound ? kind == 5 ? GuardSounds.FALL_PARRY : GuardSounds.FALL_REGULAR
                : kind == 4 ? GuardSounds.SHIELD_PERFECT : shieldSound ? GuardSounds.BLOCK : switch (result) {
                case PERFECT -> GuardSounds.PERFECT;
                case PARRY -> GuardSounds.PARRY;
                case BLOCK -> GuardSounds.BLOCK;
                default -> List.of();
            };
            int individual = kind == 3 ? GuardConfig.BLOCK_VOLUME.get() : switch (result) {
                case PERFECT -> GuardConfig.PERFECT_VOLUME.get();
                case PARRY -> GuardConfig.PARRY_VOLUME.get();
                case BLOCK -> GuardConfig.BLOCK_VOLUME.get();
                default -> 0;
            };
            // Preserve the complete server volume so each client can select its louder clip when needed.
            float volume = GuardConfig.MASTER_VOLUME.get() / 100.0F * individual / 100.0F;
            if (!sounds.isEmpty() && volume > 0) {
                int[] previous = LAST_SOUNDS.computeIfAbsent(player, ignored -> new int[]{-1, -1});
                int[] eligible = new int[sounds.size()];
                int count = 0;
                for (int variant = 0; variant < sounds.size(); variant++) {
                    int clip = kind * 16 + variant;
                    if (clip != previous[0] && clip != previous[1]) eligible[count++] = variant;
                }
                // Each fall outcome has one recording; let it play on every fall.
                if (count > 0 || kind == 5 || kind == 6) {
                    int choice = count > 0 ? eligible[ThreadLocalRandom.current().nextInt(count)] : 0;
                    previous[1] = previous[0];
                    previous[0] = kind * 16 + choice;
                    // Send the selected clip once; each listener applies their own local volume.
                    for (ServerPlayer viewer : level.players()) {
                        if (viewer.distanceToSqr(player) <= 32.0D * 32.0D)
                            PacketDistributor.sendToPlayer(viewer, new GuardPackets.HitSound(player.getX(), player.getY(), player.getZ(),
                                kind, choice, volume, viewer == player, result != GuardState.Result.BLOCK));
                    }
                }
            }
        }
        if (!GuardConfig.HIT_PARTICLES.get() || result == GuardState.Result.BLOCK) return;

        Entity attacker = source.getEntity();
        Vec3 direction = impactDirection != null ? impactDirection
            : attacker == null ? player.getLookAngle() : attacker.position().subtract(player.position());
        if (direction.horizontalDistanceSqr() < 1.0E-6D) direction = player.getLookAngle();
        Vec3 center = player.position().add(direction.normalize().scale(0.55D)).add(0, player.getBbHeight() * 0.67D, 0);
        int emitters = empty ? 0 : result == GuardState.Result.PERFECT ? 23 : 10;
        GuardPackets.Sparks packet = new GuardPackets.Sparks(center.x, center.y, center.z, emitters,
            result == GuardState.Result.PERFECT, shield,
            source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL), direction.x, direction.z, false, player instanceof net.minecraft.world.entity.Mob
                ? (result == GuardState.Result.PERFECT ? GuardConfig.MOB_PERFECT_COLOR : GuardConfig.MOB_PARRY_COLOR).get() : -1);
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(center) <= 32.0D * 32.0D) PacketDistributor.sendToPlayer(viewer, (viewer == player || player instanceof net.minecraft.world.entity.Mob && result == GuardState.Result.PERFECT && source.getEntity() == viewer)
                ? new GuardPackets.Sparks(packet.x(), packet.y(), packet.z(), packet.count(), packet.perfect(),
                    packet.shield(), packet.fall(), packet.directionX(), packet.directionZ(), true, packet.tint()) : packet);
        }
    }
}
