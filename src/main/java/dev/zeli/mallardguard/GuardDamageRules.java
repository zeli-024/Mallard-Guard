package dev.zeli.mallardguard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.ModList;

/** Server owned source rules. Unlisted sources remain parryable by default. */
public final class GuardDamageRules {
    public static final List<String> BUILTINS = List.of(
        "minecraft:drown", "minecraft:in_fire", "minecraft:on_fire", "minecraft:lava",
        "minecraft:hot_floor", "minecraft:campfire", "minecraft:starve", "minecraft:wither",
        "minecraft:magic", "minecraft:indirect_magic", "minecraft:generic_kill",
        "minecraft:fall", "minecraft:fly_into_wall");
    private static final int MAX_RULE_LENGTH = 131072, MAX_HITS = 10;
    private static final Map<UUID, ArrayDeque<String>> HITS = new java.util.HashMap<>();

    private static String cachedDefaultText;
    private static Map<String, Boolean> cachedDefaultRules = Map.of();
    private static String cachedText;
    private static Map<String, Boolean> cachedRules = Map.of();
    private static String cachedProjectileText;
    private static Map<String, LinkedHashSet<String>> cachedProjectileSources = Map.of();

    public static void clearSession(){HITS.clear();}

    private GuardDamageRules() {}

    public static void forget(ServerPlayer player) {
        HITS.remove(player.getUUID());
    }

    public static boolean builtin(String id) { return BUILTINS.contains(id); }
    public static boolean defaultAllowed(String id) {
        String defaults = GuardConfig.DAMAGE_RULES.getDefault();
        if (!defaults.equals(cachedDefaultText)) { cachedDefaultRules = parseRules(defaults); cachedDefaultText = defaults; }
        return cachedDefaultRules.getOrDefault(id, !builtin(id) || id.equals("minecraft:fall") || id.equals("minecraft:fly_into_wall"));
    }

    public static boolean validId(String id) {
        if (id == null || id.length() > 128) return false;
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key != null && key.toString().equals(id);
    }

    public static LinkedHashMap<String, Boolean> parseRules(String encoded) {
        if (encoded == null || encoded.length() > MAX_RULE_LENGTH) return null;
        LinkedHashMap<String, Boolean> rules = new LinkedHashMap<>();
        if (encoded.isEmpty()) return rules;
        for (String pair : encoded.split(",", -1)) {
            if (pair.length() < 3 || pair.charAt(pair.length() - 2) != '=' ||
                (pair.charAt(pair.length() - 1) != '0' && pair.charAt(pair.length() - 1) != '1')) return null;
            String id = pair.substring(0, pair.length() - 2);
            if (!validId(id) || rules.putIfAbsent(id, pair.endsWith("1")) != null) return null;
        }
        return rules;
    }

    public static boolean validRules(String text) { return parseRules(text) != null; }

    private static LinkedHashMap<String, LinkedHashSet<String>> parseProjectileSources(String encoded) {
        if (encoded == null || encoded.length() > MAX_RULE_LENGTH) return null;
        LinkedHashMap<String, LinkedHashSet<String>> mapping = new LinkedHashMap<>();
        if (encoded.isEmpty()) return mapping;
        int total = 0;
        for (String pair : encoded.split(",", -1)) {
            int separator = pair.indexOf('=');
            if (separator < 1 || !validId(pair.substring(0, separator))
                || !validId(pair.substring(separator + 1)) || ++total > 4096) return null;
            mapping.computeIfAbsent(pair.substring(0, separator), key -> new LinkedHashSet<>())
                .add(pair.substring(separator + 1));
        }
        return mapping;
    }

    public static boolean validProjectileSources(String text) { return parseProjectileSources(text) != null; }

    public static String encodeRules(Map<String, Boolean> rules) {
        List<String> encoded = new ArrayList<>();
        rules.forEach((id, on) -> encoded.add(id + "=" + (on ? "1" : "0")));
        return String.join(",", encoded);
    }

    public static boolean canParry(DamageSource source) {
        return allowed(id(source));
    }

    public static String id(DamageSource source) {
        return source.typeHolder().unwrapKey().map(key -> key.location().toString()).orElse("");
    }

