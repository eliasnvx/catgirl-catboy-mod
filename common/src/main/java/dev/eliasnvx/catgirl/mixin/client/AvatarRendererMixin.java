package dev.eliasnvx.catgirl.mixin.client;

import dev.eliasnvx.catgirl.client.LandingPose;
import dev.eliasnvx.catgirl.client.render.CosmeticRenderData;
import dev.eliasnvx.catgirl.client.render.CosmeticRenderStateAccess;
import dev.eliasnvx.catgirl.entity.CatSeat;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Copies what a player wears into its render state (in 26.x the render layer only sees the state). */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void catgirl$extractCosmetics(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        ((CosmeticRenderStateAccess) state).catgirl$setCosmetics(CosmeticRenderData.capture(entity, state));
        if (LandingPose.active(entity)) {
            state.isCrouching = true; // Alley Cat: landed on all fours
        }
        if (CatSeat.inBed(entity)) {
            // curled up in a cat bed: the sleeping pose without vanilla sleeping (no time skip, works by day)
            state.pose = Pose.SLEEPING;
            state.bedOrientation = Direction.fromYRot(entity.getYRot());
            state.isPassenger = false;
        } else if (CatSeat.inBox(entity)) {
            state.isCrouching = true; // ducked into the box
            state.isPassenger = false;
        }
    }
}
