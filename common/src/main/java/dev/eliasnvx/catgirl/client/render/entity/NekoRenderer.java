package dev.eliasnvx.catgirl.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.client.render.CoatRenderer;
import dev.eliasnvx.catgirl.client.render.model.CatModels;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.entity.neko.CoatedNeko;
import dev.eliasnvx.catgirl.entity.neko.FisherNeko;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * The cat-person rig: the player model with a skin per mob kind and variant
 * ({@code textures/entity/neko/<kind>_<n>.png}), plus the mod's own cat ears and tail in the mob's coat. Ghosts (the
 * Cat Spirit) are drawn see-through and pale.
 */
public class NekoRenderer<T extends Mob & CoatedNeko> extends HumanoidMobRenderer<T, NekoRenderState, HumanoidModel<NekoRenderState>> {

    private static final float SHADOW = 0.5F;
    private static final Vec3 SITTING_OFFSET = new Vec3(0.0, -0.55, 0.0);
    private static final int GHOST_ALPHA = 0x99;
    private static final int GHOST_TINT = 0xDDF0FF;
    private static final float GOLDEN_ANGLE = 2.39996F;
    private static final Coat.Part[] PARTS = Coat.Part.values();

    private final Identifier[] skins = new Identifier[CoatedNeko.SKINS];
    private final boolean ghost;

    public NekoRenderer(EntityRendererProvider.Context context, String kind, boolean ghost) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), SHADOW);
        this.ghost = ghost;
        for (int i = 0; i < skins.length; i++) {
            skins[i] = CatgirlMod.id("textures/entity/neko/" + kind + "_" + i + ".png");
        }
        addLayer(new FurLayer(context, CatModels.EARS));
        addLayer(new FurLayer(context, CatModels.TAIL));
    }

    @Override
    public NekoRenderState createRenderState() {
        return new NekoRenderState();
    }

    @Override
    public void extractRenderState(T entity, NekoRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.coat = entity.coat();
        state.skin = entity.skin();
        state.sitting = entity instanceof FisherNeko fisher && fisher.isFishing();
        state.isPassenger = state.isPassenger || state.sitting; // the riding pose is the sitting pose
        state.phase = (entity.getId() * GOLDEN_ANGLE) % Mth.TWO_PI;
    }

    @Override
    public Identifier getTextureLocation(NekoRenderState state) {
        return skins[state.skin];
    }

    @Override
    public Vec3 getRenderOffset(NekoRenderState state) {
        return state.sitting ? SITTING_OFFSET : super.getRenderOffset(state);
    }

    @Override
    protected RenderType getRenderType(NekoRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
        return ghost ? RenderTypes.entityTranslucent(getTextureLocation(state)) : super.getRenderType(state, bodyVisible, translucent, glowing);
    }

    @Override
    protected int getModelTint(NekoRenderState state) {
        return ghost ? ARGB.color(GHOST_ALPHA, GHOST_TINT) : super.getModelTint(state);
    }

    /** Ears or tail: one model per coat part, posed like the mob. */
    private final class FurLayer extends RenderLayer<NekoRenderState, HumanoidModel<NekoRenderState>> {
        private final CatModels.NekoFurModel[] models = new CatModels.NekoFurModel[PARTS.length];
        private final boolean[] present = new boolean[PARTS.length];
        private final RenderType ghostType = RenderTypes.entityTranslucent(CoatRenderer.FUR);

        FurLayer(EntityRendererProvider.Context context, ModelLayerLocation layer) {
            super(NekoRenderer.this);
            for (Coat.Part part : PARTS) {
                var root = context.bakeLayer(layer);
                models[part.ordinal()] = new CatModels.NekoFurModel(root, part);
                present[part.ordinal()] = CatModels.hasPart(root, part);
            }
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, NekoRenderState state, float yRot, float xRot) {
            if (state.isInvisible) {
                return;
            }
            int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
            int[] colors = state.coat.colors();
            for (int i = 0; i < PARTS.length; i++) {
                if (present[i]) {
                    int color = ghost ? ARGB.color(GHOST_ALPHA, ARGB.scaleRGB(colors[i], GHOST_TINT)) : ARGB.opaque(colors[i]);
                    collector.submitModel(models[i], state, pose, ghost ? ghostType : CoatRenderer.TYPE, light, overlay, color, null,
                            state.outlineColor);
                }
            }
        }
    }
}
