package dev.eliasnvx.catgirl.entity.neko;

import dev.eliasnvx.catgirl.item.Stacks;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.block.CatBedBlock;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.entity.CatgirlEntities;
import dev.eliasnvx.catgirl.registry.CatgirlSounds;
import dev.eliasnvx.catgirl.registry.CatgirlTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import dev.eliasnvx.catgirl.world.trade.TradeSets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Stray Neko: lives in villages and alleys, sells ears, collars and fish (trade set
 * {@code catgirl:stray_neko}). Feed it raw fish: it may start trusting you (tamed: follows you, naps on a cat bed at
 * night), and once it trusts you, every two fish bring a kitten.
 */
public class StrayNeko extends AbstractVillager implements CoatedNeko {

    public static final ResourceLocation TRADES = CatgirlMod.id("stray_neko");

    // Balance (move to catgirl-common config once the config exists)
    static final float TAME_CHANCE = 1.0F / 3.0F;
    static final int FISH_PER_KITTEN = 2;
    static final int KITTEN_COOLDOWN_TICKS = 5 * 60 * 20;
    static final float FEED_HEAL = 2.0F;
    static final double FOLLOW_START = 6.0;
    static final double FOLLOW_STOP = 3.0;
    static final double FOLLOW_MAX = 32.0;

    private static final EntityDataAccessor<Integer> COAT = SynchedEntityData.defineId(StrayNeko.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(StrayNeko.class, EntityDataSerializers.INT);
    private static final float VOICE_PITCH = 1.3F;
    private static final float EYE_HEIGHT = 1.62F;

    private @Nullable UUID owner;
    private int fishFed;
    private long nextKittenTime;

    public StrayNeko(EntityType<? extends StrayNeko> type, Level level) {
        super(type, level);
    }

    /** A player's eye height (1.20.1 sets it on the entity, later versions on the type). */
    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return EYE_HEIGHT;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(2, new PanicGoal(this, 1.4));
        goalSelector.addGoal(4, new FollowOwnerGoal());
        goalSelector.addGoal(5, new NapOnCatBedGoal());
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(COAT, Coat.GINGER.ordinal());
        entityData.define(SKIN, 0);
    }

    @Override
    public Coat coat() {
        return Coat.values()[Math.floorMod(entityData.get(COAT), Coat.values().length)];
    }

    public void setCoat(Coat coat) {
        entityData.set(COAT, coat.ordinal());
    }

    @Override
    public int skin() {
        return Math.floorMod(entityData.get(SKIN), SKINS);
    }

    public void setSkin(int skin) {
        entityData.set(SKIN, Math.floorMod(skin, SKINS));
    }

    public @Nullable UUID owner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    public boolean isTame() {
        return owner != null;
    }

