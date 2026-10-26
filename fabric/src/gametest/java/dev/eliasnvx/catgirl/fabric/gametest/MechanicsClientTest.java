package dev.eliasnvx.catgirl.fabric.gametest;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.block.CardboardBoxBlock;
import dev.eliasnvx.catgirl.block.CatBedBlock;
import dev.eliasnvx.catgirl.block.CatgirlBlocks;
import dev.eliasnvx.catgirl.block.CatnipBlock;
import dev.eliasnvx.catgirl.effect.CatgirlEffects;
import dev.eliasnvx.catgirl.entity.CatSeat;
import dev.eliasnvx.catgirl.registry.CatgirlItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * The mechanics on a real client and integrated server: cat bed, cardboard box, catnip, yarn ball. Assertions
 * crash the client with a message, which fails {@code ./gradlew :fabric:runClientGameTest}.
 */
public final class MechanicsClientTest implements FabricClientGameTest {

    private static final int SETTLE = 20;
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int FOV = 45;
    private static final int HUNGRY = 6;
    /** A bit longer than CatSeat's nap. */
    private static final int NAP_TICKS = 215;
    private static final int CHASE_TIMEOUT = 200;
    private static final int HIDDEN_TICKS = 60;
    private static final double BALL_MOVED = 0.4;
    /** CardboardBoxBlock's shape height, in blocks. */
    private static final double BOX_TOP = 11.0 / 16.0;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("gamerule show_death_messages false");
            context.runOnClient(mc -> mc.options.fov().set(FOV));
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));

            catBed(context, world);
            box(context, world);
            catnip(context, world);
            yarnBall(context, world);
        }
    }

    private static void catBed(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runCommand("difficulty peaceful");
        BlockPos bed = world.getServer().computeOnServer(server -> player(server).blockPosition().north(2));
        world.getServer().runCommand("setblock " + bed.getX() + " " + bed.getY() + " " + bed.getZ() + " catgirl:cat_bed");
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            player.getFoodData().setFoodLevel(HUNGRY);
            player.getFoodData().setSaturation(0.0F);
            check(CatSeat.sit((ServerLevel) player.level(), bed, player, CatBedBlock.SEAT_HEIGHT), "couldn't lie down in the cat bed");
        });
        context.waitTicks(SETTLE);
        context.takeScreenshot(TestScreenshotOptions.of("catgirl_mech_cat_bed").withSize(WIDTH, HEIGHT));
        context.waitTicks(NAP_TICKS);
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            check(player.getFoodData().getFoodLevel() > HUNGRY, "resting in the cat bed didn't feed: " + player.getFoodData().getFoodLevel());
            check(player.hasEffect(CatgirlEffects.WELL_RESTED.asHolder()), "a full nap didn't give Well Rested");
            player.stopRiding();
        });
        context.waitTicks(SETTLE / 2);
        world.getServer().runOnServer(server ->
                check(server.overworld().getEntitiesOfClass(CatSeat.class, player(server).getBoundingBox().inflate(8)).isEmpty(),
                        "the seat stayed after getting up"));
        world.getServer().runCommand("setblock " + bed.getX() + " " + bed.getY() + " " + bed.getZ() + " air");
    }

    private static void box(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runCommand("difficulty easy");
        world.getServer().runCommand("time set midnight");
        BlockPos box = world.getServer().computeOnServer(server -> player(server).blockPosition().north(2));
        world.getServer().runCommand("setblock " + box.getX() + " " + box.getY() + " " + box.getZ() + " catgirl:cardboard_box");
        UUID zombieId = world.getServer().computeOnServer(server -> {
            ServerPlayer player = player(server);
            Zombie zombie = EntityTypes.ZOMBIE.create(player.level(), EntitySpawnReason.COMMAND);
            zombie.snapTo(player.getX() + 8, player.getY(), player.getZ(), 0.0F, 0.0F);
            zombie.setPersistenceRequired();
            player.level().addFreshEntity(zombie);
            return zombie.getUUID();
        });
        world.getServer().waitFor(server -> zombie(server, zombieId).getTarget() == player(server), CHASE_TIMEOUT);
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            check(zombie(server, zombieId).getTarget() == player, "the zombie never went for the player");
            check(CatSeat.sit((ServerLevel) player.level(), box, player, CardboardBoxBlock.SEAT_HEIGHT), "couldn't get into the box");
            check(zombie(server, zombieId).getTarget() == null, "the zombie still chases a player in a box");
        });
        context.waitTicks(HIDDEN_TICKS);
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            check(zombie(server, zombieId).getTarget() == null, "the zombie found the player in the box again");
            check(done(server, player, "box_enjoyer"), "Box Enjoyer not granted");
            zombie(server, zombieId).discard();
        });
        world.getServer().runCommand("time set noon");
        context.waitTicks(SETTLE / 2);
        context.takeScreenshot(TestScreenshotOptions.of("catgirl_mech_cardboard_box").withSize(WIDTH, HEIGHT));
        world.getServer().runOnServer(server -> player(server).stopRiding());
        context.waitTicks(2);
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            // out above the box: dropped at the dismount spot and landing on its 11 px top, not stuck inside or in the floor
            check(player.getY() >= box.getY() + BOX_TOP - 1.0E-3, "climbing out of the box left the player in it: y " + player.getY());
        });
        world.getServer().runCommand("setblock " + box.getX() + " " + box.getY() + " " + box.getZ() + " air");
        world.getServer().runCommand("time set noon");
        world.getServer().runCommand("difficulty peaceful");
    }

    private static void catnip(ClientGameTestContext context, TestSingleplayerContext world) {
        // the four growth stages in a row in front of the player
        context.waitTicks(SETTLE); // stepping out of the box: land first
        BlockPos base = world.getServer().computeOnServer(server -> player(server).blockPosition().north(3));
        for (int age = 0; age <= 3; age++) {
            int x = base.getX() - 2 + age;
            world.getServer().runCommand("setblock " + x + " " + base.getY() + " " + base.getZ() + " catgirl:catnip[age=" + age + "]");
        }
        context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        world.getServer().runCommand("tp @p ~ ~ ~ 180 35");
        context.waitTicks(SETTLE);
        world.getServer().runOnServer(server -> {
            for (int age = 0; age <= 3; age++) {
                BlockState state = server.overworld().getBlockState(base.west(2).east(age));
                check(state.is(CatgirlBlocks.CATNIP.get()) && state.getValue(CatnipBlock.AGE) == age, "catnip stage " + age + " not placed: " + state);
            }
        });
        context.takeScreenshot(TestScreenshotOptions.of("catgirl_mech_catnip_stages").withSize(WIDTH, HEIGHT));
        world.getServer().runCommand("tp @p ~ ~ ~ 180 0");

        // eating it: Haste + catnip on the player, zoomies for a cat nearby, Overdose on the second one
        UUID catId = world.getServer().computeOnServer(server -> spawnCat(player(server), 3.0, 0.0));
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            CatgirlItems.CATNIP.get().finishUsingItem(new ItemStack(CatgirlItems.CATNIP.get()), player.level(), player);
            check(player.hasEffect(MobEffects.HASTE), "catnip didn't give Haste");
            check(player.hasEffect(CatgirlEffects.CATNIP.asHolder()), "catnip didn't give its effect");
            check(((Cat) player.level().getEntity(catId)).hasEffect(CatgirlEffects.CATNIP.asHolder()), "the cat nearby didn't get the zoomies");
            CatgirlItems.CATNIP.get().finishUsingItem(new ItemStack(CatgirlItems.CATNIP.get()), player.level(), player);
            check(done(server, player, "catnip_overdose"), "Catnip Overdose not granted");
            player.level().getEntity(catId).discard();
            player.removeAllEffects();
        });
    }

    private static void yarnBall(ClientGameTestContext context, TestSingleplayerContext world) {
        context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        // face south-west: the front camera then looks north-east, past the player at the cat and the ball
        world.getServer().runCommand("tp @p ~ ~ ~ 45 10");
        UUID catId = world.getServer().computeOnServer(server -> spawnCat(player(server), -1.0, -4.0));
        UUID ballId = world.getServer().computeOnServer(server -> {
            ServerPlayer player = player(server);
            ItemEntity ball = new ItemEntity(player.level(), player.getX() + 4.0, player.getY() + 0.5, player.getZ() - 3.0,
                    new ItemStack(CatgirlItems.YARN_BALL.get()), 0.0, 0.0, 0.0);
            ball.setNeverPickUp();
            player.level().addFreshEntity(ball);
            return ball.getUUID();
        });
        context.waitTicks(SETTLE);
        Vec3 resting = world.getServer().computeOnServer(server -> entity(server, ballId).position());
        int ticks = world.getServer().waitFor(server -> horizontal(entity(server, ballId).position(), resting) > BALL_MOVED, CHASE_TIMEOUT);
        check(ticks < CHASE_TIMEOUT, "no cat batted the yarn ball");
        context.waitTicks(4);
        context.takeScreenshot(TestScreenshotOptions.of("catgirl_mech_yarn_ball").withSize(WIDTH, HEIGHT));
        world.getServer().runOnServer(server -> {
            entity(server, ballId).discard();
            entity(server, catId).discard();
        });
    }

    private static UUID spawnCat(ServerPlayer player, double dx, double dz) {
        Cat cat = EntityTypes.CAT.create(player.level(), EntitySpawnReason.COMMAND);
        cat.snapTo(player.getX() + dx, player.getY(), player.getZ() + dz, 0.0F, 0.0F);
        player.level().addFreshEntity(cat);
        return cat.getUUID();
    }

    private static double horizontal(Vec3 a, Vec3 b) {
        return Math.hypot(a.x - b.x, a.z - b.z);
    }

    private static Zombie zombie(MinecraftServer server, UUID id) {
        return (Zombie) entity(server, id);
    }

    private static Entity entity(MinecraftServer server, UUID id) {
        return server.overworld().getEntity(id);
    }

    private static boolean done(MinecraftServer server, ServerPlayer player, String advancement) {
        AdvancementHolder holder = server.getAdvancements().get(CatgirlMod.id(advancement));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
