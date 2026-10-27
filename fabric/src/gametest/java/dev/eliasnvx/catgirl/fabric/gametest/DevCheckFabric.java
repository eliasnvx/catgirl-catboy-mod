package dev.eliasnvx.catgirl.fabric.gametest;

import dev.eliasnvx.catgirl.gametest.DevCheck;
import net.fabricmc.api.ClientModInitializer;

/** Starts the shared {@link DevCheck} in {@code ./gradlew :fabric:runDevCheck} (no client GameTest API on 1.21.1). */
public final class DevCheckFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        if (Boolean.getBoolean("catgirl.devcheck")) {
            DevCheck.start(null, null);
        }
    }
}
