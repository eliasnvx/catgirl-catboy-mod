package dev.eliasnvx.catgirl.forge.dev;

import dev.eliasnvx.catgirl.gametest.DevCheck;
import net.minecraftforge.client.gui.ModListScreen;
import net.minecraftforge.fml.ModList;

/** Starts the shared {@link DevCheck} with Forge's mod list and the Curios check. Dev runs only, not in the released jar. */
public final class DevCheckForge {

    private DevCheckForge() {
    }

    public static void start() {
        DevCheck.start(() -> new ModListScreen(null), ModList.get().isLoaded("curios") ? DevCheckCurios::wear : null);
    }
}
