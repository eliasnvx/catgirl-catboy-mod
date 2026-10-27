package dev.eliasnvx.catgirl.forge;

import dev.architectury.platform.forge.EventBuses;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.cosmetic.CosmeticsEvents;
import dev.eliasnvx.catgirl.forge.compat.curios.CuriosSlots;
import dev.eliasnvx.catgirl.platform.forge.PlatformHelperImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.gametest.ForgeGameTestHooks;

@Mod(CatgirlMod.MOD_ID)
public final class CatgirlModForge {

    public CatgirlModForge(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        // Architectury's DeferredRegister and events on Forge need the mod bus before any registration
        EventBuses.registerModEventBus(CatgirlMod.MOD_ID, modBus);
        PlatformHelperImpl.init(modBus);
        CatgirlMod.init();
        if (ModList.get().isLoaded("curios")) {
            CuriosSlots.register();
        }
        if (ForgeGameTestHooks.isGametestEnabled()) {
            registerGameTests(modBus); // dev runs only: the class isn't in the released jar
        }
        // Architectury 9 has no start-tracking event: sync the outfits of players coming into view
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.StartTracking event) -> {
            if (event.getEntity() instanceof ServerPlayer watcher) {
                CosmeticsEvents.onStartTracking(event.getTarget(), watcher);
            }
        });
        // Forge 47's @Mod has no dist attribute: client-only setup lives in a class only loaded on the client
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CatgirlModForgeClient.init(modBus);
        }
    }

    private static void registerGameTests(IEventBus modBus) {
        try {
            Class.forName("dev.eliasnvx.catgirl.forge.dev.CatgirlGameTestsForge")
                    .getMethod("register", IEventBus.class).invoke(null, modBus);
        } catch (ReflectiveOperationException e) {
            CatgirlMod.LOGGER.error("GameTests couldn't be registered", e);
        }
    }
}
