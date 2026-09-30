package dev.zeli.mallardguard.client;

import java.util.HashMap;
import java.util.Map;
import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.GuardPackets;
import dev.zeli.mallardguard.MallardGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

@EventBusSubscriber(modid = MallardGuard.ID, value = Dist.CLIENT)
public final class GuardThirdPerson {
    private static final Map<Integer, InteractionHand> HANDS = new HashMap<>();
    private static ClientLevel world;
    private static HumanoidModel<?> renderingModel;
    private static LivingEntity renderingEntity;
    public static void begin(net.minecraft.client.model.EntityModel<?> model, LivingEntity entity) {
        renderingModel = model instanceof HumanoidModel<?> humanoid ? humanoid : null;
        renderingEntity = entity;
    }
    public static void end() { renderingModel = null; renderingEntity = null; }
    public static void afterEmfAnimation() {
        if (renderingModel != null && renderingEntity != null) apply(renderingModel, renderingEntity);
    }

    private GuardThirdPerson() {}
    public static void clear() { HANDS.clear(); world = null; end(); }
    private static void checkWorld() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current != world) { HANDS.clear(); world = current; }
    }
    public static void pose(GuardPackets.GuardPose data) {
        checkWorld();
        if (data.active()) HANDS.put(data.entityId(), data.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        else HANDS.remove(data.entityId());
    }
    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide) { HANDS.remove(event.getEntity().getId()); }
    }
    public static void apply(HumanoidModel<?> model, LivingEntity entity) {
        checkWorld();
        if (!GuardConfig.THIRD_PERSON_ANIMATION.get() || !entity.isAlive()) return;
        InteractionHand hand = entity instanceof Player player && player == Minecraft.getInstance().player
            ? GuardClient.thirdPersonHand() : HANDS.get(entity.getId());
        if (hand == null) return;
        if (entity instanceof net.minecraft.world.entity.Mob) {
            model.attackTime = 0;
            model.rightArm.xRot = model.leftArm.xRot = 0;
            model.rightArm.yRot = model.leftArm.yRot = 0;
            model.rightArm.zRot = model.leftArm.zRot = 0;
        }
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? entity.getMainArm() : entity.getMainArm().getOpposite();
        var part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        part.xRot = -1.25F + model.head.xRot * 0.25F;
        part.yRot = model.body.yRot + (arm == HumanoidArm.RIGHT ? -0.5F : 0.5F);
        part.zRot = 0.0F;
        if (model instanceof net.minecraft.client.model.PlayerModel<?> playerModel) {
            playerModel.rightSleeve.copyFrom(model.rightArm);
            playerModel.leftSleeve.copyFrom(model.leftArm);
        }
    }
}
