package dev.eliasnvx.catgirl.fabric.gametest;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.cosmetic.CatSlot;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.cosmetic.CosmeticsManager;
import dev.eliasnvx.catgirl.meow.MeowPayload;
import dev.eliasnvx.catgirl.menu.CosmeticsContainer;
import dev.eliasnvx.catgirl.menu.CosmeticsLayout;
import dev.eliasnvx.catgirl.menu.InventoryMenuCosmetics;
import dev.eliasnvx.catgirl.mixin.client.AbstractContainerScreenAccessor;
import dev.eliasnvx.catgirl.registry.CatgirlComponents;
import dev.eliasnvx.catgirl.registry.CatgirlItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * Visual check of the ears, tail and the cosmetic slots in the inventory (survival and creative). Screenshots land in
 * {@code fabric/build/run/clientGameTest/screenshots}. Run with {@code ./gradlew :fabric:runClientGameTest}.
 */
public final class CosmeticsLookClientTest implements FabricClientGameTest {

    private static final int SETTLE = 20;
    private static final int WALK_TICKS = 12;
    private static final int ZOOM_FOV = 45;
    private static final int CLOSE_UP_FOV = 30;
    private static final int DEFAULT_FOV = 70;
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("gamerule show_death_messages false");
            context.runOnClient(mc -> mc.options.fov().set(ZOOM_FOV));

            for (Coat coat : new Coat[]{Coat.GINGER, Coat.SIAMESE, Coat.CALICO, Coat.NEON}) {
                wear(world, coat);
                context.waitTicks(SETTLE);
                String name = coat.getSerializedName();
                camera(context, CameraType.THIRD_PERSON_FRONT);
                shot(context, "catgirl_" + name + "_front", CLOSE_UP_FOV);
                camera(context, CameraType.THIRD_PERSON_BACK);
                world.getServer().runCommand("tp @p ~ ~ ~ -35 0");
                context.waitTicks(SETTLE / 2);
                shot(context, "catgirl_" + name + "_back", ZOOM_FOV);
                world.getServer().runCommand("tp @p ~ ~ ~ 0 0");
            }

            // tail curve from the side, standing and walking
            wear(world, Coat.GRAY);
            camera(context, CameraType.THIRD_PERSON_BACK);
            world.getServer().runCommand("tp @p ~ ~ ~ 90 0");
            context.waitTicks(SETTLE);
            shot(context, "catgirl_side_idle", ZOOM_FOV);
            context.getInput().holdKeyFor(options -> options.keyUp, WALK_TICKS);
            shot(context, "catgirl_side_walking", ZOOM_FOV);
            world.getServer().runCommand("tp @p ~ ~ ~ 0 0");

            // ears pressed back when hurt
            camera(context, CameraType.THIRD_PERSON_FRONT);
            context.waitTicks(SETTLE);
            world.getServer().runCommand("damage @p 1 minecraft:generic");
            context.waitTicks(4);
            shot(context, "catgirl_hurt", CLOSE_UP_FOV);

            // ears on top of a helmet
            world.getServer().runCommand("item replace entity @p armor.head with minecraft:iron_helmet");
            context.waitTicks(SETTLE);
            shot(context, "catgirl_helmet", CLOSE_UP_FOV);
            world.getServer().runCommand("item replace entity @p armor.head with minecraft:air");

            // full outfits: paws, leg warmers, slippers and collars
            outfit(world, Coat.GINGER, CatgirlItems.PAW_GLOVES.get(), new ItemStack(CatgirlItems.BELL_COLLAR.get()));
            camera(context, CameraType.THIRD_PERSON_FRONT);
            context.waitTicks(SETTLE);
            shot(context, "catgirl_outfit_gloves_front", ZOOM_FOV);
            camera(context, CameraType.THIRD_PERSON_BACK);
            context.waitTicks(SETTLE / 2);
            shot(context, "catgirl_outfit_gloves_back", ZOOM_FOV);
            outfit(world, Coat.GRAY, CatgirlItems.PAW_MITTENS.get(), new ItemStack(CatgirlItems.NECK_RIBBON.get()));
            camera(context, CameraType.THIRD_PERSON_FRONT);
            context.waitTicks(SETTLE);
            shot(context, "catgirl_outfit_mittens_front", ZOOM_FOV);
            ItemStack choker = new ItemStack(CatgirlItems.VELVET_CHOKER.get());
            choker.set(DataComponents.DYED_COLOR, new DyedItemColor(0x8E44AD));
            world.getServer().runOnServer(server -> CosmeticsManager.set(player(server), CatSlot.NECK, choker));
            context.waitTicks(SETTLE / 2);
            shot(context, "catgirl_choker_dyed", CLOSE_UP_FOV);

