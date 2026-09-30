package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.zeli.mallardguard.GuardParticles;
import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.MallardGuard;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@EventBusSubscriber(modid = MallardGuard.ID, value = Dist.CLIENT)
public final class GuardSparkParticle extends TextureSheetParticle {
    private int tint = -1;
    public void tint(int rgb) { tint = rgb; }
    // Sodium 0.8.x replaces the normal particle batch. Keep the physics in
    // ParticleEngine and draw both streaks and star sprites as world geometry.
    private static final Set<GuardSparkParticle> SPARKS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final RenderType SPARK_RENDER_TYPE = RenderType.entityTranslucent(TextureAtlas.LOCATION_PARTICLES);
    private final SpriteSet sprites;
    private final boolean flash;
    private final boolean stationaryDots;
    private final Vector3f dotDirection;
    private final float dotLength;
    private final boolean regularDots;
    private final int dotCount;
    private final float stretchTicks;
    private final float hitFraction;
    private final Vector3f glideDirection;
    private final Vector3f hitPosition;
    private final float[] ballisticPath;
    private final float crossX, crossZ;
    private double olderX, olderY, olderZ;
    private int bounces;
    private int groundedTicks;
    private final float normalGravity;
    private final double originX, originY, originZ;

    private GuardSparkParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, boolean flash, boolean stationaryDots) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.originX = x; this.originY = y; this.originZ = z;
        this.flash = flash;
        this.stationaryDots = stationaryDots;
        Vector3f direction = new Vector3f((float) xd, (float) yd, (float) zd);
        if (direction.lengthSquared() < 0.0001F) direction.set(0, 1, 0);
        this.regularDots = stationaryDots && direction.lengthSquared() > 2.0F;
        this.dotDirection = direction.normalize();
        double spacing = regularDots ? GuardConfig.REGULAR_STREAK_DOT_SPACING.get() : GuardConfig.STREAK_DOT_SPACING.get();
        this.dotCount = stationaryDots ? Math.clamp((int) Math.round(3600.0D / spacing), 12, 1800) : 0;
        this.dotLength = (0.7F + random.nextFloat() * 0.65F) * 5.0F * GuardConfig.STREAK_LENGTH.get() / 100.0F;
        int curve = stationaryDots ? GuardConfig.STREAK_BALLISTIC_CURVE.get() : 0;
        float collisionFraction = 1.0F;
        Vector3f slide = new Vector3f(dotDirection);
        Vector3f contact = new Vector3f((float) x, (float) y, (float) z);
        if (stationaryDots && curve == 0) {
            Vec3 start = new Vec3(x, y, z);
            Vec3 end = start.add(dotDirection.x * dotLength, dotDirection.y * dotLength, dotDirection.z * dotLength);
            BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, Minecraft.getInstance().player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                collisionFraction = (float) Math.clamp(start.distanceTo(hit.getLocation()) / dotLength, 0.0D, 1.0D);
                contact.set((float) hit.getLocation().x, (float) hit.getLocation().y, (float) hit.getLocation().z);
                float nx = hit.getDirection().getStepX(), ny = hit.getDirection().getStepY(), nz = hit.getDirection().getStepZ();
                float intoSurface = slide.x * nx + slide.y * ny + slide.z * nz;
                slide.sub(nx * intoSurface, ny * intoSurface, nz * intoSurface);
                if (slide.lengthSquared() < 0.0001F) slide.set(ny != 0 ? dotDirection.x : -nz, ny != 0 ? 0 : 0.12F, ny != 0 ? dotDirection.z : nx);
                slide.normalize();
                contact.add(nx * 0.012F, ny * 0.012F, nz * 0.012F);
            }
        }
        this.hitFraction = collisionFraction;
        this.glideDirection = slide;
        this.hitPosition = contact;
        this.ballisticPath = stationaryDots && curve > 0 ? createBallisticPath(level, x, y, z, curve) : null;
        this.olderX = x; this.olderY = y; this.olderZ = z;
        if (stationaryDots) {
            this.hasPhysics = false;
            this.lifetime = GuardConfig.STREAK_LIFETIME.get();
            this.gravity = 0;
            this.xd = this.yd = this.zd = 0;
            this.quadSize = 0.0396F * GuardConfig.STREAK_DOT_SIZE.get() / 100.0F;
            this.crossX = this.crossZ = 1.0F;
        } else if (flash) {
            // Stars travel and bounce like streak sparks, while keeping their sprite shape.
            this.hasPhysics = true;
            this.lifetime = Math.max(1, Math.round(30 * GuardConfig.SPARK_LIFETIME.get() / 100.0F));
            this.gravity = 0.72F;
            this.quadSize = 0.125F;
            this.friction = 0.98F;
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
            // Sparks expire quickly after contact with the ground.
            this.lifetime = Math.max(1, Math.round(30 * GuardConfig.SPARK_LIFETIME.get() / 100.0F));
            setSize(quadSize, quadSize);
        }
        this.normalGravity = this.gravity;
        this.stretchTicks = stationaryDots ? Math.max(0.5F, lifetime * 0.63F * (25.0F / 1.8F)
            / GuardConfig.STREAK_STRETCH_SPEED.get()) : 0.0F;
        setSpriteFromAge(sprites);
    }

    /** Cache a curved path once; drawing dense dots never performs block raycasts. */
    private float[] createBallisticPath(ClientLevel level, double x, double y, double z, int curve) {
        final int steps = 32;
        float[] path = new float[(steps + 1) * 3];
        Vec3 position = new Vec3(x, y, z);
        double stepLength = dotLength / steps;
        double drop = dotLength * curve / 200.0D;
        // Persistent tangent projection lets the line glide without bouncing off a surface.
        Vec3 surfaceNormal = Vec3.ZERO;
        for (int step = 1; step <= steps; step++) {
            double fraction = step / (double) steps, previous = (step - 1) / (double) steps;
            Vec3 movement = new Vec3(dotDirection.x * stepLength,
                dotDirection.y * stepLength - drop * (fraction * fraction - previous * previous),
                dotDirection.z * stepLength);
            double intoSurface = movement.dot(surfaceNormal);
            if (intoSurface < 0) movement = movement.subtract(surfaceNormal.scale(intoSurface));
            else if (intoSurface > 0) surfaceNormal = Vec3.ZERO;
            Vec3 end = position.add(movement);
            BlockHitResult hit = level.clip(new ClipContext(position, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, Minecraft.getInstance().player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                surfaceNormal = new Vec3(hit.getDirection().getStepX(), hit.getDirection().getStepY(), hit.getDirection().getStepZ());
                Vec3 remainder = end.subtract(hit.getLocation());
                remainder = remainder.subtract(surfaceNormal.scale(remainder.dot(surfaceNormal)));
                Vec3 contact = hit.getLocation().add(surfaceNormal.scale(0.012D));
                Vec3 slideEnd = contact.add(remainder);
                BlockHitResult slideHit = level.clip(new ClipContext(contact, slideEnd, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, Minecraft.getInstance().player));
                position = slideHit.getType() == HitResult.Type.BLOCK
                    ? slideHit.getLocation().add(new Vec3(slideHit.getDirection().getStepX(), slideHit.getDirection().getStepY(), slideHit.getDirection().getStepZ()).scale(0.012D))
                    : slideEnd;
            } else position = end;
            int offset = step * 3;
            path[offset] = (float) (position.x - x);
            path[offset + 1] = (float) (position.y - y);
            path[offset + 2] = (float) (position.z - z);
        }
        return path;
    }

    private double scatter(double velocity) {
        return velocity * 0.5D + (random.nextDouble() * 3.0D - 1.5D) * 0.05D * (random.nextFloat() > 0.95F ? 2.0D : 1.0D);
    }

    @Override public void tick() {
        olderX = xo; olderY = yo; olderZ = zo;
        double falling = yd;
        if (!stationaryDots) {
            double distanceSquared = (x - originX) * (x - originX)
                + (y - originY) * (y - originY) + (z - originZ) * (z - originZ);
            double gravityStart = GuardConfig.SPARK_REACH.get() * 0.015D;
            gravity = distanceSquared >= gravityStart * gravityStart ? normalGravity : 0.0F;
        }
        super.tick();
        if (!isAlive()) {
            SPARKS.remove(this);
            return;
        }
        setSpriteFromAge(sprites);
        SPARKS.add(this);
        if (stationaryDots) return;
        if (onGround) {
            if (falling < -0.01D && bounces++ == 0) {
                yd = -falling * (flash ? 0.6D : 0.8D);
                xd *= 0.7D; zd *= 0.7D;
            }
            if (++groundedTicks >= (flash ? 2 : 3) || bounces > 1) {
                remove();
                SPARKS.remove(this);
                return;
            }
        } else groundedTicks = 0;
        if (flash) {
            quadSize = 0.125F * (1.0F - 0.5F * age / lifetime);
            if (age >= lifetime - 3) alpha = Math.max(0.0F, (lifetime - age) / 3.0F);
            return;
        }
        if (age >= lifetime - 4) alpha = Math.max(0.0F, (lifetime - age) / 4.0F);
    }

    private void renderFlash(VertexConsumer vertices, Camera camera, float partialTick) {
        Vector3f center = relative(camera, Mth.lerp(partialTick, xo, x), Mth.lerp(partialTick, yo, y), Mth.lerp(partialTick, zo, z));
        float size = getQuadSize(partialTick);
        vertex(vertices, new Vector3f(-1, -1, 0).rotate(camera.rotation()).mul(size).add(center), getU1(), getV1(), getLightColor(partialTick));
        vertex(vertices, new Vector3f(-1, 1, 0).rotate(camera.rotation()).mul(size).add(center), getU1(), getV0(), getLightColor(partialTick));
        vertex(vertices, new Vector3f(1, 1, 0).rotate(camera.rotation()).mul(size).add(center), getU0(), getV0(), getLightColor(partialTick));
        vertex(vertices, new Vector3f(1, -1, 0).rotate(camera.rotation()).mul(size).add(center), getU0(), getV1(), getLightColor(partialTick));
    }

    private void renderDots(VertexConsumer vertices, Camera camera, float partialTick) {
        // Positions never change. The visible front moves out by revealing successive
        // dots; each dot narrows and fades instead of becoming a moving spark.
        float elapsed = age + partialTick;
        float stretch = stretchTicks;
        float progress = Math.min(1.0F, elapsed / stretch);
        Vector3f origin = relative(camera, x, y, z);
        float originalAlpha = alpha, originalRed = rCol, originalGreen = gCol, originalBlue = bCol;
        int light = 0xF000F0;
        Vector3f right = new Vector3f(1, 0, 0).rotate(camera.rotation());
        Vector3f up = new Vector3f(0, 1, 0).rotate(camera.rotation());
        float fadeSpan = Math.max(0.1F, lifetime - stretch);
        for (int dot = 0; dot < dotCount; dot++) {
            float fraction = (dot + 1.0F) / dotCount;
            if (fraction > progress) break;
            float fadeStart = stretch + fraction * fadeSpan * 0.65F;
            float dotFade = Math.clamp(1.0F - (elapsed - fadeStart) / Math.max(0.1F, fadeSpan * 0.35F), 0.0F, 1.0F);
            if (dotFade <= 0.0F) continue;
            float size = quadSize * (1.0F - fraction * 0.60F) * dotFade;
            alpha = dotFade;
            float cx, cy, cz;
            if (ballisticPath != null) {
                float pathIndex = fraction * (ballisticPath.length / 3 - 1);
                int segment = Math.min((int) pathIndex, ballisticPath.length / 3 - 2);
                float weight = pathIndex - segment;
                int offset = segment * 3;
                cx = origin.x + Mth.lerp(weight, ballisticPath[offset], ballisticPath[offset + 3]);
                cy = origin.y + Mth.lerp(weight, ballisticPath[offset + 1], ballisticPath[offset + 4]);
                cz = origin.z + Mth.lerp(weight, ballisticPath[offset + 2], ballisticPath[offset + 5]);
            } else if (fraction <= hitFraction) {
                cx = origin.x + dotDirection.x * dotLength * fraction;
                cy = origin.y + dotDirection.y * dotLength * fraction;
                cz = origin.z + dotDirection.z * dotLength * fraction;
            } else {
                float glide = dotLength * (fraction - hitFraction);
                cx = (float) (hitPosition.x - camera.getPosition().x) + glideDirection.x * glide;
                cy = (float) (hitPosition.y - camera.getPosition().y) + glideDirection.y * glide;
                cz = (float) (hitPosition.z - camera.getPosition().z) + glideDirection.z * glide;
            }
            rCol = 1.0F;
            float colorPosition = GuardConfig.STREAK_REVERSE.get() ? 1 - fraction : fraction;
            float warmTip = Math.clamp((colorPosition - 0.64F) / 0.36F, 0.0F, 1.0F);
            gCol = 1.0F - 0.27F * warmTip;
            bCol = 1.0F - 0.70F * warmTip;
            if (tint >= 0) {
                rCol = 1.0F + (((tint >> 16 & 255) / 255.0F) - 1.0F) * warmTip;
                gCol = 1.0F + (((tint >> 8 & 255) / 255.0F) - 1.0F) * warmTip;
                bCol = 1.0F + (((tint & 255) / 255.0F) - 1.0F) * warmTip;
            }
            float rx = right.x * size, ry = right.y * size, rz = right.z * size;
            float ux = up.x * size, uy = up.y * size, uz = up.z * size;
            vertexAt(vertices, cx - rx - ux, cy - ry - uy, cz - rz - uz, getU1(), getV1(), light);
            vertexAt(vertices, cx - rx + ux, cy - ry + uy, cz - rz + uz, getU1(), getV0(), light);
            vertexAt(vertices, cx + rx + ux, cy + ry + uy, cz + rz + uz, getU0(), getV0(), light);
            vertexAt(vertices, cx + rx - ux, cy + ry - uy, cz + rz - uz, getU0(), getV1(), light);
        }
        alpha = originalAlpha;
        rCol = originalRed; gCol = originalGreen; bCol = originalBlue;
    }

    private void renderStreak(VertexConsumer vertices, Camera camera, float partialTick) {
        // The source FlyingSpark uses a VERTICAL_CROSS: four crossed faces,
        // with the long dimension stretched along the last two positions.
        Vector3f current = relative(camera, Mth.lerp(partialTick, xo, x), Mth.lerp(partialTick, yo, y), Mth.lerp(partialTick, zo, z));
        Vector3f previous = relative(camera, Mth.lerp(partialTick, olderX, xo), Mth.lerp(partialTick, olderY, yo), Mth.lerp(partialTick, olderZ, zo));
        Vector3f direction = new Vector3f(current).sub(previous);
        float distance = direction.length();
        if (distance < 0.001F) {
            direction.set(0, 1, 0);
        } else {
            direction.normalize();
        }
        Vector3f center = new Vector3f(current).add(previous).mul(0.5F);
        float halfLength = quadSize * Math.max(1.0F, distance * 40.0F) * GuardConfig.SPARK_LENGTH.get() / 100.0F;
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
        vertexAt(vertices, point.x, point.y, point.z, u, v, light);
    }

    private void vertexAt(VertexConsumer vertices, float x, float y, float z, float u, float v, int light) {
        vertices.addVertex(x, y, z)
            .setColor(rCol, gCol, bCol, alpha).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
    }

    @Override public int getLightColor(float partialTick) {
        if (flash) return 0xF000F0;
        BlockPos pos = BlockPos.containing(x, y, z);
        int glow = (int) (15.0F * (1.0F - (float) age / lifetime));
        return LightTexture.pack(Math.max(level.getBrightness(LightLayer.BLOCK, pos), glow), level.getBrightness(LightLayer.SKY, pos));
    }

    @Override public ParticleRenderType getRenderType() {
        return ParticleRenderType.NO_RENDER;
    }

    @SubscribeEvent
    public static void renderSparks(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || SPARKS.isEmpty()) return;
        ClientLevel currentLevel = Minecraft.getInstance().level;
        SPARKS.removeIf(spark -> !spark.isAlive() || spark.level != currentLevel);
        if (SPARKS.isEmpty()) return;

        // The streak vertices are camera-relative. Rendering them through an
        // entity buffer bypasses Sodium's specialized particle vertex format.
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(SPARK_RENDER_TYPE);
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for (GuardSparkParticle spark : SPARKS) {
            if (spark.stationaryDots) spark.renderDots(vertices, event.getCamera(), partialTick);
            else if (spark.flash) spark.renderFlash(vertices, event.getCamera(), partialTick);
            else spark.renderStreak(vertices, event.getCamera(), partialTick);
        }
        buffers.endBatch(SPARK_RENDER_TYPE);
    }

    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(GuardParticles.FLYING_SPARK.get(), sprites -> new Provider(sprites, false, false));
        event.registerSpriteSet(GuardParticles.SPARK_FLASH.get(), sprites -> new Provider(sprites, true, false));
        event.registerSpriteSet(GuardParticles.STREAK_DOT.get(), sprites -> new Provider(sprites, false, true));
    }

    private record Provider(SpriteSet sprites, boolean flash, boolean stationaryDots) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new GuardSparkParticle(level, x, y, z, xd, yd, zd, sprites, flash, stationaryDots);
        }
    }
}
