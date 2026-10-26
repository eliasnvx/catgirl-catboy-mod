package dev.eliasnvx.catgirl.entity.neko;

import dev.eliasnvx.catgirl.block.CardboardBoxBlock;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.entity.CatgirlEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Kitten (a real kitten, not a small humanoid): a vanilla cat at heart (tame, sit,
 * follow) in one of the mod's coats. It chases yarn balls (the mod's cat goals), hops onto cardboard boxes, and grows
 * up into a Stray Neko with the same coat, still trusting its owner.
 */
public class Kitten extends Cat {

    private static final EntityDataAccessor<Integer> COAT = SynchedEntityData.defineId(Kitten.class, EntityDataSerializers.INT);
    private static final int BOX_GOAL_PRIORITY = 6;
    private static final int GROW_UP_PARTICLES = 12;

    public Kitten(EntityType<? extends Cat> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(BOX_GOAL_PRIORITY, new HopIntoBoxGoal());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COAT, Coat.GINGER.ordinal());
    }

    public Coat coat() {
        return Coat.values()[Math.floorMod(entityData.get(COAT), Coat.values().length)];
    }

    public void setCoat(Coat coat) {
        entityData.set(COAT, coat.ordinal());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                        @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        if (reason != EntitySpawnReason.BREEDING) {
            setCoat(Coat.values()[random.nextInt(Coat.values().length)]);
        }
        setAge(AgeableMob.BABY_START_AGE);
        return result;
    }

    /** Grown up: becomes a Stray Neko (same coat, name and owner). */
    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        if (!isBaby() && level() instanceof ServerLevel level && isAlive()) {
            growUp(level);
        }
    }

    private void growUp(ServerLevel level) {
        StrayNeko neko = CatgirlEntities.STRAY_NEKO.get().create(level, EntitySpawnReason.CONVERSION);
        if (neko == null) {
            return;
        }
        neko.snapTo(getX(), getY(), getZ(), getYRot(), getXRot());
        neko.setCoat(coat());
        neko.setSkin(random.nextInt(CoatedNeko.SKINS));
        neko.setCustomName(getCustomName());
        if (getOwner() instanceof LivingEntity owner) {
            neko.setOwner(owner.getUUID());
        }
        neko.setPersistenceRequired();
        neko.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), EntitySpawnReason.CONVERSION, null);
        level.addFreshEntity(neko);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.5, getZ(), GROW_UP_PARTICLES, 0.4, 0.4, 0.4, 0.0);
        discard();
    }

    @Override
    public @Nullable Cat getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("catgirl_coat", coat().getSerializedName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setCoat(NekoData.coat(input.getStringOr("catgirl_coat", "")));
    }

    /** If it fits, it sits: a kitten climbs onto a cardboard box nearby and sits there for a while. */
    private final class HopIntoBoxGoal extends MoveToBlockGoal {
        private static final int SEARCH_RANGE = 8;

        HopIntoBoxGoal() {
            super(Kitten.this, 1.1, SEARCH_RANGE, 2);
        }

        @Override
        public boolean canUse() {
            return !isOrderedToSit() && super.canUse();
        }

        @Override
        public void stop() {
            super.stop();
            setInSittingPose(false);
        }

        @Override
        public void tick() {
            super.tick();
            setInSittingPose(isReachedTarget());
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            return level.getBlockState(pos).getBlock() instanceof CardboardBoxBlock && level.isEmptyBlock(pos.above());
        }
    }
}
