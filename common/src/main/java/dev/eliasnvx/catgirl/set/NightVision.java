package dev.eliasnvx.catgirl.set;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.catgirl.CatgirlMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Full Neko: cat eyes on a key (N by default): 10 s of night vision, then a 30 s cooldown. Without the
 * set or while cooling down, the action bar says why.
 */
public final class NightVision {

    private static final int TICKS_PER_SECOND = 20;
    /** Last use per player, in game time. Not saved: a restart resets the cooldown, which is harmless. */
    private static final Map<UUID, Long> LAST_USE = new HashMap<>();

    private NightVision() {
    }

    public static void register() {
        PlayerEvent.PLAYER_QUIT.register(player -> LAST_USE.remove(player.getUUID()));
    }

    static void use(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        if (!CatSets.has(player, CatSet.FULL_NEKO)) {
            player.displayClientMessage(Component.translatable("message.catgirl.night_vision.needs_set",
                    Component.translatable(CatSet.FULL_NEKO.translationKey())), true);
            return;
        }
        long now = player.level().getGameTime();
        Long last = LAST_USE.get(player.getUUID());
        if (last != null && now - last < SetBalance.NIGHT_VISION_COOLDOWN_TICKS) {
            long seconds = (SetBalance.NIGHT_VISION_COOLDOWN_TICKS - (now - last) + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
            player.displayClientMessage(Component.translatable("message.catgirl.night_vision.cooldown", seconds), true);
            return;
        }
        LAST_USE.put(player.getUUID(), now);
        // no swirl particles (they'd fill a dark cave), but the effect icon shows the time left
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, SetBalance.NIGHT_VISION_TICKS, 0, false, false, true));
    }

    /** C2S: the night vision key was pressed. The server checks the set and the cooldown. */
    public record Payload() implements CustomPacketPayload {

        public static final Payload INSTANCE = new Payload();
        public static final Type<Payload> TYPE = new Type<>(CatgirlMod.id("night_vision"));
        public static final StreamCodec<ByteBuf, Payload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<Payload> type() {
            return TYPE;
        }

        public static void handle(Payload payload, NetworkManager.PacketContext context) {
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    use(player);
                }
            });
        }
    }
}
