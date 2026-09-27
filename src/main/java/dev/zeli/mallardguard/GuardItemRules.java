package dev.zeli.mallardguard;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.entity.player.Player;

public final class GuardItemRules {
    private GuardItemRules() {}

    public static boolean shieldLike(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(Items.SHIELD) || stack.getUseAnimation() == UseAnim.BLOCK
            || stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/shields")))
            || stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "shields")))
            || matches(stack, GuardConfig.SHIELD_ITEMS.get()));
    }

    public static boolean consumableInEitherHand(Player player) {
        return consumable(player.getMainHandItem()) || consumable(player.getOffhandItem());
    }

    public static boolean consumable(ItemStack stack) {
        UseAnim animation = stack.getUseAnimation();
        return !stack.isEmpty() && (animation == UseAnim.EAT || animation == UseAnim.DRINK
            || stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION));
    }

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
