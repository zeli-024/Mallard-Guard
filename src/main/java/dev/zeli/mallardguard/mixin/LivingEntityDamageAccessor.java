package dev.zeli.mallardguard.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityDamageAccessor {
    @Accessor("lastHurt") float mallardguard$getLastHurt();
    @Accessor("lastHurt") void mallardguard$setLastHurt(float value);
}
