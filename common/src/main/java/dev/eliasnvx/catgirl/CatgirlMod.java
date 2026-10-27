package dev.eliasnvx.catgirl;

import dev.eliasnvx.catgirl.block.CatgirlBlocks;
import dev.eliasnvx.catgirl.cosmetic.CollarBell;
import dev.eliasnvx.catgirl.effect.CatgirlEffects;
import dev.eliasnvx.catgirl.effect.NineLives;
import dev.eliasnvx.catgirl.entity.CatgirlEntities;
import dev.eliasnvx.catgirl.world.trade.TradeSets;
import dev.eliasnvx.catgirl.item.CatgirlFoods;
import dev.eliasnvx.catgirl.world.CatgirlWorldgen;
import dev.eliasnvx.catgirl.cosmetic.CosmeticsEvents;
import dev.eliasnvx.catgirl.meow.Meow;
import dev.eliasnvx.catgirl.network.CatgirlNetwork;
import dev.eliasnvx.catgirl.registry.CatgirlComponents;
import dev.eliasnvx.catgirl.registry.CatgirlItems;
import dev.eliasnvx.catgirl.registry.CatgirlSounds;
import dev.eliasnvx.catgirl.set.SetBonusHooks;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entry point, called by each loader's main initializer. */
public final class CatgirlMod {

    public static final String MOD_ID = "catgirl";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean initialized;

    private CatgirlMod() {
    }

    public static synchronized void init() {
        if (initialized) {
            throw new IllegalStateException("CatgirlMod.init() called twice");
        }
        initialized = true;

        // Components, effects and blocks first: items read them in their properties
        CatgirlComponents.REGISTER.register();
        CatgirlEffects.REGISTER.register();
        CatgirlBlocks.REGISTER.register();
        CatgirlFoods.register(); // class-load the food items into CatgirlItems' register first
        CatgirlItems.REGISTER.register();
        CatgirlItems.TABS.register();
        CatgirlSounds.REGISTER.register();
        CatgirlEntities.init();
        TradeSets.init();
        CatgirlWorldgen.init();

        CatgirlNetwork.register();
        NineLives.register(); // before every other death listener: it may cancel the death
        CosmeticsEvents.register();
        CollarBell.register();
        Meow.register();
        SetBonusHooks.register();

        LOGGER.info("catgirl initialized");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
