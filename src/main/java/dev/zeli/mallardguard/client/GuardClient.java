package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.GuardClientPreset;
import dev.zeli.mallardguard.GuardItemRules;
import dev.zeli.mallardguard.GuardPackets;
import dev.zeli.mallardguard.GuardParticles;
import dev.zeli.mallardguard.GuardSounds;
import dev.zeli.mallardguard.GuardState;
import dev.zeli.mallardguard.MallardGuard;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.registries.DeferredHolder;
import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.fml.ModContainer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = MallardGuard.ID, value = Dist.CLIENT)
public final class GuardClient {
    private static final KeyMapping GUARD_KEY = new KeyMapping("key.mallardguard.guard",
        InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_RIGHT, "key.categories.mallardguard");
    private static final KeyMapping CONFIG_KEY = new KeyMapping("key.mallardguard.open_config",
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_CONTROL, "key.categories.mallardguard");
    private static final ResourceLocation SHIELD = ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "textures/gui/shield.png");
    private static final ResourceLocation BLOCK_VIGNETTE = ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "textures/gui/block_vignette.png");
    private static final ResourceLocation[] MEME_FLASHES = {
        ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "textures/gui/meme_flash_1.png"),
        ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "textures/gui/meme_flash_2.png"),
        ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "textures/gui/meme_flash_3.png"),
        ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, "textures/gui/meme_flash_plankton.png")
    };
    private static final long MEME_HOLD_MS = 500;
    private static final long MEME_FADE_MS = 200;
    // Pixel coverage of the supplied 16x16 shield. Used for tinted effects without recoloring its artwork.
    private static final int[] SHIELD_PIXELS = {
        0x0000, 0x03C0, 0x0FF0, 0x1FF8, 0x3FFC, 0x3FFC, 0x3FFC, 0x1FF8,
        0x1FF8, 0x1FF8, 0x0FF0, 0x0FF0, 0x07E0, 0x0180, 0x0000, 0x0000
    };
    private static boolean wasDown;
    private static int phase;
    private static boolean offhand;
    private static int elapsed;
    private static int recharge;
    private static int window = 7;
    private static int rechargeMax = 12;
    private static int hitResult;
    private static boolean guardBreakHitResult;
    private static long resultStartMs;
    private static long screenFlashStartMs;
    private static long memeStartMs;
    private static int memeIndex = -1;
    private static int feedbackSequence;
    private static final long FLASH_DURATION_MS = 420;
    private static final long REGULAR_REACTION_MS = 190;
    private static final long PERFECT_REACTION_MS = 460;
    private static final long BLOCK_REACTION_MS = 240;
    private static final long CHARGE_GLOW_MS = 300;
    private static long chargeGlowStartMs;
    private static long stanceStartMs;
    private static long blockStartMs;
    private static long strainedBlockStartMs;
    private static boolean readyGlowQueued;
    private static boolean wasGuardKeyDown;
    private static boolean waitForGuardRelease;
    private static AttackIndicatorStatus savedAttackIndicator;
    private static boolean policyKnown, policyEnabled, policyRequested;
    private static int dontEnforceMask;
    private static int[] policyDefaults = GuardClientPreset.DEFAULTS.clone(), personalDefaults;

    private GuardClient() {}

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(GUARD_KEY);
        event.register(CONFIG_KEY);
    }

    public static void registerConfigScreen(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> {
            if (Minecraft.getInstance().getConnection() == null)
                return new GuardConfigScreen(GuardConfig.defaultSnapshot(false), parent);
            return new GuardConfigLoadingScreen(parent);
        });
    }

    public static boolean isStanceActive() {
        return phase != 0;
    }

    public static InteractionHand guardHand(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == minecraft.player) return phase == 0 || phase == 4 ? null : (offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        return null;
    }

    public static boolean isCurrentGuardHand(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && phase != 0 && phase != 4 && !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack,
            minecraft.player.getItemInHand(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND));
    }

    public static void status(GuardPackets.Status data) {
        if (phase == 4 && data.phase() == 0 && GUARD_KEY.isDown() && data.recharge() > 0) {
            waitForGuardRelease = true;
        }
        if (phase != 3 && phase != 4 && (data.phase() == 3 || data.phase() == 4)) blockStartMs = System.currentTimeMillis();
        if ((phase == 3 || phase == 4) && data.phase() != 3 && data.phase() != 4) {
            blockStartMs = 0;
            strainedBlockStartMs = 0;
        }
        if (phase == 0 && data.phase() != 0) {
            stanceStartMs = System.currentTimeMillis();
            hitResult = 0;
            resultStartMs = 0;
            screenFlashStartMs = 0;
            memeStartMs = 0;
            readyGlowQueued = false;
            chargeGlowStartMs = 0;
            strainedBlockStartMs = 0;
        }
        if (recharge > 0 && data.recharge() == 0 && data.phase() == 0 && phase != 1) {
            // A perfect parry resets recharge instantly. Its reaction is the only visual.
            readyGlowQueued = true;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            if (data.phase() == 0 && phase != 0 && mc.player.isUsingItem()
                && GuardItemRules.shieldLike(mc.player.getUseItem())) mc.player.stopUsingItem();
            InteractionHand hand = data.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            if (data.phase() != 0 && GUARD_KEY.isDown() && (!mc.player.isUsingItem() || mc.player.getUsedItemHand() != hand)
                && GuardItemRules.shieldLike(mc.player.getItemInHand(hand))
                && !mc.player.getCooldowns().isOnCooldown(mc.player.getItemInHand(hand).getItem())) mc.player.startUsingItem(hand);
        }
        phase = data.phase();
        offhand = data.offhand();
        elapsed = data.elapsed();
        recharge = data.recharge();
        window = data.window();
        rechargeMax = data.rechargeMax();
    }

    public static void settings(GuardPackets.Settings data) {
        serverConsumablePriority = data.consumablePriority();
        Minecraft minecraft = Minecraft.getInstance();
        // Ignore a late reply if the player already backed out of the Mods menu.
        if (minecraft.screen != null && !(minecraft.screen instanceof GuardConfigLoadingScreen)) return;
        Screen parent = minecraft.screen instanceof GuardConfigLoadingScreen loading ? loading.parent() : null;
        minecraft.setScreen(new GuardConfigScreen(data, parent));
    }

    public static void damageState(GuardPackets.DamageState data) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof GuardConfigScreen screen) screen.updateDamageState(data);
    }

    public static void damageHit(GuardPackets.DamageHit data) {
        if (Minecraft.getInstance().screen instanceof GuardConfigScreen screen) screen.updateDamageHit(data.id());
    }

    private static boolean serverConsumablePriority = true;
    private static GuardPackets.ShieldSettings latestShieldSettings;

    public static void shieldSettings(GuardPackets.ShieldSettings data) {
        latestShieldSettings = data;
        serverConsumablePriority = data.consumablePriority();
        GuardState.setClientCooldownPreventsGuard(data.cooldownPreventsGuard());
        if (Minecraft.getInstance().screen instanceof GuardConfigScreen screen) screen.updateShieldSettings(data);
    }

    public static GuardPackets.ShieldSettings currentShieldSettings() {
        return latestShieldSettings == null ? GuardConfig.defaultShieldSnapshot() : latestShieldSettings;
    }

    public static boolean clientCategoryLocked(int category) {
        return policyEnabled && (dontEnforceMask & category) == 0;
    }

    public static GuardPackets.ClientPolicy currentPolicy() {
        return new GuardPackets.ClientPolicy(policyEnabled, dontEnforceMask, GuardClientPreset.encode(policyDefaults));
    }

    public static void clientPolicy(GuardPackets.ClientPolicy data) {
        int[] values = GuardClientPreset.parse(data.defaults());
        if (values == null || data.dontEnforceMask() < 0 || data.dontEnforceMask() > 15) return;
        if (!policyKnown) {
            personalDefaults = GuardClientPreset.readLocal();
            policyKnown = true;
        }
        boolean firstActivation = data.enabled() && !policyEnabled;
        if (!data.enabled() && policyEnabled) {
            for (int category : new int[] {1, 2, 4, 8}) GuardClientPreset.apply(personalDefaults, category);
        }
        policyEnabled = data.enabled();
        dontEnforceMask = data.dontEnforceMask();
        policyDefaults = values;
        if (policyEnabled) {
            for (int category : new int[] {1, 2, 4, 8}) {
                if (firstActivation || clientCategoryLocked(category)) GuardClientPreset.apply(values, category);
            }
            if (firstActivation) {
                for (int index = 0; index < values.length; index++) {
                    if ((dontEnforceMask & GuardClientPreset.categoryOf(index)) != 0)
                        personalDefaults[index] = values[index];
                }
            }
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof GuardConfigScreen config) config.refreshPolicy();
    }

    public static void saveClientConfig() {
        if (policyEnabled && personalDefaults != null) {
            int[] edited = GuardClientPreset.readLocal();
            for (int index = 0; index < edited.length; index++) {
                if (!clientCategoryLocked(GuardClientPreset.categoryOf(index))) personalDefaults[index] = edited[index];
            }
            for (int category : new int[] {1, 2, 4, 8})
                if (clientCategoryLocked(category)) GuardClientPreset.apply(personalDefaults, category);
            GuardConfig.CLIENT_SPEC.save();
            for (int category : new int[] {1, 2, 4, 8})
                if (clientCategoryLocked(category)) GuardClientPreset.apply(policyDefaults, category);
        } else GuardConfig.CLIENT_SPEC.save();
    }

    private static void clearClientPolicy() {
        if (policyKnown && policyEnabled && personalDefaults != null)
            for (int category : new int[] {1, 2, 4, 8}) GuardClientPreset.apply(personalDefaults, category);
        policyKnown = policyEnabled = false;
        policyRequested = false;
        dontEnforceMask = 0;
        personalDefaults = null;
    }

    public static void hitResult(GuardPackets.HitResult data) {
        if (data.result() < 1 || data.result() > 3) return;
        hitResult = data.result();
        guardBreakHitResult = data.guardBroken();
        resultStartMs = System.currentTimeMillis();
        screenFlashStartMs = 0;
        memeStartMs = 0;
        int sequence = ++feedbackSequence;
        if (data.result() == 1 || data.result() == 2) {
            int result = data.result();
            boolean meme = GuardConfig.MEME_FLASH.get();
            GuardHitlag.trigger(() -> {
                if (sequence != feedbackSequence || Minecraft.getInstance().player == null) return;
                screenFlashStartMs = System.currentTimeMillis();
                if (meme) {
                    memeStartMs = screenFlashStartMs;
                    playMemeFlashSound(result);
                    var random = Minecraft.getInstance().player.getRandom();
                    if (memeIndex < 0) memeIndex = random.nextInt(MEME_FLASHES.length);
                    else {
                        int next = random.nextInt(MEME_FLASHES.length - 1);
                        memeIndex = next >= memeIndex ? next + 1 : next;
                    }
                }
            });
        }
        chargeGlowStartMs = 0;
        if (hitResult == 3 && strainedBlockStartMs == 0) strainedBlockStartMs = resultStartMs;
        if (hitResult == 2) readyGlowQueued = false;
    }

    public static void hitSound(GuardPackets.HitSound data) {
        if (data.defender() && data.parry() && GuardConfig.HITLAG_FRAMES.get() > 0) {
            GuardHitlag.afterFreeze(() -> playHitSoundNow(data));
            return;
        }
        playHitSoundNow(data);
    }

    private static void playHitSoundNow(GuardPackets.HitSound data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !Float.isFinite(data.volume()) || data.volume() < 0.0F || data.volume() > 4.0F) return;
        List<DeferredHolder<SoundEvent, SoundEvent>> sounds = switch (data.kind()) {
            case 1 -> GuardSounds.PARRY;
            case 2 -> GuardSounds.PERFECT;
            case 3 -> GuardSounds.BLOCK;
            case 4 -> GuardSounds.SHIELD_PERFECT;
            case 5 -> GuardSounds.FALL_PARRY;
            default -> List.of();
        };
        if (data.variant() < 0 || data.variant() >= sounds.size()) return;
        int individual = switch (data.kind()) {
            case 1 -> GuardConfig.LOCAL_PARRY_VOLUME.get();
            case 2, 4, 5 -> GuardConfig.LOCAL_PERFECT_VOLUME.get();
            default -> GuardConfig.LOCAL_BLOCK_VOLUME.get();
        };
        float gain = data.volume() * GuardConfig.LOCAL_MASTER_VOLUME.get() / 100.0F * individual / 100.0F;
        if (gain <= 0) return;
        if (gain > 1.0F) sounds = switch (data.kind()) {
            case 2 -> GuardSounds.PERFECT_BOOST;
            case 3 -> GuardSounds.BLOCK_BOOST;
            case 4 -> GuardSounds.SHIELD_PERFECT_BOOST;
            case 5 -> GuardSounds.FALL_PARRY_BOOST;
            default -> GuardSounds.PARRY_BOOST;
        };
        playWithHeadroom(mc, data.x(), data.y(), data.z(), sounds.get(data.variant()).get(), gain);
    }

    private static void playWithHeadroom(Minecraft mc, double x, double y, double z, SoundEvent sound, float gain) {
        // Use the louder recording above 100%; one source avoids simultaneous
        // copies of the same clip interfering with short parry sounds.
        float primary = gain <= 1.0F ? gain : Math.min(1.0F, 0.85F + 0.15F * (float) (Math.log(gain) / Math.log(16)));
        mc.level.playLocalSound(x, y, z, sound, SoundSource.PLAYERS, primary, 1.0F, false);
    }

    private static void playMemeFlashSound(int result) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        int individual = result == 2 ? GuardConfig.LOCAL_PERFECT_VOLUME.get() : GuardConfig.LOCAL_PARRY_VOLUME.get();
        float gain = GuardConfig.LOCAL_MASTER_VOLUME.get() / 100.0F * individual / 100.0F;
        if (gain <= 0) return;
        SoundEvent sound = gain > 1.0F ? GuardSounds.MEME_FLASH_BOOST.get() : GuardSounds.MEME_FLASH.get();
        playWithHeadroom(mc, mc.player.getX(), mc.player.getY(), mc.player.getZ(), sound, gain);
    }

    public static void sparks(GuardPackets.Sparks data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || data.count() < 1 || data.count() > 32) return;
        // Local effect toggles do not affect what other players see.
        if (GuardConfig.PARTICLE_FLASHES_ENABLED.get()) {
            mc.level.addParticle(data.perfect() ? GuardParticles.PERFECT_FLASH.get() : GuardParticles.PARRY_FLASH.get(),
                data.x(), data.y(), data.z(), 0, 0, 0);
        }
        if (!GuardConfig.SPARKS_ENABLED.get()) return;
        if (data.perfect()) {
            // Particle Interactions' separate star sprites accompany only perfect parries.
            double starOffset = mc.level.random.nextDouble() * Math.PI * 2.0D;
            int stars = GuardConfig.STAR_COUNT.get();
            for (int i = 0; i < stars; i++) {
                Vec3 direction = burstDirection(i, stars, starOffset, mc.level.random, data);
                double x = data.x() + (mc.level.random.nextDouble() - 0.5D) * 0.06D;
                double y = data.y() + (mc.level.random.nextDouble() - 0.5D) * 0.06D;
                double z = data.z() + (mc.level.random.nextDouble() - 0.5D) * 0.06D;
                // Star sprites follow the streaks' outward burst instead of lingering near the hit.
                double speed = (0.34D + mc.level.random.nextDouble() * 0.19D) * GuardConfig.SPARK_EXPLOSIVENESS.get() / 100.0D;
                if (data.fall() || !data.shield() && GuardConfig.SPARK_RING.get()) speed *= 0.60D;
                mc.level.addParticle(GuardParticles.SPARK_FLASH.get(), x, y, z,
                    direction.x * speed, direction.y * speed, direction.z * speed);
            }
        }
        double angleOffset = mc.level.random.nextDouble() * Math.PI * 2.0D;
        int amount = (int) Math.round(data.count() * (data.perfect() ? GuardConfig.PERFECT_SPARK_COUNT.get() : GuardConfig.REGULAR_SPARK_COUNT.get()) / 100.0D);
        for (int i = 0; i < amount; i++) {
            Vec3 direction = burstDirection(i, amount, angleOffset, mc.level.random, data);
            // One anvil-style spray per hit. The old three delayed emissions
            // made a single parry look as though the effect had played twice.
            double x = data.x() + (mc.level.random.nextDouble() - 0.5D) * 0.06D;
            double y = data.y() + (mc.level.random.nextDouble() - 0.5D) * 0.06D;
            double z = data.z() + (mc.level.random.nextDouble() - 0.5D) * 0.06D;
            double speed = (data.perfect() ? 0.77D : 0.61D) + mc.level.random.nextDouble() * (data.perfect() ? 0.47D : 0.44D);
            if (data.fall() || !data.shield() && GuardConfig.SPARK_RING.get()) speed *= 0.60D;
            double burst = GuardConfig.SPARK_EXPLOSIVENESS.get() / 100.0D;
            double vx = (direction.x * speed + (data.shield() ? 0.0D : (mc.level.random.nextDouble() - 0.5D) * 0.16D)) * burst;
            double vy = (direction.y * speed + (mc.level.random.nextDouble() - 0.5D) * 0.16D) * burst;
            double vz = (direction.z * speed + (data.shield() ? 0.0D : (mc.level.random.nextDouble() - 0.5D) * 0.16D)) * burst;
            mc.level.addParticle(GuardParticles.FLYING_SPARK.get(), x, y, z, vx, vy, vz);
        }
    }

    private static Vec3 burstDirection(int index, int count, double offset, RandomSource random, GuardPackets.Sparks hit) {
        if (!hit.fall() && hit.shield()) {
            // Shield impacts fan toward the incoming attacker across a 90-degree horizontal arc.
            double facing = Math.atan2(hit.directionZ(), hit.directionX());
            double angle = facing - Math.PI / 4.0D + Math.PI / 2.0D * (index + 0.5D) / Math.max(1, count);
            double vertical = 0.08D + random.nextDouble() * 0.45D;
            double horizontal = Math.sqrt(1.0D - vertical * vertical);
            return new Vec3(Math.cos(angle) * horizontal, vertical, Math.sin(angle) * horizontal);
        }
        if (hit.fall() || GuardConfig.SPARK_RING.get()) {
            // Four close diagonal arms form a short X instead of a wide, even ring.
            int arm = index % 4;
            double angle = offset + Math.PI / 4.0D + arm * Math.PI / 2.0D
                + (random.nextDouble() - 0.5D) * 0.16D;
            double vertical = (arm % 2 == 0 ? 0.16D : -0.06D)
                + (random.nextDouble() - 0.5D) * 0.10D;
            double horizontal = Math.sqrt(1.0D - vertical * vertical);
            return new Vec3(Math.cos(angle) * horizontal, vertical, Math.sin(angle) * horizontal);
        }
        // Spread across a rounded burst while avoiding steep floor-bound shots.
        // Redistribute directions into the upper sphere rather than clamping
        // a full sphere, which would pile particles into a flat bottom band.
        double vertical = Math.clamp(0.85D - 1.10D * (index + 0.5D) / count
            + (random.nextDouble() - 0.5D) * 0.08D, -0.25D, 0.85D);
        double angle = offset + index * (Math.PI * (3.0D - Math.sqrt(5.0D)))
            + (random.nextDouble() - 0.5D) * 0.3D;
        double horizontal = Math.sqrt(1.0D - vertical * vertical);
        return new Vec3(Math.cos(angle) * horizontal, vertical, Math.sin(angle) * horizontal);
    }

    @SubscribeEvent
    public static void cameraShake(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || !GuardConfig.SCREEN_SHAKE.get() || GuardConfig.SHAKE_STRENGTH.get() == 0) return;
        long age = System.currentTimeMillis() - resultStartMs;
        if (hitResult == 0 || age < 0 || age >= 320) return;
        float resultScale = hitResult == 3 ? 1.4F : hitResult == 2 ? 1.0F : 0.55F;
        float amplitude = resultScale * GuardConfig.SHAKE_STRENGTH.get() / 100.0F * (1.0F - age / 320.0F);
        event.setRoll(event.getRoll() + (float) Math.sin(age * 0.095D) * 2.5F * amplitude);
        event.setPitch(event.getPitch() + (float) Math.sin(age * 0.135D) * 1.4F * amplitude);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void keepGuardSpeedOutOfFov(ComputeFovModifierEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || event.getPlayer() != mc.player) return;
        AttributeInstance speed = mc.player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        AttributeModifier guard = speed.getModifier(GuardState.GUARD_SLOWDOWN);
        if (guard == null || guard.amount() >= 0) return;
        double walking = mc.player.getAbilities().getWalkingSpeed();
        double speedFraction = 1.0D + guard.amount();
        if (walking <= 0.0D) return;
        // Undo only our movement speed modifier's contribution to the vanilla
        // speed FOV factor. Bow, sprint, potions and other FOV modifiers remain.
        double currentFactor = (speed.getValue() / walking + 1.0D) * 0.5D;
        double unguardedSpeed = speedFraction > 0.0D ? speed.getValue() / speedFraction : speed.getBaseValue();
        double unguardedFactor = (unguardedSpeed / walking + 1.0D) * 0.5D;
        if (currentFactor > 0.0D && Double.isFinite(unguardedFactor))
            event.setNewFovModifier((float) (event.getNewFovModifier() * unguardedFactor / currentFactor));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (GuardConfig.pendingUpdates() > 0 && (mc.screen instanceof TitleScreen || mc.screen == null && mc.player != null)) {
            mc.setScreen(new GuardConfigUpdateScreen(mc.screen));
            return;
        }
        if (mc.player == null || mc.getConnection() == null) {
            clearClientPolicy();
            latestShieldSettings = null;
            serverConsumablePriority = true;
            GuardState.setClientCooldownPreventsGuard(true);
            if (mc.player != null) GuardState.updateMovement(mc.player, false);
            restoreAttackIndicator();
            wasDown = false;
            wasGuardKeyDown = false;
            waitForGuardRelease = false;
            phase = recharge = 0;
            hitResult = 0;
            resultStartMs = 0;
            chargeGlowStartMs = 0;
            stanceStartMs = 0;
            blockStartMs = 0;
            strainedBlockStartMs = 0;
            readyGlowQueued = false;
            return;
        }
        if (!policyRequested) {
            policyRequested = true;
            PacketDistributor.sendToServer(new GuardPackets.RequestClientPolicy());
        }
        while (CONFIG_KEY.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(mc.getConnection() == null
                    ? new GuardConfigScreen(GuardConfig.defaultSnapshot(false), null)
                    : new GuardConfigLoadingScreen(null));
                break;
            }
        }
        boolean guardKeyDown = mc.screen == null && GUARD_KEY.isDown();
        boolean hasConsumable = serverConsumablePriority && GuardItemRules.consumableInEitherHand(mc.player);
        if (!guardKeyDown) waitForGuardRelease = false;
        if (guardKeyDown && hasConsumable) waitForGuardRelease = true;
        if (guardKeyDown && !wasGuardKeyDown && hasConsumable && !mc.options.keyUse.isDown()
            && !mc.player.isUsingItem() && mc.gameMode != null) {
            mc.gameMode.useItem(mc.player, GuardItemRules.consumable(mc.player.getMainHandItem())
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        }
        wasGuardKeyDown = guardKeyDown;
        boolean down = guardKeyDown && !hasConsumable && !waitForGuardRelease;
        if (!down && phase == 4
            && mc.player.isUsingItem() && GuardItemRules.shieldLike(mc.player.getUseItem())) mc.player.stopUsingItem();
        GuardState.updateMovement(mc.player, down && (phase != 0 || !wasDown && recharge == 0 && GuardState.eligible(mc.player)));
        if (down != wasDown) {
            wasDown = down;
            boolean chooseOffhand = down &&
                (GuardConfig.PREFER_OFFHAND.get() ? GuardState.eligible(mc.player, InteractionHand.OFF_HAND)
                    : GuardState.eligible(mc.player, InteractionHand.OFF_HAND)
                        && (!GuardState.eligible(mc.player, InteractionHand.MAIN_HAND)
                            || GuardItemRules.shieldLike(mc.player.getOffhandItem())
                                && mc.player.getMainHandItem().getUseAnimation() == net.minecraft.world.item.UseAnim.NONE));
            InteractionHand hand = chooseOffhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            if (down && GuardItemRules.shieldLike(mc.player.getItemInHand(hand))
                && !mc.player.getCooldowns().isOnCooldown(mc.player.getItemInHand(hand).getItem())
                && (!mc.player.isUsingItem() || mc.player.getUsedItemHand() != hand)) mc.player.startUsingItem(hand);
            PacketDistributor.sendToServer(new GuardPackets.Input(down, chooseOffhand));
        }
    }

    @SubscribeEvent
    public static void blockStandaloneShieldUse(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.isUseItem() && mc.player != null && event.getHand() != null
            && !(serverConsumablePriority && GuardItemRules.consumableInEitherHand(mc.player))
            && GuardItemRules.shieldLike(mc.player.getItemInHand(event.getHand()))) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void prioritizeConsumable(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (!serverConsumablePriority || !event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND || mc.player == null
            || mc.gameMode == null || mc.player.isUsingItem()
            || GuardItemRules.consumable(mc.player.getMainHandItem())
            || !GuardItemRules.consumable(mc.player.getOffhandItem())) return;
        // Bypass the main-hand item so eating/drinking in the offhand works even
        // when that item (such as a shield) normally consumes the right click.
        event.setCanceled(true);
        mc.gameMode.useItem(mc.player, InteractionHand.OFF_HAND);
    }

    // The attack indicator shares vanilla's crosshair/hotbar layers. Hide it only
    // while each layer renders, then restore the player's option immediately.
    @SubscribeEvent
    public static void beforeLayer(RenderGuiLayerEvent.Pre event) {
        if (savedAttackIndicator != null) restoreAttackIndicator();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !GuardConfig.HUD.get() ||
            (phase == 0 && recharge == 0 && !resultVisible() && !readyGlowQueued && !readyGlowVisible())) return;
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR) || event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            savedAttackIndicator = mc.options.attackIndicator().get();
            mc.options.attackIndicator().set(AttackIndicatorStatus.OFF);
        }
    }

    @SubscribeEvent
    public static void afterLayer(RenderGuiLayerEvent.Post event) {
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR) || event.getName().equals(VanillaGuiLayers.HOTBAR)) restoreAttackIndicator();
    }

    private static void restoreAttackIndicator() {
        if (savedAttackIndicator != null) {
            Minecraft.getInstance().options.attackIndicator().set(savedAttackIndicator);
            savedAttackIndicator = null;
        }
    }

    private static boolean resultVisible() {
        long age = System.currentTimeMillis() - resultStartMs;
        return GuardConfig.SHIELD_EFFECTS.get() && hitResult != 0 && resultStartMs != 0 && age >= 0 && age < reactionDuration();
    }

    private static long reactionDuration() {
        return hitResult == 2 ? PERFECT_REACTION_MS : hitResult == 3 ? BLOCK_REACTION_MS : REGULAR_REACTION_MS;
    }

    private static boolean readyGlowVisible() {
        long age = System.currentTimeMillis() - chargeGlowStartMs;
        return GuardConfig.SHIELD_EFFECTS.get() && chargeGlowStartMs != 0 && age >= 0 && age < CHARGE_GLOW_MS;
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        restoreAttackIndicator();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        int crosshairX = (event.getGuiGraphics().guiWidth() - 15) / 2;
        int crosshairY = (event.getGuiGraphics().guiHeight() - 15) / 2;
        int shieldSize = 18;
        float shieldCenterX = crosshairX + 7.5F;
        float shieldCenterY = crosshairY + 15 + 2 + shieldSize / 2.0F;
        long now = System.currentTimeMillis();
        long resultAge = now - resultStartMs;
        long flashAge = now - screenFlashStartMs;
        int screenFlashMs = hitResult == 2 ? 260 : 190;
        if (GuardConfig.MEME_FLASH.get() && memeStartMs != 0 && memeIndex >= 0)
            renderMemeFlash(event.getGuiGraphics(), now - memeStartMs);
        if (GuardConfig.SCREEN_FLASH.get() && GuardConfig.FLASH_STRENGTH.get() > 0 &&
            (hitResult == 1 || hitResult == 2) && screenFlashStartMs != 0 && flashAge >= 0 && flashAge < screenFlashMs) {
            renderScreenFlash(event.getGuiGraphics(), flashAge);
            event.getGuiGraphics().flush();
        }
        if (GuardConfig.SHIELD_EFFECTS.get() && hitResult == 3 && resultStartMs != 0 && resultAge >= 0 && resultAge < FLASH_DURATION_MS) {
            renderBlockVignette(event.getGuiGraphics(), resultAge);
        }
        if (!GuardConfig.HUD.get()) return;
        boolean showingResult = resultVisible();
        // A reaction owns the shield until it finishes. The ready glow starts afterward.
        if (readyGlowQueued && !showingResult && phase == 0 && recharge == 0) {
            readyGlowQueued = false;
            chargeGlowStartMs = now;
        }
        long chargeAge = now - chargeGlowStartMs;
        boolean chargeGlow = readyGlowVisible() && !showingResult && phase == 0 && recharge == 0;
        if (phase == 0 && recharge == 0 && !showingResult && !chargeGlow) return;
        boolean strainedBlock = GuardConfig.SHIELD_EFFECTS.get() && (phase == 3 || phase == 4) && strainedBlockStartMs != 0 && !showingResult;
        float strainedFade = strainedBlock ? Math.clamp((now - strainedBlockStartMs - BLOCK_REACTION_MS) / 220.0F, 0.0F, 1.0F) : 0.0F;
        GuiGraphics graphics = event.getGuiGraphics();
        float progress = showingResult ? Math.clamp(resultAge / (float) reactionDuration(), 0.0F, 1.0F) : 1.0F;
        float chargeProgress = chargeGlow ? chargeAge / (float) CHARGE_GLOW_MS : 1.0F;
        float bob = chargeGlow ? -(float) Math.sin(Math.PI * chargeProgress) * 2.0F : 0.0F;
        float chargeFade = chargeGlow ? Math.clamp((1.0F - chargeProgress) / 0.48F, 0.0F, 1.0F) : 1.0F;
        float entrance = phase != 0 && stanceStartMs != 0 ? Math.clamp((now - stanceStartMs) / 90.0F, 0.0F, 1.0F) : 1.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(shieldCenterX, shieldCenterY + bob, 0);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (showingResult && hitResult == 2) {
            // One large, fading copy of the same artwork, behind the perfect parry.
            renderEcho(graphics, shieldSize / 16.0F * (1.12F + 2.28F * progress), 0.65F * (1.0F - progress), 0, 0);
        } else if (showingResult && hitResult == 3) {
            // Every block, including a weapon block, uses the shield's compact echo.
            renderEcho(graphics, shieldSize / 16.0F * (1.03F + 0.65F * progress),
                0.48F * (1.0F - progress), 0, 0);
            // Three short, staggered ripples barely clear the shield's edge.
            for (int i = 0; i < 3; i++) {
                float echoAge = (resultAge - i * 48.0F) / (BLOCK_REACTION_MS - i * 48.0F);
                if (echoAge < 0 || echoAge >= 1) continue;
                float offset = (i % 2 == 0 ? -1.0F : 1.0F) * (1.0F - echoAge);
                renderBlockEcho(graphics, shieldSize / 16.0F * (1.03F + 0.24F * echoAge),
                    0.40F * (1.0F - echoAge), offset, -offset * 0.5F);
            }
        } else if (strainedBlock) {
            // After the first blocked hit, small dark ripples persist until guard ends.
            for (int i = 0; i < 2; i++) {
                float echoProgress = ((now - strainedBlockStartMs + i * 190) % 380) / 380.0F;
                renderBlockEcho(graphics, shieldSize / 16.0F * (1.02F + 0.29F * echoProgress),
                    0.34F * (1.0F - echoProgress) * strainedFade, 0, 0);
            }
        }
        float pulse = showingResult ? (float) Math.sin(Math.PI * progress) *
            (hitResult == 2 ? 1.00F : hitResult == 1 ? 0.37F : guardBreakHitResult ? 0.23F : 0.18F) : 0.0F;
        float shakeStrength = hitResult == 2 ? 3.8F : hitResult == 3 ? 1.6F : 1.4F;
        float shake = showingResult ? (float) Math.sin(resultAge * (hitResult == 3 ? 0.20D : 0.15D)) * shakeStrength * (1.0F - progress) : 0.0F;
        float verticalShake = showingResult ? (float) Math.cos(resultAge * 0.17D) * shakeStrength * 0.35F * (1.0F - progress) : 0.0F;
        float trembleX = strainedBlock ? (float) Math.sin(now * 0.09D) * 0.80F * strainedFade : 0.0F;
        float trembleY = strainedBlock ? (float) Math.cos(now * 0.13D) * 0.55F * strainedFade : 0.0F;
        graphics.pose().translate(shake + trembleX, verticalShake + trembleY, 0);
        graphics.pose().scale(shieldSize / 16.0F * (1.0F + pulse), shieldSize / 16.0F * (1.0F + pulse), 1.0F);
        graphics.pose().translate(-8, -8, 0);
        if (showingResult) {
            if (hitResult == 3) {
                if (guardBreakHitResult) {
                    // Red marks the block that breaks guard, for both shields and weapons.
                    drawShieldTinted(graphics, 0, 16, (0.38F + 0.38F * (1.0F - progress)) * entrance,
                        0.42F, 0.37F, 0.45F);
                    graphics.pose().pushPose();
                    graphics.pose().translate(8, 8, 0);
                    float redExpansion = 1.0F + 0.23F * (float) Math.sin(Math.PI * progress);
                    graphics.pose().scale(redExpansion, redExpansion, 1.0F);
                    graphics.pose().translate(-8, -8, 0);
                    drawShieldMask(graphics, 0xEF3448, 0.64F * (1.0F - progress * progress));
                    graphics.pose().popPose();
                } else {
                    drawShield(graphics, 0.76F * entrance);
                    drawShieldMask(graphics, 0xFFFFFF, 0.18F * (1.0F - progress));
                }
            } else {
                drawShield(graphics, 0.88F * entrance);
            }
            if (hitResult == 2) {
                // Briefly outgrow the expanding shield so its white edge reads clearly.
                graphics.pose().pushPose();
                graphics.pose().translate(8, 8, 0);
                float whiteExpansion = 1.0F + 0.11F * (float) Math.sin(Math.PI * progress);
                graphics.pose().scale(whiteExpansion, whiteExpansion, 1.0F);
                graphics.pose().translate(-8, -8, 0);
                drawShieldMask(graphics, 0xFFFFFF, 0.23F * (1.0F - progress));
                graphics.pose().popPose();
            } else if (hitResult == 1) {
                drawShieldMask(graphics, 0xFFFFFF, 0.16F * (1.0F - progress));
            }
        } else {
            if (phase == 3 || phase == 4) {
                // The held-block icon is visible before impact, then settles into a darker tremble.
                float holdFade = blockStartMs == 0 ? 1.0F : Math.clamp((now - blockStartMs) / 130.0F, 0.0F, 1.0F);
                float baseOpacity = (0.33F + 0.27F * holdFade) * entrance;
                drawShieldTinted(graphics, 0, 16, baseOpacity * (1.0F - 0.10F * strainedFade),
                    1.0F - 0.45F * strainedFade, 1.0F - 0.50F * strainedFade, 1.0F - 0.41F * strainedFade);
            } else {
                drawShield(graphics, 0.33F * entrance * chargeFade);
            }
            if (chargeGlow) {
                drawShield(graphics, 0.82F * chargeFade);
            } else if (phase == 1 || phase == 2) {
                // Remove the active layer from the top while the parry window runs out.
                int drained = Math.clamp(Math.round(16.0F * elapsed / Math.max(1, window)), 0, 16);
                if (drained < 16) drawShieldRegion(graphics, drained, 16 - drained, 0.82F * entrance);
            } else if (phase != 3 && phase != 4) {
                // Refill the active layer from the bottom after releasing the stance.
                int filled = Math.clamp(Math.round(16.0F * (1.0F - recharge / (float) Math.max(1, rechargeMax))), 0, 16);
                if (filled > 0) drawShieldRegion(graphics, 16 - filled, filled, 0.82F);
            }
        }
        if (chargeGlow) {
            float glow = Math.min(1.0F, chargeProgress / 0.20F) * (1.0F - chargeProgress);
            drawShieldOutline(graphics, glow);
        }
        RenderSystem.disableBlend();
        graphics.pose().popPose();
    }

    private static void drawShield(GuiGraphics graphics, float opacity) {
        drawShieldRegion(graphics, 0, 16, opacity);
    }

    private static void drawShieldRegion(GuiGraphics graphics, int top, int height, float opacity) {
        drawShieldTinted(graphics, top, height, opacity, 1.0F, 1.0F, 1.0F);
    }

    private static void drawShieldTinted(GuiGraphics graphics, int top, int height, float opacity,
                                         float red, float green, float blue) {
        if (height <= 0 || opacity <= 0.0F) return;
        RenderSystem.setShaderColor(red, green, blue, opacity);
        graphics.blit(SHIELD, 0, top, 0, top, 16, height, 16, 16);
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderEcho(GuiGraphics graphics, float scale, float opacity, float x, float y) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-8, -8, 0);
        drawShield(graphics, opacity);
        drawShieldMask(graphics, 0xFFFFFF, opacity * 0.35F);
        graphics.pose().popPose();
    }

    private static void renderBlockEcho(GuiGraphics graphics, float scale, float opacity, float x, float y) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-8, -8, 0);
        drawShieldTinted(graphics, 0, 16, opacity, 0.58F, 0.51F, 0.62F);
        graphics.pose().popPose();
    }

    private static boolean shieldPixel(int x, int y) {
        return x >= 0 && x < 16 && y >= 0 && y < 16 && (SHIELD_PIXELS[y] & (1 << x)) != 0;
    }

    private static void drawShieldMask(GuiGraphics graphics, int color, float opacity) {
        int alpha = Math.round(Math.clamp(opacity, 0.0F, 1.0F) * 255.0F);
        if (alpha == 0) return;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if (shieldPixel(x, y)) graphics.fill(x, y, x + 1, y + 1, (alpha << 24) | color);
            }
        }
    }

    private static void drawShieldOutline(GuiGraphics graphics, float opacity) {
        int alpha = Math.round(Math.clamp(opacity, 0.0F, 1.0F) * 215.0F);
        if (alpha == 0) return;
        for (int y = -1; y <= 16; y++) {
            for (int x = -1; x <= 16; x++) {
                if (shieldPixel(x, y)) continue;
                boolean neighbor = false;
                for (int dy = -1; dy <= 1 && !neighbor; dy++) {
                    for (int dx = -1; dx <= 1; dx++) neighbor |= shieldPixel(x + dx, y + dy);
                }
                if (neighbor) graphics.fill(x, y, x + 1, y + 1, (alpha << 24) | 0xFFFFFF);
            }
        }
    }

    private static void renderBlockVignette(net.minecraft.client.gui.GuiGraphics graphics, long age) {
        // The alpha in this texture follows a rounded radial falloff, not screen-aligned bands.
        float fadeIn = Math.min(1.0F, age / 90.0F);
        float fadeOut = Math.min(1.0F, (FLASH_DURATION_MS - age) / 240.0F);
        float opacity = 0.85F * Math.max(0.0F, Math.min(fadeIn, fadeOut));
        if (opacity <= 0.0F) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, opacity);
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        graphics.blit(BLOCK_VIGNETTE, 0, 0, 0, 0, width, height, width, height);
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void renderScreenFlash(net.minecraft.client.gui.GuiGraphics graphics, long age) {
        float progress = age / (hitResult == 2 ? 260.0F : 190.0F);
        float intensity = GuardConfig.FLASH_STRENGTH.get() / 100.0F * (hitResult == 2 ? 1.0F : 0.82F);
        int alpha = Math.round(255.0F * 0.85F * (1.0F - progress) * intensity);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24 | 0xFFFFFF);
    }

    private static void renderMemeFlash(GuiGraphics graphics, long age) {
        if (age < 0 || age >= MEME_HOLD_MS + MEME_FADE_MS) return;
        float alpha = age < MEME_HOLD_MS ? 1.0F
            : 1.0F - (age - MEME_HOLD_MS) / (float) MEME_FADE_MS;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        // The declared UV size matches the destination, so the entire image fills the GUI.
        graphics.blit(MEME_FLASHES[memeIndex], 0, 0, 0, 0, width, height, width, height);
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (memeIndex == 3) {
            // Brighten the darker Plankton still while preserving its visible face.
            float burst = Math.max(0.0F, 1.0F - age / 180.0F);
            int brightness = Math.round((85.0F + 95.0F * burst) * alpha);
            graphics.fill(0, 0, width, height, brightness << 24 | 0xFFFFFF);
            graphics.flush();
        }
        RenderSystem.disableBlend();
    }

}
