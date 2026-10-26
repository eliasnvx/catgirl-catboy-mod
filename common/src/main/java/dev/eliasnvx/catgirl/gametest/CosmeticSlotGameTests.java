package dev.eliasnvx.catgirl.gametest;

import dev.eliasnvx.catgirl.cosmetic.CatSlot;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.cosmetic.CosmeticsManager;
import dev.eliasnvx.catgirl.menu.InventoryMenuCosmetics;
import dev.eliasnvx.catgirl.registry.CatgirlComponents;
import dev.eliasnvx.catgirl.registry.CatgirlItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;

/**
 * Dupe checks for the cosmetic slots, shared by both loaders (Fabric: {@code fabric/src/gametest}, NeoForge:
 * {@code neoforge/.../dev/CatgirlGameTestsNeoForge}). Every way an item gets into or out of a slot goes through the
 * real server code (menu clicks, the creative slot packet, right-click use, death and respawn), and after each step
 * the number of cat ears (menu, cursor, worn and dropped) must not change. Dev only: not in the released jars.
 */
public final class CosmeticSlotGameTests {

    /** A test: its name and body. The loader glue registers every entry of {@link #ALL}. */
    public record Entry(String name, Consumer<GameTestHelper> body) {
    }

    public static final List<Entry> ALL = List.of(
            new Entry("shift_click", CosmeticSlotGameTests::shiftClick),
            new Entry("cursor_clicks", CosmeticSlotGameTests::cursorClicks),
            new Entry("number_keys", CosmeticSlotGameTests::numberKeys),
            new Entry("drag", CosmeticSlotGameTests::drag),
            new Entry("double_click", CosmeticSlotGameTests::doubleClick),
            new Entry("throw_and_middle_click", CosmeticSlotGameTests::throwAndMiddleClick),
            new Entry("right_click_equip", CosmeticSlotGameTests::rightClickEquip),
            new Entry("creative_slot_packet", CosmeticSlotGameTests::creativeSlotPacket),
            new Entry("death_drops_once", CosmeticSlotGameTests::deathDropsOnce),
            new Entry("keep_inventory", CosmeticSlotGameTests::keepInventory),
            new Entry("leaving_the_end", CosmeticSlotGameTests::leavingTheEnd));

    private static final Vec3 TEST_AREA_CENTER = new Vec3(1.5, 1.0, 1.5);
    /** Dropped items are looked for this far around the spot the player stood on. */
    private static final double DROP_RADIUS = 3.0;
    private static final int OUTSIDE = AbstractContainerMenu.SLOT_CLICKED_OUTSIDE;

    private CosmeticSlotGameTests() {
    }

    public static void shiftClick(GameTestHelper helper) {
        run(helper, rig -> {
            rig.inventory().setItem(0, ears(Coat.GINGER));
            rig.click(rig.hotbar(0), 0, ContainerInput.QUICK_MOVE);
            rig.expectWorn(Coat.GINGER, "shift-click from the hotbar");
            rig.expectTotal(1, "shift-click in");

            // slot taken: the ears move between hotbar and inventory like vanilla, the worn ones stay
            rig.inventory().setItem(1, ears(Coat.CALICO));
            rig.click(rig.hotbar(1), 0, ContainerInput.QUICK_MOVE);
            rig.expectWorn(Coat.GINGER, "shift-click onto a taken slot");
            rig.expectTotal(2, "shift-click onto a taken slot");

            rig.click(rig.ears, 0, ContainerInput.QUICK_MOVE);
            rig.expectWornEmpty("shift-click out");
            rig.expectTotal(2, "shift-click out");

            // a stack of three (a command can make them stack): one goes on, two stay
            rig.inventory().setItem(2, stackedEars(3));
            rig.click(rig.hotbar(2), 0, ContainerInput.QUICK_MOVE);
            rig.expectWornCount(1, "shift-click from a stack");
            rig.expectTotal(5, "shift-click from a stack");

            // no room in the inventory: the worn ears stay
            rig.reset();
            rig.wear(ears(Coat.GINGER));
            rig.fillInventory();
            rig.click(rig.ears, 0, ContainerInput.QUICK_MOVE);
            rig.expectWorn(Coat.GINGER, "shift-click out with a full inventory");
            rig.expectTotal(1, "shift-click out with a full inventory");
        });
    }

