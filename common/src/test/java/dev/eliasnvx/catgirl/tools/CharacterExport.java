package dev.eliasnvx.catgirl.tools;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.eliasnvx.catgirl.client.render.CosmeticLayer;
import dev.eliasnvx.catgirl.client.render.CosmeticRenderData;
import dev.eliasnvx.catgirl.client.render.PartColors;
import dev.eliasnvx.catgirl.client.render.model.CatModels;
import dev.eliasnvx.catgirl.client.render.model.FurBuilder;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import org.joml.Vector3fc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Dev tool: whole characters in a full outfit as Blockbench projects ({@code art/characters/<look>.bbmodel}), for
 * banners and promo art drawn by hand. Each project is a player model (head, body, arms, legs with the skin's outer
 * layer) with every worn piece inside the body part it moves with, so posing a part in Blockbench takes the pieces
 * along. Two textures: the skin (swap in any 64x64 skin) and the outfit, the fur texture already colored per coat
 * part the way the game tints it.
 *
 * <p>Run: {@code ./gradlew :common:exportCharacters}. Coordinates follow Blockbench's own Bedrock import (x mirrored,
 * y up from the feet), the same conversion as {@link BlockbenchStarterExport}; every cube uses box UV.
 */
public final class CharacterExport {

    private static final float NECK_Y = 24.0F;
    private static final int SKIN_SIZE = 64;
    /** The fur texture twice in each direction: box UV can reach past its 64 px, and must stay in one color. */
    private static final int TILE = 128;
    private static final int TILES_PER_ROW = 4;
    private static final int SKIN_TEXTURE = 0;
    private static final int OUTFIT_TEXTURE = 1;
    private static final String SLIM = "_slim";
    private static final Path FUR = Path.of("common/src/main/resources/assets/catgirl/textures/entity/cosmetic/fur.png");
    private static final Path SKINS = Path.of("common/src/main/resources/assets/catgirl/textures/entity/neko");
    /** The player model's parts that the game doesn't draw on a player (cape and the deadmau5 ears, if present). */
    private static final Set<String> SKIPPED_PARTS = Set.of("cloak", "ear", "left_ear", "right_ear");
    private static final String[] FACES = {"north", "east", "south", "west", "up", "down"};
    private static final int UNDYED = -1;

    /** A worn piece; {@code dye} only for dyeable pieces ({@link #UNDYED}: the item's own color). */
    private record Piece(String item, int dye) {
        Piece(String item) {
            this(item, UNDYED);
        }
    }

    /** A character: one piece per slot, a coat for the fur pieces and a placeholder skin from the mod's nekos. */
    private record Look(String name, boolean slim, String skin, Coat coat, List<Piece> pieces) {
    }

    private static final List<Look> LOOKS = List.of(
            new Look("catgirl_ginger", true, "fisher_neko_1", Coat.GINGER, List.of(
                    new Piece("cat_ears"), new Piece("ear_bows"), new Piece("blush"), new Piece("bell_collar"),
                    new Piece("paw_gloves"), new Piece("leg_warmers"), new Piece("paw_slippers"), new Piece("cat_tail"))),
            new Look("catboy_gray", false, "stray_neko_1", Coat.GRAY, List.of(
                    new Piece("cat_ears"), new Piece("cat_ear_headphones", 0x5B8DD9), new Piece("whiskers"),
                    new Piece("velvet_choker"), new Piece("paw_mittens"), new Piece("leg_warmers"),
                    new Piece("paw_slippers"), new Piece("cat_tail"))),
            new Look("catgirl_calico", true, "stray_neko_3", Coat.CALICO, List.of(
                    new Piece("cat_ears"), new Piece("bell_headband"), new Piece("whiskers"), new Piece("neck_ribbon"),
                    new Piece("paw_mittens"), new Piece("leg_warmers"), new Piece("paw_slippers"), new Piece("cat_tail"))),
            new Look("catboy_siamese", false, "fisher_neko_0", Coat.SIAMESE, List.of(
                    new Piece("cat_ears"), new Piece("cat_ear_headphones"), new Piece("blush"), new Piece("bell_collar"),
                    new Piece("paw_gloves"), new Piece("leg_warmers"), new Piece("paw_slippers"), new Piece("cat_tail"))));

