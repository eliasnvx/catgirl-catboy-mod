package dev.eliasnvx.catgirl.mixin;

import dev.eliasnvx.catgirl.set.CatSets;
import dev.eliasnvx.catgirl.set.SetBonusHooks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Quieter steps in paw slippers or the Full Neko set. Runs on both sides: a player's own client plays its steps, the
 * server plays them for everyone else; both know the cosmetics, so both turn the volume down.
 * <p>
 * Vanilla (Fabric) and Forge 47 call {@code playSound} right in {@code playStepSound}; later NeoForge versions patch that
 * into {@code BlockState.playStepSound(level, pos, entity, volume, pitch)}. One injector per case (kept alike across the
 * branches), the group makes sure exactly one of them applies.
 */
@Mixin(Entity.class)
public abstract class EntityStepSoundMixin {

    @Group(name = "catgirl$softPaws", min = 1, max = 1)
    @ModifyArg(method = "playStepSound", index = 1, require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"))
    private float catgirl$softPaws(float volume) {
        return catgirl$quieter(volume);
    }

    @Group(name = "catgirl$softPaws", min = 1, max = 1)
    @ModifyArg(method = "playStepSound", index = 3, require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;playStepSound(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;FF)V"))
    private float catgirl$softPawsNeoForge(float volume) {
        return catgirl$quieter(volume);
    }

    private float catgirl$quieter(float volume) {
        return (Object) this instanceof Player player && CatSets.quietSteps(player) ? volume * SetBonusHooks.quietStepVolume() : volume;
    }
}
