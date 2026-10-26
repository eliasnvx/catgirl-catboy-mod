package dev.eliasnvx.catgirl;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.eliasnvx.catgirl.set.CatSet;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side events for addons, laid down early; a separate catgirl-api may follow after 1.0. Not a stable API yet.
 */
public final class CatgirlEvents {

    /** A player meowed (after the cooldown check). */
    public static final Event<Meow> MEOW = EventFactory.createLoop();

    /** A set started or stopped being worn. */
    public static final Event<SetBonusChanged> SET_BONUS_CHANGED = EventFactory.createLoop();

    @FunctionalInterface
    public interface Meow {
        void meow(ServerPlayer player);
    }

    @FunctionalInterface
    public interface SetBonusChanged {
        void changed(ServerPlayer player, CatSet set, boolean active);
    }

    private CatgirlEvents() {
    }
}
