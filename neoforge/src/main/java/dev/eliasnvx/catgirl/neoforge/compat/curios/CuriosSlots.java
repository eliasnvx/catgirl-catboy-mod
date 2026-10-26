package dev.eliasnvx.catgirl.neoforge.compat.curios;

import dev.eliasnvx.catgirl.compat.ExternalSlots;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.function.Consumer;

/**
 * Curios (soft dependency): our cosmetics in Curios slots count as worn and render like ours. Which item fits which
 * Curios slot comes from the data pack: {@code data/curios/tags/item}, our own slots in {@code data/catgirl/curios/slots}
 * (cat ears, tail, legs: Curios has none) and the player's slots in {@code data/catgirl/curios/entities}.
 * Loaded only with Curios. Ported from Femboy Mod.
 */
public final class CuriosSlots {

    private CuriosSlots() {
    }

    public static void register() {
        ExternalSlots.register((player, action) -> CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            for (ICurioStacksHandler handler : inventory.getCurios().values()) {
                visit(handler.getStacks(), action);
                visit(handler.getCosmeticStacks(), action);
            }
        }));
    }

    private static void visit(IDynamicStackHandler stacks, Consumer<ItemStack> action) {
        for (int i = 0; i < stacks.getSlots(); i++) {
            action.accept(stacks.getStackInSlot(i));
        }
    }
}
