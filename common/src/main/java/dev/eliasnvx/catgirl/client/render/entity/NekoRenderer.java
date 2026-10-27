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
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The cat-person rig: the player model with a skin per mob kind and variant
 * ({@code textures/entity/neko/<kind>_<n>.png}), plus the mod's own cat ears and tail in the mob's coat. Ghosts (the
 * Cat Spirit) are drawn see-through and pale. A fishing Fisher Neko sits (LivingEntityRendererMixin gives it the
 * riding pose).
 */
public class NekoRenderer<T extends Mob & CoatedNeko> extends HumanoidMobRenderer<T, HumanoidModel<T>> implements ModelTint {

    private static final float SHADOW = 0.5F;
    private static final Vec3 SITTING_OFFSET = new Vec3(0.0, -0.55, 0.0);
    private static final int GHOST_ALPHA = 0x99;
    private static final int GHOST_TINT = 0xDDF0FF;
    private static final float GOLDEN_ANGLE = 2.39996F;
    private static final Coat.Part[] PARTS = Coat.Part.values();

    private final ResourceLocation[] skins = new ResourceLocation[CoatedNeko.SKINS];
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
    public ResourceLocation getTextureLocation(T entity) {
        return skins[entity.skin()];
    }

    @Override
    public Vec3 getRenderOffset(T entity, float partialTick) {
        return entity instanceof FisherNeko fisher && fisher.isFishing() ? SITTING_OFFSET : super.getRenderOffset(entity, partialTick);
    }

    @Override
    protected @Nullable RenderType getRenderType(T entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        return ghost ? RenderType.entityTranslucent(getTextureLocation(entity))
                : super.getRenderType(entity, bodyVisible, translucent, glowing);
    }

    @Override
    public int catgirl$tint(int color) {
        return ghost ? ARGB32.color(GHOST_ALPHA, GHOST_TINT) : color;
    }

    /** Ears or tail: one model per coat part, posed like the mob. */
    private final class FurLayer extends RenderLayer<T, HumanoidModel<T>> {
        private final CatModels.NekoFurModel[] models = new CatModels.NekoFurModel[PARTS.length];
        private final boolean[] present = new boolean[PARTS.length];
        private final RenderType ghostType = RenderType.entityTranslucent(CoatRenderer.FUR);
        private final NekoRenderState state = new NekoRenderState();

        FurLayer(EntityRendererProvider.Context context, ModelLayerLocation layer) {
            super(NekoRenderer.this);
            for (Coat.Part part : PARTS) {
                ModelPart root = context.bakeLayer(layer);
                models[part.ordinal()] = new CatModels.NekoFurModel(root, part);
                present[part.ordinal()] = CatModels.hasPart(root, part);
            }
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entity.isInvisible()) {
                return;
            }
            state.coat = entity.coat();
            state.phase = (entity.getId() * GOLDEN_ANGLE) % Mth.TWO_PI;
            state.ageInTicks = ageInTicks;
            state.hasRedOverlay = entity.hurtTime > 0 || entity.deathTime > 0;
            int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
            int[] colors = state.coat.colors();
            for (int i = 0; i < PARTS.length; i++) {
                if (present[i]) {
                    CatModels.NekoFurModel model = models[i];
                    ((HumanoidModel) getParentModel()).copyPropertiesTo(model);
                    model.setupAnim(state);
                    int color = ghost ? ARGB32.color(GHOST_ALPHA, ARGB32.multiply(colors[i], GHOST_TINT)) : ARGB32.opaque(colors[i]);
                    model.renderToBuffer(pose, buffers.getBuffer(ghost ? ghostType : CoatRenderer.TYPE), light, overlay, color);
                }
            }
        }
    }
}
