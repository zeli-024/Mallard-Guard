package dev.zeli.mallardguard;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-owned cells and stagger duration, with bounded once-per-second proximity checks. */
public final class GuardStagger {
    private static final net.minecraft.resources.ResourceLocation STAGGER_SPEED = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "stagger");
    private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();
    private static final class State {
        int cells = GuardConfig.STAGGER_CELLS.get(), maximum = cells, lastSent = -1, lastMaximum = -1;
        long recoverAt, scanAt, stunnedUntil, lastStunnedUntil = -1;
        boolean enemies, enabled, lastEnabled;
    }
    private GuardStagger() {}
    public static void forget(ServerPlayer player) {
        STATES.remove(player);
        var speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(STAGGER_SPEED);
    }
    public static void respawn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) forget(player);
    }
    public static void parried(ServerPlayer player, boolean perfect) {
        if (!GuardConfig.STAGGER_ENABLED.get()) return;
        State s = STATES.computeIfAbsent(player, p -> new State());
        s.cells = Math.max(0, s.cells - (perfect ? GuardConfig.STAGGER_PERFECT_LOSS : GuardConfig.STAGGER_PARRY_LOSS).get());
        s.recoverAt = player.level().getGameTime() + (s.enemies ? GuardConfig.STAGGER_NEAR_TICKS : GuardConfig.STAGGER_FAR_TICKS).get();
        if (s.cells == 0 && s.stunnedUntil <= player.level().getGameTime() && GuardConfig.STAGGER_STUN_TICKS.get() > 0) {
            int duration = GuardConfig.STAGGER_STUN_TICKS.get();
            s.stunnedUntil = player.level().getGameTime() + duration;
            GuardState.input(player, false, false);
        }
        send(player, s);
    }
    public static boolean stunned(ServerPlayer player) {
        State state = STATES.get(player);
        return state != null && GuardConfig.STAGGER_ENABLED.get() && player.level().getGameTime() < state.stunnedUntil;
    }
    public static void perfect(ServerPlayer player) {
        if (!GuardConfig.STAGGER_ENABLED.get()) return;
        State s = STATES.computeIfAbsent(player, p -> new State());
        s.cells = Math.min(GuardConfig.STAGGER_CELLS.get(), s.cells + GuardConfig.STAGGER_RESTORE.get());
        send(player, s);
    }
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.isAlive()) {
            boolean hadState = STATES.containsKey(player);
            forget(player);
            if (hadState) PacketDistributor.sendToPlayer(player, new GuardPackets.Stagger(0, GuardConfig.STAGGER_CELLS.get(), GuardConfig.STAGGER_ENABLED.get(), 0));
            return;
        }
        State s = STATES.computeIfAbsent(player, p -> new State());
        if (!GuardConfig.STAGGER_ENABLED.get()) {
            s.enabled = false;
            s.stunnedUntil = 0;
            if (s.lastEnabled || s.lastSent < 0) {
                var speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                if (speed != null) speed.removeModifier(STAGGER_SPEED);
                send(player, s);
            }
            return;
        }
        int maximum = GuardConfig.STAGGER_CELLS.get();
        if (s.maximum != maximum) { s.cells = Math.min(maximum, s.cells); s.maximum = maximum; }
        s.enabled = GuardConfig.STAGGER_ENABLED.get();
        long now = player.level().getGameTime();
        var speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        var id = STAGGER_SPEED;
        if (stunned(player)) {
            if (speed != null && speed.getModifier(id) == null) speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(id, -1, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            var motion = player.getDeltaMovement(); player.setDeltaMovement(0, Math.min(0, motion.y), 0); player.hurtMarked = true;
        } else {
            if (speed != null && speed.getModifier(id) != null) speed.removeModifier(id);
            if (s.stunnedUntil > 0) { s.stunnedUntil = 0; s.cells = Math.min(maximum, GuardConfig.STAGGER_EXIT_RECOVERY.get());
                s.recoverAt = now + (s.enemies ? GuardConfig.STAGGER_NEAR_TICKS : GuardConfig.STAGGER_FAR_TICKS).get(); }
        }
        if (!stunned(player) && s.enabled && s.cells < maximum && player.isAlive()) {
            if (now >= s.scanAt) {
                boolean wasNear = s.enemies;
                int radius = GuardConfig.STAGGER_RADIUS.get();
                s.enemies = !player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius),
                    mob -> mob.isAlive() && (mob instanceof Enemy || mob.getTarget() == player) && mob.distanceToSqr(player) <= radius * radius).isEmpty();
                s.scanAt = now + 20;
                if (wasNear != s.enemies) s.recoverAt = now + (s.enemies ? GuardConfig.STAGGER_NEAR_TICKS : GuardConfig.STAGGER_FAR_TICKS).get();
            }
            int interval = (s.enemies ? GuardConfig.STAGGER_NEAR_TICKS : GuardConfig.STAGGER_FAR_TICKS).get();
            if (interval > 0 && now >= s.recoverAt) { s.cells++; s.recoverAt = now + interval; }
        }
        send(player, s);
    }
    private static void send(ServerPlayer player, State s) {
        s.enabled = GuardConfig.STAGGER_ENABLED.get();
        if (s.cells == s.lastSent && s.maximum == s.lastMaximum && s.enabled == s.lastEnabled && s.stunnedUntil == s.lastStunnedUntil) return;
        s.lastSent = s.cells; s.lastMaximum = s.maximum; s.lastEnabled = s.enabled; s.lastStunnedUntil = s.stunnedUntil;
        PacketDistributor.sendToPlayer(player, new GuardPackets.Stagger(s.cells, s.maximum, s.enabled, stunned(player) ? (int) Math.max(0, s.stunnedUntil - player.level().getGameTime()) : 0));
    }
}
