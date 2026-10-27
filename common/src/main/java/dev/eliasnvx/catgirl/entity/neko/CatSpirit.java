package dev.eliasnvx.catgirl.entity.neko;

import dev.eliasnvx.catgirl.item.Stacks;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.effect.CatgirlEffects;
import dev.eliasnvx.catgirl.registry.CatgirlSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import dev.eliasnvx.catgirl.registry.CatgirlTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * Cat Spirit: a pale, see-through cat-person that drifts about on quiet nights. Give it a flower and it
 * grants Nine Lives (one death undone) and fades away. It can't be hurt: a hit just scares it off. Gone by morning.
 */
public class CatSpirit extends NekoMob {

    // Balance (move to catgirl-common config once the config exists)
    /** Nine Lives lasts a whole in-game day if not used. */
    static final int NINE_LIVES_TICKS = 24000;
    static final int MAX_SPAWN_LIGHT = 4;
    private static final float BOB_SPEED = 0.08F;
    private static final double BOB = 0.012;
    private static final int FADE_PARTICLES = 24;

    public CatSpirit(EntityType<? extends CatSpirit> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    /** Dark, quiet nights only, never in peaceful. */
    public static boolean checkSpawnRules(EntityType<? extends Mob> type, ServerLevelAccessor level, MobSpawnType reason,
                                          BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && level.getMaxLocalRawBrightness(pos) <= MAX_SPAWN_LIGHT
                && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.5));
    }

    @Override
    protected Coat randomCoat() {
        return random.nextBoolean() ? Coat.WHITE : Coat.SIAMESE;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // a slow float up and down
        setDeltaMovement(getDeltaMovement().x, Mth.sin(tickCount * BOB_SPEED) * BOB, getDeltaMovement().z);
        if (level() instanceof ServerLevel level && level.isDay()) {
            fade(level);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(CatgirlTags.SPIRIT_GIFTS)) {
            return super.mobInteract(player, hand);
        }
        if (level() instanceof ServerLevel level) {
            Stacks.consume(held, 1, player);
            player.addEffect(new MobEffectInstance(CatgirlEffects.NINE_LIVES.get(), NINE_LIVES_TICKS));
            player.displayClientMessage(Component.translatable("message.catgirl.cat_spirit.gift"), true);
            fade(level);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level() instanceof ServerLevel level) {
            fade(level); // spirits can't be hurt: they just leave
        }
        return false;
    }

    private void fade(ServerLevel level) {
        level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.0, getZ(), FADE_PARTICLES, 0.3, 0.6, 0.3, 0.03);
        level.playSound(null, this, CatgirlSounds.CAT_SPIRIT_FADE.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        discard();
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return CatgirlSounds.CAT_BED_PURR.get();
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }
}
