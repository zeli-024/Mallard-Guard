package dev.zeli.mallardguard;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GuardParticles {
    public static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, MallardGuard.ID);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLYING_SPARK = TYPES.register("flying_spark", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPARK_DEBRIS = TYPES.register("dot_streak", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPARK_TRACER = TYPES.register("shooting_spark", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CENTER_IMPACT = TYPES.register("center_impact", () -> new SimpleParticleType(false));

    private GuardParticles() {}
}
