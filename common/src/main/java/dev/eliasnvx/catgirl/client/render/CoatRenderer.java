package dev.eliasnvx.catgirl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.client.render.model.CatModels;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiFunction;

/** Draws one worn piece: one model instance per {@link Coat.Part}, each drawn with that part's color. */
public final class CoatRenderer {

    public static final ResourceLocation FUR = CatgirlMod.id("textures/entity/cosmetic/fur.png");
    public static final RenderType TYPE = RenderType.entityCutout(FUR);
    private static final Coat.Part[] PARTS = Coat.Part.values();

    private static final RenderType TRANSLUCENT_TYPE = RenderType.entityTranslucent(FUR);
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

    /**
     * @param parent the wearer's posed model: every part model copies its pose
     * @param colors 24-bit colors indexed by {@link Coat.Part#ordinal()}
     */
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, PlayerPose state,
                       PlayerModel<AbstractClientPlayer> parent, int[] colors) {
        for (int i = 0; i < PARTS.length; i++) {
            if (present[i]) { // skip parts the model has no boxes for
                CatModels.FurModel model = models[i];
                parent.copyPropertiesTo(model);
                model.setupAnim(state);
                int color = ARGB32.color(alpha, colors[i]);
                model.renderToBuffer(poseStack, buffers.getBuffer(type), light, overlay, ARGB32.redF(color), ARGB32.greenF(color),
                        ARGB32.blueF(color), ARGB32.alphaF(color));
            }
        }
    }
}
