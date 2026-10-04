package dev.zeli.mallardguard.mixin.client;

import dev.zeli.mallardguard.client.PunchyGuardCompat;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Guard clips own their hand visibility until their release completes. */
@Pseudo
@Mixin(targets="punchy.client.PunchyClient",remap=false)
public abstract class PunchyGuardVisibilityMixin {
    @Inject(method="shouldSuppressOffhandRender(Lnet/minecraft/world/entity/player/Player;)Z",at=@At("HEAD"),cancellable=true,require=1,remap=false)
    private static void mallardguard$visible(Player player,CallbackInfoReturnable<Boolean> callback){
        if(player!=net.minecraft.client.Minecraft.getInstance().player)return;
        if(PunchyGuardCompat.ownsHandVisibility())callback.setReturnValue(false);
        else if(PunchyGuardCompat.hideLoweredOffhand())callback.setReturnValue(true);
    }
    @Inject(method="shouldLowerOffhand(Lnet/minecraft/client/player/LocalPlayer;)Z",at=@At("HEAD"),cancellable=true,require=1,remap=false)
    private static void mallardguard$lower(LocalPlayer player,CallbackInfoReturnable<Boolean> callback){
        if(player==net.minecraft.client.Minecraft.getInstance().player&&PunchyGuardCompat.ownsHandVisibility())callback.setReturnValue(false);
    }
}
