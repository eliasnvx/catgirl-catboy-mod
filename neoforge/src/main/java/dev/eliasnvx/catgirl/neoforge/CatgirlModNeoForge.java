package dev.eliasnvx.catgirl.neoforge;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.neoforge.compat.curios.CuriosSlots;
import dev.eliasnvx.catgirl.platform.neoforge.PlatformHelperImpl;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

@Mod(CatgirlMod.MOD_ID)
public final class CatgirlModNeoForge {

    public CatgirlModNeoForge(IEventBus modBus) {
        PlatformHelperImpl.init(modBus);
        CatgirlMod.init();
        if (ModList.get().isLoaded("curios")) {
            CuriosSlots.register();
        }
    }
}