    private @Nullable Player ownerPlayer() {
        return owner == null ? null : level().getPlayerByUUID(owner);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        if (reason != MobSpawnType.CONVERSION) {
            // Siamese and Calico stay rare: the fisher's quest gives them
            Coat[] common = {Coat.BLACK, Coat.WHITE, Coat.GINGER, Coat.GRAY, Coat.PINK};
            setCoat(common[random.nextInt(common.length)]);
            setSkin(random.nextInt(SKINS));
        }
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(CatgirlTags.RAW_FISH)) {
            if (player instanceof ServerPlayer serverPlayer) {
                feed(serverPlayer, held);
            }
            return InteractionResult.SUCCESS;
        }
        if (isAlive() && !isTrading()) {
            if (!level().isClientSide()) {
                if (getOffers().isEmpty()) {
                    return InteractionResult.CONSUME;
                }
                setTradingPlayer(player);
                openTradingScreen(player, getDisplayName(), 1);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    private void feed(ServerPlayer player, ItemStack fish) {
        ServerLevel level = (ServerLevel) level();
        Stacks.consume(fish, 1, player);
        heal(FEED_HEAL);
        playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
        if (!isTame()) {
            if (random.nextFloat() < TAME_CHANCE) {
                owner = player.getUUID();
                setPersistenceRequired();
                hearts(level);
                player.displayClientMessage(Component.translatable("message.catgirl.stray_neko.trusts", getDisplayName()), true);
            } else {
                level.sendParticles(ParticleTypes.SMOKE, getX(), getEyeY(), getZ(), 5, 0.3, 0.2, 0.3, 0.0);
            }
            return;
        }
        hearts(level);
        if (++fishFed >= FISH_PER_KITTEN && level.getGameTime() >= nextKittenTime) {
            fishFed = 0;
            nextKittenTime = level.getGameTime() + KITTEN_COOLDOWN_TICKS;
            spawnKitten(level, player);
        }
    }

    private void spawnKitten(ServerLevel level, ServerPlayer player) {
        Kitten kitten = CatgirlEntities.KITTEN.get().create(level);
        if (kitten == null) {
            return;
        }
        kitten.setCoat(random.nextBoolean() ? coat() : Coat.values()[random.nextInt(Coat.values().length)]);
        kitten.setAge(AgeableMob.BABY_START_AGE);
        kitten.moveTo(getX(), getY(), getZ(), random.nextFloat() * 360.0F, 0.0F);
        kitten.tame(player);
        level.addFreshEntityWithPassengers(kitten);
        player.displayClientMessage(Component.translatable("message.catgirl.stray_neko.kitten", getDisplayName()), true);
    }

    private void hearts(ServerLevel level) {
        level.sendParticles(ParticleTypes.HEART, getX(), getEyeY() + 0.4, getZ(), 4, 0.4, 0.2, 0.4, 0.0);
    }

    @Override
    protected void updateTrades() {
        TradeSets.addOffers(this, getOffers(), TRADES, random);
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        if (offer.shouldRewardExp() && level() instanceof ServerLevel level) {
            level.addFreshEntity(new ExperienceOrb(level, getX(), getY() + 0.5, getZ(), 3 + random.nextInt(4)));
        }
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null; // kittens come from feeding (above), not from breeding two nekos
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return isTrading() ? null : CatgirlSounds.PLAYER_MEOW.get();
    }

    @Override
    public float getVoicePitch() {
        return VOICE_PITCH + (random.nextFloat() - 0.5F) * 0.2F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        output.putString("catgirl_coat", coat().getSerializedName());
        output.putInt("catgirl_skin", skin());
        if (owner != null) {
            output.putString("catgirl_owner", owner.toString());
        }
        output.putInt("catgirl_fish_fed", fishFed);
        output.putLong("catgirl_next_kitten", nextKittenTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);
        setCoat(NekoData.coat(input.getString("catgirl_coat")));
        setSkin(input.getInt("catgirl_skin"));
        String savedOwner = input.getString("catgirl_owner");
        owner = savedOwner.isEmpty() ? null : UUID.fromString(savedOwner);
        fishFed = input.getInt("catgirl_fish_fed");
        nextKittenTime = input.getLong("catgirl_next_kitten");
    }

    /** A tamed neko walks after its owner (no teleporting: it's a person, not a pet). */
    private final class FollowOwnerGoal extends Goal {
        private @Nullable Player target;

        FollowOwnerGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            target = ownerPlayer();
            return target != null && !target.isSpectator() && !isTrading()
                    && distanceToSqr(target) > FOLLOW_START * FOLLOW_START && distanceToSqr(target) < FOLLOW_MAX * FOLLOW_MAX;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && !isTrading() && distanceToSqr(target) > FOLLOW_STOP * FOLLOW_STOP
                    && distanceToSqr(target) < FOLLOW_MAX * FOLLOW_MAX;
        }

        @Override
        public void tick() {
            if (target != null) {
                getLookControl().setLookAt(target, 10.0F, getMaxHeadXRot());
                if (tickCount % 10 == 0) {
                    getNavigation().moveTo(target, 1.0);
                }
            }
        }

        @Override
        public void stop() {
            target = null;
            getNavigation().stop();
        }
    }

    /** At night a tamed neko goes to a cat bed nearby and stays there until morning. */
    private final class NapOnCatBedGoal extends MoveToBlockGoal {
        private static final int SEARCH_RANGE = 12;

        NapOnCatBedGoal() {
            super(StrayNeko.this, 0.8, SEARCH_RANGE);
        }

        @Override
        public boolean canUse() {
            return isTame() && !level().isDay() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !level().isDay() && super.canContinueToUse();
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            return level.getBlockState(pos).getBlock() instanceof CatBedBlock && level.isEmptyBlock(pos.above());
        }
    }
}
