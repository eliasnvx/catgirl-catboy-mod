package dev.eliasnvx.catgirl.client.render.model;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.client.render.CosmeticRenderData;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import dev.eliasnvx.catgirl.client.render.entity.NekoRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.Equippable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static dev.eliasnvx.catgirl.cosmetic.Coat.Part.INNER;
import static dev.eliasnvx.catgirl.cosmetic.Coat.Part.MAIN;
import static dev.eliasnvx.catgirl.cosmetic.Coat.Part.PATCH;
import static dev.eliasnvx.catgirl.cosmetic.Coat.Part.POINTS;
import static dev.eliasnvx.catgirl.cosmetic.Coat.Part.TUFT;

/**
 * Placeholder ear and tail geometry built in code (pixel masks + coat parts), until the GeckoLib models from
 * Blockbench exist. Each model is a {@link PlayerModel} with a cleared mesh plus parts under head/body, so vanilla
 * posing moves them; procedural animation runs in {@code setupAnim}. Units are model pixels.
 */
public final class CatModels {

    private static final int TEXTURE_SIZE = 64;

    public static final ModelLayerLocation EARS = layer("cat_ears");
    public static final ModelLayerLocation TAIL = layer("cat_tail");
    public static final ModelLayerLocation PAW_GLOVES = layer("paw_gloves");
    public static final ModelLayerLocation PAW_GLOVES_SLIM = layer("paw_gloves_slim");
    public static final ModelLayerLocation PAW_MITTENS = layer("paw_mittens");
    public static final ModelLayerLocation PAW_MITTENS_SLIM = layer("paw_mittens_slim");
    public static final ModelLayerLocation PAW_SLIPPERS = layer("paw_slippers");
    public static final ModelLayerLocation LEG_WARMERS = layer("leg_warmers");
    public static final ModelLayerLocation BELL_COLLAR = layer("bell_collar");
    public static final ModelLayerLocation VELVET_CHOKER = layer("velvet_choker");
    public static final ModelLayerLocation NECK_RIBBON = layer("neck_ribbon");
    public static final ModelLayerLocation WHISKERS = layer("whiskers");
    public static final ModelLayerLocation BLUSH = layer("blush");
    public static final ModelLayerLocation CAT_EAR_HEADPHONES = layer("cat_ear_headphones");
    public static final ModelLayerLocation EAR_BOWS = layer("ear_bows");
    public static final ModelLayerLocation BELL_HEADBAND = layer("bell_headband");

