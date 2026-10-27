package dev.eliasnvx.catgirl.mixin;

import dev.eliasnvx.catgirl.set.CatSet;
import dev.eliasnvx.catgirl.set.CatSets;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * House Cat: stray cats and untrusting ocelots don't run from a player wearing the set. Their flee goals are vanilla
 * avoid goals with a fixed predicate, so the only place to say "not this player" is the goal's start check.
 */
@Mixin(AvoidEntityGoal.class)
public abstract class AvoidEntityGoalMixin<T extends LivingEntity> {

    @Shadow
    @Final
    protected PathfinderMob mob;

    @Shadow
    protected T toAvoid;

    @Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
    private void catgirl$trustHouseCats(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && (mob instanceof Cat || mob instanceof Ocelot)
                && toAvoid instanceof Player player && CatSets.has(player, CatSet.HOUSE_CAT)) {
            cir.setReturnValue(false);
        }
    }
}
