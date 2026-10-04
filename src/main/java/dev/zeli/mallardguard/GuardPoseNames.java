package dev.zeli.mallardguard;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Names for custom presets that are saved without a user-supplied name. */
public final class GuardPoseNames {
    private GuardPoseNames() {}
    private static final String[] START = {"Waddle", "Quack", "Super", "Turbo", "Mega", "Disco", "Captain", "Sneaky", "Royal", "Spicy", "Cosmic", "Fluffy"};
    private static final String[] END = {"Me-Nuts", "a-Whack", "Featherweight", "Wing-Ding", "Beak-Breaker", "Duck-Fu", "Pond-Goblin", "Bread-Bandit", "Eggsplosion", "Flap-Attack", "Quackpot", "Splash-Dash"};
    public static String unused(List<GuardPoseLibrary.Pose> poses) {
        Set<String> used = new HashSet<>();
        for (var pose : poses) if (pose.name != null) used.add(pose.name.strip().toLowerCase(Locale.ROOT));
        List<String> available = new ArrayList<>(START.length * END.length);
        for (String start : START) for (String end : END) {
            String name = start + "-" + end;
            if (!used.contains(name.toLowerCase(Locale.ROOT))) available.add(name);
        }
        if (!available.isEmpty()) return available.get(ThreadLocalRandom.current().nextInt(available.size()));
        // Keep names unique even if imported custom names exhaust the pool.
        for (int suffix = 2; ; suffix++) {
            String name = "Quack-a-Whack-" + suffix;
            if (!used.contains(name.toLowerCase(Locale.ROOT))) return name;
        }
    }
}
