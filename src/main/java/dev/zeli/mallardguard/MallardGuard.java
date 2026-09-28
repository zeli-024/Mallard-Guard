package dev.zeli.mallardguard;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.phys.Vec3;

@Mod(MallardGuard.ID)
public final class MallardGuard {
    public static final String ID = "mallardguard";
    private static final ThreadLocal<Boolean> RETURNING_DAMAGE = ThreadLocal.withInitial(() -> false);

    public MallardGuard(IEventBus modBus, ModContainer container) {
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            dev.zeli.mallardguard.client.GuardClient.registerConfigScreen(container);
            modBus.addListener(dev.zeli.mallardguard.client.GuardClient::registerKeyMappings);
        }
        GuardConfig.setCurrentVersion(container.getModInfo().getVersion().toString());
        GuardConfig.snapshotExistingConfigs();
        modBus.addListener(GuardConfig::onLoad);
        container.registerConfig(ModConfig.Type.SERVER, GuardConfig.SERVER_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, GuardConfig.CLIENT_SPEC);
        GuardSounds.EVENTS.register(modBus);
        GuardParticles.TYPES.register(modBus);
        modBus.addListener(GuardPackets::register);
        NeoForge.EVENT_BUS.addListener(GuardState::tick);
        NeoForge.EVENT_BUS.addListener(GuardState::preventVanillaShieldUse);
        NeoForge.EVENT_BUS.addListener(this::commands);
        NeoForge.EVENT_BUS.addListener(this::playerJoined);
        NeoForge.EVENT_BUS.addListener(this::playerLeft);
        NeoForge.EVENT_BUS.addListener(this::incomingDamage);
        NeoForge.EVENT_BUS.addListener(this::shieldBlock);
        NeoForge.EVENT_BUS.addListener(GuardCombatEffects::projectileImpact);
        NeoForge.EVENT_BUS.addListener(GuardCombatEffects::tickStun);
    }

    private void commands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("mallardguardconfig").executes(ctx -> open(ctx.getSource().getPlayerOrException())));
        dispatcher.register(Commands.literal("mgc").executes(ctx -> open(ctx.getSource().getPlayerOrException())));
    }

    private int open(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, GuardPackets.clientPolicy());
        PacketDistributor.sendToPlayer(player, GuardConfig.snapshot(player.hasPermissions(2)));
        PacketDistributor.sendToPlayer(player, GuardConfig.shieldSnapshot());
        PacketDistributor.sendToPlayer(player, GuardDamageRules.state(player));
        return 1;
    }

    private void playerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, GuardPackets.clientPolicy());
            PacketDistributor.sendToPlayer(player, GuardConfig.shieldSnapshot());
        }
    }

    private void playerLeft(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GuardState.forget(player);
            GuardDamageRules.forget(player);
            GuardEffects.forget(player);
        }
    }

    private void incomingDamage(LivingIncomingDamageEvent event) {
        if (RETURNING_DAMAGE.get()) return;
        if (event.getSource().getEntity() instanceof LivingEntity attacking
            && event.getSource().getDirectEntity() == attacking && GuardCombatEffects.stunned(attacking)) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (GuardCombatEffects.isOwnFallBlast(player, event.getSource())) {
            event.setCanceled(true);
            return;
        }
        GuardDamageRules.record(player, event.getSource());
        GuardState.Result result = GuardState.handleHit(player, event.getSource());
        if (result == GuardState.Result.PERFECT || result == GuardState.Result.PARRY) {
            boolean shield = GuardState.shieldGuard(player);
            PacketDistributor.sendToPlayer(player, new GuardPackets.HitResult(result == GuardState.Result.PERFECT ? 2 : 1, false));
            GuardEffects.onHit(player, event.getSource(), result);
            GuardState.wear(player, result);
            event.setCanceled(true);
            if (event.getSource().is(DamageTypes.FALL) || event.getSource().is(DamageTypes.FLY_INTO_WALL)) {
                GuardCombatEffects.fall(player, event.getAmount(), result == GuardState.Result.PERFECT);
            } else {
                Vec3 origin = event.getSource().getSourcePosition();
                GuardCombatEffects.pushDefender(player, origin, shield);
            }
            if (shield && result == GuardState.Result.PERFECT)
                GuardCombatEffects.shieldPerfect(player, event.getAmount());
            else if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker != player
                && event.getSource().getDirectEntity() == attacker) {
                float multiplier = result == GuardState.Result.PERFECT ? GuardConfig.PERFECT_RETALIATION.get().floatValue() : GuardConfig.PARRY_RETALIATION.get().floatValue();
                if (multiplier > 0 && event.getAmount() > 0) {
                    returnDamage(player, attacker, event.getAmount() * multiplier);
                }
            }
            if ((result == GuardState.Result.PARRY || !shield && result == GuardState.Result.PERFECT)
                && event.getSource().getEntity() instanceof LivingEntity attacker
                && attacker != player && event.getSource().getDirectEntity() == attacker)
                GuardCombatEffects.pushAttacker(player, attacker, shield);
            return;
        }
        if (result == GuardState.Result.BLOCK) {
            PacketDistributor.sendToPlayer(player, new GuardPackets.HitResult(3, GuardState.guardBreakPending(player)));
            GuardEffects.onHit(player, event.getSource(), result);
            GuardState.wear(player, result);
            event.setAmount(event.getAmount() * (1.0F - GuardConfig.BLOCK_REDUCTION.get().floatValue()));
        }
    }

    private void shieldBlock(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !GuardState.isShieldBlocking(player)) return;
        if (GuardState.guardBreakPending(player)) { event.setBlocked(false); return; }
        if (!event.getBlocked() || event.getBlockedDamage() <= 0) return;
        GuardState.shieldBlocked(player);
        PacketDistributor.sendToPlayer(player, new GuardPackets.HitResult(3, GuardState.guardBreakPending(player)));
        GuardEffects.onHit(player, event.getDamageSource(), GuardState.Result.BLOCK);
        // The item's own shield logic handles damage reduction and durability.
    }

    public static void returnDamage(ServerPlayer defender, LivingEntity target, float amount) {
        RETURNING_DAMAGE.set(true);
        try { target.hurt(defender.damageSources().playerAttack(defender), amount); }
        finally { RETURNING_DAMAGE.remove(); }
    }
}
