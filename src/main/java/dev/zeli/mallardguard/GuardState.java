package dev.zeli.mallardguard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;

public final class GuardState {
    private static final Map<UUID, GuardState> STATES = new HashMap<>();
    private boolean held;
    private int elapsed;
    private int recharge;
    private int phase;
    private ItemStack heldItem = ItemStack.EMPTY;

    private GuardState() {}

    public static boolean eligible(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return false;
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) return false;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().is(Attributes.ATTACK_DAMAGE) && entry.modifier().amount() > 0 && entry.slot().test(net.minecraft.world.entity.EquipmentSlot.MAINHAND)) return true;
        }
        return false;
    }

    public static void input(ServerPlayer player, boolean down) {
        GuardState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new GuardState());
        if (!down) {
            state.held = false;
            state.phase = 0;
            sync(player, state);
            return;
        }
        if (state.held || state.recharge > 0 || !eligible(player) || (!GuardConfig.PARRY.get() && !GuardConfig.BLOCK.get())) return;
        state.held = true;
        state.elapsed = 0;
        state.heldItem = player.getMainHandItem();
        state.phase = GuardConfig.PARRY.get() ? (GuardConfig.PERFECT_TICKS.get() > 0 ? 1 : 2) : 3;
        state.recharge = GuardConfig.RECHARGE_TICKS.get();
        sync(player, state);
    }

    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        GuardState state = STATES.get(player.getUUID());
        if (state == null) return;
        if (state.recharge > 0) state.recharge--;
        if (state.held) {
            if (!eligible(player) || state.heldItem != player.getMainHandItem()) {
                state.held = false;
                state.phase = 0;
            } else if (state.phase == 1 || state.phase == 2) {
                state.elapsed++;
                if (state.elapsed >= GuardConfig.PARRY_TICKS.get()) {
                    state.phase = GuardConfig.BLOCK.get() ? 3 : 0;
                    if (state.phase == 0) state.held = false;
                } else if (state.elapsed >= GuardConfig.PERFECT_TICKS.get()) {
                    state.phase = 2;
                }
            }
        }
        sync(player, state);
    }

    private static void sync(ServerPlayer player, GuardState state) {
        PacketDistributor.sendToPlayer(player, new GuardPackets.Status(state.phase, state.elapsed, state.recharge, GuardConfig.PARRY_TICKS.get(), GuardConfig.RECHARGE_TICKS.get()));
    }
}
