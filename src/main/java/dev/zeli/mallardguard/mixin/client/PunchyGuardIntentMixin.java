package dev.zeli.mallardguard.mixin.client;

import dev.zeli.mallardguard.client.PunchyGuardCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "punchy.client.animation.AnimationIntentCoordinator", remap = false)
public abstract class PunchyGuardIntentMixin {
    @Shadow(remap = false)
    public static void discardPendingForEquipTransition() {
        throw new AssertionError();
    }

    @Inject(method = "flush()V", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void mallardguard$guardPriority(CallbackInfo callback) {
        if (PunchyGuardCompat.beforeIntentFlush()) {
            discardPendingForEquipTransition();
            callback.cancel();
        }
    }
}
