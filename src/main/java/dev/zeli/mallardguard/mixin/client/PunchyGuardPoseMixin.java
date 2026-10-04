package dev.zeli.mallardguard.mixin.client;

import dev.zeli.mallardguard.client.PunchyGuardCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "punchy.client.animation.PoseHandler", remap = false)
public abstract class PunchyGuardPoseMixin {
    @Inject(method="applyTransformations(Ljava/util/Map;FZ)V",at=@At("RETURN"),require=1,remap=false)
    private void mallardguard$lowerEmptyHand(java.util.Map<String,net.minecraft.client.model.geom.ModelPart> parts,float partialTick,boolean mirrored,CallbackInfo callback){
        PunchyGuardCompat.applyOwnedArmOffsets(this,parts,partialTick);
    }

    @Inject(method="isDualHandedPoseActive",at=@At("HEAD"),cancellable=true,require=1,remap=false)
    private void mallardguard$bothHands(float partialTick,CallbackInfoReturnable<Boolean> callback){
        if(PunchyGuardCompat.ownsHandVisibility(this))callback.setReturnValue(true);
    }

    @Inject(method = "captureCurrentArmSamplesInto", at = @At("RETURN"), require = 0, remap = false)
    private void mallardguard$transitionSource(java.util.Map<String, Object> samples, net.minecraft.world.entity.HumanoidArm arm, CallbackInfo callback) {
        PunchyGuardCompat.preserveVisibleTransition(this, samples, arm);
    }

    @Inject(method = "shouldForceBlendInFromVisibleSource", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void mallardguard$visibleStart(CallbackInfoReturnable<Boolean> callback) {
        if (PunchyGuardCompat.captureVisibleStart(this)) callback.setReturnValue(true);
    }

    @Inject(method = "applyBlendInEasing", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void mallardguard$easing(float progress, CallbackInfoReturnable<Float> callback) {
        Float value = PunchyGuardCompat.transitionEasing(this, progress);
        if (value != null) callback.setReturnValue(value);
    }

    @Inject(method="getBlendOutProgress",at=@At("RETURN"),cancellable=true,require=1,remap=false)
    private void mallardguard$returnCurve(float partialTick,CallbackInfoReturnable<Float> callback){
        Float value=PunchyGuardCompat.returnEasing(this,partialTick);
        if(value!=null)callback.setReturnValue(value);
    }

    @ModifyVariable(method = "tick(FF)V", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 1, remap = false)
    private float mallardguard$guardSpeed(float speed) {
        return PunchyGuardCompat.guardPlaybackSpeed(this, speed);
    }

    @Inject(method = "tick(FF)V", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void mallardguard$guardPose(float delta, float speed, CallbackInfo callback) {
        PunchyGuardCompat.beforePoseTick(this);
        if(PunchyGuardCompat.pauseOwnedAnimation(this))callback.cancel();
    }
}
