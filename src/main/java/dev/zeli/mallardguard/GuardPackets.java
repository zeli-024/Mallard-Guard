package dev.zeli.mallardguard;

import dev.zeli.mallardguard.client.GuardClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;

public final class GuardPackets {
    private GuardPackets() {}

    public record Input(boolean pressed) implements CustomPacketPayload {
        public static final Type<Input> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "input"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Input> CODEC = StreamCodec.of(
            (buf, data) -> buf.writeBoolean(data.pressed), buf -> new Input(buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Status(int phase, int elapsed, int recharge, int window, int rechargeMax) implements CustomPacketPayload {
        public static final Type<Status> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "status"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Status> CODEC = StreamCodec.of(
            (buf, data) -> { buf.writeVarInt(data.phase); buf.writeVarInt(data.elapsed); buf.writeVarInt(data.recharge); buf.writeVarInt(data.window); buf.writeVarInt(data.rechargeMax); },
            buf -> new Status(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record HitResult(int result) implements CustomPacketPayload {
        public static final Type<HitResult> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "hit_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, HitResult> CODEC = StreamCodec.of(
            (buf, data) -> buf.writeVarInt(data.result), buf -> new HitResult(buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Settings(boolean parry, boolean block, int perfect, int window, int recharge, int angle, int reductionPercent, int followUp, int parryReturnPercent, int perfectReturnPercent, int parryWear, int perfectWear, int blockWear, int addedDurability, boolean hitSounds, boolean hitParticles, int masterVolume, int perfectVolume, int parryVolume, int blockVolume, boolean operator) implements CustomPacketPayload {
        public static final Type<Settings> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Settings> CODEC = StreamCodec.of(
            (buf, data) -> {
                buf.writeBoolean(data.parry); buf.writeBoolean(data.block);
                buf.writeVarInt(data.perfect);
                buf.writeVarInt(data.window);
                buf.writeVarInt(data.recharge);
                buf.writeVarInt(data.angle);
                buf.writeVarInt(data.reductionPercent);
                buf.writeVarInt(data.followUp);
                buf.writeVarInt(data.parryReturnPercent);
                buf.writeVarInt(data.perfectReturnPercent);
                buf.writeVarInt(data.parryWear);
                buf.writeVarInt(data.perfectWear);
                buf.writeVarInt(data.blockWear);
                buf.writeVarInt(data.addedDurability);
                buf.writeBoolean(data.hitSounds); buf.writeBoolean(data.hitParticles);
                buf.writeVarInt(data.masterVolume); buf.writeVarInt(data.perfectVolume); buf.writeVarInt(data.parryVolume); buf.writeVarInt(data.blockVolume);
                buf.writeBoolean(data.operator);
            },
            buf -> new Settings(buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Save(boolean parry, boolean block, int perfect, int window, int recharge, int angle, int reductionPercent, int followUp, int parryReturnPercent, int perfectReturnPercent, int parryWear, int perfectWear, int blockWear, int addedDurability, boolean hitSounds, boolean hitParticles, int masterVolume, int perfectVolume, int parryVolume, int blockVolume) implements CustomPacketPayload {
        public static final Type<Save> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "save"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Save> CODEC = StreamCodec.of(
            (buf, data) -> {
                buf.writeBoolean(data.parry); buf.writeBoolean(data.block);
                buf.writeVarInt(data.perfect);
                buf.writeVarInt(data.window);
                buf.writeVarInt(data.recharge);
                buf.writeVarInt(data.angle);
                buf.writeVarInt(data.reductionPercent);
                buf.writeVarInt(data.followUp);
                buf.writeVarInt(data.parryReturnPercent);
                buf.writeVarInt(data.perfectReturnPercent);
                buf.writeVarInt(data.parryWear);
                buf.writeVarInt(data.perfectWear);
                buf.writeVarInt(data.blockWear);
                buf.writeVarInt(data.addedDurability);
                buf.writeBoolean(data.hitSounds); buf.writeBoolean(data.hitParticles);
                buf.writeVarInt(data.masterVolume); buf.writeVarInt(data.perfectVolume); buf.writeVarInt(data.parryVolume); buf.writeVarInt(data.blockVolume);
            },
            buf -> new Save(buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(Input.TYPE, Input.CODEC, (data, context) -> {
            if (context.player() instanceof ServerPlayer player) GuardState.input(player, data.pressed());
        });
        registrar.playToServer(Save.TYPE, Save.CODEC, GuardPackets::save);
        registrar.playToClient(Status.TYPE, Status.CODEC, (data, context) -> GuardClient.status(data));
        registrar.playToClient(HitResult.TYPE, HitResult.CODEC, (data, context) -> GuardClient.hitResult(data));
        registrar.playToClient(Settings.TYPE, Settings.CODEC, (data, context) -> GuardClient.settings(data));
    }

    private static void save(Save data, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.hasPermissions(2)) return;
        if (data.perfect() < 0 || data.perfect() > 5 || data.window() < 1 || data.window() > 10
            || data.recharge() < 1 || data.recharge() > 60 || data.angle() < 0 || data.angle() > 360
            || data.reductionPercent() < 0 || data.reductionPercent() > 100
            || data.followUp() < 0 || data.followUp() > 20
            || data.parryReturnPercent() < 0 || data.parryReturnPercent() > 300
            || data.perfectReturnPercent() < 0 || data.perfectReturnPercent() > 300
            || data.parryWear() < 0 || data.parryWear() > 100
            || data.perfectWear() < 0 || data.perfectWear() > 100
            || data.blockWear() < 0 || data.blockWear() > 100
            || data.addedDurability() < 0 || data.addedDurability() > 10000
            || data.masterVolume() < 0 || data.masterVolume() > 200
            || data.perfectVolume() < 0 || data.perfectVolume() > 200
            || data.parryVolume() < 0 || data.parryVolume() > 200
            || data.blockVolume() < 0 || data.blockVolume() > 200) return;
        GuardConfig.apply(new Settings(data.parry(), data.block(), data.perfect(), data.window(), data.recharge(), data.angle(), data.reductionPercent(), data.followUp(), data.parryReturnPercent(), data.perfectReturnPercent(), data.parryWear(), data.perfectWear(), data.blockWear(), data.addedDurability(), data.hitSounds(), data.hitParticles(), data.masterVolume(), data.perfectVolume(), data.parryVolume(), data.blockVolume(), true));
    }
}
