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

    public static final int SAVE_RULES = 1, SAVE_MOBS = 2, SAVE_SHIELD = 4, SAVE_DAMAGE = 8, SAVE_POLICY = 16;
    public record SaveConfig(int request, Save rules, SaveMobs mobs, SaveShield shield,
                             SaveDamageRules damage, SaveClientPolicy policy) implements CustomPacketPayload {
        public static final Type<SaveConfig> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "save_config"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveConfig> CODEC = StreamCodec.of(
            (buf, data) -> {
                buf.writeVarInt(data.request());
                writeOptional(buf, Save.CODEC, data.rules());
                writeOptional(buf, SaveMobs.CODEC, data.mobs());
                writeOptional(buf, SaveShield.CODEC, data.shield());
                writeOptional(buf, SaveDamageRules.CODEC, data.damage());
                writeOptional(buf, SaveClientPolicy.CODEC, data.policy());
            }, buf -> new SaveConfig(buf.readVarInt(), readOptional(buf, Save.CODEC), readOptional(buf, SaveMobs.CODEC),
                                     readOptional(buf, SaveShield.CODEC), readOptional(buf, SaveDamageRules.CODEC), readOptional(buf, SaveClientPolicy.CODEC)));
        public int mask() {
            return (rules != null ? SAVE_RULES : 0) | (mobs != null ? SAVE_MOBS : 0) | (shield != null ? SAVE_SHIELD : 0)
                | (damage != null ? SAVE_DAMAGE : 0) | (policy != null ? SAVE_POLICY : 0);
        }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record SaveResult(int request, int accepted, String error) implements CustomPacketPayload {
        public static final Type<SaveResult> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "save_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveResult> CODEC = StreamCodec.of(
            (buf, data) -> { buf.writeVarInt(data.request()); buf.writeVarInt(data.accepted()); buf.writeUtf(data.error(), 1024); },
            buf -> new SaveResult(buf.readVarInt(), buf.readVarInt(), buf.readUtf(1024)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private static <T> void writeOptional(RegistryFriendlyByteBuf buf, StreamCodec<RegistryFriendlyByteBuf, T> codec, T value) {
        buf.writeBoolean(value != null);
        if (value != null) codec.encode(buf, value);
    }
    private static <T> T readOptional(RegistryFriendlyByteBuf buf, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        return buf.readBoolean() ? codec.decode(buf) : null;
    }
    private static boolean attemptSave(java.util.function.BooleanSupplier action) {
        try { return action.getAsBoolean(); }
        catch (RuntimeException error) {
            System.err.println("Mallard Guard: server config save failed: " + error.getMessage());
            return false;
        }
    }
    private static void saveConfig(SaveConfig data, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!player.hasPermissions(2)) {
            PacketDistributor.sendToPlayer(player, new SaveResult(data.request(), 0, "Only operators can change server settings."));
            return;
        }
        int accepted = 0;
        if (data.rules() != null && attemptSave(() -> save(data.rules(), context))) accepted |= SAVE_RULES;
        if (data.mobs() != null && attemptSave(() -> saveMobs(data.mobs(), context))) accepted |= SAVE_MOBS;
        if (data.shield() != null && attemptSave(() -> saveShield(data.shield(), context))) accepted |= SAVE_SHIELD;
        if (data.damage() != null && attemptSave(() -> saveDamageRules(data.damage(), context))) accepted |= SAVE_DAMAGE;
        if (data.policy() != null && attemptSave(() -> saveClientPolicy(data.policy(), context))) accepted |= SAVE_POLICY;
        int rejected = data.mask() & ~accepted;
        java.util.List<String> sections = new java.util.ArrayList<>();
        if ((rejected & SAVE_RULES) != 0) sections.add("server rules");
        if ((rejected & SAVE_MOBS) != 0) sections.add("mob guard");
        if ((rejected & SAVE_SHIELD) != 0) sections.add("shield and eligibility settings");
        if ((rejected & SAVE_DAMAGE) != 0) sections.add("damage types");
        if ((rejected & SAVE_POLICY) != 0) sections.add("enforcement");
        String error = sections.isEmpty() ? "" : "Could not save " + String.join(", ", sections) + ". These changes remain unsaved. Check the values and server log.";
        PacketDistributor.sendToPlayer(player, new SaveResult(data.request(), accepted, error));
    }

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
        String stunnableBosses, boolean consumablePriority, boolean cooldownPreventsGuard, boolean allowEmptyHand, int retaliationCap, int shieldParryPriority, boolean coneSparks, int parryHandPriority, boolean forceCrouchOffhand, String shieldBlacklist) implements CustomPacketPayload {
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
        buf.writeVarInt(data.retaliationCap()); buf.writeVarInt(data.shieldParryPriority()); buf.writeBoolean(data.coneSparks()); buf.writeVarInt(data.parryHandPriority()); buf.writeBoolean(data.forceCrouchOffhand()); buf.writeUtf(data.shieldBlacklist(), 1024);
    }

    private static ShieldSettings readShield(RegistryFriendlyByteBuf buf) {
        return new ShieldSettings(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
            buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
            buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(1024), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readVarInt(), buf.readBoolean(), buf.readUtf(1024));
    }

    public record MobSettings(boolean enabled, int difficulty, boolean blocking, boolean retaliation, int parryColor, int perfectColor, int hitsBeforeGuard, int perfectTicks, int parryTicks, int counterTicks, int approachChance, int tacticalChance, int tacticalSeconds, int tacticalCooldownTicks, String humanoidIds, String blacklist, int movement, int approachDistance, int approachSeconds, int rushChance, int gearChance) implements CustomPacketPayload {
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
        buf.writeBoolean(data.enabled()); buf.writeVarInt(data.difficulty()); buf.writeBoolean(data.blocking()); buf.writeBoolean(data.retaliation()); buf.writeVarInt(data.parryColor()); buf.writeVarInt(data.perfectColor()); buf.writeVarInt(data.hitsBeforeGuard()); buf.writeVarInt(data.perfectTicks()); buf.writeVarInt(data.parryTicks()); buf.writeVarInt(data.counterTicks()); buf.writeVarInt(data.approachChance()); buf.writeVarInt(data.tacticalChance()); buf.writeVarInt(data.tacticalSeconds()); buf.writeVarInt(data.tacticalCooldownTicks()); buf.writeUtf(data.humanoidIds()); buf.writeUtf(data.blacklist(), 1024); buf.writeVarInt(data.movement()); buf.writeVarInt(data.approachDistance()); buf.writeVarInt(data.approachSeconds()); buf.writeVarInt(data.rushChance()); buf.writeVarInt(data.gearChance());
    }
    private static MobSettings readMobs(RegistryFriendlyByteBuf buf) {
        return new MobSettings(buf.readBoolean(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(1024), buf.readUtf(1024), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }
    public record MobCounter(boolean perfect) implements CustomPacketPayload {
        public static final Type<MobCounter> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "mob_counter"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MobCounter> CODEC = StreamCodec.of((b,d) -> { b.writeBoolean(d.perfect()); }, b -> new MobCounter(b.readBoolean()));
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

    public record ClientPolicy(boolean enabled, int dontEnforceMask, String defaults, String animations) implements CustomPacketPayload {
        public static final Type<ClientPolicy> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "client_policy"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientPolicy> CODEC = StreamCodec.of(
            (buf, data) -> { buf.writeBoolean(data.enabled); buf.writeVarInt(data.dontEnforceMask); buf.writeUtf(data.defaults, 2048);buf.writeUtf(data.animations,24576); },
            buf -> new ClientPolicy(buf.readBoolean(), buf.readVarInt(), buf.readUtf(2048),buf.readUtf(24576)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record RequestClientPolicy() implements CustomPacketPayload {
        public static final Type<RequestClientPolicy> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "request_client_policy"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestClientPolicy> CODEC = StreamCodec.of(
            (buf, data) -> {}, buf -> new RequestClientPolicy());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SaveClientPolicy(boolean enabled, int dontEnforceMask, String defaults, String animations) implements CustomPacketPayload {
        public static final Type<SaveClientPolicy> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "save_client_policy"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveClientPolicy> CODEC = StreamCodec.of(
            (buf, data) -> { buf.writeBoolean(data.enabled); buf.writeVarInt(data.dontEnforceMask); buf.writeUtf(data.defaults, 2048);buf.writeUtf(data.animations,24576); },
            buf -> new SaveClientPolicy(buf.readBoolean(), buf.readVarInt(), buf.readUtf(2048),buf.readUtf(24576)));
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
        PayloadRegistrar registrar = event.registrar("47");
        registrar.playToServer(FeintEnded.TYPE, FeintEnded.CODEC, (data, context) -> {
            if (context.player() instanceof ServerPlayer player) GuardState.feinted(player);
        });
        registrar.playToClient(MobCounter.TYPE, MobCounter.CODEC, (data, context) -> GuardClient.mobCounter(data));
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
        registrar.playToClient(MobSettings.TYPE, MobSettings.CODEC, (data, context) -> GuardClient.mobSettings(data));
        registrar.playToClient(GuardPose.TYPE, GuardPose.CODEC, (data, context) -> dev.zeli.mallardguard.client.GuardThirdPerson.pose(data));
        registrar.playToServer(SaveConfig.TYPE, SaveConfig.CODEC, GuardPackets::saveConfig);
        registrar.playToClient(SaveResult.TYPE, SaveResult.CODEC, (data, context) -> GuardClient.configSaveResult(data));
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
        PacketDistributor.sendToPlayer(player, GuardConfig.snapshot(player.hasPermissions(2)));
        PacketDistributor.sendToPlayer(player, GuardConfig.shieldSnapshot());
        PacketDistributor.sendToPlayer(player, GuardDamageRules.state(player));
    }

    public static ClientPolicy clientPolicy() {
        int[] saved = GuardClientPreset.parse(GuardConfig.CLIENT_PRESET.get());
        int[] effective = GuardClientPreset.readEnforceTemplate(saved == null ? GuardClientPreset.DEFAULTS : saved);
        GuardRetaliation.enforcedFrames(effective[3]);
        return new ClientPolicy(GuardConfig.ENFORCE_CLIENT.get(), GuardConfig.CLIENT_EXEMPT_MASK.get() & GuardClientPreset.ALL_CATEGORIES, GuardClientPreset.encode(effective),GuardPoseLibrary.enforcedAnimations());
    }

    private static boolean saveClientPolicy(SaveClientPolicy data, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return false;
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("Mallard Guard: Only operators can change client enforcement."));
            return false;
        }
        if (data.dontEnforceMask() < 0 || (data.dontEnforceMask() & ~GuardClientPreset.ALL_CATEGORIES) != 0 || !GuardClientPreset.valid(data.defaults()) || !data.animations().isEmpty() && GuardPoseLibrary.parseAnimations(data.animations())==null) {
            player.sendSystemMessage(Component.literal("Mallard Guard: Invalid client enforcement settings."));
            return false;
        }
        int[] preset = GuardClientPreset.parse(data.defaults());
        if (preset == null || !GuardPoseLibrary.saveEnforcedAnimations(data.animations()) || !GuardClientPreset.writeEnforceTemplate(preset)) {
            player.sendSystemMessage(Component.literal("Mallard Guard: Could not save the enforce template."));
            return false;
        }
        GuardConfig.ENFORCE_CLIENT.set(data.enabled());
        GuardConfig.CLIENT_EXEMPT_MASK.set(data.dontEnforceMask());
        GuardConfig.CLIENT_PRESET.set(data.defaults());
        GuardConfig.SERVER_SPEC.save();
        GuardConfig.clearConfigBackups();
        ClientPolicy policy = clientPolicy();
        for (ServerPlayer connected : player.serverLevel().getServer().getPlayerList().getPlayers())
            PacketDistributor.sendToPlayer(connected, policy);
        return true;
    }

    private static boolean save(Save data, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return false;
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("Mallard Guard: Only operators can change server settings."));
            return false;
        }
        if (!GuardSettingRanges.accepts(GuardConfig.PERFECT_TICKS, data.perfect()) || data.perfect() > data.window() || !GuardSettingRanges.accepts(GuardConfig.PARRY_TICKS, data.window())
            || !GuardSettingRanges.accepts(GuardConfig.RECHARGE_TICKS, data.recharge()) || !GuardSettingRanges.accepts(GuardConfig.FACING_ANGLE, data.angle())
            || !GuardSettingRanges.accepts(GuardConfig.BLOCK_REDUCTION, data.reductionPercent() / 100.0D)
            || !GuardSettingRanges.accepts(GuardConfig.FOLLOW_UP_TICKS, data.followUp())
            || !GuardSettingRanges.accepts(GuardConfig.PARRY_RETALIATION, data.parryReturnPercent() / 100.0D)
            || !GuardSettingRanges.accepts(GuardConfig.PERFECT_RETALIATION, data.perfectReturnPercent() / 100.0D)
            || !GuardSettingRanges.accepts(GuardConfig.PARRY_WEAR, data.parryWear())
            || !GuardSettingRanges.accepts(GuardConfig.PERFECT_WEAR, data.perfectWear())
            || !GuardSettingRanges.accepts(GuardConfig.BLOCK_WEAR, data.blockWear())
            || !GuardSettingRanges.accepts(GuardConfig.MASTER_VOLUME, data.masterVolume())
            || !GuardSettingRanges.accepts(GuardConfig.PERFECT_VOLUME, data.perfectVolume())
            || !GuardSettingRanges.accepts(GuardConfig.PARRY_VOLUME, data.parryVolume())
            || !GuardSettingRanges.accepts(GuardConfig.BLOCK_VOLUME, data.blockVolume())
            || !GuardSettingRanges.accepts(GuardConfig.FALL_BLAST_STRENGTH, data.fallBlastStrength())
            || !GuardSettingRanges.accepts(GuardConfig.FALL_LAUNCH_POWER, data.fallLaunchPower())
            || !GuardSettingRanges.accepts(GuardConfig.KNOCKBACK_STRENGTH, data.knockbackStrength())
            || !GuardSettingRanges.accepts(GuardConfig.GUARD_MOVEMENT_PERCENT, data.guardMovementPercent())
            || !GuardSettingRanges.accepts(GuardConfig.BLOCK_DEFLECT_CHANCE, data.blockDeflectChance())
            || !GuardItemRules.valid(data.includedItems()) || !GuardItemRules.valid(data.excludedItems()) || !GuardItemRules.valid(data.shieldItems())) {
            player.sendSystemMessage(Component.literal("Mallard Guard: Server settings were rejected. Check the values and item IDs."));
            return false;
        }
        GuardConfig.apply(new Settings(data.parry(), data.block(), data.parryDrowningFire(), data.parryStarvation(), data.parryGenericKill(), data.perfect(), data.window(), data.recharge(), data.angle(), data.reductionPercent(), data.followUp(), data.parryReturnPercent(), data.perfectReturnPercent(), data.parryWear(), data.perfectWear(), data.blockWear(), data.hitSounds(), data.hitParticles(), data.masterVolume(), data.perfectVolume(), data.parryVolume(), data.blockVolume(), data.fallParry(), data.fallPerfectParry(), data.fallLookDown(), data.fallBreakBlocks(), data.fallBlastStrength(), data.fallLaunchPower(), data.parryExplosions(), data.perfectExplosionsOnly(), data.blockExplosions(), data.parryProjectiles(), data.blockProjectiles(), data.defenderKnockback(), data.knockbackStrength(), data.guardMovementPercent(), data.blockDeflectChance(), data.allowAnyItem(), data.allowUsableItems(), data.includedItems(), data.excludedItems(), data.shieldItems(), data.consumablePriority(), true));
        broadcastEligibility(player.serverLevel().getServer());
        return true;
    }

    public static void broadcastEligibility(net.minecraft.server.MinecraftServer server) {
        Settings operator = GuardConfig.snapshot(true), visitor = GuardConfig.snapshot(false);
        for (ServerPlayer connected : server.getPlayerList().getPlayers())
            PacketDistributor.sendToPlayer(connected, connected.hasPermissions(2) ? operator : visitor);
    }

    private static boolean saveDamageRules(SaveDamageRules data, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return false;
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("Mallard Guard: Only operators can change damage type rules."));
            return false;
        }
        var rules = GuardDamageRules.parseRules(data.rules());
        if (rules == null) {
            player.sendSystemMessage(Component.literal("Mallard Guard: Invalid damage type rules."));
            return false;
        }
        GuardConfig.DAMAGE_RULES.set(data.rules());
        GuardConfig.SERVER_SPEC.save();
        GuardConfig.clearConfigBackups();
        return true;
    }



    private static boolean saveMobs(SaveMobs packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.hasPermissions(2)) return false;
        MobSettings data = packet.settings();
        if (!GuardSettingRanges.accepts(GuardConfig.MOB_DIFFICULTY, data.difficulty())
            || !GuardSettingRanges.accepts(GuardConfig.MOB_PARRY_COLOR, data.parryColor())
            || !GuardSettingRanges.accepts(GuardConfig.MOB_PERFECT_COLOR, data.perfectColor())
            || !GuardSettingRanges.accepts(GuardConfig.MOB_HITS_TO_GUARD, data.hitsBeforeGuard())
            || !GuardSettingRanges.accepts(GuardConfig.MOB_PERFECT_TICKS, data.perfectTicks())
            || !GuardSettingRanges.accepts(GuardConfig.MOB_PARRY_TICKS, data.parryTicks())
            || !GuardSettingRanges.accepts(GuardConfig.MOB_COUNTER_TICKS, data.counterTicks())
            || !GuardSettingRanges.accepts(GuardConfig.MOB_APPROACH_CHANCE, data.approachChance())
            || !GuardSettingRanges.accepts(GuardConfig.MOB_TACTICAL_CHANCE, data.tacticalChance())
            || !GuardSettingRanges.accepts(GuardConfig.MOB_TACTICAL_SECONDS, data.tacticalSeconds())
            || !GuardSettingRanges.accepts(GuardConfig.MOB_TACTICAL_COOLDOWN, data.tacticalCooldownTicks())
            || !GuardSettingRanges.accepts(GuardConfig.MOB_MOVEMENT, data.movement())
            || !GuardSettingRanges.accepts(GuardConfig.MOB_APPROACH_DISTANCE, data.approachDistance())
            || !GuardSettingRanges.accepts(GuardConfig.MOB_APPROACH_SECONDS, data.approachSeconds())
            || !GuardSettingRanges.accepts(GuardConfig.MOB_RUSH_CHANCE, data.rushChance())
            || !GuardSettingRanges.accepts(GuardConfig.MOB_GEAR_CHANCE, data.gearChance())
            || !GuardItemRules.valid(data.blacklist()) || data.blacklist().contains("#")
            || !GuardItemRules.valid(data.humanoidIds()) || data.humanoidIds().contains("#")) return false;
        GuardConfig.MOB_BLACKLIST.set(data.blacklist());
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
        return true;
    }

    private static boolean saveShield(SaveShield packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return false;
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("Mallard Guard: Only operators can change shield settings."));
            return false;
        }
        ShieldSettings data = packet.settings();
        if (!GuardSettingRanges.accepts(GuardConfig.SHIELD_PERFECT_TICKS, data.perfect()) || data.perfect() > data.window() || !GuardSettingRanges.accepts(GuardConfig.SHIELD_PARRY_TICKS, data.window())
            || !GuardSettingRanges.accepts(GuardConfig.SHIELD_RECHARGE_TICKS, data.rechargeTicks())
            || !GuardSettingRanges.accepts(GuardConfig.SHIELD_MAX_BLOCKS, data.maxBlocks()) || !GuardSettingRanges.accepts(GuardConfig.TOOL_MAX_BLOCKS, data.toolMaxBlocks())
            || data.breakTicks() < GuardConfig.RECHARGE_TICKS.get() || !GuardSettingRanges.accepts(GuardConfig.SHIELD_BREAK_TICKS, data.breakTicks())
            || !GuardSettingRanges.accepts(GuardConfig.SHIELD_CONE_DEGREES, data.cone()) || !GuardSettingRanges.accepts(GuardConfig.SHIELD_CONE_REACH, data.reach())
            || !GuardSettingRanges.accepts(GuardConfig.SHIELD_RETALIATION_PERCENT, data.retaliation()) || !GuardSettingRanges.accepts(GuardConfig.RETALIATION_CAP, data.retaliationCap())
            || !GuardSettingRanges.accepts(GuardConfig.SHIELD_STUN_TICKS, data.stunTicks()) || !GuardSettingRanges.accepts(GuardConfig.SHIELD_PUSHBACK_PERCENT, data.perfectPushback())
            || !GuardSettingRanges.accepts(GuardConfig.SHIELD_PARRY_PUSHBACK_PERCENT, data.regularPushback()) || !GuardSettingRanges.accepts(GuardConfig.TOOL_PUSHBACK_PERCENT, data.weaponPushback())
            || !GuardItemRules.valid(data.stunnableBosses()) || !GuardItemRules.valid(data.shieldBlacklist())
            || !GuardSettingRanges.accepts(GuardConfig.PARRY_HAND_PRIORITY, data.parryHandPriority()) || !GuardSettingRanges.accepts(GuardConfig.SHIELD_PARRY_PRIORITY, data.shieldParryPriority())) {
            player.sendSystemMessage(Component.literal("Mallard Guard: Invalid shield settings."));
            return false;
        }
        GuardConfig.applyShield(data);
        ShieldSettings snapshot = GuardConfig.shieldSnapshot();
        for (ServerPlayer connected : player.serverLevel().getServer().getPlayerList().getPlayers())
            PacketDistributor.sendToPlayer(connected, snapshot);
        return true;
    }
}
