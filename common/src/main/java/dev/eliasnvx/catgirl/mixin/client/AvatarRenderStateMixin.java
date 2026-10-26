package dev.eliasnvx.catgirl.mixin.client;

import dev.eliasnvx.catgirl.client.render.CosmeticRenderData;
import dev.eliasnvx.catgirl.client.render.CosmeticRenderStateAccess;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements CosmeticRenderStateAccess {

    @Unique
    private @Nullable CosmeticRenderData catgirl$cosmetics;

    @Override
    public @Nullable CosmeticRenderData catgirl$getCosmetics() {
        return catgirl$cosmetics;
    }

    @Override
    public void catgirl$setCosmetics(@Nullable CosmeticRenderData data) {
        catgirl$cosmetics = data;
    }
}
