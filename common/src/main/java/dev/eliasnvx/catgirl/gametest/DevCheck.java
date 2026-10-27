package dev.eliasnvx.catgirl.gametest;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.platform.Platform;
import dev.eliasnvx.catgirl.network.CatgirlNetwork;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.block.CatgirlBlocks;
import dev.eliasnvx.catgirl.block.CatnipBlock;
import dev.eliasnvx.catgirl.cosmetic.CatSlot;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.cosmetic.CosmeticsManager;
import dev.eliasnvx.catgirl.entity.CatgirlEntities;
import dev.eliasnvx.catgirl.entity.neko.StrayNeko;
import dev.eliasnvx.catgirl.item.CatgirlFoods;
import dev.eliasnvx.catgirl.meow.MeowPayload;
import dev.eliasnvx.catgirl.registry.CatgirlItems;
import dev.eliasnvx.catgirl.set.CatSet;
import dev.eliasnvx.catgirl.set.CatSets;
import dev.eliasnvx.catgirl.set.NightVision;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

/**
 * Dev-only check in a real client, on both loaders (1.20.1 has no client GameTest API): {@code ./gradlew
 * :forge:runDevCheck} or {@code :fabric:runDevCheck} opens the world {@code saves/catgirl-check} from the title screen
 * (a flat world, created on the first run), dresses the player, places the mechanics' blocks and the mobs,
 * meows, checks a few things on the server, takes screenshots (F2-style, {@code screenshots} in the run directory, named in
 * the log) and quits. Results are logged as "DEVCHECK ok|FAIL ...". Not in the released jars (the gametest package is
 * excluded), and only started by the loaders' dev glue with {@code -Dcatgirl.devcheck=true}.
 */
public final class DevCheck {

    private static final String WORLD = "catgirl-check";
    private static int ticks = -1;
    private static boolean opening;
    private static @Nullable Supplier<Screen> modList;
    private static @Nullable Predicate<ServerPlayer> curios;

    private DevCheck() {
    }

    /**
     * @param modList the loader's mod list screen, or null to skip that shot
     * @param curios  puts the tail and leg warmers into Curios' slots (true if it worked); null without Curios support
     */
    public static void start(@Nullable Supplier<Screen> modList, @Nullable Predicate<ServerPlayer> curios) {
        DevCheck.modList = modList;
        DevCheck.curios = curios;
        ClientTickEvent.CLIENT_POST.register(DevCheck::tick);
        CatgirlMod.LOGGER.info("DEVCHECK armed");
    }

