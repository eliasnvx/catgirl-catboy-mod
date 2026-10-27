package dev.eliasnvx.catgirl.neoforge.dev;

import dev.eliasnvx.catgirl.gametest.DevCheck;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.ModListScreen;

/** Starts the shared {@link DevCheck} with NeoForge's mod list and the Curios check. Dev runs only, not in the released jar. */
public final class DevCheckNeoForge {

    private DevCheckNeoForge() {
    }

    public static void start() {
        DevCheck.start(() -> new ModListScreen(null), ModList.get().isLoaded("curios") ? DevCheckCurios::wear : null);
    }
}
