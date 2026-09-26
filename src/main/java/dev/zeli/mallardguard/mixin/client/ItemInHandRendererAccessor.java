package dev.zeli.mallardguard.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererAccessor {
    @Invoker("applyItemArmTransform")
    void mallardguard$applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float equipProgress);

    @Invoker("applyItemArmAttackTransform")
    void mallardguard$applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float swingProgress);
}
