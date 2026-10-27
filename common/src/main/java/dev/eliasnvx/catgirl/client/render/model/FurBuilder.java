package dev.eliasnvx.catgirl.client.render.model;

import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.mixin.client.ModelPartAccessor;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Collects a pivot part's boxes per {@link Coat.Part} under holder children named {@code g_<part>}. A model
 * instance shows one part's holders and is drawn with that part's coat color, so one grayscale texture
 * gives all 8 coats (same idea as Femboy Mod's color groups).
 */
public final class FurBuilder {

    private static final int TEX_RANGE = 48;

    private final PartDefinition pivot;
    private final Map<Coat.Part, CubeListBuilder> cubes = new EnumMap<>(Coat.Part.class);
    private int texCursor;

    public FurBuilder(PartDefinition pivot) {
        this.pivot = pivot;
    }

    public static String holderName(Coat.Part part) {
        return "g_" + part.name().toLowerCase(Locale.ROOT);
    }

    /** Shows only {@code visible}'s holders in every part of the model tree. */
    public static void show(ModelPart root, Coat.Part visible) {
        for (ModelPart part : (Iterable<ModelPart>) root.getAllParts()::iterator) {
            for (Coat.Part fur : Coat.Part.values()) {
                if (part.hasChild(holderName(fur))) {
                    part.getChild(holderName(fur)).visible = fur == visible;
                }
            }
        }
    }

    /** Adds a box; texture offsets vary per box so small boxes pick up different bits of the fur texture. */
    public FurBuilder box(Coat.Part part, float x, float y, float z, float w, float h, float d) {
        int u = (texCursor * 7) % TEX_RANGE;
        int v = (texCursor * 13) % TEX_RANGE;
        texCursor++;
        cubes.computeIfAbsent(part, p -> CubeListBuilder.create()).texOffs(u, v).addBox(x, y, z, w, h, d);
        return this;
    }

    /**
     * Extrudes a pixel mask into boxes (rows top to bottom, runs of the same char merged).
     * {@code legend} maps mask chars to parts; other chars are empty.
     */
    public FurBuilder extrude(String[] rows, Map<Character, Coat.Part> legend, float x0, float y0, float z0, float pixel, float depth) {
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            int col = 0;
            while (col < line.length()) {
                char c = line.charAt(col);
                int end = col + 1;
                while (end < line.length() && line.charAt(end) == c) {
                    end++;
                }
                Coat.Part part = legend.get(c);
                if (part != null) {
                    box(part, x0 + col * pixel, y0 + row * pixel, z0, (end - col) * pixel, pixel, depth);
                }
                col = end;
            }
        }
        return this;
    }

    /** Every part of the tree by name (first match wins), like the later versions' {@code ModelPart.createPartLookup}. */
    public static Function<String, ModelPart> partLookup(ModelPart root) {
        Map<String, ModelPart> parts = new HashMap<>();
        collect(root, parts);
        return parts::get;
    }

    private static void collect(ModelPart part, Map<String, ModelPart> out) {
        ((ModelPartAccessor) (Object) part).catgirl$children().forEach((name, child) -> {
            out.putIfAbsent(name, child);
            collect(child, out);
        });
    }

    /** Attaches the holders; call once after adding all boxes. */
    public PartDefinition build() {
        cubes.forEach((part, list) -> pivot.addOrReplaceChild(holderName(part), list, PartPose.ZERO));
        return pivot;
    }
}
