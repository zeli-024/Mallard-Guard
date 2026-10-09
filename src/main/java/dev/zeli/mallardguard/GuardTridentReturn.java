package dev.zeli.mallardguard;

import net.minecraft.world.entity.Entity;

/** Preserve recovery ownership when a thrown trident changes combat owners. */
public interface GuardTridentReturn {
    void mallardguard$rememberThrower(Entity thrower);
}
