package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardConfig;
import dev.zeli.mallardguard.GuardPackets;
import dev.zeli.mallardguard.GuardParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** World particle implementation; timing and network feedback remain outside this class. */
public final class GuardWorldParticles {
    private GuardWorldParticles() {}
    public static void clear() { GuardSparkParticle.clear(); }
    private static void tintedParticle(Minecraft mc, net.minecraft.core.particles.SimpleParticleType type, GuardPackets.Sparks data,
        double x, double y, double z, double dx, double dy, double dz) {
        var particle = mc.particleEngine.createParticle(type, x, y, z, dx, dy, dz);
        if (particle != null && data.tint() >= 0) {
            if (particle instanceof GuardSparkParticle spark) spark.tint(data.tint());
            else particle.setColor((data.tint() >> 16 & 255) / 255.0F, (data.tint() >> 8 & 255) / 255.0F, (data.tint() & 255) / 255.0F);
        }
    }
    public static void spawn(GuardPackets.Sparks data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || data.count() < 0 || data.count() > 32) return;
        // Local effect toggles do not affect what other players see.
        if (GuardConfig.PARTICLE_FLASHES_ENABLED.get()) {
            tintedParticle(mc, data.perfect() ? GuardParticles.PERFECT_FLASH.get() : GuardParticles.PARRY_FLASH.get(), data, data.x(), data.y(), data.z(), 0, 0, 0);
        }
        if (data.count() == 0) return;
        if (GuardConfig.STREAKS_ENABLED.get() && GuardConfig.STREAK_LENGTH.get() > 0
            && GuardConfig.STREAK_DOT_SIZE.get() > 0 && GuardConfig.STREAK_STRETCH_SPEED.get() > 0) {
            int count = (int) Math.round((data.perfect() ? 12 : 7)
                * (data.perfect() ? GuardConfig.STREAK_AMOUNT.get() : GuardConfig.REGULAR_STREAK_AMOUNT.get()) * 0.75D / 100.0D);
            double angle = mc.level.random.nextDouble() * Math.PI * 2.0D;
            for (int i = 0; i < count; i++) {
                // Dotted lines keep the spherical pattern even for shield impacts.
                Vec3 direction = burstDirection(i, count, angle, mc.level.random, data, false);
                double lineMarker = data.perfect() ? 1.0D : 2.0D;
                tintedParticle(mc, GuardParticles.STREAK_DOT.get(), data, data.x(), data.y(), data.z(),
                    direction.x * lineMarker, direction.y * lineMarker, direction.z * lineMarker);
            }
        }
        if (!GuardConfig.SPARKS_ENABLED.get() || GuardConfig.SPARK_LIFETIME.get() == 0) return;
        if (data.perfect()) {
            // Particle Interactions' separate star sprites accompany only perfect parries.
            double starOffset = mc.level.random.nextDouble() * Math.PI * 2.0D;
            int stars = GuardConfig.STAR_COUNT.get();
            for (int i = 0; i < stars; i++) {
                Vec3 direction = burstDirection(i, stars, starOffset, mc.level.random, data, true);
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
        int amount = (int) Math.round(data.count() * (data.perfect() ? GuardConfig.PERFECT_SPARK_COUNT.get() : GuardConfig.REGULAR_SPARK_COUNT.get()) * 0.7D / 100.0D);
        for (int i = 0; i < amount; i++) {
            Vec3 direction = burstDirection(i, amount, angleOffset, mc.level.random, data, true);
            // One outward spray per hit.
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

    private static Vec3 burstDirection(int index, int count, double offset, RandomSource random, GuardPackets.Sparks hit, boolean sparkPattern) {
        if (sparkPattern && !hit.fall() && hit.shield() && GuardClient.currentShieldSettings() != null && GuardClient.currentShieldSettings().coneSparks()) {
            // Shield impacts fan toward the incoming attacker across a 90-degree horizontal arc.
            double facing = Math.atan2(hit.directionZ(), hit.directionX());
            double angle = facing - Math.PI / 4.0D + Math.PI / 2.0D * (index + 0.5D) / Math.max(1, count);
            double vertical = 0.08D + random.nextDouble() * 0.45D;
            double horizontal = Math.sqrt(1.0D - vertical * vertical);
            return new Vec3(Math.cos(angle) * horizontal, vertical, Math.sin(angle) * horizontal);
        }
        if (sparkPattern && (hit.fall() || !hit.shield() && GuardConfig.SPARK_RING.get())) {
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
        double vertical = Math.clamp(0.82D - 1.14D * (index + 0.5D) / Math.max(1, count)
            + (random.nextDouble() - 0.5D) * 0.08D, -0.32D, 0.82D);
        double angle = offset + index * (Math.PI * (3.0D - Math.sqrt(5.0D)))
            + (random.nextDouble() - 0.5D) * 0.3D;
        double horizontal = Math.sqrt(1.0D - vertical * vertical);
        return new Vec3(Math.cos(angle) * horizontal, vertical, Math.sin(angle) * horizontal);
    }

}