    public static void cursorClicks(GameTestHelper helper) {
        run(helper, rig -> {
            rig.inventory().setItem(0, stackedEars(3));
            rig.click(rig.hotbar(0), 0, ContainerInput.PICKUP);
            rig.click(rig.ears, 0, ContainerInput.PICKUP);
            rig.expectWornCount(1, "placing a stack of three");
            rig.helper.assertValueEqual(rig.menu.getCarried().getCount(), 2, "ears left on the cursor");
            rig.click(rig.ears, 0, ContainerInput.PICKUP);
            rig.click(rig.ears, 1, ContainerInput.PICKUP);
            rig.expectWornCount(1, "clicking a full slot with more of the same");
            rig.click(rig.hotbar(0), 0, ContainerInput.PICKUP);
            rig.expectTotal(3, "placing from the cursor");

            // different ears on the cursor swap with the worn ones
            rig.reset();
            rig.wear(ears(Coat.GINGER));
            rig.inventory().setItem(0, ears(Coat.CALICO));
            rig.click(rig.hotbar(0), 0, ContainerInput.PICKUP);
            rig.click(rig.ears, 0, ContainerInput.PICKUP);
            rig.expectWorn(Coat.CALICO, "swap with the cursor");
            rig.helper.assertTrue(coat(rig.menu.getCarried()) == Coat.GINGER, "the old ears should be on the cursor");
            rig.expectTotal(2, "swap with the cursor");
            rig.click(rig.hotbar(0), 0, ContainerInput.PICKUP);

            // a stick and leg warmers don't fit
            rig.inventory().setItem(1, new ItemStack(Items.STICK));
            rig.click(rig.hotbar(1), 0, ContainerInput.PICKUP);
            rig.click(rig.ears, 0, ContainerInput.PICKUP);
            rig.expectWorn(Coat.CALICO, "a stick on the ears slot");
            rig.click(rig.hotbar(1), 0, ContainerInput.PICKUP);
            rig.inventory().setItem(2, new ItemStack(CatgirlItems.LEG_WARMERS.get()));
            rig.click(rig.hotbar(2), 0, ContainerInput.PICKUP);
            rig.click(rig.ears, 0, ContainerInput.PICKUP);
            rig.expectWorn(Coat.CALICO, "leg warmers on the ears slot");
            rig.click(rig.hotbar(2), 0, ContainerInput.PICKUP);

            // right-click takes the worn ears onto the cursor
            rig.click(rig.ears, 1, ContainerInput.PICKUP);
            rig.expectWornEmpty("right-click take");
            rig.expectTotal(2, "right-click take");
        });
    }

    public static void numberKeys(GameTestHelper helper) {
        run(helper, rig -> {
            rig.inventory().setItem(2, ears(Coat.GINGER));
            rig.click(rig.ears, 2, ContainerInput.SWAP);
            rig.expectWorn(Coat.GINGER, "key 3 from the hotbar");
            rig.click(rig.ears, 2, ContainerInput.SWAP);
            rig.expectWornEmpty("key 3 back to the hotbar");
            rig.expectTotal(1, "key 3 there and back");

            rig.inventory().setItem(Inventory.SLOT_OFFHAND, ears(Coat.CALICO));
            rig.click(rig.ears, Inventory.SLOT_OFFHAND, ContainerInput.SWAP);
            rig.expectWorn(Coat.CALICO, "F from the offhand");
            rig.click(rig.ears, 2, ContainerInput.SWAP);
            rig.expectWorn(Coat.GINGER, "key 3 onto a taken slot");
            rig.expectTotal(2, "swapping with the hotbar");

            // a stack of three: one goes on, the worn ears go to the inventory
            rig.inventory().setItem(4, stackedEars(3));
            rig.click(rig.ears, 4, ContainerInput.SWAP);
            rig.expectWornCount(1, "key 5 with a stack");
            rig.expectTotal(5, "key 5 with a stack");

            // ... or to the ground when the inventory is full
            rig.reset();
            rig.wear(ears(Coat.GINGER));
            rig.inventory().setItem(4, stackedEars(3));
            rig.fillInventory();
            rig.click(rig.ears, 4, ContainerInput.SWAP);
            rig.expectWornCount(1, "key 5 with a stack and a full inventory");
            rig.expectTotal(4, "key 5 with a stack and a full inventory");

            rig.reset();
            rig.inventory().setItem(5, new ItemStack(Items.STICK));
            rig.inventory().setItem(6, new ItemStack(CatgirlItems.LEG_WARMERS.get()));
            rig.click(rig.ears, 5, ContainerInput.SWAP);
            rig.click(rig.ears, 6, ContainerInput.SWAP);
            rig.expectWornEmpty("a stick and leg warmers by key");
        });
    }

