package dev.eliasnvx.catgirl.tools;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.eliasnvx.catgirl.client.render.model.CatModels;
import dev.eliasnvx.catgirl.client.render.model.FurBuilder;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.joml.Vector3fc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Dev tool: exports every code cosmetic model ({@link CatModels#definitions()}) as a STARTING Blockbench/GeckoLib model
 * into {@code art/blockbench/<item>/}: {@code <item>.geo.json} (slim arms: {@code <item>_slim.geo.json}) plus
 * {@code <item>.png}, a copy of the fur texture the code models use. Nothing goes into {@code src/}: the files are
 * polished in Blockbench first (art/blockbench/README.md), then copied into the mod's assets.
 *
 * <p>Run: {@code ./gradlew :common:exportBlockbenchStarters}. Layout follows {@code GeoCosmeticRenderer}: body parts
 * become the armor bones, a code part's coat-part holders ({@code g_inner}…) become child bones
 * {@code <part>_inner}…, and the moving parts keep their code names so they move like the code model. The tail is
 * exported in its rest pose. Conversion: Java model space (y down, neck at y=0) to Bedrock (y up, neck at y=24):
 * positions keep x and z, y becomes {@code 24 - y}; rotations are written unchanged. Blockbench and GeckoLib mirror x
 * when they load a Bedrock file and negate the x and y rotations with it, which gives Java's own rotations in their
 * mirrored, y-up space (the tail then droops in Blockbench as in game).
 */
public final class BlockbenchStarterExport {

    private static final int TEXTURE_SIZE = 64;
    private static final float NECK_Y = 24.0F;
    private static final String SLIM = "_slim";
    private static final Path FUR = Path.of("common/src/main/resources/assets/catgirl/textures/entity/cosmetic/fur.png");
    private static final Map<String, String> ARMOR_BONES = Map.of(
            "head", "armorHead", "body", "armorBody", "left_arm", "armorLeftArm",
            "right_arm", "armorRightArm", "left_leg", "armorLeftLeg", "right_leg", "armorRightLeg");
    private static final Map<String, Integer> ARMOR_ORDER = Map.of(
            "armorHead", 0, "armorBody", 1, "armorLeftArm", 2, "armorRightArm", 3, "armorLeftLeg", 4, "armorRightLeg", 5);

    private BlockbenchStarterExport() {
    }

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 1 ? args[1] : ".");
        Path out = Path.of(args.length > 0 ? args[0] : "art/blockbench");
        for (Map.Entry<ModelLayerLocation, Supplier<LayerDefinition>> entry : CatModels.definitions().entrySet()) {
            String layer = entry.getKey().model().getPath();
            boolean slim = layer.endsWith(SLIM);
            String item = slim ? layer.substring(0, layer.length() - SLIM.length()) : layer;
            ModelPart model = entry.getValue().get().bakeRoot();
            CatModels.applyRestPose(entry.getKey(), model);
            Path dir = out.resolve(item);
            export(model, item, layer, dir);
            if (!slim) {
                Files.copy(root.resolve(FUR), dir.resolve(item + ".png"), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void export(ModelPart root, String item, String file, Path dir) throws IOException {
        Map<String, JsonObject> bones = new LinkedHashMap<>();
        int[] cubeCount = {0};
        collect(root, "", null, 0, 0, 0, null, bones, new LinkedHashSet<>(), cubeCount);

        List<JsonObject> ordered = new ArrayList<>(bones.values());
        ordered.sort((a, b) -> Integer.compare(order(a), order(b)));
        JsonArray boneArray = new JsonArray();
        ordered.stream().filter(bone -> isArmor(bone) || hasCubesBelow(bone, ordered)).forEach(boneArray::add);

        JsonObject description = new JsonObject();
        description.addProperty("identifier", "geometry.catgirl." + file);
        description.addProperty("texture_width", TEXTURE_SIZE);
        description.addProperty("texture_height", TEXTURE_SIZE);
        description.addProperty("visible_bounds_width", 3);
        description.addProperty("visible_bounds_height", 3);
        description.add("visible_bounds_offset", array(0, 1.5F, 0));
        JsonObject geometry = new JsonObject();
        geometry.add("description", description);
        geometry.add("bones", boneArray);
        JsonArray geometries = new JsonArray();
        geometries.add(geometry);
        JsonObject json = new JsonObject();
        json.addProperty("format_version", "1.12.0");
        json.add("minecraft:geometry", geometries);

        Files.createDirectories(dir);
        Files.writeString(dir.resolve(file + ".geo.json"), new GsonBuilder().setPrettyPrinting().create().toJson(json));
        System.out.printf(Locale.ROOT, "%s: %d bones, %d cubes -> %s%n", file, boneArray.size(), cubeCount[0], dir);
    }

    /**
     * Walks the part tree. A coat-part holder ({@code g_inner}…) becomes a child bone {@code <parent>_<part>} holding
     * the cubes, so the renderer knows which coat color they take. {@code absX/Y/Z} is the part's pivot in Java space.
     */
    private static void collect(ModelPart part, String name, String parentBone, float absX, float absY, float absZ,
                                Coat.Part coatPart, Map<String, JsonObject> bones, Set<String> used, int[] cubeCount) {
        String bone = parentBone;
        if (!name.isEmpty()) {
            String base = coatPart != null ? parentBone + "_" + coatPart.name().toLowerCase(Locale.ROOT)
                    : parentBone == null ? ARMOR_BONES.getOrDefault(name, name) : name;
            bone = unique(base, used);
            JsonObject json = new JsonObject();
            json.addProperty("name", bone);
            if (parentBone != null) {
                json.addProperty("parent", parentBone);
            }
            json.add("pivot", array(absX, NECK_Y - absY, absZ));
            if (part.xRot != 0 || part.yRot != 0 || part.zRot != 0) {
                json.add("rotation", array((float) Math.toDegrees(part.xRot), (float) Math.toDegrees(part.yRot),
                        (float) Math.toDegrees(part.zRot)));
            }
            JsonArray cubes = new JsonArray();
            for (ModelPart.Cube cube : BlockbenchStarterExport.<List<ModelPart.Cube>>field(part, "cubes")) {
                cubes.add(cube(cube, absX, absY, absZ));
                cubeCount[0]++;
            }
            if (!cubes.isEmpty()) {
                json.add("cubes", cubes);
            }
            bones.put(bone, json);
        }
        for (String child : new ArrayList<>(BlockbenchStarterExport.<Map<String, ModelPart>>field(part, "children").keySet())) {
            ModelPart c = part.getChild(child);
            collect(c, child, bone, absX + c.x, absY + c.y, absZ + c.z, holderPart(child), bones, used, cubeCount);
        }
    }

    private static Coat.Part holderPart(String name) {
        for (Coat.Part part : Coat.Part.values()) {
            if (FurBuilder.holderName(part).equals(name)) {
                return part;
            }
        }
        return null;
    }

    private static String unique(String base, Set<String> used) {
        String name = base;
        for (int i = 2; !used.add(name); i++) {
            name = base + "_" + i;
        }
        return name;
    }

    /** Dev tool only: ModelPart keeps its cubes and children private. Also used by {@link CharacterExport}. */
    @SuppressWarnings("unchecked")
    static <T> T field(ModelPart part, String name) {
        try {
            var field = ModelPart.class.getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(part);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("ModelPart." + name + " not found; update the exporter", e);
        }
    }

    /** A box with per-face UVs taken from the code model's polygons (the same patch of fur it shows in game). */
    private static JsonObject cube(ModelPart.Cube cube, float px, float py, float pz) {
        JsonObject json = new JsonObject();
        json.add("origin", array(px + cube.minX, NECK_Y - (py + cube.maxY), pz + cube.minZ));
        json.add("size", array(cube.maxX - cube.minX, cube.maxY - cube.minY, cube.maxZ - cube.minZ));
        JsonObject uv = new JsonObject();
        for (ModelPart.Polygon polygon : cube.polygons) {
            String face = face(polygon.normal());
            float minU = Float.MAX_VALUE, minV = Float.MAX_VALUE, maxU = -Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
            for (ModelPart.Vertex v : polygon.vertices()) {
                minU = Math.min(minU, v.u());
                minV = Math.min(minV, v.v());
                maxU = Math.max(maxU, v.u());
                maxV = Math.max(maxV, v.v());
            }
            JsonObject f = new JsonObject();
            f.add("uv", array(minU * TEXTURE_SIZE, minV * TEXTURE_SIZE));
            f.add("uv_size", array((maxU - minU) * TEXTURE_SIZE, (maxV - minV) * TEXTURE_SIZE));
            uv.add(face, f);
        }
        json.add("uv", uv);
        return json;
    }

    /** Java model normals (y down) to Bedrock face names (y up). */
    private static String face(Vector3fc n) {
        if (Math.abs(n.y()) > 0.5F) {
            return n.y() < 0 ? "up" : "down";
        }
        if (Math.abs(n.x()) > 0.5F) {
            return n.x() > 0 ? "east" : "west";
        }
        return n.z() > 0 ? "south" : "north";
    }

    private static boolean isArmor(JsonObject bone) {
        return ARMOR_ORDER.containsKey(bone.get("name").getAsString());
    }

    /** Keeps bones that hold cubes or lead to some (empty helper parts like the hat layer are dropped). */
    private static boolean hasCubesBelow(JsonObject bone, List<JsonObject> all) {
        if (bone.has("cubes")) {
            return true;
        }
        String name = bone.get("name").getAsString();
        return all.stream().anyMatch(other -> other.has("parent") && other.get("parent").getAsString().equals(name)
                && hasCubesBelow(other, all));
    }

    private static int order(JsonObject bone) {
        return ARMOR_ORDER.getOrDefault(bone.get("name").getAsString(), bone.has("parent") ? 100 : 50);
    }

    private static JsonArray array(float... values) {
        JsonArray array = new JsonArray();
        for (float value : values) {
            array.add(Math.round(value * 10000.0F) / 10000.0F);
        }
        return array;
    }
}
