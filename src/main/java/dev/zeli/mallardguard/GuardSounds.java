package dev.zeli.mallardguard;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GuardSounds {
    public static final DeferredRegister<SoundEvent> EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MallardGuard.ID);
    public static final DeferredHolder<SoundEvent, SoundEvent> PARRY = register("parry");
    public static final DeferredHolder<SoundEvent, SoundEvent> PERFECT = register("perfect");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK = register("block");

    private GuardSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, name)));
    }
}
