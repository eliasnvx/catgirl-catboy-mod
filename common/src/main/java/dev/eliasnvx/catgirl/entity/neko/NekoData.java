package dev.eliasnvx.catgirl.entity.neko;

import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.registry.CatgirlItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Small helpers shared by the cat-person mobs. */
public final class NekoData {

    private NekoData() {
    }

    /** Reads a saved coat name; unknown or missing names give the default coat. */
    public static Coat coat(String name) {
        for (Coat coat : Coat.values()) {
            if (coat.getSerializedName().equals(name)) {
                return coat;
            }
        }
        return Coat.DEFAULT;
    }

    /** Hands an item to a player, or drops it at their feet if the inventory is full. */
    public static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.spawnAtLocation(player.level(), stack);
        }
    }

    /**
     * The Fisher Neko's quest reward: any fur piece (ears, tail, paws, leg warmers, slippers) in a rare coat. Siamese
     * and Calico can't be crafted; the repeatable quest is how a whole rare outfit comes together.
     */
    public static ItemStack rarePiece(Entity source) {
        List<RegistrySupplier<Item>> pieces = CatgirlItems.coated();
        Item piece = pieces.get(source.getRandom().nextInt(pieces.size())).get();
        return CatgirlItems.withCoat(piece, source.getRandom().nextBoolean() ? Coat.SIAMESE : Coat.CALICO);
    }
}
