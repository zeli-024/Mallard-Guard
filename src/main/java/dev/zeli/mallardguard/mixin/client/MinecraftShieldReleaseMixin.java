package dev.zeli.mallardguard.mixin.client;

import dev.zeli.mallardguard.client.GuardClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Vanilla's Use Item key may differ from the dedicated Guard key. */
@Mixin(Minecraft.class)
public abstract class MinecraftShieldReleaseMixin {
    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;releaseUsingItem(Lnet/minecraft/world/entity/player/Player;)V"))
    private void mallardguard$keepReboundGuardShield(MultiPlayerGameMode gameMode, Player player) {
        if (!GuardClient.maintainGuardShieldUse()) gameMode.releaseUsingItem(player);
    }
}
