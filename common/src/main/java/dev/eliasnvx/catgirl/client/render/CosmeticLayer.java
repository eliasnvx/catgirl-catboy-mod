package dev.eliasnvx.catgirl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.catgirl.client.render.model.CatModels;
import dev.eliasnvx.catgirl.cosmetic.CatSlot;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.registry.CatgirlItems;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.Item;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/** Draws everything a player wears. Added to every player renderer on both loaders. */
public final class CosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    /** A worn item's renderers (slim arms get their own geometry) and where its colors come from. */
    private record Entry(CoatRenderer wide, CoatRenderer slim, PartColors colors) {
    }

    /** Blush is a tint on the cheeks, not paint: the skin shows through. */
    private static final float BLUSH_OPACITY = 0.6F;

    private final Map<Item, Entry> entries = new IdentityHashMap<>();

    public CosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet models) {
        super(parent);
        coat(models, CatgirlItems.CAT_EARS.get(), CatModels.EARS, CatModels.EarsModel::new);
        coat(models, CatgirlItems.CAT_TAIL.get(), CatModels.TAIL, CatModels.TailModel::new);
        hands(models, CatgirlItems.PAW_GLOVES.get(), CatModels.PAW_GLOVES, CatModels.PAW_GLOVES_SLIM);
        hands(models, CatgirlItems.PAW_MITTENS.get(), CatModels.PAW_MITTENS, CatModels.PAW_MITTENS_SLIM);
        coat(models, CatgirlItems.PAW_SLIPPERS.get(), CatModels.PAW_SLIPPERS, CatModels.FurModel::new);
        coat(models, CatgirlItems.LEG_WARMERS.get(), CatModels.LEG_WARMERS, CatModels.FurModel::new);
        CoatRenderer bellCollar = new CoatRenderer(models, CatModels.BELL_COLLAR, CatModels.BellCollarModel::new);
        entries.put(CatgirlItems.BELL_COLLAR.get(), new Entry(bellCollar, bellCollar, PartColors.dyed(CatgirlItems.BELL_COLLAR_COLOR)));
        dyed(models, CatgirlItems.VELVET_CHOKER.get(), CatModels.VELVET_CHOKER, CatgirlItems.VELVET_CHOKER_COLOR);
        dyed(models, CatgirlItems.NECK_RIBBON.get(), CatModels.NECK_RIBBON, CatgirlItems.NECK_RIBBON_COLOR);
        dyed(models, CatgirlItems.WHISKERS.get(), CatModels.WHISKERS, CatgirlItems.WHISKERS_COLOR);
        CoatRenderer blush = new CoatRenderer(models, CatModels.BLUSH, CatModels.FurModel::new, BLUSH_OPACITY);
        entries.put(CatgirlItems.BLUSH.get(), new Entry(blush, blush, PartColors.dyed(CatgirlItems.BLUSH_COLOR)));
        dyed(models, CatgirlItems.CAT_EAR_HEADPHONES.get(), CatModels.CAT_EAR_HEADPHONES, CatgirlItems.CAT_EAR_HEADPHONES_COLOR);
        dyed(models, CatgirlItems.EAR_BOWS.get(), CatModels.EAR_BOWS, CatgirlItems.EAR_BOWS_COLOR);
        dyed(models, CatgirlItems.BELL_HEADBAND.get(), CatModels.BELL_HEADBAND, CatgirlItems.BELL_HEADBAND_COLOR);
    }

    private void coat(EntityModelSet models, Item item, ModelLayerLocation layer,
                      BiFunction<ModelPart, Coat.Part, CatModels.FurModel> factory) {
        CoatRenderer renderer = new CoatRenderer(models, layer, factory);
        entries.put(item, new Entry(renderer, renderer, PartColors.COAT));
    }

    private void hands(EntityModelSet models, Item item, ModelLayerLocation wide, ModelLayerLocation slim) {
        entries.put(item, new Entry(new CoatRenderer(models, wide, CatModels.FurModel::new),
                new CoatRenderer(models, slim, CatModels.FurModel::new), PartColors.COAT));
    }

    private void dyed(EntityModelSet models, Item item, ModelLayerLocation layer, int defaultDye) {
        CoatRenderer renderer = new CoatRenderer(models, layer, CatModels.FurModel::new);
        entries.put(item, new Entry(renderer, renderer, PartColors.dyed(defaultDye)));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        CosmeticRenderData data = ((CosmeticRenderStateAccess) state).catgirl$getCosmetics();
        if (data == null || state.isInvisible) {
            return;
        }
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
        boolean slim = state.skin.model() == PlayerModelType.SLIM;
        // a visible helmet covers the head slot (headphones, bows, headband); ears poke out on top by themselves
        boolean helmet = CatModels.EarsModel.wearsHelmet(state);
        List<CosmeticRenderData.Worn> worn = data.worn();
        for (int i = 0; i < worn.size(); i++) {
            CosmeticRenderData.Worn item = worn.get(i);
            Entry entry = entries.get(item.item());
            if (entry != null && !(helmet && item.slot() == CatSlot.HEAD)) {
                (slim ? entry.slim() : entry.wide()).submit(poseStack, collector, light, overlay, state, entry.colors().of(item));
            }
        }
    }
}