            // face and head pieces, close up from the front
            camera(context, CameraType.THIRD_PERSON_FRONT);
            // ears worn too: the headphones' own ears hide them (#catgirl:covers_ears)
            faceAndHead(world, Coat.WHITE, new ItemStack(CatgirlItems.WHISKERS.get()), new ItemStack(CatgirlItems.CAT_EAR_HEADPHONES.get()), true);
            context.waitTicks(SETTLE / 2);
            shot(context, "catgirl_face_whiskers_headphones", CLOSE_UP_FOV);
            faceAndHead(world, Coat.GINGER, new ItemStack(CatgirlItems.BLUSH.get()), new ItemStack(CatgirlItems.EAR_BOWS.get()), true);
            context.waitTicks(SETTLE / 2);
            shot(context, "catgirl_face_blush_bows", CLOSE_UP_FOV);
            ItemStack whiteWhiskers = new ItemStack(CatgirlItems.WHISKERS.get());
            whiteWhiskers.set(DataComponents.DYED_COLOR, new DyedItemColor(0xFFFFFF));
            faceAndHead(world, Coat.BLACK, whiteWhiskers, new ItemStack(CatgirlItems.BELL_HEADBAND.get()), true);
            context.waitTicks(SETTLE / 2);
            shot(context, "catgirl_face_white_whiskers_headband", CLOSE_UP_FOV);
            world.getServer().runCommand("item replace entity @p armor.head with minecraft:golden_helmet");
            context.waitTicks(SETTLE / 2);
            shot(context, "catgirl_head_under_helmet", CLOSE_UP_FOV);
            world.getServer().runCommand("item replace entity @p armor.head with minecraft:air");

            // paws in first person, both kinds
            camera(context, CameraType.FIRST_PERSON);
            context.runOnClient(mc -> mc.options.fov().set(DEFAULT_FOV));
            context.waitTicks(SETTLE / 2);
            // with the HUD on: hiding it (F1) also hides the hand
            context.takeScreenshot(TestScreenshotOptions.of("catgirl_first_person_mittens").withSize(WIDTH, HEIGHT));
            world.getServer().runOnServer(server -> CosmeticsManager.set(player(server), CatSlot.HANDS,
                    CatgirlItems.withCoat(CatgirlItems.PAW_GLOVES.get(), Coat.CALICO)));
            context.waitTicks(SETTLE / 2);
            context.takeScreenshot(TestScreenshotOptions.of("catgirl_first_person_gloves").withSize(WIDTH, HEIGHT));

            // meow among five cats: notes float up, the cats answer, both meow advancements are granted
            camera(context, CameraType.THIRD_PERSON_FRONT);
            for (int i = 0; i < 5; i++) {
                // at the player (server commands run at world spawn) and without AI, so they stay in the herd radius
                world.getServer().runCommand("execute at @p run summon minecraft:cat ~" + (i - 2) + " ~ ~-3 {NoAI:1b}");
            }
            context.waitTicks(SETTLE);
            context.runOnClient(mc -> NetworkManager.sendToServer(MeowPayload.INSTANCE));
            // the notes reach the client after a server tick and live 6 ticks
            context.waitTicks(3);
            context.takeScreenshot(TestScreenshotOptions.of("catgirl_meow").withSize(WIDTH, HEIGHT));
            context.waitTicks(SETTLE * 2);
            world.getServer().runOnServer(server -> {
                for (String id : new String[]{"first_meow", "herding_cats"}) {
                    AdvancementHolder advancement = server.getAdvancements().get(CatgirlMod.id(id));
                    if (advancement == null || !player(server).getAdvancements().getOrStartProgress(advancement).isDone()) {
                        throw new AssertionError("Meowing among five cats did not grant catgirl:" + id);
                    }
                }
            });
            world.getServer().runCommand("kill @e[type=minecraft:cat]");

