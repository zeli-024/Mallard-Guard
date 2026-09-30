package dev.zeli.mallardguard;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.entity.player.Player;

public final class GuardItemRules {
    private GuardItemRules() {}

    public static boolean shieldLike(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(Items.SHIELD)
            || stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/shields")))
            || stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "shields")))
            || matches(stack, GuardConfig.SHIELD_ITEMS.get())
            || stack.getUseAnimation() == UseAnim.BLOCK && !hasAttackDamage(stack));
    }

    public static boolean hasAttackDamage(ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) return false;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().is(Attributes.ATTACK_DAMAGE) && entry.modifier().amount() > 0
                && (entry.slot().test(EquipmentSlot.MAINHAND) || entry.slot().test(EquipmentSlot.OFFHAND))) return true;
        }
        return false;
    }

    public static boolean simplySwordsWeapon(ItemStack stack) {
        return !stack.isEmpty() && !shieldLike(stack) && hasAttackDamage(stack)
            && BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("simplyswords");
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
