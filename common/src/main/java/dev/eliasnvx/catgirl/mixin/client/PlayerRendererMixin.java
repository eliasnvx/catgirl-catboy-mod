package dev.eliasnvx.catgirl.mixin.client;

import dev.eliasnvx.catgirl.client.LandingPose;
import dev.eliasnvx.catgirl.entity.CatSeat;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Crouches players who landed on all fours (Alley Cat) or ducked into a cardboard box. */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(method = "setModelProperties", at = @At("TAIL"))
    private void catgirl$crouch(AbstractClientPlayer player, CallbackInfo ci) {
        if (catgirl$crouches(player)) {
            ((PlayerRenderer) (Object) this).getModel().crouching = true;
        }
    }

    @Inject(method = "getRenderOffset(Lnet/minecraft/client/player/AbstractClientPlayer;F)Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"), cancellable = true)
    private void catgirl$crouchOffset(AbstractClientPlayer player, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (!player.isCrouching() && catgirl$crouches(player)) {
            cir.setReturnValue(new Vec3(0.0, player.getScale() * -2.0F / 16.0, 0.0)); // vanilla's crouch offset
        }
    }

    private static boolean catgirl$crouches(AbstractClientPlayer player) {
        return LandingPose.active(player) || CatSeat.inBox(player);
    }
}
