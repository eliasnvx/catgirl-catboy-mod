package dev.eliasnvx.catgirl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.catgirl.client.render.model.CatModels;
import dev.eliasnvx.catgirl.client.render.model.FurBuilder;
import dev.eliasnvx.catgirl.cosmetic.CatSlot;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.cosmetic.CosmeticsManager;
import dev.eliasnvx.catgirl.registry.CatgirlComponents;
import dev.eliasnvx.catgirl.registry.CatgirlItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Paw gloves and mittens on the first-person hand (they show in first person too), called from PlayerRendererHandMixin.
 * Each coat part has its own baked copy of the arm showing only that part.
 */
public final class FirstPersonPaws {

    private static final Coat.Part[] PARTS = Coat.Part.values();
    private static final int WIDE = 0;
    private static final int SLIM = 1;

    /** Per item: [wide/slim][part] -> baked layer root showing only that part. */
    private static final Map<Item, ModelPart[][]> BAKED = new IdentityHashMap<>();
    private static EntityModelSet bakedFrom;

    private FirstPersonPaws() {
    }

    public static void render(PlayerModel<AbstractClientPlayer> model, ModelPart arm, PoseStack pose, MultiBufferSource buffers,
                              int light, AbstractClientPlayer player) {
        ItemStack hands = CosmeticsManager.worn(player).get(CatSlot.HANDS);
        if (hands.isEmpty()) {
            return;
        }
        EntityModelSet models = Minecraft.getInstance().getEntityModels();
        if (models != bakedFrom) { // re-bake after resource reloads
            bake(models);
        }
        ModelPart[][] parts = BAKED.get(hands.getItem());
        if (parts == null) {
            return;
        }
        ModelPart[] roots = parts[player.getSkin().model() == PlayerSkin.Model.SLIM ? SLIM : WIDE];
        String side = arm == model.rightArm ? "right_arm" : "left_arm";
        int[] colors = hands.getOrDefault(CatgirlComponents.COAT.get(), Coat.DEFAULT).colors();
        for (int i = 0; i < PARTS.length; i++) {
            if (roots[i] == null) {
                continue;
            }
            ModelPart paw = roots[i].getChild(side);
            paw.loadPose(arm.storePose());
            paw.render(pose, buffers.getBuffer(CoatRenderer.TYPE), light, OverlayTexture.NO_OVERLAY, ARGB32.opaque(colors[i]));
        }
    }

    private static void bake(EntityModelSet models) {
        BAKED.clear();
        BAKED.put(CatgirlItems.PAW_GLOVES.get(), bake(models, CatModels.PAW_GLOVES, CatModels.PAW_GLOVES_SLIM));
        BAKED.put(CatgirlItems.PAW_MITTENS.get(), bake(models, CatModels.PAW_MITTENS, CatModels.PAW_MITTENS_SLIM));
        bakedFrom = models;
    }

    private static ModelPart[][] bake(EntityModelSet models, ModelLayerLocation wide, ModelLayerLocation slim) {
        ModelPart[][] result = new ModelPart[2][PARTS.length];
        ModelLayerLocation[] layers = {wide, slim};
        for (int variant = 0; variant < layers.length; variant++) {
            for (Coat.Part part : PARTS) {
                ModelPart root = models.bakeLayer(layers[variant]);
                if (CatModels.hasPart(root, part)) {
                    FurBuilder.show(root, part);
                    result[variant][part.ordinal()] = root;
                }
            }
        }
        return result;
    }
}
