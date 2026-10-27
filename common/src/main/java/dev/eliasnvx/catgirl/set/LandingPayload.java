package dev.eliasnvx.catgirl.set;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.network.CatgirlNetwork;
import dev.eliasnvx.catgirl.network.CatgirlPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** S2C: this player just landed on all fours (Alley Cat); clients play the crouch pose for a moment. */
public record LandingPayload(int entityId) implements CatgirlPacket {

    public static final ResourceLocation ID = CatgirlMod.id("landing");

    /** Set by the client entry point; the payload class itself stays loadable on a dedicated server. */
    public static volatile Receiver onClient = (entityId, ticks) -> {
    };

    @FunctionalInterface
    public interface Receiver {
        void landed(int entityId, int poseTicks);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
    }

    public static LandingPayload read(FriendlyByteBuf buf) {
        return new LandingPayload(buf.readVarInt());
    }

    public static void handle(LandingPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> onClient.landed(payload.entityId(), SetBonuses.landingPoseTicks()));
    }

    static void sendToTrackingAndSelf(ServerPlayer player) {
        Packet<ClientGamePacketListener> packet = CatgirlNetwork.toClientPacket(new LandingPayload(player.getId()));
        // 1.20.1 has no filtered tracking broadcast: players in view of the chunk are a superset of the trackers
        for (ServerPlayer watcher : ((ServerLevel) player.level()).getChunkSource().chunkMap.getPlayers(player.chunkPosition(), false)) {
            if (watcher != player && CatgirlNetwork.canReceive(watcher, ID)) {
                watcher.connection.send(packet);
            }
        }
        if (CatgirlNetwork.canReceive(player, ID)) {
            player.connection.send(packet);
        }
    }
}
