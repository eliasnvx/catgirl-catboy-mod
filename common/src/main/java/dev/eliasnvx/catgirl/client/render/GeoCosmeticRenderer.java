package dev.eliasnvx.catgirl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.client.render.model.CatModels;
import dev.eliasnvx.catgirl.client.render.model.FurBuilder;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.core.state.BoneSnapshot;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * A Blockbench/GeckoLib model for a worn piece, used instead of the code model whenever its file exists (a resource
 * pack can add one too). Adapted from Femboy Mod's geo renderer, plus coat parts and the code models' motion.
 *
 * <p>Files for an item {@code catgirl:<item>} (GeckoLib 4 paths):
 * <ul>
 *     <li>model: {@code assets/catgirl/geo/cosmetic/<item>.geo.json}; slim-armed players use
 *     {@code <item>_slim.geo.json} when it exists</li>
 *     <li>texture: {@code assets/catgirl/textures/cosmetic/<item>.png}, grayscale: every pixel is multiplied by the
 *     color of the coat part (or dye) its bone belongs to</li>
 * </ul>
 * <p>Bones:
 * <ul>
 *     <li>{@code armorHead}, {@code armorBody}, {@code armorLeftArm}, {@code armorRightArm}, {@code armorLeftLeg},
 *     {@code armorRightLeg} follow the player's body, like GeckoLib armor</li>
 *     <li>a bone whose name ends in {@code _inner}, {@code _points}, {@code _patch} or {@code _tuft} is drawn in that
 *     coat part's color; any other bone in the main color. The model is drawn once per part present, with the other
 *     parts' bones hidden (their children still draw)</li>
 *     <li>a bone named like a moving part of the code model ({@code left_ear}, {@code segment0}…, {@code bell}) gets
 *     the same motion on top of its own rest pose: ear twitches and flattening, tail sway, bell swing</li>
 * </ul>
 * The baked model is shared, so every draw sets the bones it needs right before GeckoLib draws them.
 */
public final class GeoCosmeticRenderer {

    private static final Coat.Part[] PARTS = Coat.Part.values();
    private static final String[] SEGMENT_BONES = {"armorHead", "armorBody", "armorLeftArm", "armorRightArm",
            "armorLeftLeg", "armorRightLeg"};
    /** Rest pivots of the vanilla humanoid arms and legs (GeoArmorRenderer#applyBaseTransformations), per segment. */
    private static final float[] SEGMENT_X = {0.0F, 0.0F, -5.0F, 5.0F, -2.0F, 2.0F};
    private static final float[] SEGMENT_Y = {0.0F, 0.0F, 2.0F, 2.0F, 12.0F, 12.0F};
    private static final int OPAQUE = 255;

    private final ResourceLocation[] models;
    private final ResourceLocation texture;
    private final CatModels.FurModel posed;
    private final Function<String, ModelPart> codeParts;
    private final Function<String, ModelPart> restParts;
    private final Animatable animatable = new Animatable();
    private final Renderer renderer = new Renderer(new Model());
    private final Related related = new Related();

    // what the current baked model holds; rebuilt when a resource reload brings a new one
    private @Nullable BakedGeoModel indexed;
    private GeoBone[] bones = new GeoBone[0];
    private Coat.Part[] boneParts = new Coat.Part[0];
    private final boolean[] partPresent = new boolean[PARTS.length];
    private final GeoBone[] segmentBones = new GeoBone[SEGMENT_BONES.length];
    private final ModelPart[] bodyParts = new ModelPart[SEGMENT_BONES.length];
    private GeoBone[] movingBones = new GeoBone[0];
    private ModelPart[] movingParts = new ModelPart[0];
    private PartPose[] restPoses = new PartPose[0];

