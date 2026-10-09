package dev.zeli.mallardguard.mixin;

import dev.zeli.mallardguard.GuardTridentReturn;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownTrident.class)
public abstract class ThrownTridentReturnMixin implements GuardTridentReturn {
    @Unique private static final EntityDataAccessor<Optional<UUID>> mallardguard$RETURN_OWNER =
        SynchedEntityData.defineId(ThrownTrident.class, EntityDataSerializers.OPTIONAL_UUID);
    @Unique private static final EntityDataAccessor<Integer> mallardguard$RETURN_ENTITY =
        SynchedEntityData.defineId(ThrownTrident.class, EntityDataSerializers.INT);
    @Unique private Entity mallardguard$pickupCombatOwner;
    @Unique private AbstractArrow.Pickup mallardguard$pickupMode;
    @Unique private boolean mallardguard$pickupOwnerSwapped;

    @Unique private ThrownTrident mallardguard$self() { return (ThrownTrident) (Object) this; }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void mallardguard$defineReturnOwner(SynchedEntityData.Builder builder, CallbackInfo callback) {
        builder.define(mallardguard$RETURN_OWNER, Optional.empty());
        builder.define(mallardguard$RETURN_ENTITY, -1);
    }

    @Override
    public void mallardguard$rememberThrower(Entity thrower) {
        var data = mallardguard$self().getEntityData();
        if (thrower == null || data.get(mallardguard$RETURN_OWNER).isPresent()) return;
        data.set(mallardguard$RETURN_OWNER, Optional.of(thrower.getUUID()));
        data.set(mallardguard$RETURN_ENTITY, thrower.getId());
    }

    @Unique private Entity mallardguard$returnOwner() {
        ThrownTrident trident = mallardguard$self();
        Optional<UUID> uuid = trident.getEntityData().get(mallardguard$RETURN_OWNER);
        if (uuid.isEmpty()) return trident.getOwner();
        if (trident.level() instanceof ServerLevel server) {
            Entity owner = server.getEntity(uuid.get());
            if (owner != null) trident.getEntityData().set(mallardguard$RETURN_ENTITY, owner.getId());
            return owner;
        }
        Player player = trident.level().getPlayerByUUID(uuid.get());
        if (player != null) return player;
        Entity entity = trident.level().getEntity(trident.getEntityData().get(mallardguard$RETURN_ENTITY));
        return entity != null && uuid.get().equals(entity.getUUID()) ? entity : null;
    }

    // Only Loyalty's return calculations use the thrower; damage keeps the combat owner.
    @Redirect(method = {"tick", "isAcceptibleReturnOwner"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/projectile/ThrownTrident;getOwner()Lnet/minecraft/world/entity/Entity;"))
    private Entity mallardguard$loyaltyOwner(ThrownTrident trident) {
        return mallardguard$returnOwner();
    }

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void mallardguard$beginPickup(Player player, CallbackInfo callback) {
        ThrownTrident trident = mallardguard$self();
        if (trident.getEntityData().get(mallardguard$RETURN_OWNER).isEmpty()) return;
        mallardguard$pickupCombatOwner = trident.getOwner();
        mallardguard$pickupMode = trident.pickup;
        mallardguard$pickupOwnerSwapped = true;
        trident.setOwner(mallardguard$returnOwner());
        trident.pickup = mallardguard$pickupMode;
    }

    @Inject(method = "playerTouch", at = @At("RETURN"))
    private void mallardguard$endPickup(Player player, CallbackInfo callback) {
        if (!mallardguard$pickupOwnerSwapped) return;
        mallardguard$self().setOwner(mallardguard$pickupCombatOwner);
        mallardguard$self().pickup = mallardguard$pickupMode;
        mallardguard$pickupCombatOwner = null;
        mallardguard$pickupMode = null;
        mallardguard$pickupOwnerSwapped = false;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void mallardguard$saveReturnOwner(CompoundTag tag, CallbackInfo callback) {
        mallardguard$self().getEntityData().get(mallardguard$RETURN_OWNER)
            .ifPresent(uuid -> tag.putUUID("MallardGuardReturnOwner", uuid));
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void mallardguard$loadReturnOwner(CompoundTag tag, CallbackInfo callback) {
        var data = mallardguard$self().getEntityData();
        data.set(mallardguard$RETURN_OWNER, tag.hasUUID("MallardGuardReturnOwner")
            ? Optional.of(tag.getUUID("MallardGuardReturnOwner")) : Optional.empty());
        Entity owner = mallardguard$returnOwner();
        data.set(mallardguard$RETURN_ENTITY, owner == null ? -1 : owner.getId());
    }
}
