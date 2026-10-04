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

    private static final TagKey<Item> COMMON_SHIELDS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/shields"));
    private static final TagKey<Item> FORGE_SHIELDS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "shields"));
    private record Rule(ResourceLocation id, TagKey<Item> tag) {}
    // Bound the cache because item rules may also come from a remote server.
    private static final java.util.Map<String, java.util.List<Rule>> RULES = new java.util.LinkedHashMap<>(64, 0.75F, true);

    private static synchronized java.util.List<Rule> parsedRules(String text) {
        java.util.List<Rule> cached = RULES.get(text);
        if (cached != null) return cached;
        java.util.List<Rule> parsed = new java.util.ArrayList<>();
        for (String entry : text.split(",")) {
            String value = entry.trim();
            boolean tag = value.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? value.substring(1) : value);
            if (id != null) parsed.add(new Rule(id, tag ? TagKey.create(Registries.ITEM, id) : null));
        }
        cached = java.util.List.copyOf(parsed);
        if (RULES.size() >= 64) RULES.remove(RULES.keySet().iterator().next());
        RULES.put(text, cached);
        return cached;
    }

    public static boolean shieldLike(ItemStack stack) {
        return shieldLike(stack,GuardConfig.SHIELD_ITEMS.get(),GuardConfig.SHIELD_BLACKLIST.get());
    }
    public static boolean shieldLike(ItemStack stack,String whitelist,String blacklist) {
        return !stack.isEmpty() && !matches(stack, blacklist) && (stack.is(Items.SHIELD)
            || stack.is(COMMON_SHIELDS)
            || stack.is(FORGE_SHIELDS)
            || matches(stack, whitelist)
            || stack.getUseAnimation() == UseAnim.BLOCK && !hasAttackDamage(stack));
    }

    public static boolean hasAttackDamage(ItemStack stack) {
        return hasAttackDamage(stack, true);
    }

    public static boolean hasAttackDamage(ItemStack stack, boolean acceptOffhandSlot) {
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) return false;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().is(Attributes.ATTACK_DAMAGE.unwrapKey().orElseThrow()) && entry.modifier().amount() > 0
                && (entry.slot().test(EquipmentSlot.MAINHAND) || acceptOffhandSlot && entry.slot().test(EquipmentSlot.OFFHAND))) return true;
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
        for (Rule rule : parsedRules(rules)) {
            // Resolve membership live so datapack tag reloads do not need cache invalidation.
            if (rule.tag() == null ? rule.id().equals(itemId) : stack.is(rule.tag())) return true;
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
