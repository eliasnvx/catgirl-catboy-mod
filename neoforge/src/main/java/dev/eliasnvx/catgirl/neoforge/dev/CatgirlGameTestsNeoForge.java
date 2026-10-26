package dev.eliasnvx.catgirl.neoforge.dev;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.gametest.CosmeticSlotGameTests;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Registers the shared {@link CosmeticSlotGameTests} on NeoForge. Started by {@code CatgirlModNeoForge} only when
 * GameTests are enabled ({@code ./gradlew :neoforge:runGameTestServer}, part of {@code build}); not in the released jar.
 */
public final class CatgirlGameTestsNeoForge {

    /** 3x3x3 of air, {@code data/catgirl/structure/empty.nbt} (dev only, like this class). */
    private static final Identifier EMPTY_STRUCTURE = CatgirlMod.id("empty");
    private static final int MAX_TICKS = 100;

    private CatgirlGameTestsNeoForge() {
    }

    public static void register(IEventBus modBus) {
        DeferredRegister<Consumer<GameTestHelper>> functions = DeferredRegister.create(Registries.TEST_FUNCTION, CatgirlMod.MOD_ID);
        List<DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> holders = new ArrayList<>();
        for (CosmeticSlotGameTests.Entry entry : CosmeticSlotGameTests.ALL) {
            holders.add(functions.register(entry.name(), entry::body));
        }
        functions.register(modBus);

        modBus.addListener((RegisterGameTestsEvent event) -> {
            Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(CatgirlMod.id("default"));
            for (DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> holder : holders) {
                event.registerTest(holder.getId(), new FunctionGameTestInstance(holder.getKey(),
                        new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, 0, true)));
            }
        });
    }
}
