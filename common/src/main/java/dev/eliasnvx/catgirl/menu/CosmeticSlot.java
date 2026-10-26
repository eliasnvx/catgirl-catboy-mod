package dev.eliasnvx.catgirl.menu;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.cosmetic.CatSlot;
import dev.eliasnvx.catgirl.cosmetic.CosmeticsManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class CosmeticSlot extends Slot {

    private static final CatSlot[] SLOTS = CatSlot.values();
    /** Outline icons of empty slots, like vanilla's armor slots. */
    private static final Identifier[] ICONS = new Identifier[SLOTS.length];

    static {
        for (CatSlot slot : SLOTS) {
            ICONS[slot.ordinal()] = CatgirlMod.id("container/slot/" + slot.getSerializedName());
        }
    }

    public CosmeticSlot(CosmeticsContainer container, int index, int x, int y) {
        super(container, index, x, y);
    }

    public CatSlot catSlot() {
        return CosmeticsContainer.slot(getContainerSlot());
    }

    /**
     * The cosmetic slot behind an empty-slot icon. The creative inventory wraps slots and only passes the icon
     * through, so this is how its tooltips find out which slot is hovered.
     */
    public static @Nullable CatSlot slotOfIcon(@Nullable Identifier icon) {
        for (int i = 0; i < ICONS.length; i++) {
            if (ICONS[i].equals(icon)) {
                return SLOTS[i];
            }
        }
        return null;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return CosmeticsManager.canEquip(catSlot(), stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public Identifier getNoItemIcon() {
        return ICONS[getContainerSlot()];
    }
}
