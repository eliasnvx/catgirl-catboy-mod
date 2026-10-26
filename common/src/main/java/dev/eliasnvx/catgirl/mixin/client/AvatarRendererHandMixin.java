package dev.eliasnvx.catgirl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.catgirl.client.render.FirstPersonPaws;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws worn paws over the first-person hand (both loaders share it; no loader has a common hook for this). */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererHandMixin {

    @Inject(method = "renderHand", at = @At("TAIL"))
    private void catgirl$paws(PoseStack poseStack, SubmitNodeCollector collector, int light, Identifier skin,
                              ModelPart arm, boolean hasSleeve, CallbackInfo ci) {
        FirstPersonPaws.submit(((AvatarRenderer<?>) (Object) this).getModel(), arm, poseStack, collector, light);
    }
}