    /**
     * @param item   the item's path ({@code paw_gloves})
     * @param slim   whether this renderer draws for slim-armed players (tries {@code <item>_slim} first)
     * @param layer  the code model's layer: its moving parts drive the geo model's bones of the same name
     * @param posed  an instance of the code model, posed here every draw to read the motion from
     */
    public GeoCosmeticRenderer(String item, boolean slim, ModelLayerLocation layer, CatModels.FurModel posed, ModelPart restRoot) {
        ResourceLocation wide = modelId(item);
        this.models = slim ? new ResourceLocation[]{modelId(item + "_slim"), wide} : new ResourceLocation[]{wide};
        this.texture = CatgirlMod.id("textures/cosmetic/" + item + ".png");
        this.posed = posed;
        this.codeParts = FurBuilder.partLookup(posed.root());
        CatModels.applyRestPose(layer, restRoot);
        this.restParts = FurBuilder.partLookup(restRoot);
    }

    private static ResourceLocation modelId(String name) {
        return CatgirlMod.id("geo/cosmetic/" + name + ".geo.json");
    }

    /** The baked geo model to draw, or null to fall back to the code model. Checked every frame. */
    private @Nullable BakedGeoModel model() {
        for (ResourceLocation id : models) {
            BakedGeoModel model = GeckoLibCache.getBakedModels().get(id);
            if (model != null) {
                related.model = id;
                return model;
            }
        }
        return null;
    }

    /** @return false when no geo model exists for this item (the caller draws the code model) */
    public boolean render(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, PlayerPose state,
                          PlayerModel<AbstractClientPlayer> parent, int[] colors, int alpha) {
        BakedGeoModel model = model();
        if (model == null) {
            return false;
        }
        if (model != indexed) {
            index(model);
        }
        parent.copyPropertiesTo(posed);
        posed.setupAnim(state);
        related.entityId = state.id;
        related.overlay = overlay;
        related.parent = parent;
        RenderType type = alpha < OPAQUE ? RenderType.entityTranslucent(texture) : RenderType.entityCutout(texture);
        for (Coat.Part part : PARTS) {
            if (!partPresent[part.ordinal()]) {
                continue;
            }
            related.part = part;
            related.color = ARGB32.color(alpha, colors[part.ordinal()]);
            // a non-null buffer keeps GeckoLib on our buffer source
            renderer.render(poseStack, animatable, buffers, type, buffers.getBuffer(type), light);
        }
        related.parent = null;
        return true;
    }

    /** Sorts the model's bones into coat parts, body bones and moving bones. */
    private void index(BakedGeoModel model) {
        List<GeoBone> all = new ArrayList<>();
        for (GeoBone bone : model.topLevelBones()) {
            collect(bone, all);
        }
        bones = all.toArray(new GeoBone[0]);
        boneParts = new Coat.Part[bones.length];
        Arrays.fill(partPresent, false);
        List<GeoBone> moving = new ArrayList<>();
        List<ModelPart> movingCode = new ArrayList<>();
        List<PartPose> rest = new ArrayList<>();
        for (int i = 0; i < bones.length; i++) {
            boneParts[i] = partOf(bones[i].getName());
            partPresent[boneParts[i].ordinal()] = true;
            ModelPart code = codeParts.apply(bones[i].getName());
            ModelPart restPart = restParts.apply(bones[i].getName());
            if (code != null && restPart != null && !isSegmentBone(bones[i].getName())) {
                moving.add(bones[i]);
                movingCode.add(code);
                rest.add(restPart.storePose());
            }
        }
        for (int s = 0; s < SEGMENT_BONES.length; s++) {
            segmentBones[s] = model.getBone(SEGMENT_BONES[s]).orElse(null);
        }
        movingBones = moving.toArray(new GeoBone[0]);
        movingParts = movingCode.toArray(new ModelPart[0]);
        restPoses = rest.toArray(new PartPose[0]);
        indexed = model;
    }

    private static void collect(GeoBone bone, List<GeoBone> out) {
        out.add(bone);
        for (GeoBone child : bone.getChildBones()) {
            collect(child, out);
        }
    }

