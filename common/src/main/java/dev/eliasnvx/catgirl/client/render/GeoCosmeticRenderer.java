package dev.eliasnvx.catgirl.client.render;

import com.geckolib.animatable.SingletonGeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoArmorRenderer.ArmorSegment;
import com.geckolib.renderer.GeoObjectRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.GeckoLibUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.client.render.model.CatModels;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * A Blockbench/GeckoLib model for a worn piece, used instead of the code model whenever its file exists (a resource
 * pack can add one too). Adapted from Femboy Mod's geo renderer, plus coat parts and the code models' motion.
 *
 * <p>Files for an item {@code catgirl:<item>}:
 * <ul>
 *     <li>model: {@code assets/catgirl/geckolib/models/cosmetic/<item>.geo.json}; slim-armed players use
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
 *     parts' bones skipped (their children still draw)</li>
 *     <li>a bone named like a moving part of the code model ({@code left_ear}, {@code segment0}…, {@code bell}) gets
 *     the same motion on top of its own rest pose: ear twitches and flattening, tail sway, bell swing</li>
 * </ul>
 * GeckoLib runs bone updaters when the frame is drawn, after every submission, so each player and coat part has its
 * own {@link Pass} holding the pose copied at submit time (reused frame to frame, no per-frame allocation).
 */
public final class GeoCosmeticRenderer {

    private static final Coat.Part[] PARTS = Coat.Part.values();
    private static final ArmorSegment[] SEGMENTS = {ArmorSegment.HEAD, ArmorSegment.CHEST, ArmorSegment.LEFT_ARM,
            ArmorSegment.RIGHT_ARM, ArmorSegment.LEFT_LEG, ArmorSegment.RIGHT_LEG};
    private static final String[] SEGMENT_BONES = {"armorHead", "armorBody", "armorLeftArm", "armorRightArm",
            "armorLeftLeg", "armorRightLeg"};
    /** x, y, z, xRot, yRot, zRot */
    private static final int POSE = 6;
    private static final int OPAQUE = 255;

    private final Identifier[] models;
    private final Identifier texture;
    private final CatModels.FurModel posed;
    private final Function<String, ModelPart> codeParts;
    private final Function<String, ModelPart> restParts;
    private final Animatable animatable = new Animatable();
    private final Renderer renderer = new Renderer(new Model());
    private final Related related = new Related();
    private final Int2ObjectOpenHashMap<Pass> passes = new Int2ObjectOpenHashMap<>();
    private final Vector3f scratch = new Vector3f();

    // what the current baked model holds; rebuilt when a resource reload brings a new one
    private @Nullable BakedGeoModel indexed;
    private GeoBone[] bones = new GeoBone[0];
    private Coat.Part[] boneParts = new Coat.Part[0];
    private final boolean[] partPresent = new boolean[PARTS.length];
    private final GeoBone[] segmentBones = new GeoBone[SEGMENTS.length];
    private GeoBone[] movingBones = new GeoBone[0];
    private ModelPart[] movingParts = new ModelPart[0];
    private PartPose[] restPoses = new PartPose[0];

    /**
     * @param item   the item's path ({@code paw_gloves})
     * @param slim   whether this renderer draws for slim-armed players (tries {@code <item>_slim} first)
     * @param layer  the code model's layer: its moving parts drive the geo model's bones of the same name
     * @param posed  an instance of the code model, posed here every submission to read the motion from
     */
    public GeoCosmeticRenderer(String item, boolean slim, ModelLayerLocation layer, CatModels.FurModel posed, ModelPart restRoot) {
        Identifier wide = CatgirlMod.id("cosmetic/" + item);
        this.models = slim ? new Identifier[]{CatgirlMod.id("cosmetic/" + item + "_slim"), wide} : new Identifier[]{wide};
        this.texture = CatgirlMod.id("textures/cosmetic/" + item + ".png");
        this.posed = posed;
        this.codeParts = posed.root().createPartLookup();
        CatModels.applyRestPose(layer, restRoot);
        this.restParts = restRoot.createPartLookup();
    }

