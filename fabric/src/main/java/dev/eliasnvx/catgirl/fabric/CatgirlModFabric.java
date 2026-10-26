package dev.eliasnvx.catgirl.fabric;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.platform.fabric.PlatformHelperImpl;
import net.fabricmc.api.ModInitializer;

public final class CatgirlModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        PlatformHelperImpl.init();
        CatgirlMod.init();
    }
}
