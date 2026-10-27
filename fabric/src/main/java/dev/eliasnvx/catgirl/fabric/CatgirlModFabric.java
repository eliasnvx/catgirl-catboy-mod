package dev.eliasnvx.catgirl.fabric;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.cosmetic.CosmeticsEvents;
import dev.eliasnvx.catgirl.platform.fabric.PlatformHelperImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;

public final class CatgirlModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        PlatformHelperImpl.init();
        CatgirlMod.init();
        // Architectury 13 has no start-tracking event: sync the outfits of players coming into view
        EntityTrackingEvents.START_TRACKING.register(CosmeticsEvents::onStartTracking);
    }
}
