package dev.eliasnvx.catgirl.registry;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.cosmetic.Cosmetic;
import dev.eliasnvx.catgirl.item.ItemData;

/**
 * Item data of the mod. Minecraft 1.20.1 has no data components, so each value lives in the stack's NBT under
 * {@code catgirl:<name>} (the later versions' component ids). Read with {@code X.get(stack)}, write with
 * {@code X.set(stack, value)}.
 */
public final class CatgirlComponents {

    /** Which slot an item is worn in; a per-item default (set when the item is created), so no NBT on normal stacks. */
    public static final ItemData<Cosmetic> COSMETIC = new ItemData<>(CatgirlMod.MOD_ID + ":cosmetic", Cosmetic.CODEC);
    public static final ItemData<Coat> COAT = new ItemData<>(CatgirlMod.MOD_ID + ":coat", Coat.CODEC);

    private CatgirlComponents() {
    }
}
