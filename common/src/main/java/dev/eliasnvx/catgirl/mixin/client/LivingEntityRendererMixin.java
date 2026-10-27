package dev.eliasnvx.catgirl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.eliasnvx.catgirl.client.render.ARGB32;
import dev.eliasnvx.catgirl.client.render.entity.ModelTint;
import net.minecraft.client.model.EntityModel;
import dev.eliasnvx.catgirl.entity.CatSeat;
import dev.eliasnvx.catgirl.entity.neko.FisherNeko;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Seated players: one curled up in a cat bed is drawn lying down (the sleeping pose without vanilla sleeping: no time
 * skip, works by day), and neither the bed nor the box uses the riding pose; a fishing Fisher Neko sits. Also lets our
 * renderers tint the model.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isPassenger()Z", ordinal = 0))
    private boolean catgirl$ridingPose(LivingEntity entity) {
        if (entity instanceof FisherNeko fisher && fisher.isFishing()) {
            return true;
        }
        return entity.isPassenger() && !CatSeat.inBed(entity) && !CatSeat.inBox(entity);
    }

    @Redirect(method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            "setupRotations"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hasPose(Lnet/minecraft/world/entity/Pose;)Z"))
    private boolean catgirl$lyingInBed(LivingEntity entity, Pose pose) {
        return entity.hasPose(pose) || pose == Pose.SLEEPING && CatSeat.inBed(entity);
    }

    @Redirect(method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            "setupRotations"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getBedOrientation()Lnet/minecraft/core/Direction;"))
    private Direction catgirl$bedFacing(LivingEntity entity) {
        return CatSeat.inBed(entity) ? Direction.fromYRot(entity.getYRot()) : entity.getBedOrientation();
    }

    @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void catgirl$tint(EntityModel<?> model, PoseStack pose, VertexConsumer buffer, int light, int overlay,
                              float red, float green, float blue, float alpha) {
        if (this instanceof ModelTint tint) {
            int color = tint.catgirl$tint(ARGB32.color(Math.round(alpha * 255), Math.round(red * 255), Math.round(green * 255),
                    Math.round(blue * 255)));
            model.renderToBuffer(pose, buffer, light, overlay, ARGB32.redF(color), ARGB32.greenF(color), ARGB32.blueF(color),
                    ARGB32.alphaF(color));
        } else {
            model.renderToBuffer(pose, buffer, light, overlay, red, green, blue, alpha);
        }
    }
}
