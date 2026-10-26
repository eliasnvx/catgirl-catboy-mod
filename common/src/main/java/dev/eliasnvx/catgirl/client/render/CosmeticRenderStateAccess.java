package dev.eliasnvx.catgirl.client.render;

import org.jetbrains.annotations.Nullable;

/** Duck interface mixed into AvatarRenderState. */
public interface CosmeticRenderStateAccess {

    @Nullable CosmeticRenderData catgirl$getCosmetics();

    void catgirl$setCosmetics(@Nullable CosmeticRenderData data);
}
