package dev.eliasnvx.catgirl.meow;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.eliasnvx.catgirl.CatgirlAdvancements;
import dev.eliasnvx.catgirl.CatgirlEvents;
import dev.eliasnvx.catgirl.set.CatSet;
import dev.eliasnvx.catgirl.set.CatSets;
import dev.eliasnvx.catgirl.registry.CatgirlSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The meow key: the player meows, notes float up, and cats and ocelots nearby answer one by one
 * and look at the player. Server side; the client only sends {@link MeowPayload}.
 */
public final class Meow {

    // Balance (move to catgirl-common config once the config exists)
    static final int COOLDOWN_TICKS = 30;
    static final double ANSWER_RADIUS = 12.0;
    static final int MAX_ANSWERS = 6;
    static final int ANSWER_DELAY_MIN = 6;
    static final int ANSWER_DELAY_MAX = 30;
    /** "Herding Cats": this many cats within this radius when you meow. */
    static final int HERD_SIZE = 5;
    static final double HERD_RADIUS = 8.0;

    private static final float VOLUME = 1.0F;
    private static final float PITCH_MIN = 1.05F;
    private static final float PITCH_SPREAD = 0.25F;
    private static final int NOTES = 3;
    private static final int HEARTS = 4;
    private static final double NOTE_SPREAD = 0.35;
    private static final double HEAD_ABOVE = 0.4;


    private record Answer(Mob cat, ServerPlayer player, long dueTick) {
    }

    private static final Map<UUID, Long> LAST_MEOW = new HashMap<>();
    private static final List<Answer> ANSWERS = new ArrayList<>();

    private Meow() {
    }

    public static void register() {
        TickEvent.SERVER_POST.register(Meow::tickAnswers);
        PlayerEvent.PLAYER_QUIT.register(player -> LAST_MEOW.remove(player.getUUID()));
    }

    /** Server thread. Ignored while the cooldown runs, so holding the key doesn't spam. */
    public static void meow(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        Long last = LAST_MEOW.get(player.getUUID());
        if (!player.isAlive() || player.isSpectator() || last != null && now - last < COOLDOWN_TICKS) {
            return;
        }
        LAST_MEOW.put(player.getUUID(), now);

        RandomSource random = player.getRandom();
        level.playSound(null, player, CatgirlSounds.PLAYER_MEOW.get(), SoundSource.PLAYERS, VOLUME,
                PITCH_MIN + random.nextFloat() * PITCH_SPREAD);
        level.sendParticles(ParticleTypes.NOTE, player.getX(), player.getEyeY() + HEAD_ABOVE, player.getZ(),
                NOTES, NOTE_SPREAD, NOTE_SPREAD / 2, NOTE_SPREAD, 1.0);
        if (CatSets.has(player, CatSet.FULL_NEKO)) { // hearts when meowing in the full set
            level.sendParticles(ParticleTypes.HEART, player.getX(), player.getEyeY() + HEAD_ABOVE, player.getZ(),
                    HEARTS, NOTE_SPREAD * 2, NOTE_SPREAD, NOTE_SPREAD * 2, 0.0);
        }

        AABB area = player.getBoundingBox().inflate(ANSWER_RADIUS);
        List<Mob> cats = new ArrayList<>(level.getEntitiesOfClass(Cat.class, area, Mob::isAlive));
        cats.addAll(level.getEntitiesOfClass(Ocelot.class, area, Mob::isAlive));
        cats.sort((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)));
        int herd = 0;
        for (int i = 0; i < cats.size(); i++) {
            Mob cat = cats.get(i);
            if (cat.distanceToSqr(player) <= HERD_RADIUS * HERD_RADIUS) {
                herd++;
            }
            if (i < MAX_ANSWERS) {
                long delay = ANSWER_DELAY_MIN + random.nextInt(ANSWER_DELAY_MAX - ANSWER_DELAY_MIN + 1);
                ANSWERS.add(new Answer(cat, player, now + delay));
            }
        }

        CatgirlAdvancements.award(player, CatgirlAdvancements.FIRST_MEOW);
        if (herd >= HERD_SIZE) {
            CatgirlAdvancements.award(player, CatgirlAdvancements.HERDING_CATS);
        }
        CatgirlEvents.MEOW.invoker().meow(player);
    }

    private static void tickAnswers(MinecraftServer server) {
        if (ANSWERS.isEmpty()) {
            return;
        }
        for (Iterator<Answer> it = ANSWERS.iterator(); it.hasNext(); ) {
            Answer answer = it.next();
            Mob cat = answer.cat();
            if (cat.isRemoved() || !cat.isAlive()) {
                it.remove();
                continue;
            }
            if (cat.level().getGameTime() >= answer.dueTick()) {
                cat.playAmbientSound();
                cat.getLookControl().setLookAt(answer.player(), cat.getMaxHeadYRot(), cat.getMaxHeadXRot());
                it.remove();
            }
        }
    }

}
