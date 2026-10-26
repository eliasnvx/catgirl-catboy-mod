package dev.eliasnvx.catgirl.neoforge;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.client.CatgirlModClient;
import dev.eliasnvx.catgirl.client.render.CoatTintSource;
import dev.eliasnvx.catgirl.client.render.CosmeticLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@Mod(value = CatgirlMod.MOD_ID, dist = Dist.CLIENT)
public final class CatgirlModNeoForgeClient {

    public CatgirlModNeoForgeClient(IEventBus modBus) {
        CatgirlModClient.init();
        if (Boolean.getBoolean("catgirl.devcheck")) {
            startDevCheck(); // dev runs only: the class isn't in the released jar
        }
        modBus.addListener((RegisterColorHandlersEvent.ItemTintSources event) ->
                event.register(CoatTintSource.ID, CoatTintSource.MAP_CODEC));
        modBus.addListener((EntityRenderersEvent.AddLayers event) -> {
            for (PlayerModelType skin : event.getSkins()) {
                AvatarRenderer<?> renderer = event.getPlayerRenderer(skin);
                if (renderer != null) {
                    renderer.addLayer(new CosmeticLayer(renderer, event.getEntityModels()));
                }
            }
        });
    }

    private static void startDevCheck() {
        try {
            Class.forName("dev.eliasnvx.catgirl.neoforge.dev.DevCheck").getMethod("start").invoke(null);
        } catch (ReflectiveOperationException e) {
            CatgirlMod.LOGGER.error("DEVCHECK couldn't start", e);
        }
    }
}