    /** The baked geo model to draw, or null to fall back to the code model. Checked every frame. */
    private @Nullable BakedGeoModel model() {
        for (Identifier id : models) {
            BakedGeoModel model = GeckoLibResources.getBakedModels().cache().get(id);
            if (model != null) {
                related.model = id;
                return model;
            }
        }
        return null;
    }

    /** @return false when no geo model exists for this item (the caller draws the code model) */
    public boolean submit(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, AvatarRenderState state,
                          PlayerModel parent, int[] colors, int alpha) {
        BakedGeoModel model = model();
        if (model == null) {
            return false;
        }
        if (model != indexed) {
            index(model);
        }
        posed.setupAnim(state);
        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        related.entityId = state.id;
        related.overlay = overlay;
        related.translucent = alpha < OPAQUE;
        for (Coat.Part part : PARTS) {
            if (!partPresent[part.ordinal()]) {
                continue;
            }
            int key = state.id * PARTS.length + part.ordinal();
            Pass pass = passes.get(key);
            if (pass == null) {
                pass = new Pass();
                passes.put(key, pass);
            }
            pass.capture(part, parent);
            related.color = ARGB.color(alpha, colors[part.ordinal()]);
            renderer.performRenderPass(animatable, related, poseStack, collector,
                    minecraft.gameRenderer.gameRenderState().levelRenderState.cameraRenderState, light, partialTick, pass);
        }
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
            boneParts[i] = partOf(bones[i].name());
            partPresent[boneParts[i].ordinal()] = true;
            ModelPart code = codeParts.apply(bones[i].name());
            ModelPart restPart = restParts.apply(bones[i].name());
            if (code != null && restPart != null && !isSegmentBone(bones[i].name())) {
                moving.add(bones[i]);
                movingCode.add(code);
                rest.add(restPart.storePose());
            }
        }
        for (int s = 0; s < SEGMENTS.length; s++) {
            segmentBones[s] = model.getBone(SEGMENT_BONES[s]).orElse(null);
        }
        movingBones = moving.toArray(new GeoBone[0]);
        movingParts = movingCode.toArray(new ModelPart[0]);
        restPoses = rest.toArray(new PartPose[0]);
        passes.clear();
        indexed = model;
    }

    private static void collect(GeoBone bone, List<GeoBone> out) {
        out.add(bone);
        for (GeoBone child : bone.children()) {
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

    /** One player's pose for one coat part's draw, copied at submit time and applied when GeckoLib draws. */
    private final class Pass implements RenderPassInfo.BoneUpdater<GeoRenderState> {
        private Coat.Part part = Coat.Part.MAIN;
        private final float[] body = new float[SEGMENTS.length * POSE];
        private float[] moving = new float[0];
        private GeoBone[] bonesAtCapture = new GeoBone[0];
        private Coat.Part[] partsAtCapture = new Coat.Part[0];
        private GeoBone[] segmentsAtCapture = new GeoBone[SEGMENTS.length];
        private GeoBone[] movingAtCapture = new GeoBone[0];

        void capture(Coat.Part part, PlayerModel parent) {
            this.part = part;
            this.bonesAtCapture = bones;
            this.partsAtCapture = boneParts;
            this.movingAtCapture = movingBones;
            System.arraycopy(segmentBones, 0, segmentsAtCapture, 0, SEGMENTS.length);
            for (int s = 0; s < SEGMENTS.length; s++) {
                ModelPart bodyPart = SEGMENTS[s].modelPartGetter.apply(parent);
                Vector3f pos = SEGMENTS[s].modelPartMatcher.apply(scratch.set(bodyPart.x, bodyPart.y, bodyPart.z));
                int o = s * POSE;
                body[o] = pos.x;
                body[o + 1] = pos.y;
                body[o + 2] = pos.z;
                body[o + 3] = -bodyPart.xRot;
                body[o + 4] = -bodyPart.yRot;
                body[o + 5] = bodyPart.zRot;
            }
            if (moving.length != movingParts.length * POSE) {
                moving = new float[movingParts.length * POSE];
            }
            // Java model space -> GeckoLib: x and z stay, y flips; rotations flip x and y
            for (int i = 0; i < movingParts.length; i++) {
                ModelPart now = movingParts[i];
                PartPose rest = restPoses[i];
                int o = i * POSE;
                moving[o] = now.x - rest.x();
                moving[o + 1] = -(now.y - rest.y());
                moving[o + 2] = now.z - rest.z();
                moving[o + 3] = -(now.xRot - rest.xRot());
                moving[o + 4] = -(now.yRot - rest.yRot());
                moving[o + 5] = now.zRot - rest.zRot();
            }
        }

        @Override
        public void run(RenderPassInfo<GeoRenderState> info, BoneSnapshots snapshots) {
            for (int i = 0; i < bonesAtCapture.length; i++) {
                snapshots.get(bonesAtCapture[i]).skipRender(partsAtCapture[i] != part);
            }
            for (int s = 0; s < SEGMENTS.length; s++) {
                GeoBone bone = segmentsAtCapture[s];
                if (bone != null) {
                    int o = s * POSE;
                    snapshots.get(bone).setTranslateX(body[o]).setTranslateY(body[o + 1]).setTranslateZ(body[o + 2])
                            .setRotX(body[o + 3]).setRotY(body[o + 4]).setRotZ(body[o + 5]);
                }
            }
            for (int i = 0; i < movingAtCapture.length; i++) {
                int o = i * POSE;
                snapshots.get(movingAtCapture[i]).setTranslateX(moving[o]).setTranslateY(moving[o + 1]).setTranslateZ(moving[o + 2])
                        .setRotX(moving[o + 3]).setRotY(moving[o + 4]).setRotZ(moving[o + 5]);
            }
        }
    }

    /** What GeckoLib reads at submit time (color, overlay, render type, model and instance). */
    private static final class Related {
        int entityId;
        int color;
        int overlay;
        boolean translucent;
        Identifier model;
    }

    private final class Animatable implements SingletonGeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            // motion comes from the code model, see Pass
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }
    }

    private final class Model extends GeoModel<Animatable> {
        @Override
        public Identifier getModelResource(GeoRenderState state) {
            return related.model;
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return texture;
        }

        @Override
        public Identifier getAnimationResource(Animatable animatable) {
            return related.model;
        }
    }

    private final class Renderer extends GeoObjectRenderer<Animatable, Related, GeoRenderState> {
        /** Same transform GeckoLib applies to armor, from player model space. */
        private static final float ARMOR_Y_OFFSET = 24.0F / 16.0F;

        Renderer(GeoModel<Animatable> model) {
            super(model);
        }

        @Override
        public void adjustRenderPose(RenderPassInfo<GeoRenderState> info) {
            info.poseStack().translate(0.0F, ARMOR_Y_OFFSET, 0.0F);
            info.poseStack().scale(-1.0F, -1.0F, 1.0F);
        }

        @Override
        public long getInstanceId(Animatable animatable, @Nullable Related related) {
            return related == null ? 0 : related.entityId;
        }

        @Override
        public int getRenderColor(Animatable animatable, @Nullable Related related, float partialTick) {
            return related == null ? 0xFFFFFFFF : related.color;
        }

        @Override
        public int getPackedOverlay(Animatable animatable, @Nullable Related related, float u, float partialTick) {
            return related == null ? super.getPackedOverlay(animatable, null, u, partialTick) : related.overlay;
        }

        @Override
        public @Nullable RenderType getRenderType(GeoRenderState state, Identifier texture) {
            return related.translucent ? RenderTypes.entityTranslucent(texture) : RenderTypes.entityCutout(texture);
        }
    }
}
