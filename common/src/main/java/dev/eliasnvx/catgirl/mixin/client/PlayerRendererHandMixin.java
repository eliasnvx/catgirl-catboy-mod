package dev.eliasnvx.catgirl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.catgirl.client.render.FirstPersonPaws;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws worn paws over the first-person hand (both loaders share it; no loader has a common hook for this). */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererHandMixin {

    @Inject(method = "renderHand", at = @At("TAIL"))
    private void catgirl$paws(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                              ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        FirstPersonPaws.render(((PlayerRenderer) (Object) this).getModel(), arm, poseStack, buffers, light, player);
    }
}
