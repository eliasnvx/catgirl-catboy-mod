package dev.eliasnvx.catgirl.entity.neko;

import dev.eliasnvx.catgirl.item.Stacks;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.registry.CatgirlItems;
import dev.eliasnvx.catgirl.registry.CatgirlSounds;
import dev.eliasnvx.catgirl.set.CatSet;
import dev.eliasnvx.catgirl.set.CatSets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.nbt.CompoundTag;

/**
 * Bakeneko: a rare night prowler in forests and bamboo, the mod's one "bad" mob, but harmless. Come too
 * close without the Full Neko set and it snatches the item in your hand and runs. Someone in Full Neko gets it back
 * for catnip. It never loses the item: it drops it when killed or when it slinks off at dawn.
 */
public class Bakeneko extends NekoMob {

    // Balance (move to catgirl-common config once the config exists)
    static final double STEAL_RANGE = 2.5;
    static final int STEAL_COOLDOWN_TICKS = 30 * 20;
    static final int CALM_TICKS = 5 * 60 * 20;
    static final int FLEE_TICKS = 10 * 20;
    static final int SEARCH_INTERVAL = 10;
    /** Darkness needed to prowl (spawning). */
    static final int MAX_SPAWN_LIGHT = 7;

    private int stealCooldown;
    private int fleeTicks;

    public Bakeneko(EntityType<? extends Bakeneko> type, Level level) {
        super(type, level);
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return NekoMob.createAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.32);
    }

    /** Dark spots at night, never in peaceful. */
    public static boolean checkSpawnRules(EntityType<? extends Mob> type, ServerLevelAccessor level, MobSpawnType reason,
                                          BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && level.getMaxLocalRawBrightness(pos) <= MAX_SPAWN_LIGHT
                && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 12.0F, 1.2, 1.6, player -> fleeTicks > 0));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
    }

    @Override
    protected Coat randomCoat() {
        Coat[] coats = {Coat.BLACK, Coat.BLACK, Coat.GRAY, Coat.CALICO};
        return coats[random.nextInt(coats.length)];
    }

    public boolean hasLoot() {
        return !getMainHandItem().isEmpty();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) level();
        if (stealCooldown > 0) {
            stealCooldown--;
        }
        if (fleeTicks > 0) {
            fleeTicks--;
        }
        if (level.isDay() && level.canSeeSky(blockPosition())) {
            vanish(level); // gone by morning, loot left behind
            return;
        }
        if (tickCount % SEARCH_INTERVAL == 0 && !hasLoot() && stealCooldown <= 0) {
            Player target = level.getNearestPlayer(this, STEAL_RANGE);
            if (target instanceof ServerPlayer player && !player.isCreative() && !player.isSpectator()
                    && !CatSets.has(player, CatSet.FULL_NEKO) && !player.getMainHandItem().isEmpty()) {
                steal(level, player);
            }
        }
    }

    private void steal(ServerLevel level, ServerPlayer player) {
        ItemStack loot = player.getMainHandItem().copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        setItemSlot(EquipmentSlot.MAINHAND, loot);
        fleeTicks = FLEE_TICKS;
        stealCooldown = STEAL_COOLDOWN_TICKS;
        level.playSound(null, this, CatgirlSounds.BAKENEKO_HISS.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 8, 0.3, 0.4, 0.3, 0.02);
        player.displayClientMessage(Component.translatable("message.catgirl.bakeneko.stole", loot.getHoverName()), true);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer) || !hasLoot()) {
            return super.mobInteract(player, hand);
        }
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(CatgirlItems.CATNIP.get())) {
            return super.mobInteract(player, hand);
        }
        if (!CatSets.has(player, CatSet.FULL_NEKO)) {
            playSound(CatgirlSounds.BAKENEKO_HISS.get(), 1.0F, 1.2F);
            player.displayClientMessage(Component.translatable("message.catgirl.bakeneko.distrust"), true);
            return InteractionResult.SUCCESS;
        }
        Stacks.consume(held, 1, player);
        NekoData.give(serverPlayer, getMainHandItem().copy());
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        stealCooldown = CALM_TICKS;
        fleeTicks = 0;
        playSound(CatgirlSounds.CAT_BED_PURR.get(), 1.0F, 1.0F);
        ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, getX(), getEyeY() + 0.4, getZ(), 4, 0.4, 0.2, 0.4, 0.0);
        player.displayClientMessage(Component.translatable("message.catgirl.bakeneko.returned"), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        fleeTicks = FLEE_TICKS;
        return super.hurt(source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean killedByPlayer) {
        super.dropCustomDeathLoot(source, looting, killedByPlayer);
        if (level() instanceof ServerLevel level) {
            dropLoot(level);
        }
    }

    private void dropLoot(ServerLevel level) {
        if (hasLoot()) {
            spawnAtLocation(getMainHandItem().copy());
            setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    private void vanish(ServerLevel level) {
        dropLoot(level);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        discard();
    }

    /** Never despawns with someone's item. */
    @Override
    public boolean requiresCustomPersistence() {
        return hasLoot() || super.requiresCustomPersistence();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        output.putInt("catgirl_steal_cooldown", stealCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);
        stealCooldown = input.getInt("catgirl_steal_cooldown");
    }
}
