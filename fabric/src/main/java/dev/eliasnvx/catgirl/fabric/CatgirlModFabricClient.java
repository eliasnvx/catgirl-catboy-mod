package dev.eliasnvx.catgirl.fabric;

import dev.eliasnvx.catgirl.client.CatgirlModClient;
import dev.eliasnvx.catgirl.client.render.CosmeticLayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

public final class CatgirlModFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CatgirlModClient.init();
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof PlayerRenderer player) {
                helper.register(new CosmeticLayer(player, context.getModelSet()));
            }
        });
    }
}
