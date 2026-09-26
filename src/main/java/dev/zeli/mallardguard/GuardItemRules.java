package dev.zeli.mallardguard;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class GuardItemRules {
    private GuardItemRules() {}

    public static boolean matches(ItemStack stack, String rules) {
        if (rules.isBlank()) return false;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (String entry : rules.split(",")) {
            String value = entry.trim();
            boolean tag = value.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? value.substring(1) : value);
            if (id != null && (tag ? stack.is(TagKey.create(Registries.ITEM, id)) : id.equals(itemId))) return true;
        }
        return false;
    }

    public static boolean valid(String rules) {
        if (rules == null || rules.length() > 1024) return false;
        if (rules.isBlank()) return true;
        for (String entry : rules.split(",", -1)) {
            String value = entry.trim();
            if (value.isEmpty() || ResourceLocation.tryParse(value.startsWith("#") ? value.substring(1) : value) == null) return false;
        }
        return true;
    }
}
