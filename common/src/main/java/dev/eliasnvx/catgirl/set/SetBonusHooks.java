package dev.eliasnvx.catgirl.set;

import dev.eliasnvx.catgirl.network.CatgirlNetwork;
import dev.architectury.networking.NetworkManager;

/** Entry points the mixins and the rest of the mod call into; keeps {@link SetBalance} package-private. */
public final class SetBonusHooks {

    private SetBonusHooks() {
    }

    public static void register() {
        SetBonuses.register();
        NightVision.register();
        CatgirlNetwork.registerC2S(NightVision.Payload.TYPE, NightVision.Payload.STREAM_CODEC, NightVision.Payload::handle);
        CatgirlNetwork.registerS2C(LandingPayload.TYPE, LandingPayload.STREAM_CODEC, LandingPayload::handle);
    }

    public static float quietStepVolume() {
        return SetBalance.QUIET_STEP_VOLUME;
    }
}
