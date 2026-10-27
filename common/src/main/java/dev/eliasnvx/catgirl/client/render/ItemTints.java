package dev.eliasnvx.catgirl.client.render;

import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.registry.CatgirlComponents;
import dev.eliasnvx.catgirl.registry.CatgirlItems;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DyeableLeatherItem;

import static dev.eliasnvx.catgirl.cosmetic.Coat.Part.INNER;
import static dev.eliasnvx.catgirl.cosmetic.Coat.Part.MAIN;
import static dev.eliasnvx.catgirl.cosmetic.Coat.Part.PATCH;
import static dev.eliasnvx.catgirl.cosmetic.Coat.Part.POINTS;
import static dev.eliasnvx.catgirl.cosmetic.Coat.Part.TUFT;

/**
 * Icon colors: fur pieces color each grayscale icon layer with one part of their coat, accessories take the dye (or
 * their default color). Layer {@code layerN} of an item model is tint index N.
 */
public final class ItemTints {

    private ItemTints() {
    }

    public static void register() {
        coat(CatgirlItems.CAT_EARS, MAIN, INNER, POINTS);
        coat(CatgirlItems.CAT_TAIL, MAIN, PATCH, POINTS);
        coat(CatgirlItems.LEG_WARMERS, MAIN, PATCH, INNER, TUFT);
        coat(CatgirlItems.PAW_GLOVES, MAIN, INNER, TUFT);
        coat(CatgirlItems.PAW_MITTENS, MAIN, INNER, TUFT);
        coat(CatgirlItems.PAW_SLIPPERS, MAIN, INNER, TUFT);
        dyed(CatgirlItems.BELL_COLLAR, CatgirlItems.BELL_COLLAR_COLOR);
        dyed(CatgirlItems.VELVET_CHOKER, CatgirlItems.VELVET_CHOKER_COLOR);
        dyed(CatgirlItems.NECK_RIBBON, CatgirlItems.NECK_RIBBON_COLOR);
        dyed(CatgirlItems.WHISKERS, CatgirlItems.WHISKERS_COLOR);
        dyed(CatgirlItems.BLUSH, CatgirlItems.BLUSH_COLOR);
        dyed(CatgirlItems.CAT_EAR_HEADPHONES, CatgirlItems.CAT_EAR_HEADPHONES_COLOR);
        dyed(CatgirlItems.EAR_BOWS, CatgirlItems.EAR_BOWS_COLOR);
        dyed(CatgirlItems.BELL_HEADBAND, CatgirlItems.BELL_HEADBAND_COLOR);
    }

    /** @param layers the coat part each icon layer is drawn in */
    private static void coat(RegistrySupplier<? extends Item> item, Coat.Part... layers) {
        ColorHandlerRegistry.registerItemColors((stack, tintIndex) -> tintIndex < 0 || tintIndex >= layers.length ? -1
                : ARGB32.opaque(CatgirlComponents.COAT.getOrDefault(stack, Coat.DEFAULT).color(layers[tintIndex])), item);
    }

    /** Layer 0 takes the dye; other layers stay as drawn. */
    private static void dyed(RegistrySupplier<? extends Item> item, int defaultColor) {
        ColorHandlerRegistry.registerItemColors((stack, tintIndex) -> tintIndex != 0 ? -1
                : ARGB32.opaque(stack.getItem() instanceof DyeableLeatherItem dyeable && dyeable.hasCustomColor(stack)
                        ? dyeable.getColor(stack) : defaultColor), item);
    }
}
