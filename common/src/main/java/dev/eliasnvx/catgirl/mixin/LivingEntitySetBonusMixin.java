package dev.eliasnvx.catgirl.mixin;

import dev.eliasnvx.catgirl.set.SetBonusHooks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Full Neko jumps a bit higher and Alley Cat falls further without damage. Later versions do this with the jump
 * strength and safe fall distance attributes, which 1.20.1 players don't have; same amounts. Runs on both sides: the
 * client moves its own player.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntitySetBonusMixin extends Entity {

    private LivingEntitySetBonusMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "getJumpPower", at = @At("RETURN"), cancellable = true)
    private void catgirl$fullNekoJump(CallbackInfoReturnable<Float> cir) {
        float bonus = SetBonusHooks.jumpBonus((LivingEntity) (Object) this);
        if (bonus > 0.0F) {
            cir.setReturnValue(cir.getReturnValueF() + bonus * getBlockJumpFactor());
        }
    }

    @ModifyVariable(method = "calculateFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float catgirl$alleyCatSafeFall(float fallDistance) {
        return fallDistance - SetBonusHooks.safeFallBonus((LivingEntity) (Object) this);
    }
}
