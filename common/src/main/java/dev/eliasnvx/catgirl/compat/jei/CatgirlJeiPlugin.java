package dev.eliasnvx.catgirl.compat.jei;

import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.client.CosmeticsPanel;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.item.CatgirlFoods;
import dev.eliasnvx.catgirl.registry.CatgirlComponents;
import dev.eliasnvx.catgirl.registry.CatgirlItems;
import dev.eliasnvx.catgirl.set.CatSet;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI (soft dependency): the recipes are vanilla types JEI already shows; this adds one entry per coat for the fur
 * pieces, info pages (how to wear and colour things, what each set gives, what the mechanics, foods and mobs do), and
 * keeps JEI's item list clear of the cosmetics panel.
 * Loaded by JEI only (NeoForge: {@link JeiPlugin}, Fabric: the {@code jei_mod_plugin} entrypoint).
 */
@JeiPlugin
public final class CatgirlJeiPlugin implements IModPlugin {

    private static final Identifier UID = CatgirlMod.id("jei_plugin");
    private static final String INFO = "jei.catgirl.info.";

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        for (RegistrySupplier<Item> item : CatgirlItems.coated()) {
            registration.registerFromDataComponentTypes(item.get(), CatgirlComponents.COAT.get());
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<ItemStack> coated = new ArrayList<>();
        for (RegistrySupplier<Item> item : CatgirlItems.coated()) {
            for (Coat coat : Coat.values()) {
                coated.add(CatgirlItems.withCoat(item.get(), coat));
            }
        }
        List<ItemStack> dyeable = CatgirlItems.dyeable().stream().map(item -> new ItemStack(item.get())).toList();
        List<ItemStack> cosmetics = new ArrayList<>(coated);
        cosmetics.addAll(dyeable);

        registration.addItemStackInfo(cosmetics, Component.translatable(INFO + "cosmetic"));
        registration.addItemStackInfo(coated, Component.translatable(INFO + "coats"));
        registration.addItemStackInfo(dyeable, Component.translatable(INFO + "dyeable"));
        for (CatSet set : CatSet.values()) {
            List<ItemStack> pieces = cosmetics.stream().filter(set::includes).toList();
            if (!pieces.isEmpty()) {
                registration.addItemStackInfo(pieces, setInfo(set));
            }
        }

        info(registration, CatgirlItems.CAT_BED.get(), "cat_bed");
        info(registration, CatgirlItems.CARDBOARD_BOX.get(), "cardboard_box");
        info(registration, CatgirlItems.CATNIP.get(), "catnip");
        info(registration, CatgirlItems.YARN_BALL.get(), "yarn_ball");
        for (RegistrySupplier<Item> food : CatgirlFoods.ALL) {
            info(registration, food.get(), food.getId().getPath());
        }
        for (RegistrySupplier<Item> egg : CatgirlItems.spawnEggs()) {
            info(registration, egg.get(), egg.getId().getPath());
        }
    }

    /** The cosmetics panel sits right of the inventory, where JEI would put its item list: tell JEI to keep clear. */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(InventoryScreen.class, new PanelArea<>());
        registration.addGuiContainerHandler(CreativeModeInventoryScreen.class, new PanelArea<>());
    }

    private static final class PanelArea<T extends AbstractContainerScreen<?>> implements IGuiContainerHandler<T> {
        @Override
        public List<Rect2i> getGuiExtraAreas(T screen) {
            Rect2i area = CosmeticsPanel.area(screen);
            return area == null ? List.of() : List.of(area);
        }
    }

    /** "<set>: <pieces> together. <bonus>" */
    private static Component setInfo(CatSet set) {
        MutableComponent pieces = Component.empty();
        for (int i = 0; i < set.pieces().size(); i++) {
            if (i > 0) {
                pieces.append(", ");
            }
            pieces.append(Component.translatable(set.pieces().get(i).slot().translationKey()));
        }
        return Component.translatable(INFO + "set", Component.translatable(set.translationKey()), pieces,
                Component.translatable(set.translationKey() + ".bonus"));
    }

    private static void info(IRecipeRegistration registration, Item item, String key) {
        registration.addIngredientInfo(item, Component.translatable(INFO + key));
    }
}
