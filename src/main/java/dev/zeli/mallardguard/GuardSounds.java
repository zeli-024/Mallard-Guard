package dev.zeli.mallardguard;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.List;

public final class GuardSounds {
    public static final DeferredRegister<SoundEvent> EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MallardGuard.ID);
    // Separate events let the server choose a variant without repeating the last one.
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PARRY = List.of(
        register("parry_1"), register("parry_2"), register("parry_3"),
        register("parry_4"), register("parry_5"), register("parry_6"));
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PERFECT = PARRY;
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BLOCK = List.of(
        register("block_1"), register("block_2"), register("block_3"), register("block_4"), register("block_5"));

    private GuardSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, name)));
    }
}
