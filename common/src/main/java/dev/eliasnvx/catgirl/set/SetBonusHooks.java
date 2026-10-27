package dev.eliasnvx.catgirl.set;

import dev.eliasnvx.catgirl.network.CatgirlNetwork;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Entry points the mixins and the rest of the mod call into; keeps {@link SetBalance} package-private. */
public final class SetBonusHooks {

    private SetBonusHooks() {
    }

    public static void register() {
        SetBonuses.register();
        NightVision.register();
        CatgirlNetwork.registerC2S(NightVision.Payload.ID, buf -> NightVision.Payload.INSTANCE, NightVision.Payload::handle);
        CatgirlNetwork.registerS2C(LandingPayload.ID, LandingPayload::read, LandingPayload::handle);
    }

    public static float quietStepVolume() {
        return SetBalance.QUIET_STEP_VOLUME;
    }

    /** Full Neko: added to the jump power (later versions: a jump strength modifier). */
    public static float jumpBonus(LivingEntity entity) {
        return entity instanceof Player player && CatSets.has(player, CatSet.FULL_NEKO) ? (float) SetBalance.JUMP_BONUS : 0.0F;
    }

    /** Alley Cat: blocks more a fall may be without damage (later versions: a safe fall distance modifier). */
    public static float safeFallBonus(LivingEntity entity) {
        return entity instanceof Player player && CatSets.has(player, CatSet.ALLEY_CAT) ? (float) SetBalance.SAFE_FALL_BONUS : 0.0F;
    }
}
