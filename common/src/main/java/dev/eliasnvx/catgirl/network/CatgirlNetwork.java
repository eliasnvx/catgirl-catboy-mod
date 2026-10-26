package dev.eliasnvx.catgirl.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.catgirl.meow.MeowPayload;

public final class CatgirlNetwork {

    private CatgirlNetwork() {
    }

    public static void register() {
        NetworkManager.registerS2C(CosmeticsSyncPayload.TYPE, CosmeticsSyncPayload.STREAM_CODEC, CosmeticsSyncPayload::handle);
        NetworkManager.registerC2S(MeowPayload.TYPE, MeowPayload.STREAM_CODEC, MeowPayload::handle);
    }
}
