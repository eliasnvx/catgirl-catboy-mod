package dev.eliasnvx.catgirl.forge.dev;

import dev.eliasnvx.catgirl.cosmetic.CatSlot;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.cosmetic.CosmeticsManager;
import dev.eliasnvx.catgirl.registry.CatgirlItems;
import net.minecraft.server.level.ServerPlayer;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * DevCheck's Curios part (only touched when Curios is loaded): the tail and the leg warmers move from our slots into
 * Curios' "tail" and "legs" slots, so the sets and the render have to find them there.
 */
final class DevCheckCurios {

    private DevCheckCurios() {
    }

    static boolean wear(ServerPlayer player) {
        CosmeticsManager.set(player, CatSlot.TAIL, net.minecraft.world.item.ItemStack.EMPTY);
        CosmeticsManager.set(player, CatSlot.LEGS, net.minecraft.world.item.ItemStack.EMPTY);
        return CuriosApi.getCuriosInventory(player).map(inventory -> {
            inventory.setEquippedCurio("tail", 0, CatgirlItems.withCoat(CatgirlItems.CAT_TAIL.get(), Coat.CALICO));
            inventory.setEquippedCurio("legs", 0, CatgirlItems.withCoat(CatgirlItems.LEG_WARMERS.get(), Coat.CALICO));
            return true;
        }).orElse(false);
    }
}
