package dev.eliasnvx.catgirl.neoforge;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.client.CatgirlModClient;
import dev.eliasnvx.catgirl.client.render.CosmeticLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = CatgirlMod.MOD_ID, dist = Dist.CLIENT)
public final class CatgirlModNeoForgeClient {

    public CatgirlModNeoForgeClient(IEventBus modBus) {
        CatgirlModClient.init();
        if (Boolean.getBoolean("catgirl.devcheck")) {
            startDevCheck(); // dev runs only: the class isn't in the released jar
        }
        modBus.addListener((EntityRenderersEvent.AddLayers event) -> {
            for (PlayerSkin.Model skin : event.getSkins()) {
                if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                    renderer.addLayer(new CosmeticLayer(renderer, event.getEntityModels()));
                }
            }
        });
    }

    private static void startDevCheck() {
        try {
            Class.forName("dev.eliasnvx.catgirl.neoforge.dev.DevCheckNeoForge").getMethod("start").invoke(null);
        } catch (ReflectiveOperationException e) {
            CatgirlMod.LOGGER.error("DEVCHECK couldn't start", e);
        }
    }
}
