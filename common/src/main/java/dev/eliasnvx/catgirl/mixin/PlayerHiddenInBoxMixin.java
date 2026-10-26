package dev.eliasnvx.catgirl.mixin;

import dev.eliasnvx.catgirl.entity.CatSeat;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cardboard box: a player sitting in one can't be picked as a target (every mob's targeting checks this, the same way
 * spectators and invulnerable players are skipped). Mobs that were already after them are told to forget in CatSeat.
 */
@Mixin(Player.class)
public abstract class PlayerHiddenInBoxMixin {

    @Inject(method = "canBeSeenAsEnemy", at = @At("HEAD"), cancellable = true)
    private void catgirl$hiddenInBox(CallbackInfoReturnable<Boolean> cir) {
        if (CatSeat.inBox((Player) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
