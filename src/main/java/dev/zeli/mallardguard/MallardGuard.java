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
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.entity.LivingEntity;

@Mod(MallardGuard.ID)
public final class MallardGuard {
    public static final String ID = "mallardguard";
    private static final ThreadLocal<Boolean> RETURNING_DAMAGE = ThreadLocal.withInitial(() -> false);

    public MallardGuard(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, GuardConfig.SERVER_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, GuardConfig.CLIENT_SPEC);
        GuardSounds.EVENTS.register(modBus);
        GuardParticles.TYPES.register(modBus);
        modBus.addListener(GuardPackets::register);
        NeoForge.EVENT_BUS.addListener(GuardState::tick);
        NeoForge.EVENT_BUS.addListener(this::commands);
        NeoForge.EVENT_BUS.addListener(this::incomingDamage);
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
        GuardState.Result result = GuardState.handleHit(player, event.getSource());
        if (result == GuardState.Result.PERFECT || result == GuardState.Result.PARRY) {
            GuardEffects.onHit(player, event.getSource(), result);
            PacketDistributor.sendToPlayer(player, new GuardPackets.HitResult(result == GuardState.Result.PERFECT ? 2 : 1));
            GuardState.wear(player, result);
            event.setCanceled(true);
            if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker != player) {
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
}
