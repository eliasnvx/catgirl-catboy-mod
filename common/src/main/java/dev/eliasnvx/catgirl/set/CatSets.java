package dev.eliasnvx.catgirl.set;

import dev.eliasnvx.catgirl.cosmetic.CatSlot;
import dev.eliasnvx.catgirl.cosmetic.CosmeticInventory;
import dev.eliasnvx.catgirl.cosmetic.CosmeticsManager;
import dev.eliasnvx.catgirl.registry.CatgirlTags;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Which sets a player wears, on either side (the client knows everyone's cosmetics from the sync). Evaluated again
 * only when the player's cosmetics snapshot changes, so it is cheap enough for per-tick and per-step checks.
 */
public final class CatSets {

    private static final CatSet[] SETS = CatSet.values();

    private record Cached(CosmeticInventory source, int mask) {
    }

    /** The client and the integrated server share this map from different threads. */
    private static final Map<Player, Cached> CACHE = new WeakHashMap<>();

    private CatSets() {
    }

    /** Bit {@code 1 << set.ordinal()} for every worn set. */
    public static synchronized int mask(Player player) {
        CosmeticInventory inventory = CosmeticsManager.worn(player);
        Cached cached = CACHE.get(player);
        if (cached == null || cached.source() != inventory) {
            int mask = 0;
            for (CatSet set : SETS) {
                if (set.isWorn(inventory)) {
                    mask |= 1 << set.ordinal();
                }
            }
            cached = new Cached(inventory, mask);
            CACHE.put(player, cached);
        }
        return cached.mask();
    }

    public static boolean has(Player player, CatSet set) {
        return (mask(player) & (1 << set.ordinal())) != 0;
    }

    /** Full Neko, or paw slippers on their own: both make steps quieter. */
    public static boolean quietSteps(Player player) {
        return has(player, CatSet.FULL_NEKO) || CosmeticsManager.worn(player).get(CatSlot.FEET).is(CatgirlTags.FOOT_PAWS);
    }
}
