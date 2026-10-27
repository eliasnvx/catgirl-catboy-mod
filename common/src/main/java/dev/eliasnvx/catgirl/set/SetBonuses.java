package dev.eliasnvx.catgirl.set;

import dev.architectury.event.events.common.TickEvent;
import dev.eliasnvx.catgirl.CatgirlAdvancements;
import dev.eliasnvx.catgirl.CatgirlEvents;
import dev.eliasnvx.catgirl.CatgirlMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Applies the set bonuses on the server: attribute modifiers while a set is worn, set advancements, House Cat's
 * rest (its follow goal is added with the other cat goals in CatgirlEntities), Alley Cat's landings. Bonuses that need the client (quiet steps, the landing pose) read
 * {@link CatSets} there.
 */
public final class SetBonuses {

    private static final CatSet[] SETS = CatSet.values();
    private static final ResourceLocation JUMP_MODIFIER = CatgirlMod.id("full_neko_jump");
    private static final ResourceLocation SAFE_FALL_MODIFIER = CatgirlMod.id("alley_cat_safe_fall");
    private static final int LANDING_PARTICLES = 6;
    private static final double LANDING_SPREAD = 0.3;
    private static final float LANDING_VOLUME = 0.6F;
    private static final float LANDING_PITCH = 1.4F;

    /** Per-player state; a respawned player is a new object and starts fresh (its attributes are fresh too). */
    private static final class State {
        int sets = -1;
        double fall;
        boolean wasOnGround = true;
    }

    private static final Map<Player, State> STATES = new WeakHashMap<>();

    private SetBonuses() {
    }

    public static void register() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                tick(serverPlayer);
            }
        });
    }

    private static void tick(ServerPlayer player) {
        State state = STATES.computeIfAbsent(player, p -> new State());
        int sets = CatSets.mask(player);
        if (sets != state.sets) {
            int previous = state.sets < 0 ? 0 : state.sets;
            state.sets = sets;
            changed(player, previous, sets);
        }
        if (CatSets.has(player, CatSet.HOUSE_CAT) && player.tickCount % SetBalance.REST_CHECK_TICKS == 0) {
            restNearCat(player);
        }
        landing(player, state);
    }

    private static void changed(ServerPlayer player, int before, int after) {
        modifier(player.getAttribute(Attributes.JUMP_STRENGTH), JUMP_MODIFIER, SetBalance.JUMP_BONUS,
                (after & bit(CatSet.FULL_NEKO)) != 0);
        modifier(player.getAttribute(Attributes.SAFE_FALL_DISTANCE), SAFE_FALL_MODIFIER, SetBalance.SAFE_FALL_BONUS,
                (after & bit(CatSet.ALLEY_CAT)) != 0);
        if ((after & bit(CatSet.FULL_NEKO)) != 0) {
            CatgirlAdvancements.award(player, CatgirlAdvancements.FULL_NEKO);
        }
        for (CatSet set : SETS) {
            boolean was = (before & bit(set)) != 0;
            boolean is = (after & bit(set)) != 0;
            if (was != is) {
                CatgirlEvents.SET_BONUS_CHANGED.invoker().changed(player, set, is);
            }
        }
    }

    private static int bit(CatSet set) {
        return 1 << set.ordinal();
    }

    private static void modifier(AttributeInstance attribute, ResourceLocation id, double amount, boolean on) {
        if (attribute == null) {
            return;
        }
        if (on && !attribute.hasModifier(id)) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        } else if (!on && attribute.hasModifier(id)) {
            attribute.removeModifier(id);
        }
    }

    /**
     * House Cat: a tamed cat nearby counts as rest, so phantoms don't come. ("No phantoms after sleeping near a cat"
     * would be what vanilla already does: phantoms come to players who don't sleep, and avoid cats anyway.)
     */
    private static void restNearCat(ServerPlayer player) {
        if (!player.level().getEntitiesOfClass(Cat.class, player.getBoundingBox().inflate(SetBalance.REST_CAT_RANGE),
                Cat::isTame).isEmpty()) {
            player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        }
    }

    /**
     * Alley Cat: a fall from {@link SetBalance#LANDING_MIN_FALL} blocks or more ends on all fours: a soft thud, dust,
     * the crouch pose for everyone watching, and one step of "Nine Lives" if it didn't hurt. The server player's fall
     * distance is kept by the movement packets; it is reset in the tick the player lands, so the last airborne value
     * is remembered.
     */
    private static void landing(ServerPlayer player, State state) {
        boolean onGround = player.onGround();
        if (!onGround) {
            state.fall = Math.max(state.fall, player.fallDistance);
        } else if (!state.wasOnGround) {
            if (state.fall >= SetBalance.LANDING_MIN_FALL && CatSets.has(player, CatSet.ALLEY_CAT) && !player.isFallFlying()) {
                land(player, state.fall);
            }
            state.fall = 0.0;
        }
        state.wasOnGround = onGround;
    }

    private static void land(ServerPlayer player, double fall) {
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player, SoundEvents.WOOL_FALL, SoundSource.PLAYERS, LANDING_VOLUME, LANDING_PITCH);
        level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.1, player.getZ(), LANDING_PARTICLES,
                LANDING_SPREAD, 0.05, LANDING_SPREAD, 0.02);
        LandingPayload.sendToTrackingAndSelf(player);
        if (fall <= safeFall(player)) {
            CatgirlAdvancements.awardNext(player, CatgirlAdvancements.NINE_LIVES);
        }
    }

    private static double safeFall(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(Attributes.SAFE_FALL_DISTANCE);
        return attribute == null ? 0.0 : attribute.getValue();
    }

    static int landingPoseTicks() {
        return SetBalance.LANDING_POSE_TICKS;
    }
}
