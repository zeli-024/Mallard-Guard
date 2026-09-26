package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.zeli.mallardguard.GuardParticles;
import dev.zeli.mallardguard.MallardGuard;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@EventBusSubscriber(modid = MallardGuard.ID, value = Dist.CLIENT)
public final class GuardSparkParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final boolean flash;
    private final float crossX, crossZ;
    private double olderX, olderY, olderZ;
    private int bounces;
    private int groundedTicks;

    private GuardSparkParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, boolean flash) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.flash = flash;
        this.olderX = x; this.olderY = y; this.olderZ = z;
        if (flash) {
            this.hasPhysics = false;
            this.lifetime = 3 + random.nextInt(4);
            this.quadSize = 0.125F;
            this.friction = 0.96F;
            this.xd = xd; this.yd = yd; this.zd = zd;
            this.crossX = this.crossZ = 1.0F;
        } else {
            // Particle Interactions' long-life FlyingSpark: its input velocity
            // is halved and given a small, occasionally stronger random spread.
            this.xd = scatter(xd);
            this.yd = scatter(yd);
            this.zd = scatter(zd);
            this.gravity = 0.8F + random.nextFloat() * 0.1F;
            this.friction = 1.0F;
            this.quadSize = random.nextBoolean() ? 0.025F : 0.03F;
            this.crossX = 0.4F + random.nextFloat() * 0.7F;
            this.crossZ = 0.4F + random.nextFloat() * 0.7F;
            // Shorter than PI's 20–60 ticks at the user's request.
            this.lifetime = 14 + random.nextInt(9);
            setSize(quadSize, quadSize);
        }
        setSpriteFromAge(sprites);
    }

    private double scatter(double velocity) {
        return velocity * 0.5D + (random.nextDouble() * 3.0D - 1.5D) * 0.05D * (random.nextFloat() > 0.95F ? 2.0D : 1.0D);
    }

    @Override public void tick() {
        olderX = xo; olderY = yo; olderZ = zo;
        double falling = yd;
        super.tick();
        if (!isAlive()) return;
        setSpriteFromAge(sprites);
        if (flash) {
            quadSize = 0.125F * (1.0F - 0.5F * age / lifetime);
            return;
        }
        if (onGround) {
            if (falling < -0.01D && bounces++ == 0) {
                yd = -falling * 0.8D;
                xd *= 0.7D; zd *= 0.7D;
            }
            if (++groundedTicks >= 3 || bounces > 1) {
                remove();
                return;
            }
        } else groundedTicks = 0;
        if (age >= lifetime - 4) alpha = Math.max(0.0F, (lifetime - age) / 4.0F);
    }

    @Override public void renderRotatedQuad(VertexConsumer vertices, Camera camera, Quaternionf rotation, float partialTick) {
        if (flash) {
            super.renderRotatedQuad(vertices, camera, rotation, partialTick);
            return;
        }
        // The source FlyingSpark uses a VERTICAL_CROSS: four crossed faces,
        // with the long dimension stretched along the last two positions.
        Vector3f current = relative(camera, Mth.lerp(partialTick, xo, x), Mth.lerp(partialTick, yo, y), Mth.lerp(partialTick, zo, z));
        Vector3f previous = relative(camera, Mth.lerp(partialTick, olderX, xo), Mth.lerp(partialTick, olderY, yo), Mth.lerp(partialTick, olderZ, zo));
        Vector3f direction = new Vector3f(current).sub(previous);
        float distance = direction.length();
        if (distance < 0.001F) {
            super.renderRotatedQuad(vertices, camera, rotation, partialTick);
            return;
        }
        direction.normalize();
        Vector3f center = new Vector3f(current).add(previous).mul(0.5F);
        float halfLength = quadSize * Math.max(1.0F, distance * 40.0F) * 0.5F;
        Vector3f head = new Vector3f(center).add(new Vector3f(direction).mul(halfLength));
        Vector3f tail = new Vector3f(center).sub(new Vector3f(direction).mul(halfLength));
        Vector3f sideA = new Vector3f(direction).cross(new Vector3f(0, 0, 1));
        if (sideA.lengthSquared() < 0.001F) sideA = new Vector3f(direction).cross(new Vector3f(0, 1, 0));
        sideA.normalize().mul(quadSize * crossX * 0.5F);
        Vector3f sideB = new Vector3f(direction).cross(sideA).normalize().mul(quadSize * crossZ * 0.5F);
        int light = getLightColor(partialTick);
        crossedFace(vertices, head, tail, sideA, light);
        crossedFace(vertices, head, tail, sideB, light);
    }

    private static Vector3f relative(Camera camera, double x, double y, double z) {
        return new Vector3f((float) (x - camera.getPosition().x), (float) (y - camera.getPosition().y), (float) (z - camera.getPosition().z));
    }

    private void crossedFace(VertexConsumer vertices, Vector3f head, Vector3f tail, Vector3f side, int light) {
        face(vertices, head, tail, side, light);
        face(vertices, head, tail, new Vector3f(side).negate(), light);
    }

    private void face(VertexConsumer vertices, Vector3f head, Vector3f tail, Vector3f side, int light) {
        vertex(vertices, new Vector3f(head).sub(side), getU0(), getV1(), light);
        vertex(vertices, new Vector3f(tail).sub(side), getU0(), getV0(), light);
        vertex(vertices, new Vector3f(tail).add(side), getU1(), getV0(), light);
        vertex(vertices, new Vector3f(head).add(side), getU1(), getV1(), light);
    }

    private void vertex(VertexConsumer vertices, Vector3f point, float u, float v, int light) {
        vertices.addVertex(point.x, point.y, point.z)
            .setUv(u, v).setColor(rCol, gCol, bCol, alpha).setLight(light);
    }

    @Override public int getLightColor(float partialTick) {
        if (flash) return 0xF000F0;
        BlockPos pos = BlockPos.containing(x, y, z);
        int glow = (int) (15.0F * (1.0F - (float) age / lifetime));
        return LightTexture.pack(Math.max(level.getBrightness(LightLayer.BLOCK, pos), glow), level.getBrightness(LightLayer.SKY, pos));
    }

    @Override public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
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
