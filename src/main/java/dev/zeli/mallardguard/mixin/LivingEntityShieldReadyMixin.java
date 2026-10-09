package dev.zeli.mallardguard.mixin;

import dev.zeli.mallardguard.GuardState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mallard Guard's shield blocking follows a successful parry without a second startup delay. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityShieldReadyMixin {
    @Inject(method="isBlocking",at=@At("HEAD"),cancellable=true)
    private void mallardguard$readyShield(CallbackInfoReturnable<Boolean> callback){
        if((Object)this instanceof ServerPlayer player && GuardState.isShieldBlocking(player))callback.setReturnValue(true);
    }
}
