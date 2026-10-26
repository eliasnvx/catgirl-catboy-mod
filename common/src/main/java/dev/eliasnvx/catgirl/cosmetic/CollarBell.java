package dev.eliasnvx.catgirl.cosmetic;

import dev.architectury.event.events.common.TickEvent;
import dev.eliasnvx.catgirl.registry.CatgirlTags;
import dev.eliasnvx.catgirl.registry.CatgirlSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Bells (the bell collar, the bell headband: #catgirl:bells) jingle when their wearer jumps. Server
 * side, so everyone nearby hears it. A jump is leaving the ground while rising; falling off a ledge doesn't count. Measured by position, because a server player's
 * velocity is not kept up to date (the client moves it).
 */
public final class CollarBell {

    private static final float VOLUME = 0.5F;
    private static final float PITCH_MIN = 0.9F;
    private static final float PITCH_SPREAD = 0.2F;
    /** Rise in the first tick of a jump (about 0.42); walking up a slab or stairs stays below it. */
    private static final double JUMP_RISE = 0.2;

    private record Last(boolean onGround, double y) {
    }

    private static final Map<Player, Last> LAST = new WeakHashMap<>();

    private CollarBell() {
    }

    public static void register() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                tick(serverPlayer);
            }
        });
    }

    private static void tick(ServerPlayer player) {
        boolean onGround = player.onGround();
        Last last = LAST.put(player, new Last(onGround, player.getY()));
        if (last == null || !last.onGround() || onGround || player.getY() - last.y() < JUMP_RISE) {
            return;
        }
        CosmeticInventory worn = CosmeticsManager.worn(player);
        if (worn.get(CatSlot.NECK).is(CatgirlTags.BELLS) || worn.get(CatSlot.HEAD).is(CatgirlTags.BELLS)) {
            player.level().playSound(null, player, CatgirlSounds.COLLAR_JINGLE.get(), SoundSource.PLAYERS, VOLUME,
                    PITCH_MIN + player.getRandom().nextFloat() * PITCH_SPREAD);
        }
    }
}
