package dev.zeli.mallardguard.mixin.client;

import dev.zeli.mallardguard.client.PunchyGuardCompat;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "punchy.client.animation.PunchyAnimationManager", remap = false)
public abstract class PunchyGuardReloadMixin {
    @Inject(method = "loadAnimations(Lnet/minecraft/server/packs/resources/ResourceManager;)V", at = @At("RETURN"), require = 0, remap = false)
    private static void mallardguard$reload(ResourceManager resources, CallbackInfo callback) {
        PunchyGuardCompat.resourcesReloaded();
    }
}
