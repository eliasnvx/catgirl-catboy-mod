package dev.eliasnvx.catgirl.meow;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.catgirl.CatgirlMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** C2S: the meow key was pressed. Carries no data; the server decides everything (cooldown, who hears it). */
public record MeowPayload() implements CustomPacketPayload {

    public static final MeowPayload INSTANCE = new MeowPayload();

    public static final Type<MeowPayload> TYPE = new Type<>(CatgirlMod.id("meow"));

    public static final StreamCodec<ByteBuf, MeowPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<MeowPayload> type() {
        return TYPE;
    }

    public static void handle(MeowPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) {
                Meow.meow(player);
            }
        });
    }
}
