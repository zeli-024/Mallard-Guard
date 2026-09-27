package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.GuardParticles;
import dev.zeli.mallardguard.MallardGuard;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import com.mojang.blaze3d.vertex.VertexConsumer;

@EventBusSubscriber(modid = MallardGuard.ID, value = Dist.CLIENT)
public final class GuardFireworkFlashParticle extends TextureSheetParticle {
    private final float size;
    private final float peakAlpha;

    private GuardFireworkFlashParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, boolean perfect) {
        super(level, x, y, z);
        lifetime = 4;
        hasPhysics = false;
        size = (perfect ? GuardConfig.PERFECT_ORB_SIZE.get() : GuardConfig.REGULAR_ORB_SIZE.get()) / 100.0F;
        peakAlpha = (perfect ? GuardConfig.PERFECT_ORB_OPACITY.get() : GuardConfig.REGULAR_ORB_OPACITY.get()) / 100.0F;
        pickSprite(sprites);
    }

    @Override public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override public float getQuadSize(float partialTick) {
        // Vanilla's flash hardcodes its size, so scale its expansion directly here.
        float progress = Mth.clamp((age + partialTick) / lifetime, 0.0F, 1.0F);
        return 7.1F * size * Mth.sin(progress * (float) Math.PI);
    }

    @Override public void render(VertexConsumer vertices, Camera camera, float partialTick) {
        float progress = Mth.clamp((age + partialTick) / lifetime, 0.0F, 1.0F);
        setAlpha(peakAlpha * (1.0F - (3.0F / 7.0F) * progress));
        super.render(vertices, camera, partialTick);
    }

    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(GuardParticles.PARRY_FLASH.get(), sprites -> new Provider(sprites, false));
        event.registerSpriteSet(GuardParticles.PERFECT_FLASH.get(), sprites -> new Provider(sprites, true));
    }

    private record Provider(SpriteSet sprites, boolean perfect) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new GuardFireworkFlashParticle(level, x, y, z, sprites, perfect);
        }
    }
}
