package dev.zeli.mallardguard;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Tracking-scoped state for each viewer's optional third-person pose. */
public final class GuardPoses {
    private GuardPoses() {}
    public static void broadcast(LivingEntity entity, InteractionHand hand) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, packet(entity, hand));
    }
    private static GuardPackets.GuardPose packet(LivingEntity entity, InteractionHand hand) {
        return new GuardPackets.GuardPose(entity.getId(), hand != null, hand == InteractionHand.OFF_HAND);
    }
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof LivingEntity target) {
            InteractionHand hand = target instanceof ServerPlayer player ? GuardState.poseHand(player)
                : target instanceof Mob mob ? GuardMobState.guardHand(mob) : null;
            PacketDistributor.sendToPlayer(viewer, packet(target, hand));
        }
    }
}
