package dev.zeli.mallardguard.mixin.client;

import dev.zeli.mallardguard.client.PunchyGuardCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep native equip delays from hiding an arm during a guard-owned transition. */
@Pseudo
@Mixin(targets="punchy.client.render.PunchyArmRenderer",remap=false)
public abstract class PunchyGuardRendererMixin {
    @Inject(method="renderFirstPerson(Lnet/minecraft/client/renderer/ItemInHandRenderer;Lnet/minecraft/client/player/LocalPlayer;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("HEAD"),require=1,remap=false)
    private static void mallardguard$handVisibility(net.minecraft.client.renderer.ItemInHandRenderer renderer,net.minecraft.client.player.LocalPlayer player,float partialTick,com.mojang.blaze3d.vertex.PoseStack poses,net.minecraft.client.renderer.MultiBufferSource buffers,int light,CallbackInfo callback){
        PunchyGuardCompat.prepareOwnedHandVisibility();
    }
}
