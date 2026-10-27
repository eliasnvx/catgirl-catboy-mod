package dev.eliasnvx.catgirl.fabric.gametest;

import dev.eliasnvx.catgirl.gametest.CosmeticSlotGameTests;
import dev.eliasnvx.catgirl.gametest.DataGameTests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.Arrays;

/**
 * Fabric glue for the shared {@link CosmeticSlotGameTests} and {@link DataGameTests} (server GameTests, part of
 * {@code build}). One method per entry of their {@code ALL} lists; {@link #everySharedTestRegistered} catches a missing one. Every test
 * runs in Fabric's empty 8x8x8 template.
 */
public final class CosmeticSlotGameTestsFabric implements FabricGameTest {

    @GameTest(template = EMPTY_STRUCTURE)
    public void shiftClick(GameTestHelper helper) {
        CosmeticSlotGameTests.shiftClick(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void cursorClicks(GameTestHelper helper) {
        CosmeticSlotGameTests.cursorClicks(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void numberKeys(GameTestHelper helper) {
        CosmeticSlotGameTests.numberKeys(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void drag(GameTestHelper helper) {
        CosmeticSlotGameTests.drag(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void doubleClick(GameTestHelper helper) {
        CosmeticSlotGameTests.doubleClick(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void throwAndMiddleClick(GameTestHelper helper) {
        CosmeticSlotGameTests.throwAndMiddleClick(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void rightClickEquip(GameTestHelper helper) {
        CosmeticSlotGameTests.rightClickEquip(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void creativeSlotPacket(GameTestHelper helper) {
        CosmeticSlotGameTests.creativeSlotPacket(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void deathDropsOnce(GameTestHelper helper) {
        CosmeticSlotGameTests.deathDropsOnce(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void keepInventory(GameTestHelper helper) {
        CosmeticSlotGameTests.keepInventory(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void leavingTheEnd(GameTestHelper helper) {
        CosmeticSlotGameTests.leavingTheEnd(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void dataLoads(GameTestHelper helper) {
        DataGameTests.dataLoads(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void everySharedTestRegistered(GameTestHelper helper) {
        long wrappers = Arrays.stream(getClass().getMethods())
                .filter(method -> method.isAnnotationPresent(GameTest.class))
                .count() - 1;
        long shared = CosmeticSlotGameTests.ALL.size() + DataGameTests.ALL.size();
        helper.assertTrue(wrappers == shared, "Fabric wrappers for the shared tests: " + wrappers + ", shared tests: " + shared);
        helper.succeed();
    }
}
