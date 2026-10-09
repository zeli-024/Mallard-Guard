package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardItemRules;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

/** Search and category filters for the shared item assignment grid. */
final class GuardItemSearch {
    static final String[] CATEGORIES={"Blocks","Tools","Consumables","Shields","Other"};
    private GuardItemSearch(){}
    static boolean matches(Item item,String query){
        String q=query.trim().toLowerCase(Locale.ROOT),id=BuiltInRegistries.ITEM.getKey(item).toString();
        if(q.isEmpty())return true;
        if(q.startsWith("@"))return BuiltInRegistries.ITEM.getKey(item).getNamespace().contains(q.substring(1));
        if(q.startsWith("#"))return item.getDefaultInstance().getTags().anyMatch(tag->tag.location().toString().contains(q.substring(1)));
        if(q.contains(":"))return id.equals(q);
        return id.contains(q)||item.getDescription().getString().toLowerCase(Locale.ROOT).contains(q)||item.getDefaultInstance().getTags().anyMatch(tag->tag.location().toString().contains(q));
    }
    static int categories(Item item){
        var stack=item.getDefaultInstance();int mask=0;
        if(item instanceof BlockItem)mask|=1;
        if(GuardItemRules.hasAttackDamage(stack))mask|=2;
        if(GuardItemRules.consumable(stack))mask|=4;
        if(GuardItemRules.shieldLike(stack))mask|=8;
        return mask==0?16:mask;
    }
}
