package dev.eliasnvx.catgirl.gametest;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.entity.CatgirlEntities;
import dev.eliasnvx.catgirl.entity.neko.StrayNeko;
import dev.eliasnvx.catgirl.registry.CatgirlComponents;
import dev.eliasnvx.catgirl.registry.CatgirlItems;
import dev.eliasnvx.catgirl.registry.CatgirlTags;
import dev.eliasnvx.catgirl.world.CatgirlWorldgen;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;
import java.util.function.Predicate;

/**
 * The data files load: every recipe, advancement and loot table of the mod, the Stray Neko's trades and the worldgen.
 * A format mistake in one of them only shows as a log line while the game keeps running, so the build checks it here.
 * Shared by both loaders like {@link CosmeticSlotGameTests}; dev only, not in the released jars.
 */
public final class DataGameTests {

    public static final List<CosmeticSlotGameTests.Entry> ALL = List.of(
            new CosmeticSlotGameTests.Entry("data_loads", DataGameTests::dataLoads));

    private DataGameTests() {
    }

    public static void dataLoads(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        every(helper, server, "recipe", id -> server.getRecipeManager().byKey(id).isPresent());
        every(helper, server, "advancement", id -> server.getAdvancements().get(id) != null);
        every(helper, server, "loot_table",
                id -> server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id)) != LootTable.EMPTY);

        ItemStack ginger = server.getRecipeManager().byKey(CatgirlMod.id("cat_ears_ginger")).orElseThrow().value()
                .getResultItem(server.registryAccess());
        helper.assertTrue(ginger.get(CatgirlComponents.COAT.get()) == Coat.GINGER, "ginger ears recipe gives " + ginger);
        helper.assertTrue(new ItemStack(CatgirlItems.CAT_EARS.get()).is(CatgirlTags.EARS), "cat ears in #catgirl:ears");

        StrayNeko neko = CatgirlEntities.STRAY_NEKO.get().create(helper.getLevel());
        helper.assertTrue(neko != null && !neko.getOffers().isEmpty(), "the Stray Neko has trades");
        if (neko != null) {
            neko.discard();
        }
        helper.assertTrue(server.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                .containsKey(CatgirlWorldgen.PATCH_CATNIP), "catnip worldgen");
        helper.succeed();
    }

    /** Each of the mod's JSON files in {@code data/catgirl/<directory>} has to be loaded. */
    private static void every(GameTestHelper helper, MinecraftServer server, String directory, Predicate<ResourceLocation> loaded) {
        FileToIdConverter files = FileToIdConverter.json(directory);
        int count = 0;
        for (ResourceLocation file : files.listMatchingResources(server.getResourceManager()).keySet()) {
            ResourceLocation id = files.fileToId(file);
            if (id.getNamespace().equals(CatgirlMod.MOD_ID)) {
                helper.assertTrue(loaded.test(id), directory + " " + id + " didn't load (see the log)");
                count++;
            }
        }
        helper.assertTrue(count > 0, "no " + directory + " files of the mod found");
    }
}
