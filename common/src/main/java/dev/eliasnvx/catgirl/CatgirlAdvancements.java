package dev.eliasnvx.catgirl;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.level.ServerPlayer;

/**
 * Advancements granted from code: their criteria use the {@code minecraft:impossible} trigger, and the mod awards them
 * when the thing happens (simpler than a custom trigger type for each).
 */
public final class CatgirlAdvancements {

    public static final String FIRST_MEOW = "first_meow";
    public static final String HERDING_CATS = "herding_cats";
    public static final String FULL_NEKO = "full_neko";
    /** Nine criteria {@code landing_1..9}, one per safe landing from a height: the screen shows the progress. */
    public static final String NINE_LIVES = "nine_lives";
    public static final String BOX_ENJOYER = "box_enjoyer";
    public static final String CATNIP_OVERDOSE = "catnip_overdose";
    public static final String DEFAULT_CRITERION = "done";

    private CatgirlAdvancements() {
    }

    public static void award(ServerPlayer player, String id) {
        award(player, id, DEFAULT_CRITERION);
    }

    public static void award(ServerPlayer player, String id, String criterion) {
        Advancement advancement = player.level().getServer().getAdvancements().getAdvancement(CatgirlMod.id(id));
        if (advancement != null) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    /** Awards the first criterion of a multi-step advancement that isn't done yet (one more step of progress). */
    public static void awardNext(ServerPlayer player, String id) {
        Advancement advancement = player.level().getServer().getAdvancements().getAdvancement(CatgirlMod.id(id));
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
            return;
        }
    }
}
