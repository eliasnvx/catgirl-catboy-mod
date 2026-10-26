package dev.eliasnvx.catgirl.set;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.catgirl.CatgirlMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** S2C: this player just landed on all fours (Alley Cat); clients play the crouch pose for a moment. */
public record LandingPayload(int entityId) implements CustomPacketPayload {

    public static final Type<LandingPayload> TYPE = new Type<>(CatgirlMod.id("landing"));

    public static final StreamCodec<ByteBuf, LandingPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(LandingPayload::new, LandingPayload::entityId);

    /** Set by the client entry point; the payload class itself stays loadable on a dedicated server. */
    public static volatile Receiver onClient = (entityId, ticks) -> {
    };

    @FunctionalInterface
    public interface Receiver {
        void landed(int entityId, int poseTicks);
    }

    @Override
    public Type<LandingPayload> type() {
        return TYPE;
    }

    public static void handle(LandingPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> onClient.landed(payload.entityId(), SetBonuses.landingPoseTicks()));
    }

    @SuppressWarnings("unchecked")
    static void sendToTrackingAndSelf(ServerPlayer player) {
        Packet<? super ClientGamePacketListener> packet = (Packet<? super ClientGamePacketListener>)
                NetworkManager.toPacket(NetworkManager.s2c(), new LandingPayload(player.getId()), player.registryAccess());
        ((ServerLevel) player.level()).getChunkSource().sendToTrackingPlayersFiltered(player, packet,
                receiver -> NetworkManager.canPlayerReceive(receiver, TYPE));
        if (NetworkManager.canPlayerReceive(player, TYPE)) {
            player.connection.send(packet);
        }
    }
}