    /** Impact hooks run before the projectile creates a DamageSource. */
    public static boolean canParryProjectile(Projectile projectile) {
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType()).toString();
        Map<String, LinkedHashSet<String>> observed = projectileSources();
        if (observed.containsKey(type)) {
            for (String id : observed.get(type)) if (!allowed(id)) return false;
            return true;
        }
        String id = projectile instanceof ThrownTrident ? "minecraft:trident"
            : projectile instanceof AbstractArrow ? "minecraft:arrow"
            : projectile instanceof ShulkerBullet ? "minecraft:bullet"
            : projectile instanceof Fireball ? "minecraft:fireball" : "";
        return id.isEmpty() || allowed(id);
    }

    private static Map<String, LinkedHashSet<String>> projectileSources() {
        String observed = GuardConfig.OBSERVED_PROJECTILE_SOURCES.get();
        if (!observed.equals(cachedProjectileText)) {
            var updated = parseProjectileSources(observed);
            cachedProjectileSources = updated == null ? Map.of() : updated;
            cachedProjectileText = observed;
        }
        return cachedProjectileSources;
    }

    private static boolean allowed(String id) {
        String text = GuardConfig.DAMAGE_RULES.get();
        if (!text.equals(cachedText)) {
            LinkedHashMap<String, Boolean> updated = parseRules(text);
            cachedRules = updated == null ? Map.of() : updated;
            cachedText = text;
        }
        return cachedRules.getOrDefault(id, defaultAllowed(id));
    }

    public static void record(ServerPlayer player, DamageSource source) {
        String id = id(source);
        if (!validId(id)) return;
        if (source.getDirectEntity() instanceof Projectile projectile) {
            String type = BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType()).toString();
            String encoded = GuardConfig.OBSERVED_PROJECTILE_SOURCES.get();
            var known = projectileSources();
            var knownSources = known.get(type);
            var mapping = knownSources != null && knownSources.contains(id) ? null : parseProjectileSources(encoded);
            if (mapping != null && mapping.values().stream().mapToInt(java.util.Set::size).sum() < 4096
                && mapping.computeIfAbsent(type, key -> new LinkedHashSet<>()).add(id)) {
                StringBuilder updated = new StringBuilder();
                mapping.forEach((entity, sources) -> sources.forEach(damage -> {
                    if (!updated.isEmpty()) updated.append(',');
                    updated.append(entity).append('=').append(damage);
                }));
                if (updated.length() <= MAX_RULE_LENGTH) {
                    GuardConfig.OBSERVED_PROJECTILE_SOURCES.set(updated.toString());
                    GuardConfig.SERVER_SPEC.save();
        GuardConfig.clearConfigBackups();
                }
            }
        }
        ArrayDeque<String> hits = HITS.computeIfAbsent(player.getUUID(), key -> new ArrayDeque<>());
        if (id.equals(hits.peekFirst())) return;
        hits.remove(id);
        hits.addFirst(id);
        while (hits.size() > MAX_HITS) hits.removeLast();
        if (player.hasPermissions(2)) PacketDistributor.sendToPlayer(player, new GuardPackets.DamageHit(id));
    }

    public static GuardPackets.DamageState state(ServerPlayer player) {
        ArrayDeque<String> hits = HITS.get(player.getUUID());
        String rules = GuardConfig.DAMAGE_RULES.get();
        java.util.Set<String> namespaces = new java.util.LinkedHashSet<>();
        List<String> registered = player.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).keySet().stream()
            .map(ResourceLocation::toString).sorted().toList();
        List<String> catalog = new ArrayList<>();
        int catalogLength = 0;
        for (String id : registered) {
            if (catalog.size() == 4096 || catalogLength + id.length() + 1 > 130000) break;
            catalog.add(id);
            catalogLength += id.length() + 1;
        }
        catalog.forEach(id -> namespaces.add(ResourceLocation.parse(id).getNamespace()));
        if (hits != null) hits.forEach(id -> namespaces.add(ResourceLocation.parse(id).getNamespace()));
        LinkedHashMap<String, Boolean> configured = parseRules(rules);
        if (configured != null) configured.keySet().forEach(id -> namespaces.add(ResourceLocation.parse(id).getNamespace()));
        List<String> names = new ArrayList<>();
        int namesLength = 0;
        for (String namespace : namespaces) {
            String displayName = namespace.equals("minecraft") ? "Minecraft" : ModList.get().getModContainerById(namespace)
                .map(mod -> mod.getModInfo().getDisplayName()).orElse(namespace);
            String clean = displayName.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
            String name = namespace + "\t" + clean.substring(0, Math.min(80, clean.length()));
            int nextLength = namesLength + (names.isEmpty() ? 0 : 1) + name.length();
            if (nextLength > 16000) break;
            names.add(name);
            namesLength = nextLength;
        }
        return new GuardPackets.DamageState(player.hasPermissions(2) && hits != null ? String.join(",", hits) : "",
            rules, String.join("\n", names), String.join(",", catalog));
    }
}
