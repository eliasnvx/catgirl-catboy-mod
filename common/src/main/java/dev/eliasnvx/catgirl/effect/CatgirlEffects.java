package dev.eliasnvx.catgirl.effect;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.catgirl.CatgirlMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class CatgirlEffects {

    public static final DeferredRegister<MobEffect> REGISTER = DeferredRegister.create(CatgirlMod.MOD_ID, Registries.MOB_EFFECT);

    private static final int WELL_RESTED_COLOR = 0xF5C8A0;
    private static final int CATNIP_COLOR = 0x8FD16A;
    private static final int NINE_LIVES_COLOR = 0xBFE6FF;
    /** Well Rested: +15% walking speed for a minute. */
    private static final double WELL_RESTED_SPEED = 0.15;

    /** Well Rested (cat bed nap). Pass {@code .asHolder()} to effect instances. */
    public static final RegistrySupplier<MobEffect> WELL_RESTED = REGISTER.register("well_rested", () ->
            new Simple(MobEffectCategory.BENEFICIAL, WELL_RESTED_COLOR).addAttributeModifier(Attributes.MOVEMENT_SPEED,
                    CatgirlMod.id("effect.well_rested"), WELL_RESTED_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    /** Nine Lives (Cat Spirit): the next death is cancelled once, like a totem (NineLives). */
    public static final RegistrySupplier<MobEffect> NINE_LIVES = REGISTER.register("nine_lives",
            () -> new Simple(MobEffectCategory.BENEFICIAL, NINE_LIVES_COLOR));
    /** Catnip: players tumble about now and then; cats get the zoomies (CatnipZoomiesGoal). */
    public static final RegistrySupplier<MobEffect> CATNIP = REGISTER.register("catnip", Catnip::new);

    private CatgirlEffects() {
    }

    static final class Simple extends MobEffect {
        Simple(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    /**
     * Random tumbles: every couple of seconds a player under catnip may tumble a step to a random side with
     * a little hop. Movement is client-driven for players, so the push is sent to the client (needsSync).
     */
    static final class Catnip extends MobEffect {
        private static final int ROLL_INTERVAL = 40;
        private static final float ROLL_CHANCE = 0.5F;
        private static final double ROLL_PUSH = 0.45;
        private static final double ROLL_HOP = 0.25;
        private static final int ROLL_PARTICLES = 4;

        Catnip() {
            super(MobEffectCategory.NEUTRAL, CATNIP_COLOR);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int remainingDuration, int amplifier) {
            return remainingDuration % ROLL_INTERVAL == 0;
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            RandomSource random = entity.getRandom();
            if (entity instanceof Player player && player.onGround() && !player.isPassenger() && random.nextFloat() < ROLL_CHANCE) {
                double angle = random.nextDouble() * Math.PI * 2;
                player.push(Math.cos(angle) * ROLL_PUSH, ROLL_HOP, Math.sin(angle) * ROLL_PUSH);
                player.needsSync = true;
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.3, player.getZ(),
                        ROLL_PARTICLES, 0.3, 0.1, 0.3, 0.0);
            }
            return true;
        }
    }
}