    public static void drag(GameTestHelper helper) {
        run(helper, rig -> {
            // left drag over the ears slot and two inventory slots: one each
            rig.menu.setCarried(stackedEars(3));
            rig.drag(0, rig.ears, InventoryMenu.INV_SLOT_START, InventoryMenu.INV_SLOT_START + 1);
            rig.expectWornCount(1, "left drag");
            rig.expectTotal(3, "left drag");

            rig.reset();
            rig.menu.setCarried(stackedEars(3));
            rig.drag(1, rig.ears, InventoryMenu.INV_SLOT_START);
            rig.expectWornCount(1, "right drag");
            rig.expectTotal(3, "right drag");

            // the creative copy drag is refused in survival
            rig.reset();
            rig.menu.setCarried(ears(Coat.GINGER));
            rig.drag(2, rig.ears, InventoryMenu.INV_SLOT_START, InventoryMenu.INV_SLOT_START + 1);
            rig.expectTotal(1, "middle drag in survival");
        });
    }

    public static void doubleClick(GameTestHelper helper) {
        run(helper, rig -> {
            rig.wear(stackedEars(1));
            rig.inventory().setItem(10, stackedEars(2));
            rig.menu.setCarried(stackedEars(1));
            rig.click(InventoryMenu.INV_SLOT_START + 11, 0, ContainerInput.PICKUP_ALL);
            rig.expectTotal(4, "double-click collect");
        });
    }

    public static void throwAndMiddleClick(GameTestHelper helper) {
        run(helper, rig -> {
            rig.wear(ears(Coat.GINGER));
            rig.click(rig.ears, 0, ContainerInput.THROW);
            rig.expectWornEmpty("Q on the ears slot");
            rig.expectTotal(1, "Q on the ears slot");

            rig.wear(ears(Coat.CALICO));
            rig.click(rig.ears, 1, ContainerInput.THROW);
            rig.expectWornEmpty("Ctrl+Q on the ears slot");
            rig.expectTotal(2, "Ctrl+Q on the ears slot");

            // nothing is thrown while the cursor holds something
            rig.wear(ears(Coat.WHITE));
            rig.menu.setCarried(new ItemStack(Items.STICK));
            rig.click(rig.ears, 0, ContainerInput.THROW);
            rig.expectWorn(Coat.WHITE, "Q with a full cursor");
            rig.menu.setCarried(ItemStack.EMPTY);

            // middle click copies only in creative
            rig.click(rig.ears, 2, ContainerInput.CLONE);
            rig.helper.assertTrue(rig.menu.getCarried().isEmpty(), "middle click must not copy in survival");
            rig.expectTotal(3, "middle click in survival");
        });
    }

    public static void rightClickEquip(GameTestHelper helper) {
        run(helper, rig -> {
            rig.use(InteractionHand.MAIN_HAND, ears(Coat.GINGER));
            rig.expectWorn(Coat.GINGER, "right-click with ears");
            rig.helper.assertTrue(rig.player.getMainHandItem().isEmpty(), "the hand should be empty");

            rig.use(InteractionHand.MAIN_HAND, ears(Coat.CALICO));
            rig.expectWorn(Coat.CALICO, "right-click with other ears");
            rig.helper.assertTrue(coat(rig.player.getMainHandItem()) == Coat.GINGER, "the old ears should be in the hand");
            rig.expectTotal(2, "right-click swap");

            // a stack of three: one goes on, two stay in the hand, the worn ears go to the inventory
            rig.inventory().setItem(1, rig.player.getMainHandItem());
            rig.use(InteractionHand.MAIN_HAND, stackedEars(3));
            rig.expectWornCount(1, "right-click with a stack");
            rig.helper.assertValueEqual(rig.player.getMainHandItem().getCount(), 2, "ears left in the hand");
            rig.expectTotal(5, "right-click with a stack");

            // ... or to the ground when the inventory is full
            rig.reset();
            rig.wear(ears(Coat.GINGER));
            rig.player.setItemInHand(InteractionHand.MAIN_HAND, stackedEars(3));
            rig.fillInventory();
            rig.use(InteractionHand.MAIN_HAND, rig.player.getMainHandItem());
            rig.expectWornCount(1, "right-click with a stack and a full inventory");
            rig.expectTotal(4, "right-click with a stack and a full inventory");

            rig.reset();
            rig.use(InteractionHand.OFF_HAND, ears(Coat.WHITE));
            rig.expectWorn(Coat.WHITE, "right-click with ears in the offhand");
            rig.expectTotal(1, "right-click from the offhand");
        });
    }

