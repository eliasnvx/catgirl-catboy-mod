package dev.eliasnvx.catgirl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.client.render.model.CatModels;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.function.BiFunction;

/** Draws one worn piece: one model instance per {@link Coat.Part}, each submitted with that part's color. */
public final class CoatRenderer {

    public static final Identifier FUR = CatgirlMod.id("textures/entity/cosmetic/fur.png");
    public static final RenderType TYPE = RenderTypes.entityCutout(FUR);
    private static final Coat.Part[] PARTS = Coat.Part.values();

    private static final RenderType TRANSLUCENT_TYPE = RenderTypes.entityTranslucent(FUR);
    private static final int OPAQUE = 255;

    private final CatModels.FurModel[] models = new CatModels.FurModel[PARTS.length];
    private final boolean[] present = new boolean[PARTS.length];
    private final RenderType type;
    private final int alpha;

    public CoatRenderer(EntityModelSet set, ModelLayerLocation layer, BiFunction<ModelPart, Coat.Part, CatModels.FurModel> factory) {
        this(set, layer, factory, 1.0F);
    }

    /** @param opacity below 1 draws see-through (blush) */
    public CoatRenderer(EntityModelSet set, ModelLayerLocation layer, BiFunction<ModelPart, Coat.Part, CatModels.FurModel> factory,
                        float opacity) {
        this.type = opacity < 1.0F ? TRANSLUCENT_TYPE : TYPE;
        this.alpha = Math.round(opacity * OPAQUE);
        for (Coat.Part part : PARTS) {
            ModelPart root = set.bakeLayer(layer);
            models[part.ordinal()] = factory.apply(root, part);
            present[part.ordinal()] = CatModels.hasPart(root, part);
        }
    }

    /** @param colors 24-bit colors indexed by {@link Coat.Part#ordinal()} */
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, AvatarRenderState state, int[] colors) {
        for (int i = 0; i < PARTS.length; i++) {
            if (present[i]) { // skip parts the model has no boxes for
                collector.submitModel(models[i], state, poseStack, type, light, overlay, ARGB.color(alpha, colors[i]), null, state.outlineColor);
            }
        }
    }
}
