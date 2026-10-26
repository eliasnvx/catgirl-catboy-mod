package dev.eliasnvx.catgirl.menu;

/** Implemented by the player's {@code InventoryMenu} (mixin): where its cosmetic slots start. */
public interface InventoryMenuCosmetics {

    /** Menu index of the first cosmetic slot (EARS); the others follow in CatSlot order. */
    int catgirl$cosmeticStart();
}
