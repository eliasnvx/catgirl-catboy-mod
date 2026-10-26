package dev.eliasnvx.catgirl.fabric.gametest;

import dev.eliasnvx.catgirl.cosmetic.CatSlot;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.cosmetic.CosmeticsManager;
import dev.eliasnvx.catgirl.effect.CatgirlEffects;
import dev.eliasnvx.catgirl.entity.CatgirlEntities;
import dev.eliasnvx.catgirl.entity.neko.Bakeneko;
import dev.eliasnvx.catgirl.entity.neko.CatSpirit;
import dev.eliasnvx.catgirl.entity.neko.FisherNeko;
import dev.eliasnvx.catgirl.entity.neko.Kitten;
import dev.eliasnvx.catgirl.entity.neko.NekoMob;
import dev.eliasnvx.catgirl.entity.neko.StrayNeko;
import dev.eliasnvx.catgirl.registry.CatgirlComponents;
import dev.eliasnvx.catgirl.registry.CatgirlItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * The five mobs on a real client and integrated server: a line-up for the looks, then each one's behaviour (trades,
 * taming and kittens, a kitten growing up, the fisher's quest, the bakeneko's theft and return, the spirit's gift).
 * Assertions crash the client with a message.
 */
public final class MobsClientTest implements FabricClientGameTest {

    private static final int SETTLE = 20;
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int FOV = 50;
    private static final int MAX_FEEDS = 40;
    private static final int THEFT_TIMEOUT = 60;
    private static final int INVULNERABILITY_TICKS = 25;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("gamerule spawn_mobs false");
            world.getServer().runCommand("tp @p ~ ~ ~ 180 0"); // face north: the line-up stands there
            context.runOnClient(mc -> {
                mc.options.fov().set(FOV);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            });

