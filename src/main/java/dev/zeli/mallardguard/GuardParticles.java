package dev.zeli.mallardguard;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GuardParticles {
    public static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, MallardGuard.ID);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLYING_SPARK = TYPES.register("flying_spark", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STREAK_DOT = TYPES.register("streak_dot", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPARK_FLASH = TYPES.register("spark_flash", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PARRY_FLASH = TYPES.register("parry_flash", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PERFECT_FLASH = TYPES.register("perfect_flash", () -> new SimpleParticleType(false));

    private GuardParticles() {}
}
