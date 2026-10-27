package dev.eliasnvx.catgirl.client.render.entity;

import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.entity.neko.Kitten;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import dev.eliasnvx.catgirl.client.render.ARGB32;
import net.minecraft.world.entity.animal.Cat;

/**
 * Kitten: the vanilla cat model and renderer (sitting, lying, collar), with the vanilla coat texture closest to its
 * mod coat (referenced, not shipped). Pink and Neon have no vanilla coat: the white one, tinted.
 */
public final class KittenRenderer extends CatRenderer implements ModelTint {

    private static final int NO_TINT = 0xFFFFFF;

    /** The kitten being drawn: the tint is asked for in the middle of drawing it. Render thread only. */
    private Coat drawing = Coat.DEFAULT;

    public KittenRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Cat cat) {
        if (cat instanceof Kitten kitten) {
            drawing = kitten.coat();
            return texture(vanillaCoat(drawing));
        }
        drawing = Coat.DEFAULT;
        return super.getTextureLocation(cat);
    }

    @Override
    public int catgirl$tint(int color) {
        int tint = drawing == Coat.PINK || drawing == Coat.NEON ? drawing.color(Coat.Part.MAIN) : NO_TINT;
        return ARGB32.multiply(color, ARGB32.opaque(tint));
    }

    private static String vanillaCoat(Coat coat) {
        return switch (coat) {
            case BLACK -> "all_black";
            case GINGER -> "red";
            case GRAY -> "british_shorthair";
            case SIAMESE -> "siamese";
            case CALICO -> "calico";
            case WHITE, PINK, NEON -> "white";
        };
    }

    private static ResourceLocation texture(String coat) {
        return new ResourceLocation("textures/entity/cat/" + coat + ".png");
    }
}
