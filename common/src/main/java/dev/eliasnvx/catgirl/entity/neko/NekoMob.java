package dev.eliasnvx.catgirl.entity.neko;

import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.registry.CatgirlSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * Base of the cat-person mobs that don't trade (Fisher Neko, Bakeneko, Cat Spirit): all share the humanoid rig
 * and differ by skin, coat and behaviour. The Stray Neko is a merchant, so it extends
 * AbstractVillager instead and repeats the coat/skin bits.
 */
public abstract class NekoMob extends PathfinderMob implements CoatedNeko {

    private static final EntityDataAccessor<Integer> COAT = SynchedEntityData.defineId(NekoMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(NekoMob.class, EntityDataSerializers.INT);
    private static final float VOICE_PITCH = 1.3F;
    private static final float EYE_HEIGHT = 1.62F;

    protected NekoMob(EntityType<? extends NekoMob> type, Level level) {
        super(type, level);
    }

    /** A player's eye height (1.20.1 sets it on the entity, later versions on the type). */
    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return EYE_HEIGHT;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.28);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(COAT, Coat.DEFAULT.ordinal());
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

    /** Coats a freshly spawned one of this kind may have. */
    protected Coat randomCoat() {
        return Coat.values()[random.nextInt(Coat.values().length)];
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        setCoat(randomCoat());
        setSkin(random.nextInt(SKINS));
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        output.putString("catgirl_coat", coat().getSerializedName());
        output.putInt("catgirl_skin", skin());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);
        setCoat(NekoData.coat(input.getString("catgirl_coat")));
        setSkin(input.getInt("catgirl_skin"));
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return CatgirlSounds.PLAYER_MEOW.get();
    }

    @Override
    public float getVoicePitch() {
        return VOICE_PITCH + (random.nextFloat() - 0.5F) * 0.2F;
    }
}
