package dev.eliasnvx.catgirl.world;

import dev.architectury.registry.level.biome.BiomeModifications;
import dev.eliasnvx.catgirl.CatgirlMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Wild catnip patches on grass. Patch size and rarity are data
 * ({@code data/catgirl/worldgen/placed_feature/patch_catnip.json}); the biomes are the tag {@code #catgirl:has_catnip}.
 */
public final class CatgirlWorldgen {

    public static final TagKey<Biome> HAS_CATNIP = TagKey.create(Registries.BIOME, CatgirlMod.id("has_catnip"));
    public static final ResourceKey<PlacedFeature> PATCH_CATNIP = ResourceKey.create(Registries.PLACED_FEATURE, CatgirlMod.id("patch_catnip"));

    private CatgirlWorldgen() {
    }

    public static void init() {
        BiomeModifications.addProperties(ctx -> ctx.hasTag(HAS_CATNIP), (ctx, props) ->
                props.getGenerationProperties().addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, PATCH_CATNIP));
    }
}
