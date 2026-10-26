package dev.eliasnvx.catgirl.menu;

/**
 * The cosmetic slots live in the player's own inventory: a panel attached to the right of the survival inventory
 * and of the creative inventory tab. Two columns of four, head items left, body right (CatSlot order), the worn
 * sets as stars underneath. Positions are relative to the screen's top-left, so the server menu knows them too.
 */
public final class CosmeticsLayout {

    public static final int SLOT = 18;
    public static final int ROWS = 4;
    /** Between the inventory and the panel. */
    public static final int GAP = 2;
    public static final int PADDING = 7;
    /** The row of set stars under the slots. */
    public static final int SETS_ROW = 12;
    public static final int WIDTH = PADDING * 2 + SLOT * 2;
    public static final int HEIGHT = PADDING * 2 + SLOT * ROWS + SETS_ROW;
    /** Vanilla survival inventory width; the creative screen passes its own. */
    public static final int SURVIVAL_WIDTH = 176;

    private CosmeticsLayout() {
    }

    public static int panelX(int imageWidth) {
        return imageWidth + GAP;
    }

    /** Item position of cosmetic slot {@code index} (the slot frame is one pixel up-left). */
    public static int slotX(int imageWidth, int index) {
        return panelX(imageWidth) + PADDING + 1 + index / ROWS * SLOT;
    }

    public static int slotY(int index) {
        return PADDING + 1 + index % ROWS * SLOT;
    }

    public static int setsY() {
        return PADDING + SLOT * ROWS + 3;
    }
}