            // first person: the cosmetic slots in the inventory's own panel
            camera(context, CameraType.FIRST_PERSON);
            context.runOnClient(mc -> mc.options.fov().set(DEFAULT_FOV));
            world.getServer().runOnServer(server -> {
                ServerPlayer player = player(server);
                CosmeticsManager.set(player, CatSlot.EARS, ItemStack.EMPTY);
                for (Coat coat : Coat.values()) {
                    player.getInventory().add(CatgirlItems.withCoat(CatgirlItems.CAT_EARS.get(), coat));
                }
            });
            context.waitTicks(SETTLE / 2);
            context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
            context.waitTicks(SETTLE / 2);
            // advancement and recipe toasts sit on the inventory's top-right corner, next to the panel
            context.runOnClient(mc -> mc.gui.toastManager().clear());
            context.takeScreenshot("catgirl_inventory_panel_empty_ears");
            // shift-click ears in the inventory: they go into the ears slot, and back out
            context.runOnClient(mc -> {
                InventoryMenu menu = mc.player.inventoryMenu;
                mc.gameMode.handleContainerInput(menu.containerId, firstSlotWith(menu, CatgirlItems.CAT_EARS.get()), 0,
                        ContainerInput.QUICK_MOVE, mc.player);
            });
            context.waitTicks(SETTLE / 2);
            world.getServer().runOnServer(server -> check(CosmeticsManager.get(player(server)).get(CatSlot.EARS).is(CatgirlItems.CAT_EARS.get()),
                    "shift-click did not put the ears into their slot"));
            context.runOnClient(mc -> mc.gui.toastManager().clear());
            context.takeScreenshot("catgirl_inventory_panel");
            context.runOnClient(mc -> mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId,
                    cosmeticSlot(mc.player.inventoryMenu, CatSlot.EARS), 0, ContainerInput.QUICK_MOVE, mc.player));
            context.waitTicks(SETTLE / 2);
            world.getServer().runOnServer(server -> check(CosmeticsManager.get(player(server)).get(CatSlot.EARS).isEmpty(),
                    "shift-click did not take the ears out of their slot"));
            context.setScreen(() -> null);

            // creative: the same slots on the inventory tab, and what is put there reaches the server
            world.getServer().runCommand("gamemode creative @a");
            context.waitTicks(SETTLE / 2);
            context.setScreen(() -> {
                Minecraft mc = Minecraft.getInstance();
                return new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), false);
            });
            context.waitTicks(2);
            context.runOnClient(mc -> {
                CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) mc.gui.screen();
                selectInventoryTab(screen);
                int found = 0;
                for (Slot slot : screen.getMenu().slots) {
                    if (slot.container instanceof CosmeticsContainer) {
                        check(slot.x >= CosmeticsLayout.panelX(((AbstractContainerScreenAccessor) screen).catgirl$imageWidth()), "a cosmetic slot sits inside the creative inventory: x=" + slot.x);
                        found++;
                    }
                }
                check(found == CatSlot.values().length, "creative inventory tab shows " + found + " cosmetic slots");
                InventoryMenu menu = mc.player.inventoryMenu;
                menu.getSlot(cosmeticSlot(menu, CatSlot.TAIL)).set(CatgirlItems.withCoat(CatgirlItems.CAT_TAIL.get(), Coat.GINGER));
                menu.broadcastChanges();
            });
            context.waitTicks(SETTLE / 2);
            world.getServer().runOnServer(server -> check(
                    CosmeticsManager.get(player(server)).get(CatSlot.TAIL).get(CatgirlComponents.COAT.get()) == Coat.GINGER,
                    "a tail put into the creative inventory's cosmetic slot did not reach the server"));
            context.runOnClient(mc -> mc.gui.toastManager().clear());
            context.takeScreenshot("catgirl_creative_panel");
            context.setScreen(() -> null);
            world.getServer().runCommand("gamemode survival @a");
        }
    }

    private static int firstSlotWith(InventoryMenu menu, Item item) {
        for (int i = InventoryMenu.INV_SLOT_START; i < InventoryMenu.USE_ROW_SLOT_END; i++) {
            if (menu.getSlot(i).getItem().is(item)) {
                return i;
            }
        }
        throw new AssertionError("no " + item + " in the inventory");
    }

    private static int cosmeticSlot(InventoryMenu menu, CatSlot slot) {
        return ((InventoryMenuCosmetics) menu).catgirl$cosmeticStart() + slot.ordinal();
    }

    /** selectTab is private; the test runs unobfuscated, so reflection finds it by name. */
    private static void selectInventoryTab(CreativeModeInventoryScreen screen) {
        try {
            var selectTab = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
            selectTab.setAccessible(true);
            for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
                if (tab.getType() == CreativeModeTab.Type.INVENTORY) {
                    selectTab.invoke(screen, tab);
                    return;
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("couldn't open the creative inventory tab", e);
        }
        throw new AssertionError("no creative inventory tab");
    }

    private static void check(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }

    private static void wear(TestSingleplayerContext world, Coat coat) {
        world.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            CosmeticsManager.set(player, CatSlot.EARS, CatgirlItems.withCoat(CatgirlItems.CAT_EARS.get(), coat));
            CosmeticsManager.set(player, CatSlot.TAIL, CatgirlItems.withCoat(CatgirlItems.CAT_TAIL.get(), coat));
        });
    }

    private static ServerPlayer player(net.minecraft.server.MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void outfit(TestSingleplayerContext world, Coat coat, Item hands, ItemStack neck) {
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            CosmeticsManager.set(player, CatSlot.EARS, CatgirlItems.withCoat(CatgirlItems.CAT_EARS.get(), coat));
            CosmeticsManager.set(player, CatSlot.TAIL, CatgirlItems.withCoat(CatgirlItems.CAT_TAIL.get(), coat));
            CosmeticsManager.set(player, CatSlot.HANDS, CatgirlItems.withCoat(hands, coat));
            CosmeticsManager.set(player, CatSlot.LEGS, CatgirlItems.withCoat(CatgirlItems.LEG_WARMERS.get(), coat));
            CosmeticsManager.set(player, CatSlot.FEET, CatgirlItems.withCoat(CatgirlItems.PAW_SLIPPERS.get(), coat));
            CosmeticsManager.set(player, CatSlot.NECK, neck);
        });
    }

    private static void faceAndHead(TestSingleplayerContext world, Coat ears, ItemStack face, ItemStack head, boolean withEars) {
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            CosmeticsManager.set(player, CatSlot.EARS, withEars ? CatgirlItems.withCoat(CatgirlItems.CAT_EARS.get(), ears) : ItemStack.EMPTY);
            CosmeticsManager.set(player, CatSlot.FACE, face);
            CosmeticsManager.set(player, CatSlot.HEAD, head);
        });
    }

    private static void camera(ClientGameTestContext context, CameraType type) {
        context.runOnClient(mc -> mc.options.setCameraType(type));
    }

    /** A screenshot without the HUD at the given field of view. */
    private static void shot(ClientGameTestContext context, String name, int fov) {
        context.runOnClient(mc -> {
            mc.options.fov().set(fov);
            if (!mc.gui.hud.isHidden()) {
                mc.gui.hud.toggle();
            }
        });
        context.waitTicks(SETTLE / 4);
        context.takeScreenshot(TestScreenshotOptions.of(name).withSize(WIDTH, HEIGHT));
        context.runOnClient(mc -> {
            mc.options.fov().set(ZOOM_FOV);
            mc.gui.hud.toggle();
        });
    }
}