            lineUp(context, world);
            strayNekoAndKitten(world);
            fisherQuest(world);
            bakeneko(world);
            catSpirit(context, world);
        }
    }

    private static void lineUp(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            ServerLevel level = (ServerLevel) player.level();
            BlockPos base = player.blockPosition().north(9);
            List<EntityType<? extends Mob>> types = List.of(CatgirlEntities.STRAY_NEKO.get(), CatgirlEntities.FISHER_NEKO.get(),
                    CatgirlEntities.KITTEN.get(), CatgirlEntities.BAKENEKO.get(), CatgirlEntities.CAT_SPIRIT.get());
            for (int i = 0; i < types.size(); i++) {
                Mob mob = types.get(i).spawn(level, base.west(4).east(i * 2), EntitySpawnReason.SPAWN_ITEM_USE);
                frozen(mob, 0.0F);
            }
            // the four Stray Neko skins, a row further back
            for (int skin = 0; skin < 4; skin++) {
                StrayNeko neko = CatgirlEntities.STRAY_NEKO.get().spawn(level, base.north(3).west(3).east(skin * 2), EntitySpawnReason.SPAWN_ITEM_USE);
                neko.setSkin(skin);
                neko.setCoat(Coat.values()[skin]);
                frozen(neko, 0.0F);
            }
        });
        context.waitTicks(SETTLE);
        context.runOnClient(mc -> mc.gui.hud.toggle());
        context.takeScreenshot(TestScreenshotOptions.of("catgirl_mobs_lineup").withSize(WIDTH, HEIGHT));
        context.runOnClient(mc -> mc.gui.hud.toggle());
        world.getServer().runOnServer(server -> server.overworld().getEntitiesOfClass(Mob.class,
                player(server).getBoundingBox().inflate(32)).forEach(Mob::discard));
        context.waitTicks(2);
    }

    /** Faces the camera (south), no AI, so the line-up stays put. */
    private static void frozen(Mob mob, float yaw) {
        mob.setNoAi(true);
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
    }

    private static void strayNekoAndKitten(TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            ServerLevel level = (ServerLevel) player.level();
            StrayNeko neko = CatgirlEntities.STRAY_NEKO.get().spawn(level, player.blockPosition().east(2), EntitySpawnReason.SPAWN_ITEM_USE);
            check(!neko.getOffers().isEmpty(), "the Stray Neko has nothing to trade");

            for (int i = 0; i < MAX_FEEDS && !neko.isTame(); i++) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD));
                neko.mobInteract(player, InteractionHand.MAIN_HAND);
            }
            check(neko.isTame() && player.getUUID().equals(neko.owner()), "feeding fish never tamed the Stray Neko");
            int kittensBefore = kittens(level, player).size();
            for (int i = 0; i < 2; i++) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SALMON));
                neko.mobInteract(player, InteractionHand.MAIN_HAND);
            }
            List<Kitten> kittens = kittens(level, player);
            check(kittens.size() == kittensBefore + 1, "two fish to a tame Stray Neko brought no kitten");
            Kitten kitten = kittens.getFirst();
            check(kitten.isBaby() && player.equals(kitten.getOwner()), "the kitten isn't a baby or isn't the feeder's");

            // growing up: a Stray Neko with the kitten's coat, still the owner's
            Coat coat = kitten.coat();
            kitten.setAge(0);
            check(kitten.isRemoved(), "the grown kitten stayed a kitten");
            List<StrayNeko> grown = level.getEntitiesOfClass(StrayNeko.class, player.getBoundingBox().inflate(8),
                    n -> n != neko && n.coat() == coat && player.getUUID().equals(n.owner()));
            check(!grown.isEmpty(), "the kitten didn't grow into its owner's Stray Neko");
            level.getEntitiesOfClass(StrayNeko.class, player.getBoundingBox().inflate(8)).forEach(Mob::discard);
        });
    }

    private static void fisherQuest(TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            ServerLevel level = (ServerLevel) player.level();
            FisherNeko fisher = CatgirlEntities.FISHER_NEKO.get().spawn(level, player.blockPosition().east(2), EntitySpawnReason.SPAWN_ITEM_USE);
            check(fisher.getMainHandItem().is(Items.FISHING_ROD), "the Fisher Neko has no rod");
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD, 7));
            fisher.mobInteract(player, InteractionHand.MAIN_HAND);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SALMON, 5));
            fisher.mobInteract(player, InteractionHand.MAIN_HAND);
            check(player.getMainHandItem().getCount() == 2, "the fisher took more fish than the quest needs");
            boolean rarePiece = false;
            for (ItemStack stack : player.getInventory()) {
                Coat coat = stack.get(CatgirlComponents.COAT.get());
                rarePiece |= coat == Coat.SIAMESE || coat == Coat.CALICO;
            }
            check(rarePiece, "ten fish didn't earn a fur piece in a rare coat");
            fisher.discard();
        });
    }

    private static void bakeneko(TestSingleplayerContext world) {
        world.getServer().runCommand("time set midnight");
        world.getServer().runCommand("gamemode survival @a");
        world.getServer().runCommand("difficulty easy");
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
            Bakeneko cat = CatgirlEntities.BAKENEKO.get().spawn((ServerLevel) player.level(), player.blockPosition().east(1),
                    EntitySpawnReason.SPAWN_ITEM_USE);
            cat.setNoAi(false);
        });
        int ticks = world.getServer().waitFor(server -> player(server).getMainHandItem().isEmpty(), THEFT_TIMEOUT);
        check(ticks < THEFT_TIMEOUT, "the bakeneko didn't steal the held diamond");
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            Bakeneko cat = server.overworld().getEntitiesOfClass(Bakeneko.class, player.getBoundingBox().inflate(30)).getFirst();
            check(cat.getMainHandItem().is(Items.DIAMOND), "the bakeneko doesn't hold the stolen diamond");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CatgirlItems.CATNIP.get()));
            cat.mobInteract(player, InteractionHand.MAIN_HAND);
            check(cat.hasLoot(), "the bakeneko gave the loot back to someone without the Full Neko set");

            CosmeticsManager.set(player, CatSlot.EARS, CatgirlItems.withCoat(CatgirlItems.CAT_EARS.get(), Coat.BLACK));
            CosmeticsManager.set(player, CatSlot.TAIL, CatgirlItems.withCoat(CatgirlItems.CAT_TAIL.get(), Coat.BLACK));
            CosmeticsManager.set(player, CatSlot.NECK, new ItemStack(CatgirlItems.BELL_COLLAR.get()));
            CosmeticsManager.set(player, CatSlot.HANDS, CatgirlItems.withCoat(CatgirlItems.PAW_GLOVES.get(), Coat.BLACK));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CatgirlItems.CATNIP.get()));
            cat.mobInteract(player, InteractionHand.MAIN_HAND);
            check(!cat.hasLoot() && player.getInventory().contains(new ItemStack(Items.DIAMOND)),
                    "the bakeneko didn't trade the diamond back for catnip in Full Neko");
            cat.discard();
        });
    }

    private static void catSpirit(ClientGameTestContext context, TestSingleplayerContext world) {
        // a portrait at night (spirits fade by day), see-through over the dark grass
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            CatSpirit spirit = CatgirlEntities.CAT_SPIRIT.get().spawn((ServerLevel) player.level(), player.blockPosition().north(3),
                    EntitySpawnReason.SPAWN_ITEM_USE);
            frozen(spirit, 0.0F);
        });
        world.getServer().runCommand("effect give @p minecraft:night_vision 30 0 true");
        context.waitTicks(SETTLE);
        context.runOnClient(mc -> mc.gui.hud.toggle());
        context.takeScreenshot(TestScreenshotOptions.of("catgirl_mobs_cat_spirit").withSize(WIDTH, HEIGHT));
        context.runOnClient(mc -> mc.gui.hud.toggle());
        world.getServer().runOnServer(server -> server.overworld().getEntitiesOfClass(CatSpirit.class,
                player(server).getBoundingBox().inflate(16)).forEach(Mob::discard));
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            ServerLevel level = (ServerLevel) player.level();
            CatSpirit spirit = CatgirlEntities.CAT_SPIRIT.get().spawn(level, player.blockPosition().east(2), EntitySpawnReason.SPAWN_ITEM_USE);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.POPPY));
            spirit.mobInteract(player, InteractionHand.MAIN_HAND);
            check(player.hasEffect(CatgirlEffects.NINE_LIVES.asHolder()), "a flower to the Cat Spirit didn't give Nine Lives");
            check(spirit.isRemoved(), "the Cat Spirit didn't fade after its gift");

            player.hurtServer(level, level.damageSources().generic(), 1000.0F);
            check(player.isAlive() && player.getHealth() > 0, "Nine Lives didn't save the player");
            check(!player.hasEffect(CatgirlEffects.NINE_LIVES.asHolder()), "Nine Lives wasn't used up");
        });
        context.waitTicks(INVULNERABILITY_TICKS); // the first hit leaves invulnerability frames
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            player.hurtServer((ServerLevel) player.level(), player.level().damageSources().generic(), 1000.0F);
            check(!player.isAlive() || player.getHealth() <= 0, "Nine Lives saved twice");
        });
        world.getServer().runCommand("gamemode creative @a");
    }

    private static List<Kitten> kittens(ServerLevel level, ServerPlayer player) {
        return level.getEntitiesOfClass(Kitten.class, new AABB(player.blockPosition()).inflate(10), Kitten::isAlive);
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
