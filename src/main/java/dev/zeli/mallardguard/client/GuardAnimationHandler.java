package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.zeli.mallardguard.MallardGuard;
import dev.zeli.mallardguard.GuardItemRules;
import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.mixin.client.ItemInHandRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@EventBusSubscriber(modid = MallardGuard.ID, value = Dist.CLIENT)
public final class GuardAnimationHandler {
    private GuardAnimationHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderFirstPerson(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !GuardConfig.FIRST_PERSON_ANIMATION.get() || event.getHand() != GuardClient.guardHand(player)) return;
        ItemStack stack = event.getItemStack();
        // Let shield-like items keep their own first-person animation in every phase.
        if (stack.isEmpty() || GuardItemRules.shieldLike(stack) || !GuardClient.isCurrentGuardHand(stack)) return;

        ItemInHandRenderer renderer = minecraft.getEntityRenderDispatcher().getItemInHandRenderer();
        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean right = arm == HumanoidArm.RIGHT;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        ItemInHandRendererAccessor accessor = (ItemInHandRendererAccessor) renderer;
        accessor.mallardguard$applyItemArmTransform(pose, arm, event.getEquipProgress());
        accessor.mallardguard$applyItemArmAttackTransform(pose, arm, 0.0F);
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        pose.translate(direction * -0.14142136F, 0.08F, 0.14142136F);
        pose.mulPose(Axis.XP.rotationDegrees(-102.25F));
        pose.mulPose(Axis.YP.rotationDegrees(direction * 13.365F));
        pose.mulPose(Axis.ZP.rotationDegrees(direction * 78.05F));
        renderer.renderItem(player, stack,
                right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                !right, pose, event.getMultiBufferSource(), event.getPackedLight());
        pose.popPose();
        event.setCanceled(true);
    }

}
