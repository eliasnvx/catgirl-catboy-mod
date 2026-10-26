package dev.eliasnvx.catgirl.fabric;

import dev.eliasnvx.catgirl.client.CatgirlModClient;
import dev.eliasnvx.catgirl.client.render.CoatTintSource;
import dev.eliasnvx.catgirl.client.render.CosmeticLayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public final class CatgirlModFabricClient implements ClientModInitializer {

    @Override
    @SuppressWarnings("unchecked")
    public void onInitializeClient() {
        CatgirlModClient.init();
        // ID_MAPPER is widened by fabric-transitive-access-wideners
        ItemTintSources.ID_MAPPER.put(CoatTintSource.ID, CoatTintSource.MAP_CODEC);
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof AvatarRenderer<?> avatar) {
                helper.register(new CosmeticLayer((RenderLayerParent<AvatarRenderState, PlayerModel>) avatar, context.getModelSet()));
            }
        });
    }
}
