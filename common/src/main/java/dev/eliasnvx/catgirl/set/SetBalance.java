package dev.eliasnvx.catgirl.set;

/** Balance values of the set bonuses. Move to catgirl-common config once the config exists. */
final class SetBalance {

    /** Full Neko: added to the 0.42 jump strength, about a third of a block higher. */
    static final double JUMP_BONUS = 0.06;
    /** Full Neko or paw slippers: step sound volume multiplier. */
    static final float QUIET_STEP_VOLUME = 0.35F;
    static final int NIGHT_VISION_TICKS = 10 * 20;
    static final int NIGHT_VISION_COOLDOWN_TICKS = 30 * 20;

    /** House Cat: how far cats and ocelots come from to follow, and where they stop. */
    static final double FOLLOW_RANGE = 10.0;
    static final double FOLLOW_STOP = 2.5;
    static final double FOLLOW_SPEED = 0.9;
    /** House Cat: a tamed cat this close counts as a night's sleep (no phantoms). */
    static final double REST_CAT_RANGE = 16.0;
    static final int REST_CHECK_TICKS = 20;

    /** Alley Cat: added to the 3 block safe fall distance, so falls up to 10 blocks don't hurt. */
    static final double SAFE_FALL_BONUS = 7.0;
    /** Alley Cat: a fall this high ends in the four-paw landing. */
    static final double LANDING_MIN_FALL = 4.0;
    static final int LANDING_POSE_TICKS = 8;

    private SetBalance() {
    }
}
