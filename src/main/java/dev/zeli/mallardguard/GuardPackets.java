package dev.zeli.mallardguard;

import dev.zeli.mallardguard.client.GuardClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;

public final class GuardPackets {
    private GuardPackets() {}

    public record Input(boolean pressed, boolean offhand, int hitlagFrames) implements CustomPacketPayload {
        public static final Type<Input> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "input"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Input> CODEC = StreamCodec.of(
            (buf, data) -> { buf.writeBoolean(data.pressed); buf.writeBoolean(data.offhand); buf.writeVarInt(data.hitlagFrames); },
            buf -> new Input(buf.readBoolean(), buf.readBoolean(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record AttackStarted() implements CustomPacketPayload {
        public static final Type<AttackStarted> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "attack_started"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AttackStarted> CODEC = StreamCodec.of(
            (buf, data) -> {}, buf -> new AttackStarted());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record RequestSettings() implements CustomPacketPayload {
        public static final Type<RequestSettings> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "request_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestSettings> CODEC = StreamCodec.of(
            (buf, data) -> {}, buf -> new RequestSettings());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record OpenConfig() implements CustomPacketPayload {
        public static final Type<OpenConfig> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "open_config"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenConfig> CODEC = StreamCodec.of(
            (buf, data) -> {}, buf -> new OpenConfig());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ConfigUpdate(String changes) implements CustomPacketPayload {
        public static final Type<ConfigUpdate> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "config_update"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigUpdate> CODEC = StreamCodec.of(
            (buf, data) -> buf.writeUtf(data.changes(), 32767), buf -> new ConfigUpdate(buf.readUtf(32767)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ResolveConfigUpdate(boolean keepValues) implements CustomPacketPayload {
        public static final Type<ResolveConfigUpdate> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "resolve_config_update"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ResolveConfigUpdate> CODEC = StreamCodec.of(
            (buf, data) -> buf.writeBoolean(data.keepValues()), buf -> new ResolveConfigUpdate(buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record DamageState(String hits, String rules, String modNames, String catalog) implements CustomPacketPayload {
        public static final Type<DamageState> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "damage_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DamageState> CODEC = StreamCodec.of(
            (buf, data) -> { buf.writeUtf(data.hits, 4096); buf.writeUtf(data.rules, 131072); buf.writeUtf(data.modNames, 16384); buf.writeUtf(data.catalog, 131072); },
            buf -> new DamageState(buf.readUtf(4096), buf.readUtf(131072), buf.readUtf(16384), buf.readUtf(131072)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record DamageHit(String id) implements CustomPacketPayload {
        public static final Type<DamageHit> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "damage_hit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DamageHit> CODEC = StreamCodec.of(
            (buf, data) -> buf.writeUtf(data.id(), 128), buf -> new DamageHit(buf.readUtf(128)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ShieldSettings(int perfect, int window, int rechargeTicks, int maxBlocks, int toolMaxBlocks, int breakTicks, int cone, int reach,
        int retaliation, int stunTicks, int perfectPushback, int regularPushback, int weaponPushback,
        String stunnableBosses, boolean consumablePriority, boolean cooldownPreventsGuard, boolean allowEmptyHand, int retaliationCap, boolean randomShieldWeapon, boolean coneSparks, boolean randomDualTools) implements CustomPacketPayload {
        public static final Type<ShieldSettings> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "shield_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ShieldSettings> CODEC = StreamCodec.of(
            GuardPackets::writeShield, GuardPackets::readShield);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SaveShield(ShieldSettings settings) implements CustomPacketPayload {
        public static final Type<SaveShield> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "save_shield"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveShield> CODEC = StreamCodec.of(
            (buf, data) -> writeShield(buf, data.settings()), buf -> new SaveShield(readShield(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static void writeShield(RegistryFriendlyByteBuf buf, ShieldSettings data) {
        buf.writeVarInt(data.perfect()); buf.writeVarInt(data.window()); buf.writeVarInt(data.rechargeTicks()); buf.writeVarInt(data.maxBlocks()); buf.writeVarInt(data.toolMaxBlocks());
        buf.writeVarInt(data.breakTicks()); buf.writeVarInt(data.cone()); buf.writeVarInt(data.reach()); buf.writeVarInt(data.retaliation());
        buf.writeVarInt(data.stunTicks()); buf.writeVarInt(data.perfectPushback());
        buf.writeVarInt(data.regularPushback()); buf.writeVarInt(data.weaponPushback());
        buf.writeUtf(data.stunnableBosses(), 1024); buf.writeBoolean(data.consumablePriority()); buf.writeBoolean(data.cooldownPreventsGuard()); buf.writeBoolean(data.allowEmptyHand());
        buf.writeVarInt(data.retaliationCap()); buf.writeBoolean(data.randomShieldWeapon()); buf.writeBoolean(data.coneSparks()); buf.writeBoolean(data.randomDualTools());
    }

    private static ShieldSettings readShield(RegistryFriendlyByteBuf buf) {
        return new ShieldSettings(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
            buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
            buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(1024), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public record MobSettings(boolean enabled, int difficulty, boolean blocking, boolean retaliation, int parryColor, int perfectColor, int hitsBeforeGuard, int perfectTicks, int parryTicks, int counterTicks, int approachChance, int tacticalChance, int tacticalSeconds, int tacticalCooldownTicks, String humanoidIds, int movement, int approachDistance, int approachSeconds, int rushChance, int gearChance) implements CustomPacketPayload {
        public static final Type<MobSettings> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "mob_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MobSettings> CODEC = StreamCodec.of(GuardPackets::writeMobs, GuardPackets::readMobs);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record SaveMobs(MobSettings settings) implements CustomPacketPayload {
        public static final Type<SaveMobs> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "save_mobs"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveMobs> CODEC = StreamCodec.of(
            (buf, data) -> writeMobs(buf, data.settings()), buf -> new SaveMobs(readMobs(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private static void writeMobs(RegistryFriendlyByteBuf buf, MobSettings data) {
        buf.writeBoolean(data.enabled()); buf.writeVarInt(data.difficulty()); buf.writeBoolean(data.blocking()); buf.writeBoolean(data.retaliation()); buf.writeVarInt(data.parryColor()); buf.writeVarInt(data.perfectColor()); buf.writeVarInt(data.hitsBeforeGuard()); buf.writeVarInt(data.perfectTicks()); buf.writeVarInt(data.parryTicks()); buf.writeVarInt(data.counterTicks()); buf.writeVarInt(data.approachChance()); buf.writeVarInt(data.tacticalChance()); buf.writeVarInt(data.tacticalSeconds()); buf.writeVarInt(data.tacticalCooldownTicks()); buf.writeUtf(data.humanoidIds()); buf.writeVarInt(data.movement()); buf.writeVarInt(data.approachDistance()); buf.writeVarInt(data.approachSeconds()); buf.writeVarInt(data.rushChance()); buf.writeVarInt(data.gearChance());
    }
    private static MobSettings readMobs(RegistryFriendlyByteBuf buf) {
        return new MobSettings(buf.readBoolean(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(1024), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }
    public record MobCounter(boolean perfect) implements CustomPacketPayload {
        public static final Type<MobCounter> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "mob_counter"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MobCounter> CODEC = StreamCodec.of((b,d) -> { b.writeBoolean(d.perfect()); }, b -> new MobCounter(b.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Stagger(int cells, int maximum, boolean enabled, int stunTicks) implements CustomPacketPayload {
        public static final Type<Stagger> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "stagger"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Stagger> CODEC = StreamCodec.of((b,d) -> { b.writeVarInt(d.cells()); b.writeVarInt(d.maximum()); b.writeBoolean(d.enabled()); b.writeVarInt(d.stunTicks()); }, b -> new Stagger(b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record StaggerSettings(boolean enabled, int cells, int parryLoss, int perfectLoss, int restore, int nearTicks, int farTicks, int radius, int stunTicks, int exitRecovery) implements CustomPacketPayload {
        public static final Type<StaggerSettings> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "stagger_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StaggerSettings> CODEC = StreamCodec.of(GuardPackets::writeStaggerSettings, GuardPackets::readStaggerSettings);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private static void writeStaggerSettings(RegistryFriendlyByteBuf b, StaggerSettings d) {
        b.writeBoolean(d.enabled()); b.writeVarInt(d.cells()); b.writeVarInt(d.parryLoss()); b.writeVarInt(d.perfectLoss()); b.writeVarInt(d.restore()); b.writeVarInt(d.nearTicks()); b.writeVarInt(d.farTicks()); b.writeVarInt(d.radius()); b.writeVarInt(d.stunTicks()); b.writeVarInt(d.exitRecovery());
    }
    private static StaggerSettings readStaggerSettings(RegistryFriendlyByteBuf b) { return new StaggerSettings(b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt()); }
    public record SaveStagger(StaggerSettings settings) implements CustomPacketPayload {
        public static final Type<SaveStagger> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "save_stagger"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveStagger> CODEC = StreamCodec.of((b,d) -> writeStaggerSettings(b,d.settings()), b -> new SaveStagger(readStaggerSettings(b)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record FeintEnded() implements CustomPacketPayload {
        public static final Type<FeintEnded> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "feint_ended"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FeintEnded> CODEC = StreamCodec.unit(new FeintEnded());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record GuardPose(int entityId, boolean active, boolean offhand) implements CustomPacketPayload {
        public static final Type<GuardPose> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "guard_pose"));
        public static final StreamCodec<RegistryFriendlyByteBuf, GuardPose> CODEC = StreamCodec.of(
            (buf, data) -> { buf.writeVarInt(data.entityId()); buf.writeBoolean(data.active()); buf.writeBoolean(data.offhand()); },
            buf -> new GuardPose(buf.readVarInt(), buf.readBoolean(), buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SaveDamageRules(String rules) implements CustomPacketPayload {
        public static final Type<SaveDamageRules> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "save_damage_rules"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveDamageRules> CODEC = StreamCodec.of(
            (buf, data) -> buf.writeUtf(data.rules, 131072),
            buf -> new SaveDamageRules(buf.readUtf(131072)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ClientPolicy(boolean enabled, int dontEnforceMask, String defaults) implements CustomPacketPayload {
        public static final Type<ClientPolicy> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "client_policy"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientPolicy> CODEC = StreamCodec.of(
            (buf, data) -> { buf.writeBoolean(data.enabled); buf.writeVarInt(data.dontEnforceMask); buf.writeUtf(data.defaults, 512); },
            buf -> new ClientPolicy(buf.readBoolean(), buf.readVarInt(), buf.readUtf(512)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record RequestClientPolicy() implements CustomPacketPayload {
        public static final Type<RequestClientPolicy> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "request_client_policy"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestClientPolicy> CODEC = StreamCodec.of(
            (buf, data) -> {}, buf -> new RequestClientPolicy());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SaveClientPolicy(boolean enabled, int dontEnforceMask, String defaults) implements CustomPacketPayload {
        public static final Type<SaveClientPolicy> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "save_client_policy"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveClientPolicy> CODEC = StreamCodec.of(
            (buf, data) -> { buf.writeBoolean(data.enabled); buf.writeVarInt(data.dontEnforceMask); buf.writeUtf(data.defaults, 512); },
            buf -> new SaveClientPolicy(buf.readBoolean(), buf.readVarInt(), buf.readUtf(512)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Status(int phase, int elapsed, int recharge, int window, int rechargeMax, boolean offhand) implements CustomPacketPayload {
        public static final Type<Status> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "status"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Status> CODEC = StreamCodec.of(
            (buf, data) -> { buf.writeVarInt(data.phase); buf.writeVarInt(data.elapsed); buf.writeVarInt(data.recharge); buf.writeVarInt(data.window); buf.writeVarInt(data.rechargeMax); buf.writeBoolean(data.offhand); },
            buf -> new Status(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record HitResult(int result, boolean guardBroken, int retaliationToken) implements CustomPacketPayload {
        public static final Type<HitResult> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "hit_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, HitResult> CODEC = StreamCodec.of(
            (buf, data) -> { buf.writeVarInt(data.result); buf.writeBoolean(data.guardBroken); buf.writeVarInt(data.retaliationToken); },
            buf -> new HitResult(buf.readVarInt(), buf.readBoolean(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record FeedbackComplete(int token) implements CustomPacketPayload {
        public static final Type<FeedbackComplete> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "feedback_complete"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FeedbackComplete> CODEC = StreamCodec.of(
            (buf, data) -> buf.writeVarInt(data.token()), buf -> new FeedbackComplete(buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Sparks(double x, double y, double z, int count, boolean perfect,
                         boolean shield, boolean fall, double directionX, double directionZ, boolean defender, int tint) implements CustomPacketPayload {
        public static final Type<Sparks> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "sparks"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sparks> CODEC = StreamCodec.of(
            (buf, data) -> { buf.writeDouble(data.x); buf.writeDouble(data.y); buf.writeDouble(data.z); buf.writeVarInt(data.count); buf.writeBoolean(data.perfect); buf.writeBoolean(data.shield); buf.writeBoolean(data.fall); buf.writeDouble(data.directionX); buf.writeDouble(data.directionZ); buf.writeBoolean(data.defender); buf.writeInt(data.tint); },
            buf -> new Sparks(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readBoolean(), buf.readInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record HitSound(double x, double y, double z, int kind, int variant, float volume,
        boolean defender, boolean parry) implements CustomPacketPayload {
        public static final Type<HitSound> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "hit_sound"));
        public static final StreamCodec<RegistryFriendlyByteBuf, HitSound> CODEC = StreamCodec.of(
            (buf, data) -> { buf.writeDouble(data.x); buf.writeDouble(data.y); buf.writeDouble(data.z); buf.writeVarInt(data.kind); buf.writeVarInt(data.variant); buf.writeFloat(data.volume); buf.writeBoolean(data.defender); buf.writeBoolean(data.parry); },
            buf -> new HitSound(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readBoolean(), buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Settings(boolean parry, boolean block, boolean parryDrowningFire, boolean parryStarvation, boolean parryGenericKill, int perfect, int window, int recharge, int angle, int reductionPercent, int followUp, int parryReturnPercent, int perfectReturnPercent, int parryWear, int perfectWear, int blockWear, boolean hitSounds, boolean hitParticles, int masterVolume, int perfectVolume, int parryVolume, int blockVolume, boolean fallParry, boolean fallPerfectParry, boolean fallLookDown, boolean fallBreakBlocks, int fallBlastStrength, int fallLaunchPower, boolean parryExplosions, boolean perfectExplosionsOnly, boolean blockExplosions, boolean parryProjectiles, boolean blockProjectiles, boolean defenderKnockback, int knockbackStrength, int guardMovementPercent, int blockDeflectChance, boolean allowAnyItem, boolean allowUsableItems, String includedItems, String excludedItems, String shieldItems, boolean consumablePriority, boolean operator) implements CustomPacketPayload {
        public static final Type<Settings> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Settings> CODEC = StreamCodec.of(
            (buf, data) -> {
                buf.writeBoolean(data.parry); buf.writeBoolean(data.block); buf.writeBoolean(data.parryDrowningFire); buf.writeBoolean(data.parryStarvation); buf.writeBoolean(data.parryGenericKill);
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
                buf.writeBoolean(data.hitSounds); buf.writeBoolean(data.hitParticles);
                buf.writeVarInt(data.masterVolume); buf.writeVarInt(data.perfectVolume); buf.writeVarInt(data.parryVolume); buf.writeVarInt(data.blockVolume);
                writeSources(buf, data.fallParry, data.fallPerfectParry, data.fallLookDown, data.fallBreakBlocks, data.fallBlastStrength, data.fallLaunchPower, data.parryExplosions, data.perfectExplosionsOnly, data.blockExplosions, data.parryProjectiles, data.blockProjectiles, data.defenderKnockback, data.knockbackStrength, data.guardMovementPercent, data.blockDeflectChance);
                writeItemRules(buf, data.allowAnyItem, data.allowUsableItems, data.includedItems, data.excludedItems, data.shieldItems, data.consumablePriority);
                buf.writeBoolean(data.operator);
            },
            buf -> readSettings(buf, true));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Save(boolean parry, boolean block, boolean parryDrowningFire, boolean parryStarvation, boolean parryGenericKill, int perfect, int window, int recharge, int angle, int reductionPercent, int followUp, int parryReturnPercent, int perfectReturnPercent, int parryWear, int perfectWear, int blockWear, boolean hitSounds, boolean hitParticles, int masterVolume, int perfectVolume, int parryVolume, int blockVolume, boolean fallParry, boolean fallPerfectParry, boolean fallLookDown, boolean fallBreakBlocks, int fallBlastStrength, int fallLaunchPower, boolean parryExplosions, boolean perfectExplosionsOnly, boolean blockExplosions, boolean parryProjectiles, boolean blockProjectiles, boolean defenderKnockback, int knockbackStrength, int guardMovementPercent, int blockDeflectChance, boolean allowAnyItem, boolean allowUsableItems, String includedItems, String excludedItems, String shieldItems, boolean consumablePriority) implements CustomPacketPayload {
        public static final Type<Save> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "save"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Save> CODEC = StreamCodec.of(
            (buf, data) -> {
                buf.writeBoolean(data.parry); buf.writeBoolean(data.block); buf.writeBoolean(data.parryDrowningFire); buf.writeBoolean(data.parryStarvation); buf.writeBoolean(data.parryGenericKill);
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
                buf.writeBoolean(data.hitSounds); buf.writeBoolean(data.hitParticles);
                buf.writeVarInt(data.masterVolume); buf.writeVarInt(data.perfectVolume); buf.writeVarInt(data.parryVolume); buf.writeVarInt(data.blockVolume);
                writeSources(buf, data.fallParry, data.fallPerfectParry, data.fallLookDown, data.fallBreakBlocks, data.fallBlastStrength, data.fallLaunchPower, data.parryExplosions, data.perfectExplosionsOnly, data.blockExplosions, data.parryProjectiles, data.blockProjectiles, data.defenderKnockback, data.knockbackStrength, data.guardMovementPercent, data.blockDeflectChance);
                writeItemRules(buf, data.allowAnyItem, data.allowUsableItems, data.includedItems, data.excludedItems, data.shieldItems, data.consumablePriority);
            },
            GuardPackets::readSave);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static void writeItemRules(RegistryFriendlyByteBuf buf, boolean allowAnyItem, boolean allowUsableItems, String includedItems, String excludedItems, String shieldItems, boolean consumablePriority) {
        buf.writeBoolean(allowAnyItem); buf.writeBoolean(allowUsableItems);
        buf.writeUtf(includedItems, 1024);
        buf.writeUtf(excludedItems, 1024);
        buf.writeUtf(shieldItems, 1024); buf.writeBoolean(consumablePriority);
    }

    private static void writeSources(RegistryFriendlyByteBuf buf, boolean fallParry, boolean fallPerfectParry, boolean fallLookDown, boolean fallBreakBlocks, int fallBlastStrength, int fallLaunchPower, boolean parryExplosions, boolean perfectExplosionsOnly, boolean blockExplosions, boolean parryProjectiles, boolean blockProjectiles, boolean defenderKnockback, int knockbackStrength, int guardMovementPercent, int blockDeflectChance) {
        buf.writeBoolean(fallParry); buf.writeBoolean(fallPerfectParry); buf.writeBoolean(fallLookDown); buf.writeBoolean(fallBreakBlocks);
        buf.writeVarInt(fallBlastStrength); buf.writeVarInt(fallLaunchPower);
        buf.writeBoolean(parryExplosions); buf.writeBoolean(perfectExplosionsOnly); buf.writeBoolean(blockExplosions);
        buf.writeBoolean(parryProjectiles); buf.writeBoolean(blockProjectiles); buf.writeBoolean(defenderKnockback);
        buf.writeVarInt(knockbackStrength);
        buf.writeVarInt(guardMovementPercent);
        buf.writeVarInt(blockDeflectChance);
    }

    private static Settings readSettings(RegistryFriendlyByteBuf buf, boolean hasOperatorFlag) {
        boolean parry = buf.readBoolean(), block = buf.readBoolean(), parryDrowningFire = buf.readBoolean(), parryStarvation = buf.readBoolean(), parryGenericKill = buf.readBoolean();
        int perfect = buf.readVarInt(), window = buf.readVarInt(), recharge = buf.readVarInt(), angle = buf.readVarInt(), reduction = buf.readVarInt();
        int followUp = buf.readVarInt(), parryReturn = buf.readVarInt(), perfectReturn = buf.readVarInt();
        int parryWear = buf.readVarInt(), perfectWear = buf.readVarInt(), blockWear = buf.readVarInt();
        boolean sounds = buf.readBoolean(), particles = buf.readBoolean();
        int masterVolume = buf.readVarInt(), perfectVolume = buf.readVarInt(), parryVolume = buf.readVarInt(), blockVolume = buf.readVarInt();
        boolean fall = buf.readBoolean(), fallPerfectParry = buf.readBoolean(), fallLookDown = buf.readBoolean(), breakBlocks = buf.readBoolean();
        int blast = buf.readVarInt(), launch = buf.readVarInt();
        boolean explosion = buf.readBoolean(), perfectOnly = buf.readBoolean(), explosionBlock = buf.readBoolean();
        boolean projectile = buf.readBoolean(), projectileBlock = buf.readBoolean(), knockback = buf.readBoolean();
        int knockbackPower = buf.readVarInt();
        int movementPercent = buf.readVarInt(), deflectChance = buf.readVarInt();
        boolean anyItem = buf.readBoolean(), usable = buf.readBoolean();
        String included = buf.readUtf(1024), excluded = buf.readUtf(1024), shields = buf.readUtf(1024);
        boolean consumablePriority = buf.readBoolean();
        return new Settings(parry, block, parryDrowningFire, parryStarvation, parryGenericKill, perfect, window, recharge, angle, reduction, followUp, parryReturn, perfectReturn, parryWear, perfectWear, blockWear, sounds, particles, masterVolume, perfectVolume, parryVolume, blockVolume, fall, fallPerfectParry, fallLookDown, breakBlocks, blast, launch, explosion, perfectOnly, explosionBlock, projectile, projectileBlock, knockback, knockbackPower, movementPercent, deflectChance, anyItem, usable, included, excluded, shields, consumablePriority, hasOperatorFlag && buf.readBoolean());
    }

    private static Save readSave(RegistryFriendlyByteBuf buf) {
        Settings data = readSettings(buf, false);
        return new Save(data.parry, data.block, data.parryDrowningFire, data.parryStarvation, data.parryGenericKill, data.perfect, data.window, data.recharge, data.angle, data.reductionPercent, data.followUp, data.parryReturnPercent, data.perfectReturnPercent, data.parryWear, data.perfectWear, data.blockWear, data.hitSounds, data.hitParticles, data.masterVolume, data.perfectVolume, data.parryVolume, data.blockVolume, data.fallParry, data.fallPerfectParry, data.fallLookDown, data.fallBreakBlocks, data.fallBlastStrength, data.fallLaunchPower, data.parryExplosions, data.perfectExplosionsOnly, data.blockExplosions, data.parryProjectiles, data.blockProjectiles, data.defenderKnockback, data.knockbackStrength, data.guardMovementPercent, data.blockDeflectChance, data.allowAnyItem, data.allowUsableItems, data.includedItems, data.excludedItems, data.shieldItems, data.consumablePriority);
    }


    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("32");
        registrar.playToServer(FeintEnded.TYPE, FeintEnded.CODEC, (data, context) -> {
            if (context.player() instanceof ServerPlayer player) GuardState.feinted(player);
        });
        registrar.playToClient(MobCounter.TYPE, MobCounter.CODEC, (data, context) -> GuardClient.mobCounter(data));
        registrar.playToClient(Stagger.TYPE, Stagger.CODEC, (data, context) -> GuardClient.stagger(data));
        registrar.playToClient(StaggerSettings.TYPE, StaggerSettings.CODEC, (data, context) -> GuardClient.staggerSettings(data));
        registrar.playToServer(SaveStagger.TYPE, SaveStagger.CODEC, GuardPackets::saveStagger);
        registrar.playToServer(AttackStarted.TYPE, AttackStarted.CODEC, (data, context) -> {
            if (context.player() instanceof ServerPlayer player) GuardState.attackStarted(player);
        });
        registrar.playToServer(Input.TYPE, Input.CODEC, (data, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                GuardRetaliation.clientFrames(player, data.hitlagFrames());
                GuardState.input(player, data.pressed(), data.offhand());
            }
        });
        registrar.playToServer(FeedbackComplete.TYPE, FeedbackComplete.CODEC, (data, context) -> {
            if (context.player() instanceof ServerPlayer player) GuardRetaliation.complete(player, data.token());
        });
        registrar.playToServer(RequestSettings.TYPE, RequestSettings.CODEC, (data, context) -> {
            if (context.player() instanceof ServerPlayer player) sendSettings(player);
        });
        registrar.playToServer(ResolveConfigUpdate.TYPE, ResolveConfigUpdate.CODEC, (data, context) -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.hasPermissions(2)) return;
            if (!GuardConfig.respondToServerUpdate(data.keepValues())) {
                player.sendSystemMessage(Component.literal("Mallard Guard: Config update failed. Check the server log."));
            }
            sendSettings(player);
        });
        registrar.playToServer(RequestClientPolicy.TYPE, RequestClientPolicy.CODEC, (data, context) -> {
            if (context.player() instanceof ServerPlayer player)
                PacketDistributor.sendToPlayer(player, clientPolicy());
        });
        registrar.playToServer(SaveMobs.TYPE, SaveMobs.CODEC, GuardPackets::saveMobs);
        registrar.playToClient(MobSettings.TYPE, MobSettings.CODEC, (data, context) -> GuardClient.mobSettings(data));
        registrar.playToClient(GuardPose.TYPE, GuardPose.CODEC, (data, context) -> dev.zeli.mallardguard.client.GuardThirdPerson.pose(data));
        registrar.playToServer(Save.TYPE, Save.CODEC, GuardPackets::save);
        registrar.playToServer(SaveShield.TYPE, SaveShield.CODEC, GuardPackets::saveShield);
        registrar.playToServer(SaveDamageRules.TYPE, SaveDamageRules.CODEC, GuardPackets::saveDamageRules);
        registrar.playToServer(SaveClientPolicy.TYPE, SaveClientPolicy.CODEC, GuardPackets::saveClientPolicy);
        registrar.playToClient(ClientPolicy.TYPE, ClientPolicy.CODEC, (data, context) -> GuardClient.clientPolicy(data));
        registrar.playToClient(Status.TYPE, Status.CODEC, (data, context) -> GuardClient.status(data));
        registrar.playToClient(HitResult.TYPE, HitResult.CODEC, (data, context) -> GuardClient.hitResult(data));
        registrar.playToClient(Sparks.TYPE, Sparks.CODEC, (data, context) -> GuardClient.sparks(data));
        registrar.playToClient(HitSound.TYPE, HitSound.CODEC, (data, context) -> GuardClient.hitSound(data));
        registrar.playToClient(Settings.TYPE, Settings.CODEC, (data, context) -> GuardClient.settings(data));
        registrar.playToClient(OpenConfig.TYPE, OpenConfig.CODEC, (data, context) -> GuardClient.openConfig());
        registrar.playToClient(DamageState.TYPE, DamageState.CODEC, (data, context) -> GuardClient.damageState(data));
        registrar.playToClient(DamageHit.TYPE, DamageHit.CODEC, (data, context) -> GuardClient.damageHit(data));
        registrar.playToClient(ShieldSettings.TYPE, ShieldSettings.CODEC, (data, context) -> GuardClient.shieldSettings(data));
        registrar.playToClient(ConfigUpdate.TYPE, ConfigUpdate.CODEC, (data, context) -> GuardClient.serverConfigUpdate(data));
    }

    public static void sendSettings(ServerPlayer player) {
        if (player.hasPermissions(2) && GuardConfig.pendingServerUpdate()) {
            PacketDistributor.sendToPlayer(player, new ConfigUpdate(String.join("\n", GuardConfig.pendingServerChanges())));
            return;
        }
        PacketDistributor.sendToPlayer(player, clientPolicy());
        PacketDistributor.sendToPlayer(player, GuardConfig.mobSnapshot(false));
        PacketDistributor.sendToPlayer(player, GuardConfig.staggerSnapshot(false));
        PacketDistributor.sendToPlayer(player, GuardConfig.snapshot(player.hasPermissions(2)));
        PacketDistributor.sendToPlayer(player, GuardConfig.shieldSnapshot());
        PacketDistributor.sendToPlayer(player, GuardDamageRules.state(player));
    }

    public static ClientPolicy clientPolicy() {
        int[] saved = GuardClientPreset.parse(GuardConfig.CLIENT_PRESET.get());
        int[] effective = GuardClientPreset.readEnforceTemplate(saved == null ? GuardClientPreset.DEFAULTS : saved);
        GuardRetaliation.enforcedFrames(effective[3]);
        return new ClientPolicy(GuardConfig.ENFORCE_CLIENT.get(), GuardConfig.CLIENT_EXEMPT_MASK.get(), GuardClientPreset.encode(effective));
    }

    private static void saveClientPolicy(SaveClientPolicy data, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("Mallard Guard: Only operators can change client enforcement."));
            return;
        }
        if (data.dontEnforceMask() < 0 || data.dontEnforceMask() > GuardClientPreset.ALL_CATEGORIES || !GuardClientPreset.valid(data.defaults())) {
            player.sendSystemMessage(Component.literal("Mallard Guard: Invalid client enforcement settings."));
            return;
        }
        int[] preset = GuardClientPreset.parse(data.defaults());
        if (preset == null || !GuardClientPreset.writeEnforceTemplate(preset)) {
            player.sendSystemMessage(Component.literal("Mallard Guard: Could not save the enforce template."));
            return;
        }
        GuardConfig.ENFORCE_CLIENT.set(data.enabled());
        GuardConfig.CLIENT_EXEMPT_MASK.set(data.dontEnforceMask());
        GuardConfig.CLIENT_PRESET.set(data.defaults());
        GuardConfig.SERVER_SPEC.save();
        GuardConfig.clearConfigBackups();
        ClientPolicy policy = clientPolicy();
        for (ServerPlayer connected : player.serverLevel().getServer().getPlayerList().getPlayers())
            PacketDistributor.sendToPlayer(connected, policy);
    }

    private static void save(Save data, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("Mallard Guard: Only operators can change server settings."));
            return;
        }
        if (data.perfect() < 0 || data.perfect() > 5 || data.window() < 1 || data.window() > 10
            || data.recharge() < 1 || data.recharge() > 60 || data.angle() < 0 || data.angle() > 360
            || data.reductionPercent() < 0 || data.reductionPercent() > 100
            || data.followUp() < 0 || data.followUp() > 20
            || data.parryReturnPercent() < 0 || data.parryReturnPercent() > 200
            || data.perfectReturnPercent() < 0 || data.perfectReturnPercent() > 200
            || data.parryWear() < 0 || data.parryWear() > 100
            || data.perfectWear() < 0 || data.perfectWear() > 100
            || data.blockWear() < 0 || data.blockWear() > 100
            || data.masterVolume() < 0 || data.masterVolume() > 200
            || data.perfectVolume() < 0 || data.perfectVolume() > 200
            || data.parryVolume() < 0 || data.parryVolume() > 200
            || data.blockVolume() < 0 || data.blockVolume() > 200
            || data.fallBlastStrength() < 0 || data.fallBlastStrength() > 200
            || data.fallLaunchPower() < 0 || data.fallLaunchPower() > 200
            || data.knockbackStrength() < 0 || data.knockbackStrength() > 200
            || data.guardMovementPercent() < 0 || data.guardMovementPercent() > 100
            || data.blockDeflectChance() < 0 || data.blockDeflectChance() > 100
            || !GuardItemRules.valid(data.includedItems()) || !GuardItemRules.valid(data.excludedItems()) || !GuardItemRules.valid(data.shieldItems())) {
            player.sendSystemMessage(Component.literal("Mallard Guard: Server settings were rejected. Check the values and item IDs."));
            return;
        }
        GuardConfig.apply(new Settings(data.parry(), data.block(), data.parryDrowningFire(), data.parryStarvation(), data.parryGenericKill(), data.perfect(), data.window(), data.recharge(), data.angle(), data.reductionPercent(), data.followUp(), data.parryReturnPercent(), data.perfectReturnPercent(), data.parryWear(), data.perfectWear(), data.blockWear(), data.hitSounds(), data.hitParticles(), data.masterVolume(), data.perfectVolume(), data.parryVolume(), data.blockVolume(), data.fallParry(), data.fallPerfectParry(), data.fallLookDown(), data.fallBreakBlocks(), data.fallBlastStrength(), data.fallLaunchPower(), data.parryExplosions(), data.perfectExplosionsOnly(), data.blockExplosions(), data.parryProjectiles(), data.blockProjectiles(), data.defenderKnockback(), data.knockbackStrength(), data.guardMovementPercent(), data.blockDeflectChance(), data.allowAnyItem(), data.allowUsableItems(), data.includedItems(), data.excludedItems(), data.shieldItems(), data.consumablePriority(), true));
    }

    private static void saveDamageRules(SaveDamageRules data, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("Mallard Guard: Only operators can change damage type rules."));
            return;
        }
        var rules = GuardDamageRules.parseRules(data.rules());
        if (rules == null) {
            player.sendSystemMessage(Component.literal("Mallard Guard: Invalid damage type rules."));
            return;
        }
        GuardConfig.DAMAGE_RULES.set(data.rules());
        GuardConfig.SERVER_SPEC.save();
        GuardConfig.clearConfigBackups();
    }

    private static void saveStagger(SaveStagger packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.hasPermissions(2)) return;
        StaggerSettings d = packet.settings();
        if (d.cells() < 1 || d.cells() > 10 || d.parryLoss() < 0 || d.parryLoss() > 20 || d.perfectLoss() < 0 || d.perfectLoss() > 20
            || d.restore() < 0 || d.restore() > 20 || d.nearTicks() < 0 || d.nearTicks() > 1200 || d.farTicks() < 0 || d.farTicks() > 1200 || d.radius() < 1 || d.radius() > 32 || d.stunTicks() < 0 || d.stunTicks() > 60 || d.exitRecovery() < 0 || d.exitRecovery() > d.cells()) return;
        GuardConfig.STAGGER_ENABLED.set(d.enabled()); GuardConfig.STAGGER_CELLS.set(d.cells()); GuardConfig.STAGGER_PARRY_LOSS.set(d.parryLoss());
        GuardConfig.STAGGER_PERFECT_LOSS.set(d.perfectLoss()); GuardConfig.STAGGER_RESTORE.set(d.restore()); GuardConfig.STAGGER_NEAR_TICKS.set(d.nearTicks());
        GuardConfig.STAGGER_FAR_TICKS.set(d.farTicks()); GuardConfig.STAGGER_RADIUS.set(d.radius()); GuardConfig.STAGGER_STUN_TICKS.set(d.stunTicks()); GuardConfig.STAGGER_EXIT_RECOVERY.set(d.exitRecovery()); GuardConfig.SERVER_SPEC.save(); GuardConfig.clearConfigBackups();
        for (ServerPlayer connected : player.serverLevel().getServer().getPlayerList().getPlayers()) PacketDistributor.sendToPlayer(connected, d);
    }

    private static void saveMobs(SaveMobs packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.hasPermissions(2)) return;
        MobSettings data = packet.settings();
        if (data.difficulty() < 0 || data.difficulty() > 100
            || data.parryColor() < 0 || data.parryColor() > 16777215
            || data.perfectColor() < 0 || data.perfectColor() > 16777215
            || data.hitsBeforeGuard() < 1 || data.hitsBeforeGuard() > 3
            || data.perfectTicks() < 0 || data.perfectTicks() > 20
            || data.parryTicks() < 1 || data.parryTicks() > 60
            || data.counterTicks() < 0 || data.counterTicks() > 40
            || data.approachChance() < 0 || data.approachChance() > 100
            || data.tacticalChance() < 0 || data.tacticalChance() > 100
            || data.tacticalSeconds() < 1 || data.tacticalSeconds() > 10
            || data.tacticalCooldownTicks() < 20 || data.tacticalCooldownTicks() > 1200
            || data.movement() < 0 || data.movement() > 100
            || data.approachDistance() < 1 || data.approachDistance() > 5
            || data.approachSeconds() < 1 || data.approachSeconds() > 10
            || data.rushChance() < 0 || data.rushChance() > 100
            || data.gearChance() < 0 || data.gearChance() > 100
            || !GuardItemRules.valid(data.humanoidIds()) || data.humanoidIds().contains("#")) return;
        GuardConfig.MOB_GUARD.set(data.enabled());
        GuardConfig.MOB_DIFFICULTY.set(data.difficulty());
        GuardConfig.MOB_BLOCKING.set(data.blocking());
        GuardConfig.MOB_RETALIATION.set(data.retaliation());
        GuardConfig.MOB_PARRY_COLOR.set(data.parryColor());
        GuardConfig.MOB_PERFECT_COLOR.set(data.perfectColor());
        GuardConfig.MOB_HITS_TO_GUARD.set(data.hitsBeforeGuard());
        GuardConfig.MOB_PERFECT_TICKS.set(data.perfectTicks());
        GuardConfig.MOB_PARRY_TICKS.set(data.parryTicks());
        GuardConfig.MOB_COUNTER_TICKS.set(data.counterTicks());
        GuardConfig.MOB_APPROACH_CHANCE.set(data.approachChance());
        GuardConfig.MOB_TACTICAL_CHANCE.set(data.tacticalChance());
        GuardConfig.MOB_TACTICAL_SECONDS.set(data.tacticalSeconds());
        GuardConfig.MOB_TACTICAL_COOLDOWN.set(data.tacticalCooldownTicks());
        GuardConfig.MOB_HUMANOID_IDS.set(data.humanoidIds());
        GuardConfig.MOB_MOVEMENT.set(data.movement());
        GuardConfig.MOB_APPROACH_DISTANCE.set(data.approachDistance());
        GuardConfig.MOB_APPROACH_SECONDS.set(data.approachSeconds());
        GuardConfig.MOB_RUSH_CHANCE.set(data.rushChance());
        GuardConfig.MOB_GEAR_CHANCE.set(data.gearChance());
        GuardConfig.SERVER_SPEC.save();
        GuardConfig.clearConfigBackups();
        for (ServerPlayer connected : player.serverLevel().getServer().getPlayerList().getPlayers())
            PacketDistributor.sendToPlayer(connected, data);
    }

    private static void saveShield(SaveShield packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("Mallard Guard: Only operators can change shield settings."));
            return;
        }
        ShieldSettings data = packet.settings();
        if (data.perfect() < 0 || data.perfect() > 3 || data.window() < 1 || data.window() > 10
            || data.perfect() > data.window() || data.rechargeTicks() < 1 || data.rechargeTicks() > 60
            || data.maxBlocks() < 0 || data.maxBlocks() > 10 || data.toolMaxBlocks() < 0 || data.toolMaxBlocks() > 5
            || data.breakTicks() < GuardConfig.RECHARGE_TICKS.get() || data.breakTicks() > 200
            || data.cone() < 0 || data.cone() > 90 || data.reach() < 1 || data.reach() > 10
            || data.retaliation() < 0 || data.retaliation() > 30 || data.retaliationCap() < 0 || data.retaliationCap() > 1000
            || data.stunTicks() < 0 || data.stunTicks() > 100 || data.perfectPushback() < 0 || data.perfectPushback() > 200
            || data.regularPushback() < 0 || data.regularPushback() > 200 || data.weaponPushback() < 0 || data.weaponPushback() > 200
            || !GuardItemRules.valid(data.stunnableBosses())) {
            player.sendSystemMessage(Component.literal("Mallard Guard: Invalid shield settings."));
            return;
        }
        GuardConfig.applyShield(data);
        for (ServerPlayer connected : player.serverLevel().getServer().getPlayerList().getPlayers())
            PacketDistributor.sendToPlayer(connected, GuardConfig.shieldSnapshot());
    }
}
