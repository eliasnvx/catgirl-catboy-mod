package dev.eliasnvx.catgirl.client.render.entity;

import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.entity.neko.Kitten;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.animal.feline.Cat;

/**
 * Kitten: the vanilla cat model and renderer (sitting, lying, collar), with the vanilla coat texture closest to its
 * mod coat (referenced, not shipped). Pink and Neon have no vanilla coat: the white one, tinted.
 */
public final class KittenRenderer extends CatRenderer {

    private static final int NO_TINT = 0xFFFFFF;

    public KittenRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static final class State extends CatRenderState {
        int tint = NO_TINT;
    }

    @Override
    public CatRenderState createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Cat cat, CatRenderState state, float partialTick) {
        super.extractRenderState(cat, state, partialTick);
        if (cat instanceof Kitten kitten && state instanceof State kittenState) {
            Coat coat = kitten.coat();
            state.texture = texture(vanillaCoat(coat), state.isBaby);
            kittenState.tint = coat == Coat.PINK || coat == Coat.NEON ? coat.color(Coat.Part.MAIN) : NO_TINT;
        }
    }

    @Override
    protected int getModelTint(CatRenderState state) {
        return state instanceof State kitten ? ARGB.opaque(kitten.tint) : super.getModelTint(state);
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

    private static Identifier texture(String coat, boolean baby) {
        return Identifier.withDefaultNamespace("textures/entity/cat/cat_" + coat + (baby ? "_baby" : "") + ".png");
    }
}
