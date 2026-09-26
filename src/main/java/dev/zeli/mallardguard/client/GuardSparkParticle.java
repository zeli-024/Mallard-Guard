package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardParticles;
import dev.zeli.mallardguard.MallardGuard;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = MallardGuard.ID, value = Dist.CLIENT)
public final class GuardSparkParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final boolean flash;

    private GuardSparkParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, boolean flash) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.flash = flash;
        this.hasPhysics = !flash;
        this.lifetime = flash ? 5 : 12 + random.nextInt(8);
        this.quadSize = flash ? 0.30F : 0.10F + random.nextFloat() * 0.07F;
        this.gravity = flash ? 0 : 0.15F;
        this.friction = 0.88F;
        this.xd = xd;
        this.yd = yd + (flash ? 0 : 0.10D);
        this.zd = zd;
        setSpriteFromAge(sprites);
    }

    @Override public void tick() {
        super.tick();
        if (isAlive()) {
            setSpriteFromAge(sprites);
            alpha = 1.0F - (float) age / lifetime;
        }
    }

    @Override public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(GuardParticles.FLYING_SPARK.get(), sprites -> new Provider(sprites, false));
        event.registerSpriteSet(GuardParticles.SPARK_FLASH.get(), sprites -> new Provider(sprites, true));
    }

    private record Provider(SpriteSet sprites, boolean flash) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new GuardSparkParticle(level, x, y, z, xd, yd, zd, sprites, flash);
        }
    }
}
