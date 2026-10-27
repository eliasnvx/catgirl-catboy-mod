package dev.eliasnvx.catgirl.neoforge.dev;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.gametest.CosmeticSlotGameTests;
import dev.eliasnvx.catgirl.gametest.DataGameTests;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;
import java.util.stream.Stream;

/**
 * Registers the shared {@link CosmeticSlotGameTests} and {@link DataGameTests} on NeoForge. Started by {@code CatgirlModNeoForge} only when
 * GameTests are enabled ({@code ./gradlew :neoforge:runGameTestServer}, part of {@code build}); not in the released jar.
 * Public because the game test registry invokes {@link #tests()} reflectively.
 */
public final class CatgirlGameTestsNeoForge {

    /** 3x3x3 of air, {@code data/catgirl/structure/empty.nbt} (dev only, like this class). */
    private static final String EMPTY_STRUCTURE = CatgirlMod.id("empty").toString();
    private static final int MAX_TICKS = 100;
    private static final long SETUP_TICKS = 0L;

    private CatgirlGameTestsNeoForge() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterGameTestsEvent event) -> event.register(CatgirlGameTestsNeoForge.class));
    }

    /** One test function per shared test, all in the {@code catgirl} batch. */
    @GameTestGenerator
    public static List<TestFunction> tests() {
        return Stream.concat(CosmeticSlotGameTests.ALL.stream(), DataGameTests.ALL.stream())
                .map(entry -> new TestFunction(CatgirlMod.MOD_ID, CatgirlMod.MOD_ID + "." + entry.name(), EMPTY_STRUCTURE,
                        MAX_TICKS, SETUP_TICKS, true, entry.body()))
                .toList();
    }
}
