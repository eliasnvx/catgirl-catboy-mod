package dev.eliasnvx.catgirl.effect;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Cat Spirit's gift: with Nine Lives on, the next death doesn't happen. Works like a totem of undying
 * (same limits: nothing saves from /kill or the void) and is used up.
 */
public final class NineLives {

    private static final float SAVED_HEALTH = 1.0F;
    private static final int REGEN_TICKS = 45 * 20;
    private static final int ABSORPTION_TICKS = 5 * 20;
    private static final int SOUL_PARTICLES = 30;

    private NineLives() {
    }

    /** Must be registered before other death listeners (cosmetic death drops): cancelling stops the ones after it. */
    public static void register() {
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (!(entity.level() instanceof ServerLevel level) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                    || !entity.hasEffect(CatgirlEffects.NINE_LIVES)) {
                return EventResult.pass();
            }
            save(level, entity);
            return EventResult.interruptFalse();
        });
    }

    private static void save(ServerLevel level, LivingEntity entity) {
        entity.setHealth(SAVED_HEALTH);
        entity.removeEffect(CatgirlEffects.NINE_LIVES);
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_TICKS, 1));
        entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ABSORPTION_TICKS, 1));
        level.playSound(null, entity, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.4F);
        level.sendParticles(ParticleTypes.SOUL, entity.getX(), entity.getY() + 1.0, entity.getZ(), SOUL_PARTICLES, 0.4, 0.6, 0.4, 0.05);
    }
}