    public static void creativeSlotPacket(GameTestHelper helper) {
        run(helper, rig -> {
            rig.creativeSet(ears(Coat.GINGER));
            rig.expectWornEmpty("a creative slot packet in survival");

            rig.player.setGameMode(GameType.CREATIVE);
            ItemStack two = ears(Coat.GINGER);
            two.setCount(2);
            rig.creativeSet(two);
            rig.expectWornEmpty("two ears by creative packet");
            rig.creativeSet(new ItemStack(Items.STICK));
            rig.expectWornEmpty("a stick by creative packet");
            rig.creativeSet(new ItemStack(CatgirlItems.LEG_WARMERS.get()));
            rig.expectWornEmpty("leg warmers by creative packet");

            rig.creativeSet(ears(Coat.GINGER));
            rig.expectWorn(Coat.GINGER, "ears by creative packet");
            rig.creativeSet(ItemStack.EMPTY);
            rig.expectWornEmpty("clearing by creative packet");
        });
    }

    public static void deathDropsOnce(GameTestHelper helper) {
        run(helper, rig -> rig.withKeepInventory(false, () -> {
            rig.wear(ears(Coat.GINGER));
            rig.inventory().setItem(0, ears(Coat.CALICO));
            rig.die();
            rig.expectWornEmpty("death");
            rig.expectTotal(2, "death (worn and carried ears on the ground)");
            rig.respawn(false);
            rig.expectWornEmpty("respawn after death");
            rig.expectTotal(2, "respawn after death");
        }));
    }

    public static void keepInventory(GameTestHelper helper) {
        run(helper, rig -> rig.withKeepInventory(true, () -> {
            rig.wear(ears(Coat.GINGER));
            rig.die();
            rig.respawn(false);
            rig.expectWorn(Coat.GINGER, "respawn with keepInventory");
            rig.expectTotal(1, "respawn with keepInventory");
        }));
    }

    public static void leavingTheEnd(GameTestHelper helper) {
        run(helper, rig -> {
            rig.wear(ears(Coat.GINGER));
            rig.respawn(true);
            rig.expectWorn(Coat.GINGER, "leaving the End");
            rig.expectTotal(1, "leaving the End");
        });
    }

    private static ItemStack ears(Coat coat) {
        return CatgirlItems.withCoat(CatgirlItems.CAT_EARS.get(), coat);
    }

    /** Ears that stack, as a command or a data pack can make them. The slot still takes one. */
    private static ItemStack stackedEars(int count) {
        ItemStack stack = ears(Coat.GRAY);
        stack.set(DataComponents.MAX_STACK_SIZE, 64);
        stack.setCount(count);
        return stack;
    }

    private static Coat coat(ItemStack stack) {
        return stack.get(CatgirlComponents.COAT.get());
    }

    /** Runs the body with a fresh survival player in the test area and always removes the player afterwards. */
    private static void run(GameTestHelper helper, Consumer<Rig> body) {
        Rig rig = new Rig(helper);
        try {
            body.accept(rig);
            helper.succeed();
        } finally {
            rig.close();
        }
    }

    /** A mock player with its inventory menu, and the checks the tests share. */
    private static final class Rig {

        final GameTestHelper helper;
        final int ears;
        private final AABB dropArea;
        ServerPlayer player;
        InventoryMenu menu;

        Rig(GameTestHelper helper) {
            this.helper = helper;
            Vec3 center = helper.absoluteVec(TEST_AREA_CENTER);
            this.dropArea = new AABB(center, center).inflate(DROP_RADIUS);
            // the mock player joins at world spawn and in creative
            this.player = helper.makeMockServerPlayerInLevel();
            player.snapTo(center);
            player.setGameMode(GameType.SURVIVAL);
            clientLoaded();
            this.menu = player.inventoryMenu;
            this.ears = ((InventoryMenuCosmetics) menu).catgirl$cosmeticStart() + CatSlot.EARS.ordinal();
        }

        Inventory inventory() {
            return player.getInventory();
        }

        int hotbar(int index) {
            return InventoryMenu.USE_ROW_SLOT_START + index;
        }

        void click(int slot, int button, ContainerInput input) {
            menu.clicked(slot, button, input, player);
        }

