package dev.zeli.mallardguard;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Bounded combat behaviors for supported goal-driven humanoids. State never retains a world or target entity. */
public final class GuardMobState {
    private static final java.util.Set<Mob> GOALS_ADDED = java.util.Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<Mob, State> STATES = new WeakHashMap<>();
    private record ProjectileDefense(java.util.UUID defender, GuardState.Result result, long tick) {}
    private static final Map<net.minecraft.world.entity.projectile.Projectile, ProjectileDefense> PROJECTILE_DEFENSES = new WeakHashMap<>();
    public static void projectileResult(net.minecraft.world.entity.projectile.Projectile projectile, Mob mob, GuardState.Result result) {
        PROJECTILE_DEFENSES.put(projectile, new ProjectileDefense(mob.getUUID(), result, mob.level().getGameTime()));
    }
    private static final String EQUIPMENT_ROLLED = "MallardGuardEquipmentRoll";
    private enum Behavior { IDLE, APPROACH, TACTICAL, PRESSURE, COUNTER }
    private static final net.minecraft.resources.ResourceLocation MOB_SLOWDOWN = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "mob_guard_slowdown");
    private static void movement(Mob mob, boolean active) {
        var speed = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        double amount = active ? GuardConfig.MOB_MOVEMENT.get() / 100.0D - 1 : 0;
        var old = speed.getModifier(MOB_SLOWDOWN);
        if (old != null && old.amount() == amount) return;
        if (old != null) speed.removeModifier(MOB_SLOWDOWN);
        if (amount != 0) speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(MOB_SLOWDOWN, amount, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
    private enum Movement { STILL, STRAFE }
    private static final Movement[] MOVEMENTS = Movement.values();
    private static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> COMMON_BOSSES = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "bosses"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> FORGE_BOSSES = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "bosses"));
    private static final class State {
        long rechargeUntil, guardUntil, shieldDisabledUntil, startedAt, nextAttempt, lastPressureHit;
        long behaviorUntil, nextRaise, tacticalReady, nextDecision, attackReady;
        int pressureHits, blocks, chains;
        java.util.UUID target;
        Behavior behavior = Behavior.IDLE;
        Movement movement = Movement.STILL;
        float side;
        double radius, approachDistance;
        boolean guarding, parryStance, approached;
        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack item = ItemStack.EMPTY;
    }
    private static String cachedIds = "";
    private static java.util.Set<net.minecraft.resources.ResourceLocation> extraIds = java.util.Set.of();
    private static String excludedText = "";
    private static java.util.Set<String> excluded = java.util.Set.of();
    private static boolean blacklisted(Mob mob) {
        String text = GuardConfig.MOB_BLACKLIST.get();
        if (!text.equals(excludedText)) { excluded = java.util.Set.copyOf(java.util.Arrays.stream(text.split(",")).map(String::trim).filter(v -> !v.isEmpty()).toList()); excludedText = text; }
        return excluded.contains(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString());
    }
    private static boolean supported(Mob mob) {
        if (blacklisted(mob)) return false;
        if (mob instanceof net.minecraft.world.entity.boss.wither.WitherBoss
            || mob instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
            || mob.getType().is(COMMON_BOSSES)
            || mob.getType().is(FORGE_BOSSES)) return false;
        String rules = GuardConfig.MOB_HUMANOID_IDS.get();
        if (!rules.equals(cachedIds)) {
            var ids = new java.util.HashSet<net.minecraft.resources.ResourceLocation>();
            for (String value : rules.split(",")) {
                var id = net.minecraft.resources.ResourceLocation.tryParse(value.trim()); if (id != null) ids.add(id);
            }
            extraIds = java.util.Set.copyOf(ids); cachedIds = rules;
        }
        var id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return extraIds.contains(id) || id.getNamespace().equals("minecraft") && (mob instanceof Zombie || mob instanceof AbstractSkeleton || mob instanceof net.minecraft.world.entity.monster.Vindicator);
    }
    private GuardMobState() {}

    public static InteractionHand defenseHand(Mob mob) { State s = STATES.get(mob); return s == null ? InteractionHand.MAIN_HAND : s.hand; }

    public static InteractionHand guardHand(Mob mob) {
        State state = STATES.get(mob);
        return state != null && state.guarding ? state.hand : null;
    }

    private static boolean ranged(Mob mob) {
        return mob.getMainHandItem().is(Items.BOW) || mob.getMainHandItem().is(Items.CROSSBOW)
            || mob.getOffhandItem().is(Items.BOW) || mob.getOffhandItem().is(Items.CROSSBOW);
    }
    private static boolean eligible(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(Items.BOW) && !stack.is(Items.CROSSBOW) && !GuardItemRules.consumable(stack)
            && !GuardItemRules.matches(stack, GuardConfig.EXCLUDED_ITEMS.get())
            && (GuardItemRules.shieldLike(stack) || GuardItemRules.hasAttackDamage(stack)
                || GuardItemRules.matches(stack, GuardConfig.INCLUDED_ITEMS.get()));
    }

    public static GuardState.Result react(Mob mob, Vec3 origin, boolean parryAllowed, boolean projectile, boolean explosion) {
        if (mob.level().isClientSide || !GuardConfig.MOB_GUARD.get() || !supported(mob) || !mob.isAlive() || mob.isNoAi() || GuardCombatEffects.stunned(mob)
            || origin == null || ranged(mob)) return GuardState.Result.NONE;
        Vec3 toward = origin.subtract(mob.getEyePosition());
        int angle = GuardConfig.FACING_ANGLE.get();
        if (angle < 360 && toward.lengthSqr() > 1.0E-6D
            && mob.getLookAngle().dot(toward.normalize()) < Math.cos(Math.toRadians(angle / 2.0D))) return GuardState.Result.NONE;
        State previous = STATES.get(mob);
        long now = mob.level().getGameTime();
        if (previous == null || !previous.guarding || now >= previous.guardUntil || now < previous.rechargeUntil
            || mob.getItemInHand(previous.hand) != previous.item) return GuardState.Result.NONE;
        InteractionHand hand = previous.hand;
        int perfectTicks = GuardConfig.MOB_PERFECT_TICKS.get();
        int parryTicks = (GuardConfig.MOB_PERFECT_TICKS.get() + GuardConfig.MOB_PARRY_TICKS.get());
        long elapsed = now - previous.startedAt;
        boolean canParry = previous.parryStance && parryAllowed && GuardConfig.PARRY.get() && elapsed < parryTicks
            && (!projectile || GuardConfig.PARRY_PROJECTILES.get()) && (!explosion || GuardConfig.PARRY_EXPLOSIONS.get());
        boolean perfect = canParry && elapsed < perfectTicks;
        if (explosion && GuardConfig.PERFECT_EXPLOSIONS_ONLY.get() && !perfect) canParry = false;
        boolean canBlock = GuardConfig.MOB_BLOCKING.get() && GuardConfig.BLOCK.get()
            && (!previous.parryStance || elapsed >= parryTicks)
            && (!projectile || GuardConfig.BLOCK_PROJECTILES.get()) && (!explosion || GuardConfig.BLOCK_EXPLOSIONS.get());
        GuardState.Result result = canParry ? perfect ? GuardState.Result.PERFECT : GuardState.Result.PARRY
            : canBlock ? GuardState.Result.BLOCK : GuardState.Result.NONE;
        if (result == GuardState.Result.NONE) return result;
        State state = previous;
        ItemStack item = mob.getItemInHand(hand);
        if (state.item != item) state.blocks = 0;
        state.item = item; state.hand = hand;
        boolean shield = GuardItemRules.shieldLike(item);
        int recharge = (shield ? GuardConfig.SHIELD_RECHARGE_TICKS : GuardConfig.RECHARGE_TICKS).get();
        if (result == GuardState.Result.BLOCK) {
            int max = (shield ? GuardConfig.SHIELD_MAX_BLOCKS : GuardConfig.TOOL_MAX_BLOCKS).get();
            if (max > 0 && ++state.blocks >= max) {
                state.rechargeUntil = now + Math.max(recharge, GuardConfig.SHIELD_BREAK_TICKS.get());
                state.blocks = 0;
            }
        } else {
            state.rechargeUntil = now + (result == GuardState.Result.PERFECT ? 1 : Math.max(1, recharge / 2));
            state.blocks = 0;
        }
        if (result != GuardState.Result.BLOCK) counter(mob, state);
        else if (state.rechargeUntil > now) finish(mob, state);
        return result;
    }

    public static void incoming(LivingIncomingDamageEvent event, Mob mob) {
        if (mob.level().isClientSide) return;
        DamageSource source = event.getSource();
        // Mobs react to combat, not environmental ticks such as hunger or burning.
        if (source.getEntity() == null && !source.is(DamageTypeTags.IS_EXPLOSION)) return;
        Vec3 origin = source.getSourcePosition();
        if (origin == null && source.getDirectEntity() != null) origin = source.getDirectEntity().position();
        State previous = STATES.get(mob);
        if (previous != null && previous.guarding && (!previous.parryStance || mob.level().getGameTime() - previous.startedAt >= GuardConfig.MOB_PERFECT_TICKS.get() + GuardConfig.MOB_PARRY_TICKS.get()) && GuardItemRules.shieldLike(previous.item)
            && source.getDirectEntity() instanceof LivingEntity attacker && (attacker.canDisableShield() || attacker.getMainHandItem().canDisableShield(previous.item, mob, attacker))) {
            previous.shieldDisabledUntil = mob.level().getGameTime() + GuardConfig.SHIELD_BREAK_TICKS.get();
            previous.rechargeUntil = previous.shieldDisabledUntil;
            finish(mob, previous);
            return;
        }
        ProjectileDefense remembered = source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile
            ? PROJECTILE_DEFENSES.remove(projectile) : null;
        GuardState.Result result = remembered != null && remembered.tick() == mob.level().getGameTime() && remembered.defender().equals(mob.getUUID()) ? remembered.result()
            : react(mob, origin, GuardDamageRules.canParry(source), source.is(DamageTypeTags.IS_PROJECTILE), source.is(DamageTypeTags.IS_EXPLOSION));
        if (result == GuardState.Result.NONE) return;
        State state = STATES.get(mob);
        boolean shield = GuardItemRules.shieldLike(state.item);
        if (shield && result == GuardState.Result.BLOCK && source.getDirectEntity() instanceof LivingEntity attacker
            && (attacker.canDisableShield() || attacker.getMainHandItem().canDisableShield(state.item, mob, attacker))) {
            state.shieldDisabledUntil = mob.level().getGameTime() + Math.max(1, GuardConfig.SHIELD_BREAK_TICKS.get());
            state.rechargeUntil = state.shieldDisabledUntil;
            finish(mob, state);
            return;
        }
        GuardEffects.onMobHit(mob, source, result, shield);
        wear(mob, state, result, event.getAmount());
        if (result == GuardState.Result.BLOCK) {
            if (shield) event.setCanceled(true);
            else event.setAmount(event.getAmount() * (1.0F - GuardConfig.BLOCK_REDUCTION.get().floatValue()));
        } else {
            event.setCanceled(true);
            GuardCombatEffects.pushDefender(mob, origin, shield);
            if (source.getEntity() instanceof LivingEntity attacker && attacker != mob) {
                GuardCombatEffects.pushAttacker(mob, attacker, shield);
                if (GuardConfig.MOB_RETALIATION.get() && source.getDirectEntity() == attacker) {
                    float multiplier = shield ? result == GuardState.Result.PERFECT ? GuardConfig.SHIELD_RETALIATION_PERCENT.get() / 100.0F : 0
                        : (result == GuardState.Result.PERFECT ? GuardConfig.PERFECT_RETALIATION : GuardConfig.PARRY_RETALIATION).get().floatValue();
                    if (multiplier > 0) MallardGuard.returnDamage(mob, attacker, event.getAmount() * multiplier);
                }
            }
        }
    }

    public static void wear(Mob mob, GuardState.Result result, float amount) {
        State state = STATES.get(mob);
        if (state != null) wear(mob, state, result, amount);
    }
    private static void wear(Mob mob, State state, GuardState.Result result, float amount) {
        if (!state.item.isDamageableItem()) return;
        boolean shield = GuardItemRules.shieldLike(state.item);
        int percent = (result == GuardState.Result.PERFECT ? GuardConfig.PERFECT_WEAR
            : result == GuardState.Result.PARRY ? GuardConfig.PARRY_WEAR : GuardConfig.BLOCK_WEAR).get();
        int wear = result == GuardState.Result.BLOCK && shield ? amount >= 3 ? 1 + (int) Math.floor(amount) : 0
            : percent == 0 ? 0 : Math.max(1, (int) Math.ceil(state.item.getMaxDamage() * percent / 1000.0D));
        if (wear > 0) state.item.hurtAndBreak(wear, mob, state.hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
    }

    private static LivingEntity target(Mob mob, State state) {
        if (state.target != null && mob.level() instanceof net.minecraft.server.level.ServerLevel level
            && level.getEntity(state.target) instanceof LivingEntity living && living.isAlive()) return living;
        return mob.getTarget() != null && mob.getTarget().isAlive() ? mob.getTarget() : null;
    }
    private static void stopMovement(Mob mob) {
        mob.getNavigation().stop(); mob.getMoveControl().strafe(0, 0);
        mob.setXxa(0); mob.setZza(0); mob.setSpeed(0);
    }
    private static void pose(Mob mob, State state, boolean raised) {
        if (state.guarding == raised) return;
        state.guarding = raised; GuardPoses.broadcast(mob, raised ? state.hand : null);
    }
    private static void finish(Mob mob, State state) {
        if (state.behavior != Behavior.IDLE) state.nextAttempt = Math.max(state.nextAttempt, mob.level().getGameTime()
            + (GuardItemRules.shieldLike(state.item) ? GuardConfig.SHIELD_RECHARGE_TICKS : GuardConfig.RECHARGE_TICKS).get());
        pose(mob, state, false); state.behavior = Behavior.IDLE;
        state.behaviorUntil = state.nextRaise = 0;
        stopMovement(mob); movement(mob, false);
    }
    private static boolean chooseHand(Mob mob, State state) {
        boolean main = eligible(mob.getMainHandItem()), off = eligible(mob.getOffhandItem());
        if (mob.level().getGameTime() < state.shieldDisabledUntil) {
            main &= !GuardItemRules.shieldLike(mob.getMainHandItem()); off &= !GuardItemRules.shieldLike(mob.getOffhandItem());
        }
        if (!main && !off) return false;
        state.hand = off && (GuardItemRules.shieldLike(mob.getOffhandItem()) || !main) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack item = mob.getItemInHand(state.hand); if (state.item != item) state.blocks = 0;
        state.item = item; return true;
    }
    private static void raise(Mob mob, State state) {
        long now = mob.level().getGameTime(); state.startedAt = now;
        state.parryStance = GuardConfig.PARRY.get();
        state.guardUntil = now + GuardConfig.MOB_PERFECT_TICKS.get() + GuardConfig.MOB_PARRY_TICKS.get()
            + (GuardConfig.MOB_BLOCKING.get() && GuardConfig.BLOCK.get() ? 20 : 0);
        pose(mob, state, true);
    }
    private static boolean begin(Mob mob, State state, LivingEntity target, Behavior behavior) {
        if (target == null || !target.isAlive() || !chooseHand(mob, state) || mob.isPassenger()) return false;
        long now = mob.level().getGameTime();
        state.target = target.getUUID(); state.behavior = behavior; state.pressureHits = 0;
        state.side = mob.getRandom().nextBoolean() ? 0.65F : -0.65F;
        state.radius = Math.max(1.2D, Math.hypot(target.getX() - mob.getX(), target.getZ() - mob.getZ()));
        state.movement = behavior == Behavior.TACTICAL
            ? MOVEMENTS[mob.getRandom().nextInt(MOVEMENTS.length)] : Movement.STILL;
        raise(mob, state);
        if (behavior == Behavior.TACTICAL) {
            int maximum = GuardConfig.MOB_TACTICAL_SECONDS.get();
            state.behaviorUntil = now + (1 + mob.getRandom().nextInt(maximum)) * 20;
            state.tacticalReady = state.behaviorUntil + GuardConfig.MOB_TACTICAL_COOLDOWN.get();
        } else if (behavior == Behavior.APPROACH) state.behaviorUntil = now + (1 + mob.getRandom().nextInt(GuardConfig.MOB_APPROACH_SECONDS.get())) * 20;
        else state.behaviorUntil = state.guardUntil;
        // Stop conflicting interruptible goals immediately; keep their registrations intact.
        for (var goal : mob.goalSelector.getAvailableGoals()) {
            if (!goal.isRunning() || !goal.isInterruptable() || goal.getGoal() instanceof GuardBehaviorGoal) continue;
            var flags = goal.getFlags();
            if (flags.contains(net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE)
                || flags.contains(net.minecraft.world.entity.ai.goal.Goal.Flag.LOOK)
                || flags.contains(net.minecraft.world.entity.ai.goal.Goal.Flag.JUMP)) goal.stop();
        }
        mob.stopUsingItem(); mob.setAggressive(false);
        movement(mob, true); stopMovement(mob);
        return true;
    }
    private static void counter(Mob mob, State state) {
        LivingEntity target = target(mob, state); long now = mob.level().getGameTime();
        finish(mob, state);
        if (target == null) return;
        state.nextAttempt = now + (GuardItemRules.shieldLike(state.item) ? GuardConfig.SHIELD_RECHARGE_TICKS : GuardConfig.RECHARGE_TICKS).get();
        // At most one immediate follow-up stance before committing to a counter.
        boolean rush = mob.getRandom().nextInt(100) < GuardConfig.MOB_RUSH_CHANCE.get();
        if (!rush && state.chains == 0 && mob.getRandom().nextBoolean() && begin(mob, state, target, Behavior.PRESSURE)) {
            state.chains = 1; state.rechargeUntil = now; return;
        }
        state.chains = 0;
        int ticks = GuardConfig.MOB_COUNTER_TICKS.get(); if (!rush || ticks == 0) return;
        state.behavior = Behavior.COUNTER; state.behaviorUntil = now + ticks;
        state.side = mob.getRandom().nextBoolean() ? 0 : mob.getRandom().nextBoolean() ? 0.65F : -0.65F;
    }
    public static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) return;
        State state = STATES.get(mob); long now = mob.level().getGameTime();
        if (!GuardConfig.MOB_GUARD.get() || !mob.isAlive() || mob.isNoAi() || GuardCombatEffects.stunned(mob) || !supported(mob) || ranged(mob)
            || !GuardConfig.PARRY.get() && (!GuardConfig.MOB_BLOCKING.get() || !GuardConfig.BLOCK.get())) {
            if (state != null) { if (state.behavior != Behavior.IDLE) finish(mob, state); STATES.remove(mob); } return;
        }
        if (!eligible(mob.getMainHandItem()) && !eligible(mob.getOffhandItem())) {
            if (state != null) { if (state.behavior != Behavior.IDLE) finish(mob, state); STATES.remove(mob); }
            return;
        }
        if (GOALS_ADDED.add(mob)) mob.goalSelector.addGoal(-1, new GuardBehaviorGoal(mob));
        LivingEntity target = state == null || state.behavior == Behavior.IDLE && mob.getTarget() != null ? mob.getTarget() : target(mob, state);
        if (target == null || !target.isAlive() || mob.distanceToSqr(target) > 256) {
            if (state != null) { if (state.behavior != Behavior.IDLE) finish(mob, state); STATES.remove(mob); } return;
        }
        if (state == null) { state = new State(); state.approachDistance = 1 + mob.getRandom().nextDouble() * (GuardConfig.MOB_APPROACH_DISTANCE.get() - 1); state.target = target.getUUID(); STATES.put(mob, state); }
        if (state.behavior != Behavior.IDLE) {
            if (now >= state.behaviorUntil || mob.getItemInHand(state.hand) != state.item) { finish(mob, state); state.chains = 0; return; }
            if (state.behavior != Behavior.COUNTER && now >= state.guardUntil && state.guarding) {
                if (state.behavior == Behavior.TACTICAL || state.behavior == Behavior.APPROACH) { pose(mob, state, false); state.nextRaise = now + 5 + mob.getRandom().nextInt(6); }
                else { finish(mob, state); return; }
            }
            if ((state.behavior == Behavior.TACTICAL || state.behavior == Behavior.APPROACH) && !state.guarding && now >= state.nextRaise) raise(mob, state);
            return;
        }
        if (!target.getUUID().equals(state.target)) { state.target = target.getUUID(); state.approached = false; state.approachDistance = 1 + mob.getRandom().nextDouble() * (GuardConfig.MOB_APPROACH_DISTANCE.get() - 1); state.pressureHits = 0; }
        if ((mob.tickCount + mob.getId()) % 5 != 0 || now < state.nextAttempt || now < state.rechargeUntil) return;
        double difficulty = GuardConfig.MOB_DIFFICULTY.get() / 100.0D;
        if (!state.approached && mob.distanceToSqr(target) <= Math.max(1, state.approachDistance * state.approachDistance)) {
            state.approached = true;
            if (mob.getRandom().nextDouble() * 100 < GuardConfig.MOB_APPROACH_CHANCE.get() * difficulty && begin(mob, state, target, Behavior.APPROACH)) return;
        }
        if (now < state.nextDecision) return;
        state.nextDecision = now + 40 + mob.getRandom().nextInt(41);
        if (now >= state.tacticalReady && mob.getRandom().nextDouble() * 100 < GuardConfig.MOB_TACTICAL_CHANCE.get() * difficulty)
            begin(mob, state, target, Behavior.TACTICAL);
    }
    public static void damaged(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide || !GuardConfig.MOB_GUARD.get() || !supported(mob) || ranged(mob)
            || !mob.isAlive() || mob.isNoAi() || GuardCombatEffects.stunned(mob) || event.getNewDamage() <= 0
            || !GuardConfig.PARRY.get() && (!GuardConfig.MOB_BLOCKING.get() || !GuardConfig.BLOCK.get())
            || !(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        State state = STATES.computeIfAbsent(mob, ignored -> { State fresh = new State(); fresh.approachDistance = 1 + mob.getRandom().nextDouble() * (GuardConfig.MOB_APPROACH_DISTANCE.get() - 1); return fresh; });
        if (state.behavior != Behavior.IDLE || !chooseHand(mob, state)) return;
        long now = mob.level().getGameTime();
        if (now - state.lastPressureHit > 200) state.pressureHits = 0;
        state.lastPressureHit = now; state.target = attacker.getUUID(); state.pressureHits = Math.min(100, state.pressureHits + 1);
        int threshold = GuardConfig.MOB_HITS_TO_GUARD.get();
        if (state.pressureHits < threshold || now < state.rechargeUntil || now < state.nextAttempt) return;
        double chance = Math.min(100, 40.0D * state.pressureHits / threshold) * GuardConfig.MOB_DIFFICULTY.get() / 100.0D;
        if (mob.getRandom().nextDouble() * 100 < chance) { state.chains = 0; begin(mob, state, attacker, Behavior.PRESSURE); }
    }
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Mob mob) {
            State state = STATES.remove(mob); if (state != null && state.behavior != Behavior.IDLE) finish(mob, state);
        }
    }
    public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        for (var entry : STATES.entrySet()) if (entry.getValue().behavior != Behavior.IDLE) finish(entry.getKey(), entry.getValue());
        STATES.clear(); GOALS_ADDED.clear(); PROJECTILE_DEFENSES.clear(); extraIds = java.util.Set.of(); cachedIds = "";
    }
    public static void join(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && GuardConfig.MOB_GUARD.get() && event.getEntity() instanceof Mob mob && supported(mob) && GOALS_ADDED.add(mob))
            mob.goalSelector.addGoal(-1, new GuardBehaviorGoal(mob));
    }
    private static final class GuardBehaviorGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final Mob mob;
        GuardBehaviorGoal(Mob mob) { this.mob = mob; setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
        @Override public boolean canUse() {
            State state = STATES.get(mob); return GuardConfig.MOB_GUARD.get() && mob.isAlive() && !mob.isNoAi()
                && !GuardCombatEffects.stunned(mob) && state != null && state.behavior != Behavior.IDLE && target(mob, state) != null;
        }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() { mob.getNavigation().stop(); mob.stopUsingItem(); mob.setAggressive(false); }
        @Override public void stop() { stopMovement(mob); }
        @Override public void tick() {
            State state = STATES.get(mob); if (state == null || state.behavior == Behavior.IDLE) return;
            LivingEntity target = target(mob, state); if (target == null) return;
            mob.getNavigation().stop(); mob.setAggressive(false);
            double dx = target.getX() - mob.getX(), dz = target.getZ() - mob.getZ(), distance = Math.hypot(dx, dz);
            float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
            mob.setYRot(yaw); mob.yBodyRot = yaw; mob.getLookControl().setLookAt(target, 30, 30);
            boolean counter = state.behavior == Behavior.COUNTER;
            if (!counter) { mob.swinging = false; mob.swingTime = 0; mob.attackAnim = 0; }
            float forward = 0, side = 0;
            if (!counter && state.behavior == Behavior.APPROACH) forward = distance > 1.1D ? 1.0F : 0;
            else if (!counter && state.movement == Movement.STRAFE) {
                side = Math.signum(state.side) * 0.90F;
                forward = (float) net.minecraft.util.Mth.clamp((distance - state.radius) * 0.5D, -0.12D, 0.12D);
            }
            if (counter) {
                // Direct, bounded approach; normal collision and move control still apply.
                double offset = state.side * Math.min(0.8D, distance * 0.3D);
                double divisor = Math.max(0.001D, distance);
                mob.getMoveControl().setWantedPosition(target.getX() - dz / divisor * offset,
                    target.getY(), target.getZ() + dx / divisor * offset, 1.35D);
            } else { movement(mob, true); mob.getMoveControl().strafe(forward, side); }
            long now = mob.level().getGameTime();
            if (counter && now >= state.attackReady && mob.isWithinMeleeAttackRange(target) && mob.getSensing().hasLineOfSight(target)) {
                state.attackReady = now + 20; mob.swing(state.hand); mob.doHurtTarget(target); finish(mob, state);
            }
        }
    }

    public static void equip(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || event.loadedFromDisk() || !GuardConfig.MOB_GUARD.get()
            || !(event.getEntity() instanceof Mob mob) || blacklisted(mob) || !net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getNamespace().equals("minecraft")
            || !(mob instanceof Zombie || mob instanceof AbstractSkeleton || mob instanceof net.minecraft.world.entity.monster.Vindicator || mob instanceof net.minecraft.world.entity.monster.Pillager)
            || mob.getPersistentData().getBoolean(EQUIPMENT_ROLLED)) return;
        mob.getPersistentData().putBoolean(EQUIPMENT_ROLLED, true);
        if (mob instanceof Zombie || mob instanceof AbstractSkeleton) {
            if (mob.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) { mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE)); mob.setDropChance(EquipmentSlot.CHEST, 0); }
            if (mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) { mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET)); mob.setDropChance(EquipmentSlot.HEAD, 0); }
        }
        var held = mob.getMainHandItem();
        if (!mob.getOffhandItem().isEmpty() || !held.isEmpty() && !held.is(Items.BOW) && !held.is(Items.CROSSBOW) && !held.is(Items.IRON_AXE)) return;
        if (mob.getRandom().nextInt(100) >= GuardConfig.MOB_GEAR_CHANCE.get()) return;
        if (mob instanceof net.minecraft.world.entity.monster.Pillager) { mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW)); return; }
        boolean skeleton = mob instanceof AbstractSkeleton;
        int weapon = mob.getRandom().nextInt(skeleton ? 4 : 3);
        if (weapon == 3) { mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW)); return; }
        int roll = mob.getRandom().nextInt(100), tier = roll < 60 ? 0 : roll < 90 ? 1 : roll < 95 ? 2 : 3;
        var item = switch (weapon) {
            case 0 -> tier == 0 ? Items.STONE_SWORD : tier == 1 ? Items.IRON_SWORD : tier == 2 ? Items.GOLDEN_SWORD : Items.DIAMOND_SWORD;
            case 1 -> tier == 0 ? Items.STONE_AXE : tier == 1 ? Items.IRON_AXE : tier == 2 ? Items.GOLDEN_AXE : Items.DIAMOND_AXE;
            default -> tier == 0 ? Items.STONE_PICKAXE : tier == 1 ? Items.IRON_PICKAXE : tier == 2 ? Items.GOLDEN_PICKAXE : Items.DIAMOND_PICKAXE;
        };
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
        if (mob.getRandom().nextInt(100) < 35) mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
    }
}