    /** Undyed colors of the dyeable pieces, as in {@code CatgirlItems}; everything else is a fur piece. */
    private static final Map<String, Integer> DYEABLE = Map.of(
            "bell_collar", 0xC8323C, "velvet_choker", 0x2A2A33, "neck_ribbon", 0xF291BE, "whiskers", 0x2A2328,
            "blush", 0xF06292, "cat_ear_headphones", 0xF7A8CC, "ear_bows", 0xE0405A, "bell_headband", 0x2A2A33);
    private static final Map<String, Float> OPACITY = Map.of("blush", CosmeticLayer.BLUSH_OPACITY);

    private final Look look;
    private final BufferedImage fur;
    private final JsonArray elements = new JsonArray();
    /** Body part name -> its group, so the pieces go into the part they move with. */
    private final Map<String, JsonObject> bodyGroups = new LinkedHashMap<>();
    /** Color with alpha -> tile index in the outfit texture. */
    private final Map<Long, Integer> tiles = new LinkedHashMap<>();
    private int uuidCounter;

    private CharacterExport(Look look, BufferedImage fur) {
        this.look = look;
        this.fur = fur;
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "art/characters");
        Path root = Path.of(args.length > 1 ? args[1] : ".");
        BufferedImage fur = ImageIO.read(root.resolve(FUR).toFile());
        Map<String, Supplier<LayerDefinition>> layers = new LinkedHashMap<>();
        CatModels.definitions().forEach((location, definition) -> layers.put(location.model().getPath(), definition));
        Map<String, ModelLayerLocation> locations = new LinkedHashMap<>();
        CatModels.definitions().keySet().forEach(location -> locations.put(location.model().getPath(), location));
        Files.createDirectories(out);
        for (Look look : LOOKS) {
            CharacterExport export = new CharacterExport(look, fur);
            JsonObject project = export.build(root, layers, locations);
            Path file = out.resolve(look.name() + ".bbmodel");
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(project));
            System.out.printf(Locale.ROOT, "%s: %d cubes, %d outfit colors -> %s%n",
                    look.name(), export.elements.size(), export.tiles.size(), file);
        }
    }

    private JsonObject build(Path root, Map<String, Supplier<LayerDefinition>> layers, Map<String, ModelLayerLocation> locations)
            throws IOException {
        JsonArray outliner = new JsonArray();
        ModelPart body = LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, look.slim()), SKIN_SIZE, SKIN_SIZE).bakeRoot();
        for (String name : childNames(body)) {
            if (SKIPPED_PARTS.contains(name)) {
                continue;
            }
            ModelPart part = body.getChild(name);
            JsonObject group = skinGroup(part, name, part.x, part.y, part.z);
            bodyGroups.put(name, group);
            outliner.add(group);
        }
        for (Piece piece : look.pieces()) {
            String layer = look.slim() && layers.containsKey(piece.item() + SLIM) ? piece.item() + SLIM : piece.item();
            ModelPart model = layers.get(layer).get().bakeRoot();
            CatModels.applyRestPose(locations.get(layer), model);
            addPiece(piece, model);
        }
        BufferedImage skin = ImageIO.read(root.resolve(SKINS).resolve(look.skin() + ".png").toFile());

        JsonObject meta = new JsonObject();
        meta.addProperty("format_version", "5.0");
        meta.addProperty("model_format", "free");
        meta.addProperty("box_uv", true);
        JsonObject resolution = new JsonObject();
        resolution.addProperty("width", SKIN_SIZE);
        resolution.addProperty("height", SKIN_SIZE);
        JsonArray textures = new JsonArray();
        textures.add(texture("skin", skin, SKIN_TEXTURE));
        textures.add(texture("outfit", outfitTexture(), OUTFIT_TEXTURE));

        JsonObject project = new JsonObject();
        project.add("meta", meta);
        project.addProperty("name", look.name());
        project.add("resolution", resolution);
        project.add("elements", elements);
        project.add("outliner", outliner);
        project.add("textures", textures);
        return project;
    }

    /** A player model part and its children (the outer layer: hat, jacket, sleeves, pants) with skin cubes. */
    private JsonObject skinGroup(ModelPart part, String name, float x, float y, float z) {
        JsonObject group = group(name, x, y, z, part);
        JsonArray children = group.getAsJsonArray("children");
        for (ModelPart.Cube cube : BlockbenchStarterExport.<List<ModelPart.Cube>>field(part, "cubes")) {
            children.add(cube(name, cube, x, y, z, SKIN_TEXTURE, 0, 0));
        }
        for (String child : childNames(part)) {
            ModelPart c = part.getChild(child);
            children.add(skinGroup(c, child, x + c.x, y + c.y, z + c.z));
        }
        return group;
    }

    /** Puts a piece's parts into the body groups of the same name, one group named after the item in each. */
    private void addPiece(Piece piece, ModelPart model) {
        int[] colors = colors(piece);
        float opacity = OPACITY.getOrDefault(piece.item(), 1.0F);
        for (String name : childNames(model)) {
            ModelPart part = model.getChild(name);
            JsonObject target = bodyGroups.get(name);
            if (target == null || !hasCubes(part)) {
                continue;
            }
            JsonObject itemGroup = group(piece.item(), part.x, part.y, part.z, null);
            fill(itemGroup, part, part.x, part.y, part.z, Coat.Part.MAIN, colors, opacity);
            target.getAsJsonArray("children").add(itemGroup);
        }
    }

    /** Adds a cosmetic part's cubes and child parts; coat-part holders ({@code g_inner}…) pick the color of what's inside. */
    private void fill(JsonObject group, ModelPart part, float x, float y, float z, Coat.Part coatPart, int[] colors, float opacity) {
        JsonArray children = group.getAsJsonArray("children");
        int tile = tile(colors[coatPart.ordinal()], opacity);
        int u = (tile % TILES_PER_ROW) * TILE;
        int v = (tile / TILES_PER_ROW) * TILE;
        String cubeName = group.get("name").getAsString();
        for (ModelPart.Cube cube : BlockbenchStarterExport.<List<ModelPart.Cube>>field(part, "cubes")) {
            children.add(cube(cubeName, cube, x, y, z, OUTFIT_TEXTURE, u, v));
        }
        for (String name : childNames(part)) {
            ModelPart c = part.getChild(name);
            if (!hasCubes(c)) {
                continue;
            }
            Coat.Part holder = holderPart(name);
            float cx = x + c.x;
            float cy = y + c.y;
            float cz = z + c.z;
            if (holder != null && c.xRot == 0 && c.yRot == 0 && c.zRot == 0 && c.x == 0 && c.y == 0 && c.z == 0) {
                // a holder only sorts cubes by color: its cubes go straight into this group
                fill(group, c, cx, cy, cz, holder, colors, opacity);
            } else {
                JsonObject child = group(holder != null ? cubeName + "_" + holder.name().toLowerCase(Locale.ROOT) : name, cx, cy, cz, c);
                fill(child, c, cx, cy, cz, holder != null ? holder : coatPart, colors, opacity);
                children.add(child);
            }
        }
    }

    /** The colors the game tints this piece's parts with: its coat, or its dye with fixed metal and dark details. */
    private int[] colors(Piece piece) {
        CosmeticRenderData.Worn worn = new CosmeticRenderData.Worn(null, null, look.coat(), piece.dye());
        Integer undyed = DYEABLE.get(piece.item());
        return (undyed == null ? PartColors.COAT : PartColors.dyed(undyed)).of(worn).clone();
    }

    private int tile(int rgb, float opacity) {
        int alpha = Math.round(opacity * 255);
        long key = ((long) alpha << 24) | (rgb & 0xFFFFFF);
        return tiles.computeIfAbsent(key, k -> tiles.size());
    }

    /** One tile per color: the grayscale fur multiplied by it, like the game's tint, repeated 2x2. */
    private BufferedImage outfitTexture() {
        int rows = Math.max(1, (tiles.size() + TILES_PER_ROW - 1) / TILES_PER_ROW);
        BufferedImage image = new BufferedImage(TILES_PER_ROW * TILE, rows * TILE, BufferedImage.TYPE_INT_ARGB);
        tiles.forEach((key, index) -> {
            int rgb = (int) (key & 0xFFFFFF);
            int alpha = (int) (key >>> 24);
            int x0 = (index % TILES_PER_ROW) * TILE;
            int y0 = (index / TILES_PER_ROW) * TILE;
            for (int y = 0; y < TILE; y++) {
                for (int x = 0; x < TILE; x++) {
                    int texel = fur.getRGB(x % fur.getWidth(), y % fur.getHeight());
                    int a = ((texel >>> 24) * alpha) / 255;
                    int r = (((texel >> 16) & 0xFF) * ((rgb >> 16) & 0xFF)) / 255;
                    int g = (((texel >> 8) & 0xFF) * ((rgb >> 8) & 0xFF)) / 255;
                    int b = ((texel & 0xFF) * (rgb & 0xFF)) / 255;
                    image.setRGB(x0 + x, y0 + y, a << 24 | r << 16 | g << 8 | b);
                }
            }
        });
        return image;
    }

    /**
     * A cube. {@code x/y/z} is the owning part's pivot in Java model space (y down, neck at 0); Blockbench gets x
     * mirrored and y up from the feet. Skin cubes use box UV from where the box starts on the skin (its texOffs), as a
     * skin expects. Outfit cubes use per-face UV, each face where the game samples it, moved by {@code u0/v0} into the
     * right tile: many are thinner than a pixel, which box UV can't map. Adds the cube to the elements and returns its
     * uuid for the outliner.
     */
    private String cube(String name, ModelPart.Cube cube, float x, float y, float z, int texture, int u0, int v0) {
        float sizeX = cube.maxX - cube.minX;
        float sizeY = cube.maxY - cube.minY;
        float sizeZ = cube.maxZ - cube.minZ;
        float minU = Float.MAX_VALUE;
        float minV = Float.MAX_VALUE;
        float minVertexX = Float.MAX_VALUE;
        float maxVertexX = -Float.MAX_VALUE;
        for (ModelPart.Polygon polygon : cube.polygons) {
            for (ModelPart.Vertex vertex : polygon.vertices()) {
                minU = Math.min(minU, vertex.u());
                minV = Math.min(minV, vertex.v());
                minVertexX = Math.min(minVertexX, vertex.x());
                maxVertexX = Math.max(maxVertexX, vertex.x());
            }
        }
        float inflate = ((maxVertexX - minVertexX) - sizeX) / 2.0F;
        float originX = x + cube.minX;
        float originY = NECK_Y - (y + cube.maxY);
        float originZ = z + cube.minZ;
        int textureSize = texture == SKIN_TEXTURE ? SKIN_SIZE : fur.getWidth();

        JsonObject json = new JsonObject();
        boolean boxUv = texture == SKIN_TEXTURE;
        json.addProperty("name", name);
        json.addProperty("box_uv", boxUv);
        json.addProperty("rescale", false);
        json.addProperty("locked", false);
        json.add("from", array(-(originX + sizeX), originY, originZ));
        json.add("to", array(-originX, originY + sizeY, originZ + sizeZ));
        json.addProperty("autouv", 0);
        json.addProperty("color", 0);
        if (Math.abs(inflate) > 0.001F) {
            json.addProperty("inflate", round(inflate));
        }
        json.add("origin", array(-x, NECK_Y - y, z));
        JsonObject faces = new JsonObject();
        if (boxUv) {
            json.add("uv_offset", array(u0 + Math.round(minU * textureSize), v0 + Math.round(minV * textureSize)));
            for (String face : FACES) {
                faces.add(face, face(texture, 0, 0, 0, 0));
            }
        } else {
            for (String face : FACES) {
                JsonObject hidden = new JsonObject();
                hidden.add("uv", array(0, 0, 0, 0));
                hidden.add("texture", null);
                faces.add(face, hidden);
            }
            for (ModelPart.Polygon polygon : cube.polygons) {
                float faceMinU = Float.MAX_VALUE;
                float faceMinV = Float.MAX_VALUE;
                float faceMaxU = -Float.MAX_VALUE;
                float faceMaxV = -Float.MAX_VALUE;
                for (ModelPart.Vertex vertex : polygon.vertices()) {
                    faceMinU = Math.min(faceMinU, vertex.u());
                    faceMinV = Math.min(faceMinV, vertex.v());
                    faceMaxU = Math.max(faceMaxU, vertex.u());
                    faceMaxV = Math.max(faceMaxV, vertex.v());
                }
                faces.add(blockbenchFace(polygon.normal()), face(texture, u0 + faceMinU * textureSize,
                        v0 + faceMinV * textureSize, u0 + faceMaxU * textureSize, v0 + faceMaxV * textureSize));
            }
        }
        json.add("faces", faces);
        json.addProperty("type", "cube");
        json.addProperty("uuid", uuid());
        elements.add(json);
        return json.get("uuid").getAsString();
    }

    private static JsonObject face(int texture, float u1, float v1, float u2, float v2) {
        JsonObject face = new JsonObject();
        face.add("uv", array(u1, v1, u2, v2));
        face.addProperty("texture", texture);
        return face;
    }

    /** A Java face normal (y down) as a Blockbench face name (x mirrored, y up). */
    private static String blockbenchFace(Vector3fc normal) {
        if (Math.abs(normal.y()) > 0.5F) {
            return normal.y() < 0 ? "up" : "down";
        }
        if (Math.abs(normal.x()) > 0.5F) {
            return normal.x() > 0 ? "west" : "east";
        }
        return normal.z() > 0 ? "south" : "north";
    }

    /**
     * A Blockbench group (bone) pivoting at the Java point {@code x/y/z}, with the part's rotation if it has one.
     * Blockbench's space is Java's turned half around z (x and y negated), so x and y rotations flip sign.
     */
    private JsonObject group(String name, float x, float y, float z, ModelPart part) {
        JsonObject group = new JsonObject();
        group.addProperty("name", name);
        group.add("origin", array(-x, NECK_Y - y, z));
        if (part != null && (part.xRot != 0 || part.yRot != 0 || part.zRot != 0)) {
            group.add("rotation", array((float) -Math.toDegrees(part.xRot), (float) -Math.toDegrees(part.yRot),
                    (float) Math.toDegrees(part.zRot)));
        }
        group.addProperty("color", 0);
        group.addProperty("uuid", uuid());
        group.addProperty("export", true);
        group.addProperty("isOpen", false);
        group.addProperty("locked", false);
        group.addProperty("visibility", true);
        group.addProperty("autouv", 0);
        group.add("children", new JsonArray());
        return group;
    }

    private static JsonObject texture(String name, BufferedImage image, int id) throws IOException {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        JsonObject json = new JsonObject();
        json.addProperty("name", name + ".png");
        json.addProperty("id", String.valueOf(id));
        json.addProperty("width", image.getWidth());
        json.addProperty("height", image.getHeight());
        json.addProperty("uv_width", image.getWidth());
        json.addProperty("uv_height", image.getHeight());
        json.addProperty("particle", false);
        json.addProperty("render_mode", "default");
        json.addProperty("render_sides", "auto");
        json.addProperty("visible", true);
        json.addProperty("internal", true);
        json.addProperty("saved", false);
        json.addProperty("uuid", UUID.nameUUIDFromBytes(("texture/" + name).getBytes(StandardCharsets.UTF_8)).toString());
        json.addProperty("source", "data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray()));
        return json;
    }

    /** Stable ids per look, so re-exporting gives the same file. */
    private String uuid() {
        return UUID.nameUUIDFromBytes((look.name() + "/" + uuidCounter++).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static List<String> childNames(ModelPart part) {
        return new ArrayList<>(BlockbenchStarterExport.<Map<String, ModelPart>>field(part, "children").keySet());
    }

    private static boolean hasCubes(ModelPart part) {
        if (!BlockbenchStarterExport.<List<ModelPart.Cube>>field(part, "cubes").isEmpty()) {
            return true;
        }
        for (String child : childNames(part)) {
            if (hasCubes(part.getChild(child))) {
                return true;
            }
        }
        return false;
    }

    private static Coat.Part holderPart(String name) {
        for (Coat.Part part : Coat.Part.values()) {
            if (FurBuilder.holderName(part).equals(name)) {
                return part;
            }
        }
        return null;
    }

    private static float round(float value) {
        return Math.round(value * 10000.0F) / 10000.0F;
    }

    private static JsonArray array(float... values) {
        JsonArray array = new JsonArray();
        for (float value : values) {
            array.add(round(value));
        }
        return array;
    }
}
