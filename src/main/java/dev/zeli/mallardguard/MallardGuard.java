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
        modBus.addListener(GuardConfig::onLoad);
        container.registerConfig(ModConfig.Type.SERVER, GuardConfig.SERVER_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, GuardConfig.CLIENT_SPEC);
        GuardSounds.EVENTS.register(modBus);
        GuardParticles.TYPES.register(modBus);
        modBus.addListener(GuardPackets::register);
        NeoForge.EVENT_BUS.addListener(GuardState::tick);
        NeoForge.EVENT_BUS.addListener(GuardState::postponeShieldUse);
        NeoForge.EVENT_BUS.addListener(this::commands);
        NeoForge.EVENT_BUS.addListener(this::incomingDamage);
        NeoForge.EVENT_BUS.addListener(this::shieldBlock);
        NeoForge.EVENT_BUS.addListener(GuardCombatEffects::projectileImpact);
    }

    private void commands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("mallardguardconfig").executes(ctx -> open(ctx.getSource().getPlayerOrException())));
        dispatcher.register(Commands.literal("mgc").executes(ctx -> open(ctx.getSource().getPlayerOrException())));
    }

    private int open(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, GuardConfig.snapshot(player.hasPermissions(2)));
        return 1;
    }

    private void incomingDamage(LivingIncomingDamageEvent event) {
        if (RETURNING_DAMAGE.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (GuardCombatEffects.isOwnFallBlast(player, event.getSource())) {
            event.setCanceled(true);
            return;
        }
        GuardState.Result result = GuardState.handleHit(player, event.getSource());
        if (result == GuardState.Result.PERFECT || result == GuardState.Result.PARRY) {
            GuardEffects.onHit(player, event.getSource(), result);
            PacketDistributor.sendToPlayer(player, new GuardPackets.HitResult(result == GuardState.Result.PERFECT ? 2 : 1));
            GuardState.wear(player, result);
            event.setCanceled(true);
            if (event.getSource().is(DamageTypes.FALL) || event.getSource().is(DamageTypes.FLY_INTO_WALL)) {
                GuardCombatEffects.fall(player, event.getAmount(), result == GuardState.Result.PERFECT);
            } else {
                Vec3 origin = event.getSource().getSourcePosition();
                GuardCombatEffects.pushDefender(player, origin);
            }
            if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker != player
                && event.getSource().getDirectEntity() == attacker) {
                float multiplier = result == GuardState.Result.PERFECT ? GuardConfig.PERFECT_RETALIATION.get().floatValue() : GuardConfig.PARRY_RETALIATION.get().floatValue();
                if (multiplier > 0 && event.getAmount() > 0) {
                    RETURNING_DAMAGE.set(true);
                    try {
                        attacker.hurt(player.damageSources().playerAttack(player), event.getAmount() * multiplier);
                    } finally {
                        RETURNING_DAMAGE.remove();
                    }
                }
            }
            return;
        }
        if (result == GuardState.Result.BLOCK) {
            GuardEffects.onHit(player, event.getSource(), result);
            PacketDistributor.sendToPlayer(player, new GuardPackets.HitResult(3));
            GuardState.wear(player, result);
            event.setAmount(event.getAmount() * (1.0F - GuardConfig.BLOCK_REDUCTION.get().floatValue()));
        }
    }

    private void shieldBlock(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !GuardState.isShieldBlocking(player)
            || !event.getBlocked() || event.getBlockedDamage() <= 0) return;
        GuardEffects.onHit(player, event.getDamageSource(), GuardState.Result.BLOCK);
        PacketDistributor.sendToPlayer(player, new GuardPackets.HitResult(3));
        // The item's own shield logic handles damage reduction and durability.
    }
}