    private CatModels() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(CatgirlMod.id(name), "main");
    }

    /** Every cosmetic layer and its definition; registered with the game, and walked by the Blockbench starter export. */
    public static Map<ModelLayerLocation, Supplier<LayerDefinition>> definitions() {
        Map<ModelLayerLocation, Supplier<LayerDefinition>> layers = new LinkedHashMap<>();
        layers.put(EARS, CatModels::ears);
        layers.put(TAIL, CatModels::tail);
        layers.put(PAW_GLOVES, () -> hands(false, false));
        layers.put(PAW_GLOVES_SLIM, () -> hands(false, true));
        layers.put(PAW_MITTENS, () -> hands(true, false));
        layers.put(PAW_MITTENS_SLIM, () -> hands(true, true));
        layers.put(PAW_SLIPPERS, CatModels::slippers);
        layers.put(LEG_WARMERS, CatModels::legWarmers);
        layers.put(BELL_COLLAR, CatModels::bellCollar);
        layers.put(VELVET_CHOKER, CatModels::velvetChoker);
        layers.put(NECK_RIBBON, CatModels::neckRibbon);
        layers.put(WHISKERS, CatModels::whiskers);
        layers.put(BLUSH, CatModels::blush);
        layers.put(CAT_EAR_HEADPHONES, CatModels::headphones);
        layers.put(EAR_BOWS, CatModels::earBows);
        layers.put(BELL_HEADBAND, CatModels::bellHeadband);
        return layers;
    }

    public static void registerLayers() {
        definitions().forEach(EntityModelLayerRegistry::register);
    }

    /**
     * Puts a freshly baked layer into its motionless pose (the tail's droop and curl live in setupAnim, not in the
     * layer). Geo models are modelled in this pose and get the code model's motion as an offset from it.
     */
    public static void applyRestPose(ModelLayerLocation layer, ModelPart root) {
        if (layer.equals(TAIL)) {
            TailModel.applyRestPose(root);
        }
    }

    /** Whether a baked layer has any boxes of this part (skips submitting empty models). */
    public static boolean hasPart(ModelPart root, Coat.Part part) {
        for (ModelPart child : root.getAllParts()) {
            if (child.hasChild(FurBuilder.holderName(part))) {
                return true;
            }
        }
        return false;
    }

    private static MeshDefinition emptyPlayerMesh() {
        return emptyPlayerMesh(false);
    }

    /** Slim meshes put the arms where slim skins have them (3 px wide arms, half a pixel lower). */
    private static MeshDefinition emptyPlayerMesh(boolean slim) {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, slim);
        mesh.getRoot().clearRecursively();
        return mesh;
    }

    private static PartDefinition pivot(PartDefinition parent, String name, PartPose pose) {
        return parent.addOrReplaceChild(name, CubeListBuilder.create(), pose);
    }

    // ------------------------------------------------------------------ ears

    /**
     * One ear seen from the front, bottom row on the head: a cat triangle, narrow enough that both ears stay inside
     * the head's outline. T points (Siamese tips), M main,
     * I inner (pink), F tufts of light fur growing out of the inner ear, P patch (only on the left ear: one dark ear
     * on a Calico, a darker edge on tabbies).
     */
    private static final String[] EAR = {
            "....TT....",
            "....TT....",
            "...TMMT...",
            "...MIIM...",
            "..MMIIMM..",
            "..PIIIIM..",
            "..PIFFIM..",
            ".PMIFFIMM.",
            ".PIIFFIIM.",
            "PPIFFFFIMM",
            "PMIFFFFIIM",
    };
    /** The lower rows get a second, thicker layer at the back: ears are fleshy at the base and thin at the tip. */
    private static final int EAR_BASE_ROWS = 4;
    private static final float EAR_PIXEL = 0.38F;
    /** Ear centres: the outer base corners end at the head's hat layer (x = 4.25). */
    private static final float EAR_SPACING = 2.3F;
    /** Ear bases sink a little into the hair layer instead of floating above the head. */
    private static final float EAR_Y = -7.8F;
    private static final float EAR_TILT = 0.24F;

    private static LayerDefinition ears() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition head = mesh.getRoot().getChild("head");
        float width = EAR[0].length() * EAR_PIXEL;
        float height = EAR.length * EAR_PIXEL;
        String[] base = java.util.Arrays.copyOfRange(EAR, EAR.length - EAR_BASE_ROWS, EAR.length);
        for (int side = -1; side <= 1; side += 2) {
            PartDefinition ear = pivot(head, side < 0 ? "left_ear" : "right_ear",
                    PartPose.offsetAndRotation(side * EAR_SPACING, EAR_Y, -0.2F, 0.0F, 0.0F, side * EAR_TILT));
            Coat.Part patch = side < 0 ? PATCH : MAIN;
            Map<Character, Coat.Part> shell = Map.of('M', MAIN, 'I', MAIN, 'F', MAIN, 'T', POINTS, 'P', patch);
            float x0 = -width / 2;
            FurBuilder b = new FurBuilder(ear);
            // back shell (the whole silhouette in fur), thicker at the base, then the pink inner ear and the tufts
            b.extrude(EAR, shell, x0, -height, -0.4F, EAR_PIXEL, 0.8F);
            b.extrude(base, shell, x0, -EAR_BASE_ROWS * EAR_PIXEL, 0.4F, EAR_PIXEL, 0.35F);
            b.extrude(EAR, Map.of('I', INNER, 'F', INNER), x0, -height, -0.55F, EAR_PIXEL, 0.15F);
            b.extrude(EAR, Map.of('F', TUFT), x0, -height, -0.75F, EAR_PIXEL, 0.2F);
            // two wisps stick out of the ear's front over the forehead
            b.box(TUFT, x0 + 3.5F * EAR_PIXEL, -2.5F * EAR_PIXEL, -1.05F, EAR_PIXEL, 2.0F * EAR_PIXEL, 0.3F);
            b.box(TUFT, x0 + 5.5F * EAR_PIXEL, -2.0F * EAR_PIXEL, -1.0F, EAR_PIXEL, 1.5F * EAR_PIXEL, 0.25F);
            b.build();
        }
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    // ------------------------------------------------------------------ tail

    /**
     * A cat tail: a slim core under a coat of fur slabs (light belly underneath), tabby stripes over the top and sides,
     * small tufts that break the boxy outline, and a fluffy tip. Many short segments, so the J curve reads smooth.
     */
    private static final int TAIL_SEGMENTS = 16;
    private static final float TAIL_SEGMENT_LENGTH = 1.0F;
    private static final float TAIL_BASE_WIDTH = 1.7F;
    private static final float TAIL_TIP_WIDTH = 1.25F;
    /** Fur slabs lie this thick over the core. */
    private static final float TAIL_FUR = 0.35F;
    /** How much the fur thickness wanders between segments. */
    private static final float TAIL_FLUFF = 0.12F;
    /** Last segments are the tip (POINTS color). */
    private static final int TAIL_TIP_FROM = 13;
    /** A stripe on every third segment (PATCH color: tabby stripes, Calico black). */
    private static final int TAIL_STRIPE_EVERY = 3;
    /** Fur clumps on every third segment (between the stripes), side ones alternating. */
    private static final int TAIL_TUFT_EVERY = 3;

    private static LayerDefinition tail() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition parent = mesh.getRoot().getChild("body");
        float len = TAIL_SEGMENT_LENGTH;
        for (int i = 0; i < TAIL_SEGMENTS; i++) {
            float w = Mth.lerp(i / (float) (TAIL_SEGMENTS - 1), TAIL_BASE_WIDTH, TAIL_TIP_WIDTH);
            float h = w / 2;
            PartPose pose = i == 0 ? PartPose.offset(0.0F, 10.5F, 1.8F) : PartPose.offset(0.0F, 0.0F, len);
            parent = pivot(parent, "segment" + i, pose);
            boolean tip = i >= TAIL_TIP_FROM;
            Coat.Part fur = tip ? POINTS : MAIN;
            FurBuilder b = new FurBuilder(parent);
            // core, a little longer than the segment so neighbours overlap while the tail bends
            b.box(fur, -h, -h, 0.0F, w, w, len + 0.15F);
            if (i == 0) {
                b.build();
                continue; // the root stays slim where it meets the body
            }
            // fur slabs: top and sides in the coat color, the belly (+y before the droop) in the light undercoat.
            // Their thickness wanders from segment to segment, so the outline reads as fluff, not as a pipe.
            float f = TAIL_FUR + TAIL_FLUFF * Mth.sin(i * 2.3F);
            b.box(fur, -h, -h - f, 0.05F, w, f, len);
            b.box(fur, -h - f, -h, 0.05F, f, w, len);
            b.box(fur, h, -h, 0.05F, f, w, len);
            b.box(tip ? POINTS : TUFT, -h + 0.15F, h, 0.05F, w - 0.3F, f, len);
            if (!tip && i % TAIL_STRIPE_EVERY == TAIL_STRIPE_EVERY - 1) {
                // a tabby stripe over the top and down the sides (not the belly), sitting just above the fur
                float s = f + 0.12F;
                b.box(PATCH, -h - s, -h - s, 0.0F, w + 2 * s, s, len + 0.1F);
                b.box(PATCH, -h - s, -h, 0.0F, s, w * 0.75F, len + 0.1F);
                b.box(PATCH, h, -h, 0.0F, s, w * 0.75F, len + 0.1F);
            }
            if (!tip && i % TAIL_TUFT_EVERY == 1) {
                // clumps of fur sticking out: one on a side (alternating) and one on top
                float side = (i / TAIL_TUFT_EVERY) % 2 == 0 ? -1.0F : 1.0F;
                float x = side < 0 ? -h - f - 0.45F : h + f - 0.15F;
                b.box(fur, x, -h, 0.2F, 0.6F, 0.9F, len * 0.7F);
                b.box(fur, -0.4F, -h - f - 0.4F, 0.4F, 0.8F, 0.55F, len * 0.6F);
            }
            if (i == TAIL_SEGMENTS - 1) {
                // fluffy tip: a flared tuft, then a smaller one and a point
                b.box(POINTS, -h - 0.25F, -h - 0.25F, len, w + 0.5F, w + 0.5F, 0.9F);
                b.box(POINTS, -h + 0.1F, -h + 0.1F, len + 0.9F, w - 0.2F, w - 0.2F, 0.6F);
                b.box(POINTS, -0.35F, -0.35F, len + 1.5F, 0.7F, 0.7F, 0.4F);
            }
            b.build();
        }
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    // ------------------------------------------------------------------ paws (hands)

    /** Hand covers start this far down the arm (0 = shoulder pivot, the hand ends at 10). */
    private static final float GLOVE_TOP = 5.5F;
    private static final float MITTEN_TOP = 4.6F;
    private static final float ARM_BOTTOM = 10.0F;

    /**
     * Paw gloves (snug, short cuff) or mittens (big and fluffy). Arm-local: arms are 4 px wide, 3 on slim skins;
     * the right arm spans x -3..1 (slim -2..1), the left -1..3 (slim -1..2); the front of the arm is -z.
     */
    private static LayerDefinition hands(boolean mittens, boolean slim) {
        MeshDefinition mesh = emptyPlayerMesh(slim);
        float w = slim ? 3.0F : 4.0F;
        for (String side : new String[]{"right_arm", "left_arm"}) {
            float x0 = side.equals("right_arm") ? (slim ? -2.0F : -3.0F) : -1.0F;
            FurBuilder b = new FurBuilder(mesh.getRoot().getChild(side));
            if (mittens) {
                mitten(b, x0, w);
            } else {
                glove(b, x0, w);
            }
            b.build();
        }
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private static void glove(FurBuilder b, float x0, float w) {
        float g = 0.35F;
        b.box(MAIN, x0 - g, GLOVE_TOP, -2 - g, w + 2 * g, ARM_BOTTOM - GLOVE_TOP + g, 4 + 2 * g);
        b.box(TUFT, x0 - g - 0.2F, GLOVE_TOP - 0.5F, -2 - g - 0.2F, w + 2 * g + 0.4F, 1.0F, 4 + 2 * g + 0.4F); // cuff
        toes(b, x0 - g, w + 2 * g, ARM_BOTTOM + g, -2 - g, 0.5F);
        pads(b, x0 + w / 2, ARM_BOTTOM - 1.6F, -2 - g - 0.15F, 1.0F, true);
    }

    private static void mitten(FurBuilder b, float x0, float w) {
        float g = 0.6F;
        b.box(MAIN, x0 - g, MITTEN_TOP, -2 - g, w + 2 * g, ARM_BOTTOM - MITTEN_TOP + g, 4 + 2 * g);
        b.box(TUFT, x0 - g - 0.3F, MITTEN_TOP - 1.0F, -2 - g - 0.3F, w + 2 * g + 0.6F, 1.4F, 4 + 2 * g + 0.6F); // fluffy cuff
        // fur clumps on the outer sides break the box outline
        b.box(MAIN, x0 - g - 0.3F, MITTEN_TOP + 1.6F, -1.0F, 0.3F, 1.2F, 2.0F);
        b.box(MAIN, x0 + w + g, MITTEN_TOP + 2.6F, -0.6F, 0.3F, 1.0F, 1.6F);
        toes(b, x0 - g, w + 2 * g, ARM_BOTTOM + g, -2 - g, 0.7F);
        pads(b, x0 + w / 2, ARM_BOTTOM - 1.8F, -2 - g - 0.15F, 1.25F, true);
    }

    /** Three round toes along the front of a paw's underside. */
    private static void toes(FurBuilder b, float x, float width, float y, float z, float height) {
        float toe = width / 3;
        for (int i = 0; i < 3; i++) {
            b.box(MAIN, x + i * toe + 0.1F, y, z + 0.2F, toe - 0.2F, height, 2.2F);
        }
    }

    /**
     * A pink paw print on a front face at depth {@code z}: the big pad at {@code y} and three beans above it, or below
     * it with {@code toesDown} (a hand: the beans sit at the fingertips, the pad toward the wrist). Both take the same
     * space; the middle bean reaches furthest.
     */
    private static void pads(FurBuilder b, float centerX, float y, float z, float scale, boolean toesDown) {
        float pad = 1.6F * scale;
        float padY = toesDown ? y - 1.15F * scale : y;
        b.box(INNER, centerX - pad / 2, padY, z, pad, 1.0F * scale, 0.15F);
        float bean = 0.5F * scale;
        float beanY = toesDown ? y + 0.25F * scale : y - 0.9F * scale;
        float middle = (toesDown ? 0.25F : -0.25F) * scale;
        for (int i = -1; i <= 1; i++) {
            b.box(INNER, centerX + i * 0.75F * scale - bean / 2, beanY + (i == 0 ? middle : 0.0F), z, bean, bean, 0.15F);
        }
    }

    // ------------------------------------------------------------------ paws (legs)

    /** Leg-local: legs span x -2..2, y 0 (hip) .. 12 (sole), z -2..2. */
    private static LayerDefinition slippers() {
        MeshDefinition mesh = emptyPlayerMesh();
        for (String side : new String[]{"right_leg", "left_leg"}) {
            FurBuilder b = new FurBuilder(mesh.getRoot().getChild(side));
            b.box(MAIN, -2.5F, 9.6F, -2.5F, 5.0F, 2.4F, 5.0F);         // slipper
            b.box(MAIN, -2.3F, 10.4F, -3.9F, 4.6F, 1.6F, 1.5F);        // round toe sticking out
            b.box(TUFT, -2.75F, 9.1F, -2.75F, 5.5F, 0.8F, 5.5F);       // fluffy rim
            b.box(INNER, -2.3F, 11.9F, -3.9F, 4.6F, 0.25F, 6.3F);      // pink sole
            // toe beans on top of the toe
            for (int i = -1; i <= 1; i++) {
                b.box(INNER, i * 1.25F - 0.35F, 10.2F, -3.6F, 0.7F, 0.25F, 0.7F);
            }
            // little cat ears on the slipper's front: a cat face
            for (int e = -1; e <= 1; e += 2) {
                float x = e * 1.25F - 0.55F;
                b.box(MAIN, x, 9.0F, -2.9F, 1.1F, 0.7F, 0.6F);
                b.box(MAIN, x + 0.25F, 8.5F, -2.9F, 0.6F, 0.5F, 0.6F);
                b.box(INNER, x + 0.3F, 9.1F, -3.0F, 0.5F, 0.6F, 0.1F);
            }
            b.build();
        }
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private static LayerDefinition legWarmers() {
        MeshDefinition mesh = emptyPlayerMesh();
        for (String side : new String[]{"right_leg", "left_leg"}) {
            FurBuilder b = new FurBuilder(mesh.getRoot().getChild(side));
            float g = 0.45F;
            b.box(MAIN, -2 - g, 4.6F, -2 - g, 4 + 2 * g, 5.0F, 4 + 2 * g);
            float c = 0.65F;
            b.box(TUFT, -2 - c, 4.0F, -2 - c, 4 + 2 * c, 1.0F, 4 + 2 * c);   // ribbed top cuff
            b.box(TUFT, -2 - c, 9.3F, -2 - c, 4 + 2 * c, 0.9F, 4 + 2 * c);   // bottom cuff
            float s = 0.55F;
            b.box(PATCH, -2 - s, 5.4F, -2 - s, 4 + 2 * s, 0.45F, 4 + 2 * s);  // knit stripes
            b.box(PATCH, -2 - s, 8.4F, -2 - s, 4 + 2 * s, 0.45F, 4 + 2 * s);
            pads(b, 0.0F, 7.0F, -2 - s - 0.1F, 1.0F, false);
            b.build();
        }
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    // ------------------------------------------------------------------ collars

    /** Body-local: the neck is the body's top (y 0), the body spans x -4..4, z -2..2. */
    private static LayerDefinition bellCollar() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition body = mesh.getRoot().getChild("body");
        FurBuilder b = new FurBuilder(body);
        b.box(MAIN, -4.55F, -0.1F, -2.55F, 9.1F, 1.3F, 5.1F);
        b.box(INNER, -4.6F, 1.0F, -2.6F, 9.2F, 0.25F, 5.2F);   // stitched edge
        b.box(POINTS, -0.3F, 1.1F, -2.8F, 0.6F, 0.4F, 0.25F);  // ring
        b.box(POINTS, 3.2F, 0.15F, -2.65F, 0.9F, 0.8F, 0.2F);  // buckle
        b.build();
        FurBuilder bell = new FurBuilder(pivot(body, "bell", PartPose.offset(0.0F, 1.4F, -3.0F)));
        bell.box(POINTS, -0.75F, 0.0F, -0.75F, 1.5F, 1.3F, 1.5F);
        bell.box(POINTS, -0.55F, -0.2F, -0.55F, 1.1F, 0.2F, 1.1F);
        bell.box(PATCH, -0.5F, 0.85F, -0.8F, 1.0F, 0.2F, 0.1F); // slit
        bell.box(PATCH, -0.15F, 1.05F, -0.8F, 0.3F, 0.3F, 0.1F);
        bell.box(POINTS, -0.25F, 1.3F, -0.25F, 0.5F, 0.25F, 0.5F);
        bell.build();
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private static LayerDefinition velvetChoker() {
        MeshDefinition mesh = emptyPlayerMesh();
        FurBuilder b = new FurBuilder(mesh.getRoot().getChild("body"));
        b.box(MAIN, -4.5F, 0.05F, -2.5F, 9.0F, 0.85F, 5.0F);
        // a tiny gold heart on the front
        b.box(POINTS, -0.6F, 0.95F, -2.7F, 0.5F, 0.45F, 0.2F);
        b.box(POINTS, 0.1F, 0.95F, -2.7F, 0.5F, 0.45F, 0.2F);
        b.box(POINTS, -0.45F, 1.4F, -2.7F, 0.9F, 0.35F, 0.2F);
        b.box(POINTS, -0.2F, 1.75F, -2.7F, 0.4F, 0.25F, 0.2F);
        b.build();
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private static LayerDefinition neckRibbon() {
        MeshDefinition mesh = emptyPlayerMesh();
        FurBuilder b = new FurBuilder(mesh.getRoot().getChild("body"));
        b.box(MAIN, -4.5F, 0.2F, -2.5F, 9.0F, 0.7F, 5.0F);
        // bow: two loops, their shaded insides, the knot and two tails
        b.box(MAIN, -2.5F, -0.6F, -2.95F, 2.0F, 1.9F, 0.4F);
        b.box(MAIN, 0.5F, -0.6F, -2.95F, 2.0F, 1.9F, 0.4F);
        b.box(INNER, -2.0F, -0.15F, -3.0F, 1.1F, 1.0F, 0.1F);
        b.box(INNER, 0.9F, -0.15F, -3.0F, 1.1F, 1.0F, 0.1F);
        b.box(INNER, -0.55F, -0.2F, -3.1F, 1.1F, 1.2F, 0.5F);
        b.box(MAIN, -1.1F, 0.9F, -2.85F, 0.65F, 1.9F, 0.2F);
        b.box(MAIN, 0.45F, 0.9F, -2.85F, 0.65F, 1.7F, 0.2F);
        b.box(TUFT, -2.3F, -0.5F, -3.0F, 0.4F, 0.3F, 0.1F); // shine
        b.build();
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    // ------------------------------------------------------------------ face

    /**
     * Head-local: the face is the head's front (z -4), the skin's hat layer sits half a pixel further out (-4.5), so
     * face pieces float just in front of it. y runs from -8 (top of the head) to 0 (chin); the eyes are around -4.5.
     */
    private static final float FACE_Z = -4.6F;
    private static final float FACE_DEPTH = 0.05F;

    /** Three whiskers on each cheek, fanning out past the edge of the face. */
    private static LayerDefinition whiskers() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition head = mesh.getRoot().getChild("head");
        float length = 2.8F;
        float thickness = 0.2F;
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 3; i++) {
                // the top whisker tilts up, the bottom one down; positive zRot turns +x down
                float tilt = (i - 1) * 0.2F * side;
                PartDefinition whisker = pivot(head, (side < 0 ? "left" : "right") + "_whisker" + i,
                        PartPose.offsetAndRotation(side * 2.0F, -2.9F + i * 0.55F, FACE_Z, 0.0F, 0.0F, tilt));
                FurBuilder b = new FurBuilder(whisker);
                b.box(MAIN, side < 0 ? -length : 0.0F, -thickness / 2, 0.0F, length, thickness, FACE_DEPTH);
                b.build();
            }
        }
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    /** Soft pink cheeks (drawn see-through) with three little strokes on each, anime style. */
    private static LayerDefinition blush() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition head = mesh.getRoot().getChild("head");
        FurBuilder cheeks = new FurBuilder(head);
        for (int side = -1; side <= 1; side += 2) {
            float x = side * 2.6F;
            cheeks.box(MAIN, x - 1.0F, -3.4F, FACE_Z, 2.0F, 0.8F, FACE_DEPTH);
            cheeks.box(MAIN, x - 0.7F, -3.6F, FACE_Z, 1.4F, 1.2F, FACE_DEPTH);
        }
        cheeks.build();
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 3; i++) {
                PartDefinition stroke = pivot(head, (side < 0 ? "left" : "right") + "_stroke" + i,
                        PartPose.offsetAndRotation(side * 2.6F + (i - 1) * 0.5F, -3.0F, FACE_Z - 0.02F, 0.0F, 0.0F, -0.6F));
                FurBuilder b = new FurBuilder(stroke);
                b.box(INNER, -0.08F, -0.35F, 0.0F, 0.16F, 0.7F, FACE_DEPTH);
                b.build();
            }
        }
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    // ------------------------------------------------------------------ head

    /** A small cat ear for headphones: M shell, I inner. */
    private static final String[] SMALL_EAR = {
            "..M..",
            ".MMM.",
            ".MIM.",
            "MMIMM",
            "MIIIM",
    };

    /**
     * Cat-ear headphones: a band over the head (above the hat layer), cups on the sides with soft cushions and a light
     * on the outside, and two small cat ears on the band.
     */
    private static LayerDefinition headphones() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition head = mesh.getRoot().getChild("head");
        FurBuilder b = new FurBuilder(head);
        // The band reaches down to the head itself, so a skin without hair in the hat layer shows no gap under it
        b.box(MAIN, -4.9F, -9.1F, -0.8F, 9.8F, 1.1F, 1.6F);              // band over the top
        for (int side = -1; side <= 1; side += 2) {
            float outer = side < 0 ? -4.9F : 4.0F;
            b.box(MAIN, outer, -8.6F, -0.7F, 0.9F, 3.0F, 1.4F);           // band down the side
            float cup = side < 0 ? -5.7F : 4.5F;
            b.box(MAIN, cup, -5.9F, -1.9F, 1.2F, 3.8F, 3.8F);             // ear cup
            b.box(INNER, side < 0 ? -4.9F : 4.45F, -5.6F, -1.6F, 0.45F, 3.2F, 3.2F); // cushion against the head
            b.box(TUFT, side < 0 ? -5.8F : 5.6F, -4.6F, -0.6F, 0.2F, 1.2F, 1.2F);    // light
        }
        b.build();
        float pixel = 0.45F;
        float width = SMALL_EAR[0].length() * pixel;
        float height = SMALL_EAR.length * pixel;
        for (int side = -1; side <= 1; side += 2) {
            PartDefinition ear = pivot(head, side < 0 ? "left_ear" : "right_ear",
                    PartPose.offsetAndRotation(side * 2.6F, -9.1F, 0.0F, 0.0F, 0.0F, side * 0.2F));
            FurBuilder e = new FurBuilder(ear);
            e.extrude(SMALL_EAR, Map.of('M', MAIN, 'I', MAIN), -width / 2, -height, -0.35F, pixel, 0.7F);
            e.extrude(SMALL_EAR, Map.of('I', TUFT), -width / 2, -height, -0.45F, pixel, 0.1F);
            e.build();
        }
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    /** A bow seen from the front: M loops, K the knot. */
    private static final String[] BOW = {
            "MM.MM",
            "MMKMM",
            "MM.MM",
    };

    /** Two bows where the cat ears meet the head, a little to the outside (they sit right with or without ears). */
    private static LayerDefinition earBows() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition head = mesh.getRoot().getChild("head");
        float pixel = 0.42F;
        float width = BOW[0].length() * pixel;
        float height = BOW.length * pixel;
        for (int side = -1; side <= 1; side += 2) {
            PartDefinition bow = pivot(head, side < 0 ? "left_bow" : "right_bow",
                    PartPose.offsetAndRotation(side * 3.5F, -8.7F, -1.0F, 0.0F, 0.0F, side * -0.35F));
            FurBuilder b = new FurBuilder(bow);
            b.extrude(BOW, Map.of('M', MAIN), -width / 2, -height / 2, -0.2F, pixel, 0.4F);
            b.extrude(BOW, Map.of('K', INNER), -width / 2, -height / 2, -0.3F, pixel, 0.6F);
            b.box(TUFT, -width / 2 + 0.1F, -height / 2 + 0.1F, -0.25F, 0.3F, 0.2F, 0.05F); // shine
            b.build();
        }
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    /** A thin headband over the top of the head with a little bell hanging off the right side. */
    private static LayerDefinition bellHeadband() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition head = mesh.getRoot().getChild("head");
        FurBuilder b = new FurBuilder(head);
        b.box(MAIN, -4.6F, -9.0F, -1.2F, 9.2F, 0.5F, 1.1F);
        b.box(MAIN, -4.75F, -8.6F, -1.2F, 0.4F, 2.4F, 1.1F);
        b.box(MAIN, 4.35F, -8.6F, -1.2F, 0.4F, 2.4F, 1.1F);
        b.box(INNER, -4.6F, -8.55F, -1.25F, 9.2F, 0.1F, 1.2F); // edge
        b.box(POINTS, 2.9F, -8.75F, -1.5F, 0.5F, 0.4F, 0.3F);  // ring
        b.build();
        FurBuilder bell = new FurBuilder(pivot(head, "bell", PartPose.offset(3.15F, -8.4F, -1.7F)));
        bell.box(POINTS, -0.6F, 0.0F, -0.6F, 1.2F, 1.05F, 1.2F);
        bell.box(PATCH, -0.4F, 0.7F, -0.65F, 0.8F, 0.15F, 0.1F);
        bell.box(POINTS, -0.2F, 1.05F, -0.2F, 0.4F, 0.2F, 0.4F);
        bell.build();
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    // ================================================================== models

    /** Base: poses like the wearer and shows one coat part of the layer. */
    public static class FurModel extends PlayerModel {

        public FurModel(ModelPart root, Coat.Part part) {
            super(root, false);
            FurBuilder.show(root, part);
        }

        /**
         * Hides the skin's outer layer parts: cosmetics never draw there, and mods that put the skin's 3D layers into
         * every player model (3D Skin Layers) would otherwise draw the wearer's outer skin with the fur texture.
         */
        @Override
        public void setupAnim(AvatarRenderState state) {
            super.setupAnim(state);
            hat.visible = false;
            jacket.visible = false;
            leftSleeve.visible = false;
            rightSleeve.visible = false;
            leftPants.visible = false;
            rightPants.visible = false;
        }
    }

    /** Ears flick now and then, bounce while walking and press back flat when the wearer is hurt. */
    public static final class EarsModel extends FurModel {
        private static final float TWITCH = 0.35F;
        private static final float TWITCH_SPEED = 0.35F;
        private static final float TWITCH_SHARPNESS = 12.0F;
        private static final float WALK_BOUNCE = 0.08F;
        private static final float FLAT_BACK = 0.9F;
        private static final float FLAT_OUT = 0.5F;
        /** A helmet's shell is 1 px above the head; ears poke out on top of it. */
        private static final float HELMET_LIFT = 1.0F;
        private final ModelPart leftEar;
        private final ModelPart rightEar;

        public EarsModel(ModelPart root, Coat.Part part) {
            super(root, part);
            this.leftEar = head.getChild("left_ear");
            this.rightEar = head.getChild("right_ear");
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            super.setupAnim(state);
            CosmeticRenderData data = CosmeticRenderData.of(state);
            float t = state.ageInTicks * TWITCH_SPEED / Mth.PI + data.phase();
            float left = (float) Math.pow(Math.max(Mth.sin(t), 0.0F), TWITCH_SHARPNESS) * TWITCH;
            float right = (float) Math.pow(Math.max(Mth.sin(t * 0.83F + 1.7F), 0.0F), TWITCH_SHARPNESS) * TWITCH;
            float bounce = Mth.sin(state.walkAnimationPos * 0.6662F * 2) * WALK_BOUNCE * data.walkAmount();
            float flat = data.flatten();
            // "left_ear" sits at -x: outward is a negative zRot; flattening folds the ears back (negative xRot)
            leftEar.zRot = -EAR_TILT - left - bounce - flat * FLAT_OUT;
            rightEar.zRot = EAR_TILT + right + bounce + flat * FLAT_OUT;
            leftEar.xRot = -left * 0.5F - flat * FLAT_BACK;
            rightEar.xRot = -right * 0.5F - flat * FLAT_BACK;
            float lift = wearsHelmet(state) ? HELMET_LIFT : 0.0F;
            leftEar.y = leftEar.getInitialPose().y() - lift;
            rightEar.y = rightEar.getInitialPose().y() - lift;
        }

        /** Head armor drawn by the armor layer (not a carved pumpkin or a skull, which have their own shapes). */
        public static boolean wearsHelmet(AvatarRenderState state) {
            if (state.headEquipment.isEmpty()) {
                return false;
            }
            Equippable equippable = state.headEquipment.get(DataComponents.EQUIPPABLE);
            return equippable != null && equippable.slot() == EquipmentSlot.HEAD && equippable.assetId().isPresent();
        }
    }

    /** Tail hangs down-back and curls up at the end, waves along its length, lifts while walking, swings on turns. */
    public static final class TailModel extends FurModel {
        private static final float DROOP = -0.95F;
        private static final float CURL = 0.1F;
        /** Per segment, so the whole tail bends sixteen times this: keep it small or it flops to the side. */
        private static final float IDLE_SWAY = 0.034F;
        private static final float WALK_SWAY = 0.075F;
        private static final float SWAY_SPEED = 0.14F;
        private static final float SEGMENT_PHASE = 0.3F;
        private static final float WALK_LIFT = 0.35F;
        private final ModelPart[] segments = new ModelPart[TAIL_SEGMENTS];

        public TailModel(ModelPart root, Coat.Part part) {
            super(root, part);
            ModelPart segment = body;
            for (int i = 0; i < TAIL_SEGMENTS; i++) {
                segment = segment.getChild("segment" + i);
                segments[i] = segment;
            }
        }

        /** The tail hanging still: the first segment droops, the rest curl a little. */
        public static void applyRestPose(ModelPart root) {
            ModelPart segment = root.getChild("body");
            for (int i = 0; i < TAIL_SEGMENTS; i++) {
                segment = segment.getChild("segment" + i);
                segment.xRot = i == 0 ? DROOP : CURL;
                segment.yRot = 0.0F;
            }
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            super.setupAnim(state);
            CosmeticRenderData data = CosmeticRenderData.of(state);
            float amplitude = IDLE_SWAY + WALK_SWAY * data.walkAmount();
            for (int i = 0; i < TAIL_SEGMENTS; i++) {
                ModelPart segment = segments[i];
                segment.xRot = i == 0 ? DROOP + WALK_LIFT * data.walkAmount() : CURL;
                float wave = Mth.sin(state.ageInTicks * SWAY_SPEED + data.phase() - i * SEGMENT_PHASE);
                segment.yRot = wave * amplitude + data.turnSway() / TAIL_SEGMENTS;
            }
        }
    }

    /** The bell sways while walking and when the wearer turns. */
    public static final class BellCollarModel extends FurModel {
        private static final float WALK_SWING = 0.45F;
        private static final float TURN_SWING = 0.6F;
        private final ModelPart bell;

        public BellCollarModel(ModelPart root, Coat.Part part) {
            super(root, part);
            this.bell = body.getChild("bell");
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            super.setupAnim(state);
            CosmeticRenderData data = CosmeticRenderData.of(state);
            bell.xRot = Mth.sin(state.walkAnimationPos * 0.6662F * 2) * WALK_SWING * data.walkAmount();
            bell.zRot = data.turnSway() * TURN_SWING;
        }
    }

    /**
     * Ears or tail on a cat-person mob (the humanoid rig): the same geometry and coat parts as the
     * players' cosmetics, posed like the mob's own model, with the same twitch, sway and flattening.
     */
    public static final class NekoFurModel extends HumanoidModel<NekoRenderState> {
        private static final float TWITCH = 0.35F;
        private static final float TWITCH_SPEED = 0.35F;
        private static final float TWITCH_SHARPNESS = 12.0F;
        private static final float FLAT_BACK = 0.9F;
        private static final float FLAT_OUT = 0.5F;
        private static final float DROOP = -0.95F;
        private static final float CURL = 0.1F;
        private static final float SWAY = 0.05F;
        private static final float SWAY_SPEED = 0.12F;
        private static final float SEGMENT_PHASE = 0.3F;
        private final @org.jetbrains.annotations.Nullable ModelPart leftEar;
        private final @org.jetbrains.annotations.Nullable ModelPart rightEar;
        private final ModelPart[] segments;

        public NekoFurModel(ModelPart root, Coat.Part part) {
            super(root);
            FurBuilder.show(root, part);
            leftEar = head.hasChild("left_ear") ? head.getChild("left_ear") : null;
            rightEar = head.hasChild("right_ear") ? head.getChild("right_ear") : null;
            java.util.List<ModelPart> chain = new java.util.ArrayList<>();
            ModelPart segment = body;
            while (segment.hasChild("segment" + chain.size())) {
                segment = segment.getChild("segment" + chain.size());
                chain.add(segment);
            }
            segments = chain.toArray(new ModelPart[0]);
        }

        @Override
        public void setupAnim(NekoRenderState state) {
            super.setupAnim(state);
            float phase = state.phase;
            if (leftEar != null && rightEar != null) {
                float t = state.ageInTicks * TWITCH_SPEED / Mth.PI + phase;
                float left = (float) Math.pow(Math.max(Mth.sin(t), 0.0F), TWITCH_SHARPNESS) * TWITCH;
                float right = (float) Math.pow(Math.max(Mth.sin(t * 0.83F + 1.7F), 0.0F), TWITCH_SHARPNESS) * TWITCH;
                float flat = state.hasRedOverlay ? 1.0F : 0.0F;
                leftEar.zRot = -EAR_TILT - left - flat * FLAT_OUT;
                rightEar.zRot = EAR_TILT + right + flat * FLAT_OUT;
                leftEar.xRot = -left * 0.5F - flat * FLAT_BACK;
                rightEar.xRot = -right * 0.5F - flat * FLAT_BACK;
            }
            for (int i = 0; i < segments.length; i++) {
                segments[i].xRot = i == 0 ? DROOP : CURL;
                segments[i].yRot = Mth.sin(state.ageInTicks * SWAY_SPEED + phase - i * SEGMENT_PHASE) * SWAY;
            }
        }
    }
}
