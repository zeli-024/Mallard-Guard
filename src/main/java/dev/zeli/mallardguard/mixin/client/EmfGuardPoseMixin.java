package dev.zeli.mallardguard.mixin.client;
import dev.zeli.mallardguard.client.GuardThirdPerson;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** EMF animates during part rendering, after vanilla model setup. */
@Pseudo
@Mixin(targets = "traben.entity_model_features.models.parts.EMFModelPartRoot", remap = false)
abstract class EmfGuardPoseMixin {
    @Inject(method = {"animate", "animateNoPause"}, at = @At("RETURN"), require = 0, remap = false)
    private void mallardguard$afterAnimation(CallbackInfo ci) { GuardThirdPerson.afterEmfAnimation(); }
}
