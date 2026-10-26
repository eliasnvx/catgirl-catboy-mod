package dev.eliasnvx.catgirl.fabric.gametest;

import dev.eliasnvx.catgirl.gametest.CosmeticSlotGameTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.Arrays;

/**
 * Fabric glue for the shared {@link CosmeticSlotGameTests} (server GameTests, part of {@code build}). One method per
 * entry of {@link CosmeticSlotGameTests#ALL}; {@link #everySharedTestRegistered} catches a missing one.
 */
public final class CosmeticSlotGameTestsFabric {

    @GameTest
    public void shiftClick(GameTestHelper helper) {
        CosmeticSlotGameTests.shiftClick(helper);
    }

    @GameTest
    public void cursorClicks(GameTestHelper helper) {
        CosmeticSlotGameTests.cursorClicks(helper);
    }

    @GameTest
    public void numberKeys(GameTestHelper helper) {
        CosmeticSlotGameTests.numberKeys(helper);
    }

    @GameTest
    public void drag(GameTestHelper helper) {
        CosmeticSlotGameTests.drag(helper);
    }

    @GameTest
    public void doubleClick(GameTestHelper helper) {
        CosmeticSlotGameTests.doubleClick(helper);
    }

    @GameTest
    public void throwAndMiddleClick(GameTestHelper helper) {
        CosmeticSlotGameTests.throwAndMiddleClick(helper);
    }

    @GameTest
    public void rightClickEquip(GameTestHelper helper) {
        CosmeticSlotGameTests.rightClickEquip(helper);
    }

    @GameTest
    public void creativeSlotPacket(GameTestHelper helper) {
        CosmeticSlotGameTests.creativeSlotPacket(helper);
    }

    @GameTest
    public void deathDropsOnce(GameTestHelper helper) {
        CosmeticSlotGameTests.deathDropsOnce(helper);
    }

    @GameTest
    public void keepInventory(GameTestHelper helper) {
        CosmeticSlotGameTests.keepInventory(helper);
    }

    @GameTest
    public void leavingTheEnd(GameTestHelper helper) {
        CosmeticSlotGameTests.leavingTheEnd(helper);
    }

    @GameTest
    public void everySharedTestRegistered(GameTestHelper helper) {
        long wrappers = Arrays.stream(getClass().getMethods())
                .filter(method -> method.isAnnotationPresent(GameTest.class))
                .count() - 1;
        helper.assertValueEqual(wrappers, (long) CosmeticSlotGameTests.ALL.size(), "Fabric wrappers for the shared tests");
        helper.succeed();
    }
}