    private static boolean isSegmentBone(String name) {
        for (String segment : SEGMENT_BONES) {
            if (segment.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** {@code left_ear_inner} → INNER; anything without a part suffix is MAIN. */
    static Coat.Part partOf(String bone) {
        String name = bone.toLowerCase(Locale.ROOT);
        for (Coat.Part part : PARTS) {
            if (name.endsWith("_" + part.name().toLowerCase(Locale.ROOT))) {
                return part;
            }
        }
        return Coat.Part.MAIN;
    }

    /** Sets the shared model's bones for the current draw: this part only, the wearer's body, the code model's motion. */
    private void pose(PlayerModel<AbstractClientPlayer> parent, Coat.Part part) {
        for (int i = 0; i < bones.length; i++) {
            bones[i].setHidden(boneParts[i] != part);
        }
        ModelPart[] body = bodyParts;
        body[0] = parent.head;
        body[1] = parent.body;
        body[2] = parent.leftArm;
        body[3] = parent.rightArm;
        body[4] = parent.leftLeg;
        body[5] = parent.rightLeg;
        for (int s = 0; s < SEGMENT_BONES.length; s++) {
            GeoBone bone = segmentBones[s];
            if (bone != null) {
                RenderUtils.matchModelPartRot(body[s], bone);
                bone.updatePosition(body[s].x + SEGMENT_X[s], SEGMENT_Y[s] - body[s].y, body[s].z);
            }
        }
        // Java model space -> GeckoLib: x and z stay, y flips; rotations flip x and y
        for (int i = 0; i < movingParts.length; i++) {
            ModelPart now = movingParts[i];
            PartPose rest = restPoses[i];
            BoneSnapshot initial = movingBones[i].getInitialSnapshot();
            movingBones[i].updateRotation(initial.getRotX() - (now.xRot - rest.xRot), initial.getRotY() - (now.yRot - rest.yRot),
                    initial.getRotZ() + now.zRot - rest.zRot);
            movingBones[i].updatePosition(initial.getOffsetX() + now.x - rest.x, initial.getOffsetY() - (now.y - rest.y),
                    initial.getOffsetZ() + now.z - rest.z);
        }
    }

    /** What the renderer callbacks read for the current draw. */
    private static final class Related {
        int entityId;
        int color = 0xFFFFFFFF;
        int overlay;
        Coat.Part part = Coat.Part.MAIN;
        ResourceLocation model;
        @Nullable PlayerModel<AbstractClientPlayer> parent;
        /** Last color handed to GeckoLib, reused while unchanged. */
        Color cachedColor = Color.WHITE;
    }

    /**
     * Plain core {@link GeoAnimatable} with a singleton (per-instance-id) cache: GeckoLib's {@code SingletonGeoAnimatable}
     * differs between its Fabric and Forge builds on 1.20.1, this does not.
     */
    private final class Animatable implements GeoAnimatable {
        private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            // motion comes from the code model, see pose()
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }

        @Override
        public double getTick(Object object) {
            return RenderUtils.getCurrentTick();
        }
    }

    private final class Model extends GeoModel<Animatable> {
        @Override
        public ResourceLocation getModelResource(Animatable animatable) {
            return related.model;
        }

        @Override
        public ResourceLocation getTextureResource(Animatable animatable) {
            return texture;
        }

        @Override
        public ResourceLocation getAnimationResource(Animatable animatable) {
            return related.model;
        }
    }

    private final class Renderer extends GeoObjectRenderer<Animatable> {
        /** Same transform GeckoLib applies to armor, from player model space. */
        private static final float ARMOR_Y_OFFSET = 24.0F / 16.0F;

        Renderer(GeoModel<Animatable> model) {
            super(model);
        }

        /** Replaces the object renderer's block-centered offset with the armor transform, and poses the bones. */
        @Override
        public void preRender(PoseStack poseStack, Animatable animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                              @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                              int packedOverlay, float red, float green, float blue, float alpha) {
            if (related.parent != null) {
                pose(related.parent, related.part);
            }
            poseStack.translate(0.0F, ARMOR_Y_OFFSET, 0.0F);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
        }

        @Override
        public long getInstanceId(Animatable animatable) {
            return related.entityId;
        }

        @Override
        public Color getRenderColor(Animatable animatable, float partialTick, int packedLight) {
            if (related.cachedColor.argbInt() != related.color) {
                related.cachedColor = new Color(related.color);
            }
            return related.cachedColor;
        }

        @Override
        public int getPackedOverlay(Animatable animatable, float u, float partialTick) {
            return related.overlay;
        }
    }
}
