package dev.zeli.mallardguard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.UseAnim;

public final class GuardState {
    private static final Map<UUID, GuardState> STATES = new HashMap<>();
    private boolean held;
    private int elapsed;
    private int recharge;
    private int phase;
    private ItemStack heldItem = ItemStack.EMPTY;
    private long safetyThroughTick = -1;

    public enum Result { NONE, PERFECT, PARRY, BLOCK }

    private GuardState() {}

    public static boolean eligible(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return false;
        if (GuardItemRules.matches(stack, GuardConfig.EXCLUDED_ITEMS.get())) return false;
        if (GuardItemRules.matches(stack, GuardConfig.INCLUDED_ITEMS.get())) return true;
        if (!GuardConfig.ALLOW_USABLE_ITEMS.get() && stack.getUseAnimation() != UseAnim.NONE) return false;
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
        if (state.recharge > 0 && state.phase != 3) state.recharge--;
        if (state.held) {
            if (!eligible(player) || state.heldItem != player.getMainHandItem()) {
                state.held = false;
                state.phase = 0;
            } else if (state.phase == 1 || state.phase == 2) {
                state.elapsed++;
                if (state.elapsed >= GuardConfig.PARRY_TICKS.get()) {
                    state.phase = GuardConfig.BLOCK.get() ? 3 : 0;
                    if (state.phase == 3) state.recharge = GuardConfig.RECHARGE_TICKS.get();
                    else state.held = false;
                } else if (state.elapsed >= GuardConfig.PERFECT_TICKS.get()) {
                    state.phase = 2;
                }
            }
        }
        sync(player, state);
    }

    public static Result handleHit(ServerPlayer player, DamageSource source) {
        if (source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL)) {
            return GuardConfig.FALL_PARRY.get() ? handleHit(player, SourceKind.FALL, null) : Result.NONE;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            Vec3 position = source.getSourcePosition();
            return handleHit(player, SourceKind.EXPLOSION, position);
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE) || source.getDirectEntity() instanceof Projectile) {
            // Entity projectiles parry at ProjectileImpactEvent, before their collision can consume them.
            // Damage-only sources still honor blocking, but cannot be physically redirected.
            return handleHit(player, SourceKind.PROJECTILE_DAMAGE, source.getSourcePosition());
        }
        if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker || player.distanceTo(attacker) > 4.0D) return Result.NONE;
        return handleHit(player, SourceKind.MELEE, attacker.position());
    }

    public static Result handleProjectile(ServerPlayer player, Projectile projectile) {
        if (!GuardConfig.PARRY_PROJECTILES.get()) return Result.NONE;
        return handleHit(player, SourceKind.PROJECTILE_IMPACT, projectile.position());
    }

    private enum SourceKind { MELEE, FALL, EXPLOSION, PROJECTILE_IMPACT, PROJECTILE_DAMAGE }

    private static Result handleHit(ServerPlayer player, SourceKind kind, Vec3 origin) {
        GuardState state = STATES.get(player.getUUID());
        if (state == null || !eligible(player) || (kind != SourceKind.FALL && !inAngle(player, origin))) return Result.NONE;
        // A successful parry closes the stance immediately; safety is a separate deadline.
        if (state.phase == 0 && player.level().getGameTime() <= state.safetyThroughTick) {
            return canParry(kind, Result.PARRY) ? Result.PARRY : Result.NONE;
        }
        if (state.phase == 0) return Result.NONE;
        Result result = state.phase == 1 ? Result.PERFECT : state.phase == 2 ? Result.PARRY : Result.BLOCK;
        if (result == Result.BLOCK && !canBlock(kind) || result != Result.BLOCK && !canParry(kind, result)) return Result.NONE;
        if (result == Result.PERFECT) state.recharge = 0;
        else if (result == Result.PARRY) state.recharge = Math.max(0, GuardConfig.RECHARGE_TICKS.get() / 2);
        if (result == Result.PERFECT || result == Result.PARRY) {
            state.safetyThroughTick = GuardConfig.FOLLOW_UP_TICKS.get() == 0 ? -1 : player.level().getGameTime() + GuardConfig.FOLLOW_UP_TICKS.get();
            state.held = false;
            state.phase = 0;
            state.elapsed = 0;
        }
        sync(player, state);
        return result;
    }

    public static void wear(ServerPlayer player, Result result) {
        int base = switch (result) {
            case PERFECT -> GuardConfig.PERFECT_WEAR.get();
            case PARRY -> GuardConfig.PARRY_WEAR.get();
            case BLOCK -> GuardConfig.BLOCK_WEAR.get();
            default -> 0;
        };
        if (base <= 0) return;
        ItemStack stack = player.getMainHandItem();
        if (!stack.isDamageableItem() && !stack.has(DataComponents.UNBREAKABLE) && GuardConfig.ADDED_DURABILITY.get() > 0) {
            if (stack.getCount() > 1) {
                ItemStack remainder = stack.copy();
                remainder.setCount(stack.getCount() - 1);
                stack.setCount(1);
                if (!player.getInventory().add(remainder) && !remainder.isEmpty()) player.drop(remainder, false);
            }
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
            stack.set(DataComponents.MAX_DAMAGE, GuardConfig.ADDED_DURABILITY.get());
            stack.set(DataComponents.DAMAGE, 0);
        }
        if (!stack.isDamageableItem()) return;
        // Fragile tools pay more, while durable tools still take at least base wear.
        double scale = Math.min(4.0D, Math.max(1.0D, Math.sqrt(250.0D / Math.max(1, stack.getMaxDamage()))));
        int wear = Math.max(1, (int) Math.ceil(base * scale));
        stack.hurtAndBreak(wear, player, EquipmentSlot.MAINHAND);
    }

    private static boolean canParry(SourceKind kind, Result result) {
        return switch (kind) {
            case MELEE -> true;
            case FALL -> GuardConfig.FALL_PARRY.get();
            case EXPLOSION -> GuardConfig.PARRY_EXPLOSIONS.get() && (!GuardConfig.PERFECT_EXPLOSIONS_ONLY.get() || result == Result.PERFECT);
            case PROJECTILE_IMPACT -> GuardConfig.PARRY_PROJECTILES.get();
            case PROJECTILE_DAMAGE -> false;
        };
    }

    private static boolean canBlock(SourceKind kind) {
        return switch (kind) {
            case MELEE -> true;
            case FALL, PROJECTILE_IMPACT -> false;
            case EXPLOSION -> GuardConfig.BLOCK_EXPLOSIONS.get();
            case PROJECTILE_DAMAGE -> GuardConfig.BLOCK_PROJECTILES.get();
        };
    }

    private static boolean inAngle(ServerPlayer player, Vec3 origin) {
        if (origin == null) return true;
        if (GuardConfig.FACING_ANGLE.get() >= 360) return true;
        Vec3 offset = origin.subtract(player.position());
        if (offset.lengthSqr() < 1.0E-8D) return true;
        double dot = player.getLookAngle().normalize().dot(offset.normalize());
        return dot >= Math.cos(Math.toRadians(GuardConfig.FACING_ANGLE.get() / 2.0D));
    }

    private static void sync(ServerPlayer player, GuardState state) {
        PacketDistributor.sendToPlayer(player, new GuardPackets.Status(state.phase, state.elapsed, state.recharge, GuardConfig.PARRY_TICKS.get(), GuardConfig.RECHARGE_TICKS.get()));
    }
}
