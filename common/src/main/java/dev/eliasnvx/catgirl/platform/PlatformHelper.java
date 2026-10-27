package dev.eliasnvx.catgirl.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.eliasnvx.catgirl.cosmetic.CosmeticInventory;
import net.minecraft.world.entity.player.Player;

/** Loader-specific services. Implementations: {@code platform.fabric/forge.PlatformHelperImpl}. */
public final class PlatformHelper {

    private PlatformHelper() {
    }

    /** Reads the player's cosmetics attachment (persistent, copied on death). Never null. */
    @ExpectPlatform
    public static CosmeticInventory getCosmetics(Player player) {
        throw new AssertionError();
    }

    /** Replaces the player's cosmetics attachment. Does not sync; callers handle networking. */
    @ExpectPlatform
    public static void setCosmetics(Player player, CosmeticInventory cosmetics) {
        throw new AssertionError();
    }
}
