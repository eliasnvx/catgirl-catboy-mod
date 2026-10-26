package dev.eliasnvx.catgirl.entity;

import dev.eliasnvx.catgirl.CatgirlAdvancements;
import dev.eliasnvx.catgirl.block.CardboardBoxBlock;
import dev.eliasnvx.catgirl.block.CatBedBlock;
import dev.eliasnvx.catgirl.effect.CatgirlEffects;
import dev.eliasnvx.catgirl.registry.CatgirlSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Invisible seat that holds a player in a cat bed or a cardboard box (adapted from Femboy Mod's seat). It removes itself
 * when the player gets up or the block is gone. What happens while seated depends on the block below:
 * the bed feeds and rests the player, the box hides them from mobs (CardboardBoxMixin reads {@link #inBox}).
 */
public class CatSeat extends Entity {

    // Balance (move to catgirl-common config once the config exists)
    /** Bed: one food point and a bit of saturation per this many ticks. */
    static final int BED_FEED_INTERVAL = 40;
    static final int BED_FEED_FOOD = 1;
    static final float BED_FEED_SATURATION = 0.6F;
    /** Bed: a nap this long gives Well Rested and counts as a night's sleep for phantoms. */
    static final int BED_NAP_TICKS = 200;
    static final int WELL_RESTED_TICKS = 60 * 20;
    static final int PURR_INTERVAL = 60;
    /** Box: mobs this close that were after the player lose track of them when they hide. */
    static final double BOX_FORGET_RADIUS = 32.0;

    private static final double BLOCK_PROBE = 0.5;
    private static final float PURR_VOLUME = 0.6F;
    private static final int NAP_HEARTS = 5;

    private int restTicks;

    public CatSeat(EntityType<? extends CatSeat> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** Seats a player in the block at {@code pos}; false if someone is already there. */
    public static boolean sit(ServerLevel level, BlockPos pos, Player player, double seatHeight) {
        if (!level.getEntitiesOfClass(CatSeat.class, new AABB(pos)).isEmpty()) {
            return false;
        }
        CatSeat seat = CatgirlEntities.SEAT.get().create(level, EntitySpawnReason.TRIGGERED);
        if (seat == null) {
            return false;
        }
        seat.setPos(pos.getX() + 0.5, pos.getY() + seatHeight, pos.getZ() + 0.5);
        seat.setYRot(player.getYRot());
        level.addFreshEntity(seat);
        if (!player.startRiding(seat)) {
            seat.discard();
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer && seat.inBox()) {
            hide(level, serverPlayer);
        }
        return true;
    }

    /** Mobs chasing the player lose them; the box advancement. */
    private static void hide(ServerLevel level, ServerPlayer player) {
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(BOX_FORGET_RADIUS),
                mob -> mob.getTarget() == player)) {
            mob.setTarget(null);
        }
        CatgirlAdvancements.award(player, CatgirlAdvancements.BOX_ENJOYER);
    }

    /** Whether a player rides a seat in a cardboard box (both sides: the client knows the block too). */
    public static boolean inBox(Entity entity) {
        return entity.getVehicle() instanceof CatSeat seat && seat.inBox();
    }

    /** Whether a player lies in a cat bed (the client lays the model down). */
    public static boolean inBed(Entity entity) {
        return entity.getVehicle() instanceof CatSeat seat && seat.inBed();
    }

    public boolean inBox() {
        return block() instanceof CardboardBoxBlock;
    }

    public boolean inBed() {
        return block() instanceof CatBedBlock;
    }

    /**
     * The bed or box this seat is in. Measured half a block above the seat: in a box the seat sits below the box's
     * floor (so only the head pokes out), where {@link #blockPosition()} is already the block underneath.
     */
    private Block block() {
        return level().getBlockState(seatBlockPos()).getBlock();
    }

    private BlockPos seatBlockPos() {
        return BlockPos.containing(getX(), getY() + BLOCK_PROBE, getZ());
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        // Empty seats go on the next tick, not in removePassenger: a seat that is already removed when its rider gets
        // off makes vanilla skip getDismountLocationForPassenger, and the rider would stay half inside the floor
        if (getPassengers().isEmpty() || !(inBed() || inBox())) {
            ejectPassengers();
            discard();
            return;
        }
        if (inBed() && getFirstPassenger() instanceof ServerPlayer player) {
            rest(level, player);
        }
    }

    /** Cat nap: food back bit by bit, purring, and Well Rested once the nap is long enough. */
    private void rest(ServerLevel level, ServerPlayer player) {
        restTicks++;
        if (restTicks % BED_FEED_INTERVAL == 0) {
            player.getFoodData().eat(BED_FEED_FOOD, BED_FEED_SATURATION);
        }
        if (restTicks % PURR_INTERVAL == 0) {
            level.playSound(null, this, CatgirlSounds.CAT_BED_PURR.get(), SoundSource.PLAYERS, PURR_VOLUME, 1.0F);
        }
        if (restTicks == BED_NAP_TICKS) {
            player.addEffect(new MobEffectInstance(CatgirlEffects.WELL_RESTED.asHolder(), WELL_RESTED_TICKS));
            player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)); // a real nap: no phantoms tonight
            level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.8, getZ(), NAP_HEARTS, 0.4, 0.2, 0.4, 0.0);
        }
    }

    /**
     * The rider's feet go exactly to the seat (vanilla puts a sitting rider's feet below the attachment point). In a bed
     * the lying pose is drawn from the feet, so they must be on the cushion; in a box the seat height alone decides
     * how far the player sinks in.
     */
    @Override
    public Vec3 getPassengerRidingPosition(Entity passenger) {
        return position().add(passenger.getVehicleAttachmentPoint(this));
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return Vec3.atBottomCenterOf(seatBlockPos().above());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
