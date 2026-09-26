package dev.zeli.mallardguard.mixin.client;

import dev.zeli.mallardguard.GuardState;
import dev.zeli.mallardguard.client.GuardClient;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
abstract class HumanoidModelMixin<T extends LivingEntity> extends AgeableListModel<T> {
    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;

    @Inject(method = "setupAnim", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/HumanoidModel;setupAttackAnimation(Lnet/minecraft/world/entity/LivingEntity;F)V"))
    private void mallardguard$applyGuardPose(T entity, float limbSwing, float limbSwingAmount,
                                               float ageInTicks, float netHeadYaw, float headPitch,
                                               CallbackInfo callback) {
        if (entity instanceof Player player && GuardClient.isStanceActive() && GuardState.eligible(player)) {
            if (player.getMainArm() == HumanoidArm.RIGHT) {
                rightArm.xRot -= (float) Math.PI * 2.0F / 10.0F;
                rightArm.yRot = -(float) Math.PI / 6.0F;
            } else {
                leftArm.xRot -= (float) Math.PI * 2.0F / 10.0F;
                leftArm.yRot = (float) Math.PI / 6.0F;
            }
        }
    }
}