    private static void tick(Minecraft mc) {
        // the dev window usually isn't focused: without this the game pauses and every shot is the pause menu
        mc.options.pauseOnLostFocus = false;
        // a fresh run directory starts on the accessibility onboarding screen, not the title screen
        if (mc.level == null && !opening && (mc.screen instanceof TitleScreen || mc.screen instanceof AccessibilityOnboardingScreen)) {
            opening = true;
            openWorld(mc);
        }
        if (mc.player == null || mc.getSingleplayerServer() == null) {
            return;
        }
        // advancement and recipe toasts sit on the inventory's top-right corner, next to the cosmetics panel
        mc.getToasts().clear();
        ticks++;
        switch (ticks) {
            case 40 -> server(mc, DevCheck::setUp);
            case 80 -> camera(mc, CameraType.THIRD_PERSON_FRONT);
            case 100 -> shot(mc, prefix() + "outfit_front");
            case 105 -> camera(mc, CameraType.THIRD_PERSON_BACK);
            case 125 -> shot(mc, prefix() + "outfit_back");
            case 130 -> {
                camera(mc, CameraType.FIRST_PERSON);
                server(mc, server -> server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack(), "tp @p ~ ~ ~ 180 10"));
            }
            case 160 -> shot(mc, prefix() + "first_person_lineup");
            case 165 -> mc.setScreen(new InventoryScreen(mc.player));
            case 185 -> shot(mc, prefix() + "inventory");
            case 190 -> {
                mc.setScreen(null);
                camera(mc, CameraType.THIRD_PERSON_FRONT);
                CatgirlNetwork.sendToServer(NightVision.Payload.INSTANCE);
            }
            case 200 -> CatgirlNetwork.sendToServer(MeowPayload.INSTANCE);
            case 203 -> shot(mc, prefix() + "meow");
            case 210 -> server(mc, DevCheck::checks);
            case 212 -> mc.setScreen(modList == null ? null : modList.get());
            case 225 -> {
                if (modList != null) {
                    shot(mc, prefix() + "mod_list");
                }
            }
            case 226 -> {
                // Jade: a Stray Neko that trusts the player, right in front (the tooltip shows what only the server knows)
                mc.setScreen(null);
                camera(mc, CameraType.FIRST_PERSON);
                server(mc, DevCheck::nekoForJade);
            }
            case 285 -> {
                CatgirlMod.LOGGER.info("DEVCHECK looking at {}", mc.hitResult == null ? "nothing" : mc.hitResult.getType());
                shot(mc, prefix() + "jade_stray_neko");
            }
            case 290 -> mc.setScreen(new InventoryScreen(mc.player));
            case 305 -> shot(mc, prefix() + "inventory_jei");
            case 310 -> {
                CatgirlMod.LOGGER.info("DEVCHECK done");
                exitAfter(Thread.currentThread());
                mc.stop();
            }
            default -> {
            }
        }
    }

    private static void openWorld(Minecraft mc) {
        if (mc.getLevelSource().levelExists(WORLD)) {
            mc.createWorldOpenFlows().loadLevel(new TitleScreen(), WORLD);
            return;
        }
        LevelSettings settings = new LevelSettings(WORLD, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(),
                WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(WORLD.hashCode(), false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value()
                        .createWorldDimensions());
    }

    private static void setUp(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        ServerLevel level = (ServerLevel) player.level();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamerule doMobSpawning false");
        // face north, where the blocks and mobs go: the front shot's camera then sits south with nothing in its way
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "execute as @p at @s run tp @s ~ ~ ~ 180 0");
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32))) {
            mob.discard();
        }
        // the Full Neko outfit plus leg warmers and slippers (Alley Cat too), dyed choker swapped for the bell collar
        CosmeticsManager.set(player, CatSlot.EARS, CatgirlItems.withCoat(CatgirlItems.CAT_EARS.get(), Coat.CALICO));
        CosmeticsManager.set(player, CatSlot.TAIL, CatgirlItems.withCoat(CatgirlItems.CAT_TAIL.get(), Coat.CALICO));
        CosmeticsManager.set(player, CatSlot.NECK, new ItemStack(CatgirlItems.BELL_COLLAR.get()));
        CosmeticsManager.set(player, CatSlot.HANDS, CatgirlItems.withCoat(CatgirlItems.PAW_MITTENS.get(), Coat.CALICO));
        CosmeticsManager.set(player, CatSlot.LEGS, CatgirlItems.withCoat(CatgirlItems.LEG_WARMERS.get(), Coat.CALICO));
        CosmeticsManager.set(player, CatSlot.FEET, CatgirlItems.withCoat(CatgirlItems.PAW_SLIPPERS.get(), Coat.CALICO));
        CosmeticsManager.set(player, CatSlot.FACE, new ItemStack(CatgirlItems.WHISKERS.get()));
        CosmeticsManager.set(player, CatSlot.HEAD, new ItemStack(CatgirlItems.EAR_BOWS.get()));
        player.getInventory().clearContent();
        for (var food : CatgirlFoods.ALL) {
            player.getInventory().add(new ItemStack(food.get(), 4));
        }
        player.getInventory().add(new ItemStack(CatgirlItems.YARN_BALL.get(), 4));
        for (var egg : List.of(CatgirlItems.STRAY_NEKO_SPAWN_EGG, CatgirlItems.FISHER_NEKO_SPAWN_EGG, CatgirlItems.BAKENEKO_SPAWN_EGG,
                CatgirlItems.CAT_SPIRIT_SPAWN_EGG, CatgirlItems.KITTEN_SPAWN_EGG)) {
            player.getInventory().add(new ItemStack(egg.get()));
        }

        // mechanics' blocks and the mobs north of the player (the first-person shot looks that way)
        BlockPos base = player.blockPosition();
        level.setBlockAndUpdate(base.north(3).west(2), CatgirlBlocks.CAT_BED.get().defaultBlockState());
        level.setBlockAndUpdate(base.north(3), CatgirlBlocks.CARDBOARD_BOX.get().defaultBlockState());
        level.setBlockAndUpdate(base.north(3).east(2), CatgirlBlocks.CATNIP.get().defaultBlockState().setValue(CatnipBlock.AGE, CatnipBlock.MAX_AGE));
        List<EntityType<? extends Mob>> mobs = List.of(CatgirlEntities.STRAY_NEKO.get(), CatgirlEntities.FISHER_NEKO.get(),
                CatgirlEntities.KITTEN.get(), CatgirlEntities.BAKENEKO.get());
        for (int i = 0; i < mobs.size(); i++) {
            Mob mob = mobs.get(i).spawn(level, base.north(7).west(3).east(i * 2), MobSpawnType.SPAWN_EGG);
            if (mob != null) {
                mob.setNoAi(true);
            }
        }
        if (usesCurios()) {
            report(curios.test(player), "tail and leg warmers put into Curios slots");
        }
        CatgirlMod.LOGGER.info("DEVCHECK set up");
    }

    private static void nekoForJade(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        StrayNeko neko = CatgirlEntities.STRAY_NEKO.get().spawn((ServerLevel) player.level(), player.blockPosition().north(2),
                MobSpawnType.SPAWN_EGG);
        if (neko != null) {
            neko.setNoAi(true);
            neko.setOwner(player.getUUID());
            // exactly on the crosshair's line (the player may stand on a block edge)
            neko.moveTo(player.getX(), player.getY(), player.getZ() - 2.0, 0.0F, 0.0F);
        }
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "execute as @p at @s run tp @s ~ ~ ~ 180 10");
    }

    private static void checks(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        report(CatSets.has(player, CatSet.FULL_NEKO), "Full Neko active on the server");
        report(CatSets.has(player, CatSet.ALLEY_CAT), "Alley Cat active on the server");
        report(player.hasEffect(MobEffects.NIGHT_VISION), "night vision key works");
        report(CosmeticsManager.get(player).get(CatSlot.EARS).is(CatgirlItems.CAT_EARS.get()), "cosmetics stored in the loader's attachment");
        if (usesCurios()) {
            report(CosmeticsManager.get(player).get(CatSlot.TAIL).isEmpty()
                    && CosmeticsManager.worn(player).get(CatSlot.TAIL).is(CatgirlItems.CAT_TAIL.get()), "the tail counts from its Curios slot");
        }
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> report(mc.player != null && CatSets.has(mc.player, CatSet.FULL_NEKO), "cosmetics synced to the client"));
    }

    /**
     * Architectury Transformer's dev runtime leaves non-daemon thread pools behind, so the JVM never exits after a
     * clean stop (same as ClientTestExit on Fabric): exit once the render thread has finished shutting down.
     */
    private static void exitAfter(Thread render) {
        Thread exit = new Thread(() -> {
            try {
                render.join();
            } catch (InterruptedException e) {
                return;
            }
            System.exit(0);
        }, "catgirl-devcheck-exit");
        exit.setDaemon(true);
        exit.start();
    }

    private static boolean usesCurios() {
        return curios != null && Platform.isModLoaded("curios");
    }

    /** Screenshot names start with the loader. */
    private static String prefix() {
        return Platform.isForge() ? "forge_" : "fabric_";
    }

    private static void report(boolean ok, String what) {
        CatgirlMod.LOGGER.info("DEVCHECK {} {}", ok ? "ok" : "FAIL", what);
    }

    private static void server(Minecraft mc, Consumer<MinecraftServer> task) {
        MinecraftServer server = mc.getSingleplayerServer();
        server.execute(() -> task.accept(server));
    }

    private static void camera(Minecraft mc, CameraType type) {
        mc.options.setCameraType(type);
    }

    private static void shot(Minecraft mc, String name) {
        // like F2: a timestamped file in run/screenshots; the log says which shot it is
        CatgirlMod.LOGGER.info("DEVCHECK screenshot {}", name);
        Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), message -> {
        });
    }
}
