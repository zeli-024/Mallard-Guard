package dev.zeli.mallardguard.mixin.client;
import dev.zeli.mallardguard.client.GuardFeint;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs after Better Combat has actually cancelled its swing, not when a key is merely pressed. */
@Mixin(value = Minecraft.class, priority = 400)
abstract class BetterCombatFeintMixin {
    @Inject(method = "cancelWeaponSwing", at = @At("RETURN"), remap = false, require = 0)
    private void mallardguard$feinted(CallbackInfo ci) { GuardFeint.cancelled(); }
}
