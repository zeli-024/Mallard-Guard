package dev.zeli.mallardguard.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Immutable navigation catalogue; searching never rebuilds the menu definitions. */
final class GuardConfigSearch {
    private GuardConfigSearch() {}
    record Entry(int section, int page, String category, String name) {}
    private static final List<Entry> ENTRIES = catalogue();
    private static void addSearchEntries(List<Entry> entries, int section, int page, String category, String names) {
        for (String name : names.split("\\|")) entries.add(new Entry(section, page, category, name));
    }

    private static List<Entry> catalogue() {
        List<Entry> all = new ArrayList<>();
        addSearchEntries(all,3,12,"Mob Guard","Mob guarding|Mob guard difficulty|Parry Colors|Mob Tracer Start Color|Mob Tracer Middle Color|Mob Tracer End Color|Spawn Equipment|Mob gear chance|Mob Gear Assignments|Mob Eligibility|Mob Guard Whitelist");
        addSearchEntries(all, 0, 7, "World Particles", "Shield Forward Bias|Flying Sparks|Base Intensity|Perfect Intensity|Damage Intensity|"+String.join("|",new String[]{"Flying Size","Flying Size Variance","Flying Lifetime","Flying Base Color"})
            +"|Spark Debris|Debris Base Intensity|Debris Perfect Intensity|Debris Damage Intensity|"+String.join("|",new String[]{"Debris Size","Debris Lifetime","Debris Base Color"})
            +"|Spark Tracers|Tracer Base Intensity|Tracer Perfect Intensity|Tracer Damage Intensity|"+String.join("|",new String[]{"Tracer Size","Tracer Lifetime","Tracer Tail Length","Tracer Start Color","Tracer Middle Color","Tracer End Color"})
            +"|Center Impact|Impact Size|Impact Spin Speed|Impact Opacity|Impact Duration");
        addSearchEntries(all, 0, 2, "Screen Effects", "Screen Flash|Screen flash|Perfect only flash|Meme flash|Camera Feedback|Camera shake|Regular parry hitlag|Perfect parry hitlag|Impact frame|Perfect only impact|Live preview|Chromatic Aberration|Chromatic intensity|Chromatic duration|Perfect only chromatic|Chromatic affects HUD");
        addSearchEntries(all,0,17,"HUD Icons","Shield Icon|Shield icon|Shield reactions|Parry|Perfect Parry|Block Hit|Guard Break|Recharge");
        addSearchEntries(all,4,6,"Damage Sources","Damage Types|Recent Hits|Runtime Damage Types");
        addSearchEntries(all,4,18,"Diagnostics","Diagnostic Logging|Guard Events|Damage & Projectiles|Animations|Config & Sync|Mob Guard|Particle Collision Profiling");
        addSearchEntries(all, 0, 15, "Animations", "Hand Animation|Third-person guard pose|First-person guard pose");
        addSearchEntries(all, 0, 3, "Audio Sliders", "Master volume|Perfect volume|Parry volume|Block volume");
        addSearchEntries(all, 1, 0, "Parrying", "Timing|Parrying|Healing|Parry Healing|Parry healing amount|Perfect window|Parry window|Recharge|Facing angle|Follow-up parries|Damage Returned|Parry retaliation|Perfect retaliation|Retaliation damage cap|Parry Pushback|Defender pushback|Shield attacker pushback|Weapon attacker pushback|Projectiles|Projectile parry|Explosions|Explosion parry|Explosions: perfect only");
        addSearchEntries(all, 1, 0, "Parrying", "Fall Damage|Perfect fall parry|Fall blast|Fall launch|Fall blast breaks blocks|Look down to parry");
        addSearchEntries(all, 1, 1, "Blocking", "Guard Stance|Blocking|Damage reduction|Weapon blocks before break|Guard break recharge|Guard movement|Projectiles|Block deflect chance|Block projectiles|Explosions|Block explosions");
        addSearchEntries(all, 1, 11, "Shields", "Shield Timing|Shield perfect window|Shield parry window|Shield parry recharge|Shield blocks before break|Shield Perfect Parry|Cone half-angle|Cone reach|Shield retaliation|Stun duration|Cone pushback|Additional Shields and Bosses|Shield Item Eligibility|Item Block Counts|Stunnable bosses");
        addSearchEntries(all, 4, 6, "Damage Sources", "Common Damage Types|Drowning|Standing in fire|Burning|Lava|Hot floor|Campfire|Starvation|Wither|Poison / Instant Damage|Indirect magic|Fall damage|Wall collision|Vanilla /kill|Damage Types");
        addSearchEntries(all, 1, 2, "Multiplayer", "Sparks and flashes|Hit sounds");
        addSearchEntries(all, 1, 4, "Eligibility", "Valid Parry Conditions|Empty Hand|Tool Requirement|Item Eligibility|Respect Item Cooldown|Consumables Take Priority|Priority|Parry Hand Priority|Shield Parry Priority|Force Crouch Off-Hand|Item Durability Wear|Parry durability|Perfect durability|Block durability");
        addSearchEntries(all, 3, 14, "Punchy", "Punchy Compatibility|Release delay|Parry Animation Stance");
        addSearchEntries(all, 2, 8, "Enforce", "Enforce client settings|World Particles|Screen Effects|Shield Icon|Audio Sliders|Hand Animation|Experimental Punchy|Punchy guard poses|Release delay|Parry Animation Stance|Don't Enforce");
        for (Entry entry : List.copyOf(all)) if (entry.section() == 0)
            all.add(new Entry(2,entry.page()==7?8:entry.page()==2?19:entry.page()==17?20:entry.page()==3?21:22,"Enforce "+entry.category(),entry.name()));
        return List.copyOf(all);
    }
    static List<Entry> results(String query) {
        String[] terms = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        return ENTRIES.stream().filter(entry -> {
            String match = (entry.category() + " " + entry.name()).toLowerCase(Locale.ROOT);
            for (String term : terms) if (!match.contains(term)) return false;
            return true;
        }).toList();
    }

}
