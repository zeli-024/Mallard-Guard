package dev.zeli.mallardguard;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.List;
import java.util.stream.IntStream;

public final class GuardSounds {
    public static final DeferredRegister<SoundEvent> EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MallardGuard.ID);
    // Separate events let the server choose a variant without repeating the last one.
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PARRY = List.of(
        register("parry_1"), register("parry_2"), register("parry_3"),
        register("parry_4"), register("parry_5"), register("parry_6"));
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PERFECT = List.of(
        register("perfect_tool_1"), register("perfect_tool_2"), register("perfect_tool_3"),
        register("perfect_tool_4"), register("perfect_tool_5"), register("perfect_tool_6"));
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PARRY_BOOST = boosted("parry", 6);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PERFECT_BOOST = boosted("perfect_tool", 6);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SHIELD_PERFECT = List.of(
        register("perfect_shield_1"), register("perfect_shield_2"), register("perfect_shield_3"),
        register("perfect_shield_4"), register("perfect_shield_5"));
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SHIELD_PERFECT_BOOST = boosted("perfect_shield", 5);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FALL_PARRY = List.of(register("fall_parry"));
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FALL_PARRY_BOOST = List.of(register("fall_parry_boost"));
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BLOCK = List.of(
        register("block_1"), register("block_2"), register("block_3"), register("block_4"), register("block_5"));
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BLOCK_BOOST = boosted("block", 5);
    public static final DeferredHolder<SoundEvent, SoundEvent> MEME_FLASH = register("meme_flash");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEME_FLASH_BOOST = register("meme_flash_boost");

    private GuardSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MallardGuard.ID, name)));
    }

    private static List<DeferredHolder<SoundEvent, SoundEvent>> boosted(String kind, int count) {
        return IntStream.rangeClosed(1, count).mapToObj(number -> register(kind + "_" + number + "_boost")).toList();
    }
}