        /** A mouse drag with the cursor stack: type 0 left (split evenly), 1 right (one each), 2 middle (copy). */
        void drag(int type, int... slots) {
            click(OUTSIDE, AbstractContainerMenu.getQuickcraftMask(0, type), ContainerInput.QUICK_CRAFT);
            for (int slot : slots) {
                click(slot, AbstractContainerMenu.getQuickcraftMask(1, type), ContainerInput.QUICK_CRAFT);
            }
            click(OUTSIDE, AbstractContainerMenu.getQuickcraftMask(2, type), ContainerInput.QUICK_CRAFT);
        }

        void use(InteractionHand hand, ItemStack stack) {
            player.setItemInHand(hand, stack);
            player.gameMode.useItem(player, player.level(), stack, hand);
        }

        void creativeSet(ItemStack stack) {
            player.connection.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(ears, stack));
        }

        void wear(ItemStack stack) {
            CosmeticsManager.set(player, CatSlot.EARS, stack);
        }

        void die() {
            player.kill(level());
        }

        /** Respawns like after death ({@code false}) or like leaving the End ({@code true}); the new player stays here. */
        void respawn(boolean leavingTheEnd) {
            player = level().getServer().getPlayerList().respawn(player, leavingTheEnd,
                    leavingTheEnd ? Entity.RemovalReason.CHANGED_DIMENSION : Entity.RemovalReason.KILLED);
            clientLoaded();
            menu = player.inventoryMenu;
        }

        /** What a real client sends once the world is shown; until then the server keeps the player invulnerable. */
        private void clientLoaded() {
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        }

        void withKeepInventory(boolean keep, Runnable body) {
            GameRules rules = level().getGameRules();
            boolean before = rules.get(GameRules.KEEP_INVENTORY);
            rules.set(GameRules.KEEP_INVENTORY, keep, level().getServer());
            try {
                body.run();
            } finally {
                rules.set(GameRules.KEEP_INVENTORY, before, level().getServer());
            }
        }

        /** Fills every empty inventory slot (hotbar included) with stone. */
        void fillInventory() {
            for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
                if (inventory().getItem(i).isEmpty()) {
                    inventory().setItem(i, new ItemStack(Items.STONE, Items.STONE.getDefaultMaxStackSize()));
                }
            }
        }

        /** Empties inventory, cursor, crafting grid and cosmetics, and removes dropped items. */
        void reset() {
            inventory().clearContent();
            menu.setCarried(ItemStack.EMPTY);
            menu.getCraftSlots().clearContent();
            CosmeticsManager.clear(player);
            drops().forEach(Entity::discard);
        }

        void close() {
            drops().forEach(Entity::discard);
            level().getServer().getPlayerList().remove(player);
        }

        ItemStack worn() {
            return CosmeticsManager.get(player).get(CatSlot.EARS);
        }

        void expectWorn(Coat coat, String step) {
            helper.assertTrue(worn().is(CatgirlItems.CAT_EARS.get()) && coat(worn()) == coat,
                    "after " + step + ": " + coat + " ears should be worn, got " + worn());
        }

        void expectWornCount(int count, String step) {
            helper.assertValueEqual(worn().getCount(), count, "worn ears after " + step);
        }

        void expectWornEmpty(String step) {
            helper.assertTrue(worn().isEmpty(), "after " + step + ": nothing should be worn, got " + worn());
        }

        /** Cat ears in the menu (crafting grid, armor, inventory, offhand, cosmetic slots), on the cursor and on the ground. */
        void expectTotal(int expected, String step) {
            int total = menu.getCarried().is(CatgirlItems.CAT_EARS.get()) ? menu.getCarried().getCount() : 0;
            for (int i = InventoryMenu.CRAFT_SLOT_START; i < menu.slots.size(); i++) {
                ItemStack stack = menu.getSlot(i).getItem();
                if (stack.is(CatgirlItems.CAT_EARS.get())) {
                    total += stack.getCount();
                }
            }
            for (ItemEntity drop : drops()) {
                if (drop.getItem().is(CatgirlItems.CAT_EARS.get())) {
                    total += drop.getItem().getCount();
                }
            }
            helper.assertValueEqual(total, expected, "cat ears after " + step);
        }

        private List<ItemEntity> drops() {
            return level().getEntitiesOfClass(ItemEntity.class, dropArea, Entity::isAlive);
        }

        private ServerLevel level() {
            return helper.getLevel();
        }
    }
}
