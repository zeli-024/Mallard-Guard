package dev.zeli.mallardguard.mixin;

import dev.zeli.mallardguard.GuardMobState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.monster.piglin.Piglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Pause competing piglin Brain actions while the bounded guard goal owns combat. */
@Mixin(Piglin.class)
public abstract class PiglinGuardMixin {
    @Redirect(method="customServerAiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/ai/Brain;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void mallardguard$brainTick(Brain<Piglin> brain,ServerLevel level,LivingEntity entity){
        Piglin piglin=(Piglin)entity;
        if(!GuardMobState.controlsAi(piglin))brain.tick(level,piglin);
    }
}
