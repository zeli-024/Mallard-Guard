package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.zeli.mallardguard.MallardGuard;
import dev.zeli.mallardguard.mixin.client.ItemInHandRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

@EventBusSubscriber(modid = MallardGuard.ID, value = Dist.CLIENT)
public final class GuardAnimationHandler {
    private GuardAnimationHandler() {}

    @SubscribeEvent
    public static void renderFirstPerson(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || event.getHand() != InteractionHand.MAIN_HAND || !GuardClient.isStanceActive()) return;
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !GuardClient.isCurrentMainHand(stack)) return;

        ItemInHandRenderer renderer = minecraft.getEntityRenderDispatcher().getItemInHandRenderer();
        HumanoidArm arm = player.getMainArm();
        boolean right = arm == HumanoidArm.RIGHT;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        ItemInHandRendererAccessor accessor = (ItemInHandRendererAccessor) renderer;
        accessor.mallardguard$applyItemArmTransform(pose, arm, event.getEquipProgress());
        accessor.mallardguard$applyItemArmAttackTransform(pose, arm, event.getSwingProgress());
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

    @SubscribeEvent
    public static void renderThirdPerson(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!GuardClient.isStanceActive() || !dev.zeli.mallardguard.GuardState.eligible(player)) return;
        HumanoidModel<?> model = (HumanoidModel<?>) event.getRenderer().getModel();
        if (player.getMainArm() == HumanoidArm.RIGHT) {
            model.rightArm.xRot -= (float) Math.PI * 2.0F / 10.0F;
            model.rightArm.yRot = -(float) Math.PI / 6.0F;
        } else {
            model.leftArm.xRot -= (float) Math.PI * 2.0F / 10.0F;
            model.leftArm.yRot = (float) Math.PI / 6.0F;
        }
    }
}
