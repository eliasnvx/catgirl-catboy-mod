package dev.eliasnvx.catgirl.forge;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.client.CatgirlModClient;
import dev.eliasnvx.catgirl.client.render.CosmeticLayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** Client-only setup, called from {@link CatgirlModForge} on the physical client. */
final class CatgirlModForgeClient {

    private CatgirlModForgeClient() {
    }

    static void init(IEventBus modBus) {
        CatgirlModClient.init();
        if (Boolean.getBoolean("catgirl.devcheck")) {
            startDevCheck(); // dev runs only: the class isn't in the released jar
        }
        modBus.addListener((EntityRenderersEvent.AddLayers event) -> {
            for (String skin : event.getSkins()) {
                EntityRenderer<? extends Player> renderer = event.getSkin(skin);
                if (renderer instanceof PlayerRenderer player) {
                    player.addLayer(new CosmeticLayer(player, event.getEntityModels()));
                }
            }
        });
    }

    private static void startDevCheck() {
        try {
            Class.forName("dev.eliasnvx.catgirl.forge.dev.DevCheckForge").getMethod("start").invoke(null);
        } catch (ReflectiveOperationException e) {
            CatgirlMod.LOGGER.error("DEVCHECK couldn't start", e);
        }
    }
}
