package dev.eliasnvx.catgirl.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Vanilla-looking panels and slots drawn with fills, so layouts live in code, not in a texture. */
public final class CatGui {

    public static final int SLOT = 18;
    public static final int TEXT = 0xFF404040;
    private static final int OUTLINE = 0xFF000000;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_FILL = 0xFF8B8B8B;

    private CatGui() {
    }

    /** A window like vanilla containers: black rounded outline, white/grey bevel, light grey body. */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + 1, OUTLINE);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, OUTLINE);
        g.fill(x, y + 1, x + 1, y + h - 1, OUTLINE);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        g.fill(x + 1, y + 1, x + w - 3, y + 3, LIGHT);
        g.fill(x + 1, y + 1, x + 3, y + h - 3, LIGHT);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, SHADOW);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, SHADOW);
    }

    /** An 18x18 sunken slot frame; the item goes at (x + 1, y + 1). */
    public static void slotFrame(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + SLOT - 1, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + SLOT - 1, SLOT_DARK);
        g.fill(x + 1, y + SLOT - 1, x + SLOT, y + SLOT, LIGHT);
        g.fill(x + SLOT - 1, y + 1, x + SLOT, y + SLOT, LIGHT);
        g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, SLOT_FILL);
    }
}
