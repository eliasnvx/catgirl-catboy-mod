package dev.eliasnvx.catgirl.client.render;

import dev.eliasnvx.catgirl.cosmetic.Coat;
import net.minecraft.util.FastColor.ARGB32;

/**
 * The colors a worn item's model parts are drawn in, indexed by {@link Coat.Part#ordinal()}. Fur pieces use their
 * coat; collars use the vanilla dye color with fixed metal and dark details. Called on the render thread every frame:
 * implementations return shared arrays and never allocate.
 */
@FunctionalInterface
public interface PartColors {

    PartColors COAT = worn -> worn.coat().colors();

    int[] of(CosmeticRenderData.Worn worn);

    /**
     * Dyeable accessories: MAIN = the dye (or {@code defaultDye}), INNER = a darker shade of it (edges, knots),
     * POINTS = gold (bells, rings), PATCH = dark (bell slit), TUFT = white (highlights).
     */
    static PartColors dyed(int defaultDye) {
        int[] colors = new int[Coat.Part.values().length];
        colors[Coat.Part.POINTS.ordinal()] = Dyed.GOLD;
        colors[Coat.Part.PATCH.ordinal()] = Dyed.DARK;
        colors[Coat.Part.TUFT.ordinal()] = Dyed.WHITE;
        return worn -> {
            int dye = worn.dye() < 0 ? defaultDye : worn.dye();
            colors[Coat.Part.MAIN.ordinal()] = dye;
            colors[Coat.Part.INNER.ordinal()] = Dyed.shade(dye, Dyed.EDGE_SHADE);
            return colors;
        };
    }

    final class Dyed {
        static final int GOLD = 0xF2C94C;
        static final int DARK = 0x2A2328;
        static final int WHITE = 0xFFFFFF;
        static final float EDGE_SHADE = 0.28F;

        private Dyed() {
        }

        /** Mixes a color toward black by {@code amount}. */
        static int shade(int rgb, float amount) {
            float keep = 1.0F - amount;
            return ARGB32.color((int) (ARGB32.red(rgb) * keep), (int) (ARGB32.green(rgb) * keep), (int) (ARGB32.blue(rgb) * keep)) & 0xFFFFFF;
        }
    }
}
