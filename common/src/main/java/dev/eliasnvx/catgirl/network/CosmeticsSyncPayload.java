package dev.eliasnvx.catgirl.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.cosmetic.CosmeticInventory;
import dev.eliasnvx.catgirl.cosmetic.CosmeticsManager;
import dev.eliasnvx.catgirl.platform.PlatformHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** S2C: full cosmetic state of one player. Sent on change, on tracking start and on join/respawn/dimension change. */
public record CosmeticsSyncPayload(int entityId, CosmeticInventory cosmetics) implements CatgirlPacket {

    public static final ResourceLocation ID = CatgirlMod.id("cosmetics_sync");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeMap(cosmetics.items(), FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeItem);
    }

    public static CosmeticsSyncPayload read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        Map<ResourceLocation, ItemStack> items = buf.readMap(HashMap::new, FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readItem);
        return new CosmeticsSyncPayload(entityId, new CosmeticInventory(items));
    }

    public static CosmeticsSyncPayload of(Player player) {
        return new CosmeticsSyncPayload(player.getId(), CosmeticsManager.get(player));
    }

    /** Receiver (client side). Uses only common classes; registered on the client only (see CatgirlNetwork). */
    public static void handle(CosmeticsSyncPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> EarlySyncs.withPlayer(context, self -> {
            Entity entity = self.getId() == payload.entityId() ? self : self.level().getEntity(payload.entityId());
            if (entity instanceof Player player) {
                PlatformHelper.setCosmetics(player, payload.cosmetics());
            }
        }));
    }

    /** Clients without the mod cannot decode the payload (and Forge throws if we try). */
    public static boolean canReceive(ServerPlayer player) {
        return CatgirlNetwork.canReceive(player, ID);
    }

    public static void sendTo(ServerPlayer receiver, Player about) {
        if (canReceive(receiver)) {
            CatgirlNetwork.sendToPlayer(receiver, of(about));
        }
    }

    public static void sendToTrackingAndSelf(ServerPlayer player) {
        Packet<ClientGamePacketListener> packet = CatgirlNetwork.toClientPacket(of(player));
        // no filtered tracking broadcast; players in view of the chunk (the self included) are a superset of the
        // trackers, and the receiver ignores entity ids it does not know.
        for (ServerPlayer watcher : ((ServerLevel) player.level()).getChunkSource().chunkMap.getPlayers(player.chunkPosition(), false)) {
            if (watcher != player && canReceive(watcher)) {
                watcher.connection.send(packet);
            }
        }
        if (canReceive(player)) {
            player.connection.send(packet);
        }
    }
}
