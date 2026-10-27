package dev.eliasnvx.catgirl.item;

import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;

/** A collar or face piece dyed like leather armor: 1.20.1's dye recipe takes {@link DyeableLeatherItem}s. */
public class DyeableCosmeticItem extends Item implements DyeableLeatherItem {

    public DyeableCosmeticItem(Properties properties) {
        super(properties);
    }
}
