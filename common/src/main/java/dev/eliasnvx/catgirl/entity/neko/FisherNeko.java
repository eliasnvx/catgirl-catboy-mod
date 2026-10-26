package dev.eliasnvx.catgirl.entity.neko;

import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.registry.CatgirlTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fisher Neko: sits on beaches and river banks with a rod and now and then pulls out a fish. Quest:
 * bring it 10 raw fish and it gives ears in a rare coat (Siamese or Calico); the count is kept per player, and the
 * quest can be done again.
 */
public class FisherNeko extends NekoMob {

    // Balance (move to catgirl-common config once the config exists)
    public static final int QUEST_FISH = 10;
    static final int CATCH_MIN_TICKS = 20 * 20;
    static final int CATCH_EXTRA_TICKS = 40 * 20;
    static final int SHOW_CATCH_TICKS = 40;
    static final int FISHING_MIN_TICKS = 60 * 20;
    static final int FISHING_EXTRA_TICKS = 120 * 20;

    private static final EntityDataAccessor<Boolean> FISHING = SynchedEntityData.defineId(FisherNeko.class, EntityDataSerializers.BOOLEAN);

    /** Fish delivered toward the quest, per player. */
    private final Map<UUID, Integer> delivered = new HashMap<>();
    private int catchTimer;
    private int showCatch;
    private @Nullable BlockPos water;

    public FisherNeko(EntityType<? extends FisherNeko> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3, new FishAtWaterGoal());
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.7));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FISHING, false);
    }

    /** Sitting at the water with the line out (the client draws it sitting). */
    /** Fish this player has brought toward the current quest (shown by Jade). */
    public int delivered(UUID player) {
        return delivered.getOrDefault(player, 0);
    }

    public boolean isFishing() {
        return entityData.get(FISHING);
    }

    @Override
    protected Coat randomCoat() {
        Coat[] coats = {Coat.GINGER, Coat.GRAY, Coat.WHITE, Coat.BLACK};
        return coats[random.nextInt(coats.length)];
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                        @Nullable SpawnGroupData data) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);
        int count = delivered.getOrDefault(player.getUUID(), 0);
        if (held.is(CatgirlTags.RAW_FISH)) {
            int taken = Math.min(held.getCount(), QUEST_FISH - count);
            held.consume(taken, player);
            count += taken;
            playSound(SoundEvents.GENERIC_EAT.value(), 0.8F, 1.2F);
            if (count >= QUEST_FISH) {
                count = 0;
                NekoData.give(serverPlayer, NekoData.rarePiece(this));
                ((ServerLevel) level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getEyeY() + 0.3, getZ(), 10, 0.4, 0.3, 0.4, 0.0);
                player.sendOverlayMessage(Component.translatable("message.catgirl.fisher_neko.reward"));
            } else {
                player.sendOverlayMessage(Component.translatable("message.catgirl.fisher_neko.progress", count, QUEST_FISH));
            }
            delivered.put(player.getUUID(), count);
        } else {
            player.sendOverlayMessage(Component.translatable("message.catgirl.fisher_neko.quest", count, QUEST_FISH));
            playAmbientSound();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (showCatch > 0 && --showCatch == 0) {
            setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
        if (isFishing() && water != null && --catchTimer <= 0) {
            catchTimer = CATCH_MIN_TICKS + random.nextInt(CATCH_EXTRA_TICKS);
            level.sendParticles(ParticleTypes.SPLASH, water.getX() + 0.5, water.getY() + 1.0, water.getZ() + 0.5, 12, 0.3, 0.1, 0.3, 0.1);
            level.playSound(null, water, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.5F, 1.0F);
            setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(random.nextBoolean() ? Items.COD : Items.SALMON));
            showCatch = SHOW_CATCH_TICKS;
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        ValueOutput.TypedOutputList<String> list = output.list("catgirl_delivered", com.mojang.serialization.Codec.STRING);
        delivered.forEach((uuid, count) -> list.add(uuid + "=" + count));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        delivered.clear();
        input.listOrEmpty("catgirl_delivered", com.mojang.serialization.Codec.STRING).forEach(entry -> {
            int split = entry.indexOf('=');
            if (split > 0) {
                delivered.put(UUID.fromString(entry.substring(0, split)), Integer.parseInt(entry.substring(split + 1)));
            }
        });
    }

    /** Walks to a spot at the water's edge, sits down facing the water and fishes for a minute or few. */
    private final class FishAtWaterGoal extends Goal {
        private static final int SEARCH_RANGE = 8;
        private static final int START_CHANCE = 60;
        private @Nullable BlockPos spot;
        private int fishingTicks;

        FishAtWaterGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return random.nextInt(START_CHANCE) == 0 && findSpot();
        }

        @Override
        public boolean canContinueToUse() {
            return spot != null && fishingTicks > 0;
        }

        @Override
        public void start() {
            fishingTicks = FISHING_MIN_TICKS + random.nextInt(FISHING_EXTRA_TICKS);
            catchTimer = CATCH_MIN_TICKS + random.nextInt(CATCH_EXTRA_TICKS);
            getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0.8);
        }

        @Override
        public void stop() {
            entityData.set(FISHING, false);
            spot = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (spot == null || water == null) {
                return;
            }
            boolean arrived = distanceToSqr(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5) < 1.5;
            if (arrived) {
                getNavigation().stop();
                getLookControl().setLookAt(water.getX() + 0.5, water.getY() + 0.5, water.getZ() + 0.5);
                entityData.set(FISHING, true);
                fishingTicks--;
            } else if (getNavigation().isDone()) {
                fishingTicks = 0; // couldn't get there
            }
        }

        /** A dry, standable block next to surface water within the search range. */
        private boolean findSpot() {
            BlockPos origin = blockPosition();
            for (int i = 0; i < 20; i++) {
                BlockPos candidate = origin.offset(random.nextInt(SEARCH_RANGE * 2 + 1) - SEARCH_RANGE, 0,
                        random.nextInt(SEARCH_RANGE * 2 + 1) - SEARCH_RANGE);
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    BlockPos wet = candidate.relative(side).below();
                    if (level().getFluidState(wet).is(FluidTags.WATER) && level().isEmptyBlock(candidate)
                            && level().getBlockState(candidate.below()).isFaceSturdy(level(), candidate.below(), Direction.UP)) {
                        spot = candidate;
                        water = wet;
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
