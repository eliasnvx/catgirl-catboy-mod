package dev.eliasnvx.catgirl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.catgirl.client.render.model.CatModels;
import dev.eliasnvx.catgirl.cosmetic.CatSlot;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.registry.CatgirlItems;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.Item;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Draws everything a player wears. Added to every player renderer on both loaders. A worn piece uses its Blockbench
 * model when one exists ({@link GeoCosmeticRenderer}), its code model otherwise.
 */
public final class CosmeticLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /** A worn item's renderers (slim arms get their own geometry), where its colors come from and its opacity. */
    private record Entry(CoatRenderer wide, CoatRenderer slim, GeoCosmeticRenderer wideGeo, GeoCosmeticRenderer slimGeo,
                         PartColors colors, int alpha) {
    }

    /** Blush is a tint on the cheeks, not paint: the skin shows through. */
    public static final float BLUSH_OPACITY = 0.6F;

    private static final int OPAQUE = 255;

    private final Map<Item, Entry> entries = new IdentityHashMap<>();
    /** Filled per draw; no render states in 1.20.1. */
    private final PlayerPose state = new PlayerPose();

    public CosmeticLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        coat(models, CatgirlItems.CAT_EARS.get(), CatModels.EARS, CatModels.EarsModel::new);
        coat(models, CatgirlItems.CAT_TAIL.get(), CatModels.TAIL, CatModels.TailModel::new);
        hands(models, CatgirlItems.PAW_GLOVES.get(), CatModels.PAW_GLOVES, CatModels.PAW_GLOVES_SLIM);
        hands(models, CatgirlItems.PAW_MITTENS.get(), CatModels.PAW_MITTENS, CatModels.PAW_MITTENS_SLIM);
        coat(models, CatgirlItems.PAW_SLIPPERS.get(), CatModels.PAW_SLIPPERS, CatModels.FurModel::new);
        coat(models, CatgirlItems.LEG_WARMERS.get(), CatModels.LEG_WARMERS, CatModels.FurModel::new);
        single(models, CatgirlItems.BELL_COLLAR.get(), CatModels.BELL_COLLAR, CatModels.BellCollarModel::new,
                PartColors.dyed(CatgirlItems.BELL_COLLAR_COLOR), 1.0F);
        dyed(models, CatgirlItems.VELVET_CHOKER.get(), CatModels.VELVET_CHOKER, CatgirlItems.VELVET_CHOKER_COLOR);
        dyed(models, CatgirlItems.NECK_RIBBON.get(), CatModels.NECK_RIBBON, CatgirlItems.NECK_RIBBON_COLOR);
        dyed(models, CatgirlItems.WHISKERS.get(), CatModels.WHISKERS, CatgirlItems.WHISKERS_COLOR);
        single(models, CatgirlItems.BLUSH.get(), CatModels.BLUSH, CatModels.FurModel::new,
                PartColors.dyed(CatgirlItems.BLUSH_COLOR), BLUSH_OPACITY);
        dyed(models, CatgirlItems.CAT_EAR_HEADPHONES.get(), CatModels.CAT_EAR_HEADPHONES, CatgirlItems.CAT_EAR_HEADPHONES_COLOR);
        dyed(models, CatgirlItems.EAR_BOWS.get(), CatModels.EAR_BOWS, CatgirlItems.EAR_BOWS_COLOR);
        dyed(models, CatgirlItems.BELL_HEADBAND.get(), CatModels.BELL_HEADBAND, CatgirlItems.BELL_HEADBAND_COLOR);
    }

    private void coat(EntityModelSet models, Item item, ModelLayerLocation layer,
                      BiFunction<ModelPart, Coat.Part, CatModels.FurModel> factory) {
        single(models, item, layer, factory, PartColors.COAT, 1.0F);
    }

    private void hands(EntityModelSet models, Item item, ModelLayerLocation wide, ModelLayerLocation slim) {
        entries.put(item, new Entry(new CoatRenderer(models, wide, CatModels.FurModel::new),
                new CoatRenderer(models, slim, CatModels.FurModel::new),
                geo(models, item, false, wide, CatModels.FurModel::new), geo(models, item, true, slim, CatModels.FurModel::new),
                PartColors.COAT, OPAQUE));
    }

    private void dyed(EntityModelSet models, Item item, ModelLayerLocation layer, int defaultDye) {
        single(models, item, layer, CatModels.FurModel::new, PartColors.dyed(defaultDye), 1.0F);
    }

    /** A piece that looks the same on slim and wide arms. */
    private void single(EntityModelSet models, Item item, ModelLayerLocation layer,
                        BiFunction<ModelPart, Coat.Part, CatModels.FurModel> factory, PartColors colors, float opacity) {
        CoatRenderer renderer = new CoatRenderer(models, layer, factory, opacity);
        GeoCosmeticRenderer geo = geo(models, item, false, layer, factory);
        entries.put(item, new Entry(renderer, renderer, geo, geo, colors, Math.round(opacity * OPAQUE)));
    }

    private static GeoCosmeticRenderer geo(EntityModelSet models, Item item, boolean slim, ModelLayerLocation layer,
                                           BiFunction<ModelPart, Coat.Part, CatModels.FurModel> factory) {
        return new GeoCosmeticRenderer(BuiltInRegistries.ITEM.getKey(item).getPath(), slim, layer,
                factory.apply(models.bakeLayer(layer), Coat.Part.MAIN), models.bakeLayer(layer));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        state.capture(player, limbSwing, limbSwingAmount, partialTick, ageInTicks);
        CosmeticRenderData data = CosmeticRenderData.capture(player, state);
        if (data == null) {
            return;
        }
        state.cosmetics = data;
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        boolean slim = state.slim;
        // a visible helmet covers the head slot (headphones, bows, headband); ears poke out on top by themselves
        boolean helmet = CatModels.EarsModel.wearsHelmet(state);
        // cat-ear headphones bring their own ears: the ears slot would stick out of them
        boolean earsCovered = data.earsCovered() && !helmet;
        List<CosmeticRenderData.Worn> worn = data.worn();
        for (int i = 0; i < worn.size(); i++) {
            CosmeticRenderData.Worn item = worn.get(i);
            Entry entry = entries.get(item.item());
            boolean hidden = helmet && item.slot() == CatSlot.HEAD || earsCovered && item.slot() == CatSlot.EARS;
            if (entry != null && !hidden) {
                int[] colors = entry.colors().of(item);
                GeoCosmeticRenderer geo = slim ? entry.slimGeo() : entry.wideGeo();
                if (!geo.render(poseStack, buffers, light, overlay, state, getParentModel(), colors, entry.alpha())) {
                    (slim ? entry.slim() : entry.wide()).render(poseStack, buffers, light, overlay, state, getParentModel(), colors);
                }
            }
        }
    }
}
