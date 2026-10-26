package dev.eliasnvx.catgirl.menu;

import dev.eliasnvx.catgirl.cosmetic.CatSlot;
import dev.eliasnvx.catgirl.cosmetic.CosmeticsManager;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Container view over a player's cosmetics attachment, one index per {@link CatSlot}. Holds no items itself,
 * so there is no second copy that could drift from the stored state (dupe safety).
 */
public final class CosmeticsContainer implements Container {

    private static final CatSlot[] SLOTS = CatSlot.values();

    private final Player player;

    public CosmeticsContainer(Player player) {
        this.player = player;
    }

    public static CatSlot slot(int index) {
        return SLOTS[index];
    }

    @Override
    public int getContainerSize() {
        return SLOTS.length;
    }

    @Override
    public boolean isEmpty() {
        return CosmeticsManager.get(player).isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return CosmeticsManager.get(player).get(SLOTS[index]);
    }

    @Override
    public ItemStack removeItem(int index, int amount) {
        ItemStack current = getItem(index);
        if (current.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack remaining = current.copy();
        ItemStack taken = remaining.split(amount);
        CosmeticsManager.set(player, SLOTS[index], remaining);
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack current = getItem(index);
        CosmeticsManager.set(player, SLOTS[index], ItemStack.EMPTY);
        return current;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        CosmeticsManager.set(player, SLOTS[index], stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
        // Every mutation goes through CosmeticsManager.set, which stores and syncs.
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player && player.isAlive();
    }

    @Override
    public void clearContent() {
        for (CatSlot slot : SLOTS) {
            CosmeticsManager.set(player, slot, ItemStack.EMPTY);
        }
    }
}
