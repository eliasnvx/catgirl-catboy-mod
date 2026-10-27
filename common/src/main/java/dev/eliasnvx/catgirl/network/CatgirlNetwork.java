package dev.eliasnvx.catgirl.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.eliasnvx.catgirl.meow.MeowPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class CatgirlNetwork {

    private CatgirlNetwork() {
    }

    public static void register() {
        registerS2C(CosmeticsSyncPayload.TYPE, CosmeticsSyncPayload.STREAM_CODEC, CosmeticsSyncPayload::handle);
        registerC2S(MeowPayload.TYPE, MeowPayload.STREAM_CODEC, MeowPayload::handle);
    }

    /** Architectury 13: C2S receivers are registered on both sides (the client only needs the type). */
    public static <T extends CustomPacketPayload> void registerC2S(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, NetworkManager.NetworkReceiver<T> receiver) {
        NetworkManager.registerReceiver(NetworkManager.c2s(), type, codec, receiver);
    }

    /**
     * Architectury 13: an S2C receiver is registered on the client; a dedicated server only registers the payload type.
     * The handlers use common classes only, so referencing them on the server is safe.
     */
    public static <T extends CustomPacketPayload> void registerS2C(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, NetworkManager.NetworkReceiver<T> receiver) {
        if (Platform.getEnvironment() == Env.CLIENT) {
            NetworkManager.registerReceiver(NetworkManager.s2c(), type, codec, receiver);
        } else {
            NetworkManager.registerS2CPayloadType(type, codec);
        }
    }
}
