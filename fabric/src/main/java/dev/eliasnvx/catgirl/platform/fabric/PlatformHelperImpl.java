package dev.eliasnvx.catgirl.platform.fabric;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.cosmetic.CosmeticInventory;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;

public final class PlatformHelperImpl {

    private static final AttachmentType<CosmeticInventory> COSMETICS = AttachmentRegistry.create(
            CatgirlMod.id("cosmetics"),
            builder -> builder
                    .initializer(() -> CosmeticInventory.EMPTY)
                    .persistent(CosmeticInventory.CODEC)
                    .copyOnDeath());

    private PlatformHelperImpl() {
    }

    /** Class-loads the attachment types; must run before {@link CatgirlMod#init()}. */
    public static void init() {
        java.util.Objects.requireNonNull(COSMETICS);
    }

    public static CosmeticInventory getCosmetics(Player player) {
        return player.getAttachedOrElse(COSMETICS, CosmeticInventory.EMPTY);
    }

    public static void setCosmetics(Player player, CosmeticInventory cosmetics) {
        player.setAttached(COSMETICS, cosmetics);
    }
}
