package dev.eliasnvx.catgirl.neoforge;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.cosmetic.CosmeticsEvents;
import dev.eliasnvx.catgirl.neoforge.compat.curios.CuriosSlots;
import dev.eliasnvx.catgirl.platform.neoforge.PlatformHelperImpl;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;

@Mod(CatgirlMod.MOD_ID)
public final class CatgirlModNeoForge {

    public CatgirlModNeoForge(IEventBus modBus) {
        PlatformHelperImpl.init(modBus);
        CatgirlMod.init();
        if (ModList.get().isLoaded("curios")) {
            CuriosSlots.register();
        }
        if (GameTestHooks.isGametestEnabled()) {
            registerGameTests(modBus); // dev runs only: the class isn't in the released jar
        }
        // Architectury 13 has no start-tracking event: sync the outfits of players coming into view
        NeoForge.EVENT_BUS.addListener((PlayerEvent.StartTracking event) -> {
            if (event.getEntity() instanceof ServerPlayer watcher) {
                CosmeticsEvents.onStartTracking(event.getTarget(), watcher);
            }
        });
    }

    private static void registerGameTests(IEventBus modBus) {
        try {
            Class.forName("dev.eliasnvx.catgirl.neoforge.dev.CatgirlGameTestsNeoForge")
                    .getMethod("register", IEventBus.class).invoke(null, modBus);
        } catch (ReflectiveOperationException e) {
            CatgirlMod.LOGGER.error("GameTests couldn't be registered", e);
        }
    }
}
