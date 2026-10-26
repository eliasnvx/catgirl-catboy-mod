package dev.eliasnvx.catgirl.client;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.world.entity.Entity;

/**
 * Alley Cat: players who just landed on all fours crouch for a moment (the server tells which ones). Read every frame
 * from the render state extraction, so a primitive map, no allocation.
 */
public final class LandingPose {

    /** Entity id -> game time the pose ends. Render thread only. */
    private static final Int2LongMap UNTIL = new Int2LongOpenHashMap();

    private LandingPose() {
    }

    static void start(Entity entity, int ticks) {
        UNTIL.put(entity.getId(), entity.level().getGameTime() + ticks);
    }

    public static boolean active(Entity entity) {
        if (UNTIL.isEmpty()) {
            return false;
        }
        long until = UNTIL.getOrDefault(entity.getId(), Long.MIN_VALUE);
        if (until == Long.MIN_VALUE) {
            return false;
        }
        if (entity.level().getGameTime() >= until) {
            UNTIL.remove(entity.getId());
            return false;
        }
        return true;
    }

    static void clear() {
        UNTIL.clear();
    }
}
