package dev.zeli.mallardguard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class GuardState {
    private static final Map<UUID, GuardState> STATES = new HashMap<>();
    public static final ResourceLocation GUARD_SLOWDOWN = ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "guard_slowdown");
    private boolean held;
    private int elapsed;
    private int recharge;
    private int phase;
    private ItemStack heldItem = ItemStack.EMPTY;
    private InteractionHand guardHand = InteractionHand.MAIN_HAND;
    private long safetyThroughTick = -1;

    public enum Result { NONE, PERFECT, PARRY, BLOCK }

    private GuardState() {}

    public static boolean eligible(Player player) {
        return eligible(player, InteractionHand.MAIN_HAND) || eligible(player, InteractionHand.OFF_HAND);
    }

    public static boolean eligible(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return false;
        if (GuardItemRules.matches(stack, GuardConfig.EXCLUDED_ITEMS.get())) return false;
        if (GuardItemRules.shieldLike(stack)) return true;
        if (GuardItemRules.matches(stack, GuardConfig.INCLUDED_ITEMS.get())) return true;
        if (!GuardConfig.ALLOW_USABLE_ITEMS.get() && stack.getUseAnimation() != UseAnim.NONE) return false;
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) return false;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            // Attack weapons commonly declare only MAINHAND damage even when
            // the player chooses to guard with that same weapon in the offhand.
            if (entry.attribute().is(Attributes.ATTACK_DAMAGE) && entry.modifier().amount() > 0
                && (entry.slot().test(EquipmentSlot.MAINHAND) || hand == InteractionHand.OFF_HAND && entry.slot().test(EquipmentSlot.OFFHAND))) return true;
        }
        return false;
    }

    public static void input(ServerPlayer player, boolean down, boolean requestedOffhand) {
        GuardState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new GuardState());
        if (down && GuardItemRules.consumableInEitherHand(player)) down = false;
        if (!down) {
            if (state.phase == 4 && player.isUsingItem() && player.getUsedItemHand() == state.guardHand)
                player.stopUsingItem();
            state.held = false;
            state.phase = 0;
            updateMovement(player, false);
            sync(player, state);
            return;
        }
        if (state.held || state.recharge > 0 || (!GuardConfig.PARRY.get() && !GuardConfig.BLOCK.get())) return;
        InteractionHand requested = requestedOffhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        InteractionHand hand = eligible(player, requested) ? requested :
            eligible(player, requested == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND)
                ? (requested == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND) : null;
        if (hand == null) return;
        state.held = true;
        state.elapsed = 0;
        state.guardHand = hand;
        state.heldItem = player.getItemInHand(hand);
        state.phase = GuardConfig.PARRY.get() ? (GuardConfig.PERFECT_TICKS.get() > 0 ? 1 : 2)
            : GuardItemRules.shieldLike(state.heldItem) ? 4 : 3;
        if (GuardItemRules.shieldLike(state.heldItem) && player.isUsingItem() && player.getUsedItemHand() == hand)
            player.stopUsingItem();
        if (state.phase == 4) player.startUsingItem(hand);
        state.recharge = GuardConfig.RECHARGE_TICKS.get();
        updateMovement(player, true);
        sync(player, state);
    }

    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        GuardState state = STATES.get(player.getUUID());
        if (state == null) return;
        if (state.recharge > 0 && state.phase != 3 && state.phase != 4) state.recharge--;
        if (state.held) {
            if (GuardItemRules.consumableInEitherHand(player) || !eligible(player, state.guardHand)
                || state.heldItem != player.getItemInHand(state.guardHand)) {
                if (state.phase == 4 && player.isUsingItem() && player.getUsedItemHand() == state.guardHand)
                    player.stopUsingItem();
                state.held = false;
                state.phase = 0;
            } else if (state.phase == 1 || state.phase == 2) {
                state.elapsed++;
                if (state.elapsed >= GuardConfig.PARRY_TICKS.get()) {
                    state.phase = GuardItemRules.shieldLike(state.heldItem) ? 4 : GuardConfig.BLOCK.get() ? 3 : 0;
                    if (state.phase == 4) {
                        // After the timed parry, allow the item to do its own shield blocking.
                        player.startUsingItem(state.guardHand);
                        state.recharge = GuardConfig.RECHARGE_TICKS.get();
                    } else if (state.phase == 3) state.recharge = GuardConfig.RECHARGE_TICKS.get();
                    else state.held = false;
                } else if (state.elapsed >= GuardConfig.PERFECT_TICKS.get()) {
                    state.phase = 2;
                }
            }
            if (state.phase == 4 && (!player.isUsingItem() || player.getUsedItemHand() != state.guardHand)) {
                state.held = false;
                state.phase = 0;
            }
        }
        updateMovement(player, state.held && state.phase != 0);
        sync(player, state);
    }

    public static void updateMovement(Player player, boolean guarding) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        AttributeModifier modifier = speed.getModifier(GUARD_SLOWDOWN);
        int percent = GuardConfig.GUARD_MOVEMENT_PERCENT.get();
        double amount = percent / 100.0D - 1.0D;
        if (modifier != null && (!guarding || percent == 100 || modifier.amount() != amount))
            speed.removeModifier(GUARD_SLOWDOWN);
        if (guarding && percent < 100 && speed.getModifier(GUARD_SLOWDOWN) == null)
            speed.addTransientModifier(new AttributeModifier(GUARD_SLOWDOWN, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static Result handleHit(ServerPlayer player, DamageSource source) {
        // The optional exclusion only affects parries; normal blocking still follows its own rules.
        if (!GuardConfig.PARRY_DROWNING_FIRE.get()
            && (source.is(DamageTypes.DROWN) || source.is(DamageTypeTags.IS_FIRE))) {
            GuardState state = STATES.get(player.getUUID());
            if (state == null || state.phase != 3) return Result.NONE;
        }
        if (source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL)) {
            // Ground impacts require deliberate downward aim. Keep the existing
            // flying-into-wall behavior separate from fall damage.
            if (source.is(DamageTypes.FALL) && player.getLookAngle().y > -Math.sin(Math.toRadians(40.0D)))
                return Result.NONE;
            return GuardConfig.FALL_PARRY.get() ? handleHit(player, SourceKind.FALL, null) : Result.NONE;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            Vec3 position = source.getSourcePosition();
            return handleHit(player, SourceKind.EXPLOSION, position);
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE) || source.getDirectEntity() instanceof Projectile) {
            // Entity projectiles parry at ProjectileImpactEvent, before their collision can consume them.
            // Damage-only projectiles can be parried but have no entity to redirect.
            return handleHit(player, SourceKind.PROJECTILE_DAMAGE, source.getSourcePosition());
        }
        if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
            && player.distanceTo(attacker) <= 4.0D)
            return handleHit(player, SourceKind.MELEE, attacker.position());
        return handleHit(player, SourceKind.OTHER, source.getSourcePosition());
    }

    public static Result handleProjectile(ServerPlayer player, Projectile projectile) {
        GuardState state = STATES.get(player.getUUID());
        if (state != null && state.phase == 4) return Result.NONE;
        if (state != null && state.phase == 3) {
            if (GuardConfig.BLOCK_DEFLECT_CHANCE.get() == 0 ||
                player.getRandom().nextInt(100) >= GuardConfig.BLOCK_DEFLECT_CHANCE.get()) return Result.NONE;
        } else if (!GuardConfig.PARRY_PROJECTILES.get()) return Result.NONE;
        return handleHit(player, SourceKind.PROJECTILE_IMPACT, projectile.position());
    }

    private enum SourceKind { MELEE, FALL, EXPLOSION, PROJECTILE_IMPACT, PROJECTILE_DAMAGE, OTHER }

    private static Result handleHit(ServerPlayer player, SourceKind kind, Vec3 origin) {
        GuardState state = STATES.get(player.getUUID());
        if (state == null || !eligible(player, state.guardHand) || (kind != SourceKind.FALL && !inAngle(player, origin))) return Result.NONE;
        // A successful parry closes the stance immediately; safety is a separate deadline.
        if (state.phase == 0 && player.level().getGameTime() <= state.safetyThroughTick) {
            return canParry(kind, Result.PARRY) ? Result.PARRY : Result.NONE;
        }
        if (state.phase == 0 || state.phase == 4) return Result.NONE;
        Result result = state.phase == 1 ? Result.PERFECT : state.phase == 2 ? Result.PARRY : Result.BLOCK;
        if (result == Result.BLOCK && !canBlock(kind) || result != Result.BLOCK && !canParry(kind, result)) return Result.NONE;
        if (result == Result.PERFECT) state.recharge = 0;
        else if (result == Result.PARRY) state.recharge = Math.max(0, GuardConfig.RECHARGE_TICKS.get() / 2);
        if (result == Result.PERFECT || result == Result.PARRY) {
            state.safetyThroughTick = GuardConfig.FOLLOW_UP_TICKS.get() == 0 ? -1 : player.level().getGameTime() + GuardConfig.FOLLOW_UP_TICKS.get();
            state.held = false;
            state.phase = 0;
            state.elapsed = 0;
            updateMovement(player, false);
        }
        sync(player, state);
        return result;
    }

    public static boolean isShieldBlocking(ServerPlayer player) {
        GuardState state = STATES.get(player.getUUID());
        return state != null && state.held && state.phase == 4 && player.isUsingItem()
            && player.getUsedItemHand() == state.guardHand && GuardItemRules.shieldLike(player.getUseItem());
    }

    public static void postponeShieldUse(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        GuardState state = STATES.get(player.getUUID());
        if (state != null && state.held && (state.phase == 1 || state.phase == 2)
            && event.getHand() == state.guardHand && GuardItemRules.shieldLike(state.heldItem)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    public static void wear(ServerPlayer player, Result result) {
        int base = switch (result) {
            case PERFECT -> GuardConfig.PERFECT_WEAR.get();
            case PARRY -> GuardConfig.PARRY_WEAR.get();
            case BLOCK -> GuardConfig.BLOCK_WEAR.get();
            default -> 0;
        };
        if (base <= 0) return;
        GuardState state = STATES.get(player.getUUID());
        InteractionHand hand = state == null ? InteractionHand.MAIN_HAND : state.guardHand;
        ItemStack stack = player.getItemInHand(hand);
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
        // Scale each hit to maximum durability, including the durability given to non-damageable items.
        int wear = Math.max(1, (int) Math.ceil(stack.getMaxDamage() * (base / 1000.0D)));
        stack.hurtAndBreak(wear, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    private static boolean canParry(SourceKind kind, Result result) {
        return switch (kind) {
            case MELEE, OTHER -> true;
            case FALL -> GuardConfig.FALL_PARRY.get();
            case EXPLOSION -> GuardConfig.PARRY_EXPLOSIONS.get() && (!GuardConfig.PERFECT_EXPLOSIONS_ONLY.get() || result == Result.PERFECT);
            case PROJECTILE_IMPACT -> GuardConfig.PARRY_PROJECTILES.get();
            case PROJECTILE_DAMAGE -> GuardConfig.PARRY_PROJECTILES.get();
        };
    }

    private static boolean canBlock(SourceKind kind) {
        return switch (kind) {
            case MELEE -> true;
            case FALL -> false;
            case PROJECTILE_IMPACT -> GuardConfig.BLOCK_DEFLECT_CHANCE.get() > 0;
            case EXPLOSION -> GuardConfig.BLOCK_EXPLOSIONS.get();
            case PROJECTILE_DAMAGE -> GuardConfig.BLOCK_PROJECTILES.get();
            case OTHER -> false;
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
        PacketDistributor.sendToPlayer(player, new GuardPackets.Status(state.phase, state.elapsed, state.recharge,
            GuardConfig.PARRY_TICKS.get(), GuardConfig.RECHARGE_TICKS.get(), state.guardHand == InteractionHand.OFF_HAND));
    }
}
