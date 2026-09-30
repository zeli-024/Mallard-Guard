package dev.zeli.mallardguard.mixin.client;

import dev.zeli.mallardguard.client.GuardClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep our active shield stance raised when vanilla releases the Use Item key. */
@Mixin(MultiPlayerGameMode.class)
public abstract class MinecraftShieldReleaseMixin {
    @Inject(method = "releaseUsingItem", at = @At("HEAD"), cancellable = true)
    private void mallardguard$keepGuardShieldRaised(Player player, CallbackInfo ci) {
        if (GuardClient.maintainGuardShieldUse()) ci.cancel();
    }
}
