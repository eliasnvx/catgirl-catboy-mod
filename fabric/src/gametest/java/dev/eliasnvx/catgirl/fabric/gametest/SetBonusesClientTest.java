package dev.eliasnvx.catgirl.fabric.gametest;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.cosmetic.CatSlot;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.cosmetic.CosmeticsManager;
import dev.eliasnvx.catgirl.meow.MeowPayload;
import dev.eliasnvx.catgirl.registry.CatgirlItems;
import dev.eliasnvx.catgirl.set.CatSet;
import dev.eliasnvx.catgirl.set.CatSets;
import dev.eliasnvx.catgirl.set.NightVision;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * The three sets on a real client and integrated server: each bonus switches on with the set and off without it.
 * Screenshots for the visible parts (hearts, the landing pose, the set stars in the inventory). Assertions crash the client
 * with a message, which fails {@code ./gradlew :fabric:runClientGameTest}.
 */
public final class SetBonusesClientTest implements FabricClientGameTest {

    private static final int SETTLE = 20;
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int FOV = 45;
    /** Bonuses are compared with the attribute's own base value (stored as a float: 0.42 is 0.41999...). */
    private static final double EPSILON = 1.0E-6;
    private static final int DROP_HEIGHT = 8;
    private static final int FALL_TIMEOUT = 100;
    private static final int FOLLOW_TICKS = 160;
    private static final double CAT_START = 9.0;
    private static final double CAT_CLOSE = 4.0;
    private static final int SLEEPLESS = 100_000;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("difficulty peaceful");
            context.runOnClient(mc -> mc.options.fov().set(FOV));

            fullNeko(context, world);
            alleyCat(context, world);
            houseCat(context, world);
        }
    }

    private static void fullNeko(ClientGameTestContext context, TestSingleplayerContext world) {
        // no set yet: no jump bonus, night vision refused
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            check(!CatSets.has(player, CatSet.FULL_NEKO), "Full Neko active with nothing worn");
            check(noBonus(player, Attributes.JUMP_STRENGTH), "jump bonus without the set");
        });
        context.runOnClient(mc -> NetworkManager.sendToServer(NightVision.Payload.INSTANCE));
        context.waitTicks(SETTLE / 4);
        world.getServer().runOnServer(server ->
                check(!player(server).hasEffect(MobEffects.NIGHT_VISION), "night vision without the set"));

        wear(world, Coat.BLACK, true);
        context.waitTicks(SETTLE / 2);
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            check(CatSets.has(player, CatSet.FULL_NEKO), "Full Neko not active in ears + tail + collar + paws");
            check(!noBonus(player, Attributes.JUMP_STRENGTH), "no jump bonus in Full Neko");
            check(done(server, player, "full_neko"), "Full Neko advancement not granted");
        });
        context.runOnClient(mc -> NetworkManager.sendToServer(NightVision.Payload.INSTANCE));
        context.waitTicks(SETTLE / 4);
        world.getServer().runOnServer(server ->
                check(player(server).hasEffect(MobEffects.NIGHT_VISION), "night vision key did nothing in Full Neko"));

        // meowing in the full set: hearts with the notes
        context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        context.waitTicks(SETTLE);
        context.runOnClient(mc -> NetworkManager.sendToServer(MeowPayload.INSTANCE));
        context.waitTicks(4);
        context.takeScreenshot(TestScreenshotOptions.of("catgirl_set_full_neko_meow").withSize(WIDTH, HEIGHT));

        // taking the paws off ends the set and its jump bonus
        world.getServer().runOnServer(server -> CosmeticsManager.set(player(server), CatSlot.HANDS, ItemStack.EMPTY));
        context.waitTicks(SETTLE / 4);
        world.getServer().runOnServer(server ->
                check(noBonus(player(server), Attributes.JUMP_STRENGTH), "jump bonus stayed after the set broke"));
    }

    private static void alleyCat(ClientGameTestContext context, TestSingleplayerContext world) {
        wear(world, Coat.GINGER, true);
        context.waitTicks(SETTLE / 2);
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            check(CatSets.has(player, CatSet.ALLEY_CAT), "Alley Cat not active in tail + slippers + leg warmers");
            check(!noBonus(player, Attributes.SAFE_FALL_DISTANCE), "no safe fall bonus in Alley Cat");
        });
        // an 8 block drop: no damage, the landing pose, one step of Nine Lives
        world.getServer().runCommand("tp @p ~ ~" + DROP_HEIGHT + " ~");
        context.waitTicks(2);
        world.getServer().waitFor(server -> player(server).onGround(), FALL_TIMEOUT);
        context.waitTicks(2);
        context.takeScreenshot(TestScreenshotOptions.of("catgirl_set_alley_cat_landing").withSize(WIDTH, HEIGHT));
        context.waitTicks(SETTLE / 2);
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            check(player.getHealth() == player.getMaxHealth(), "Alley Cat took damage from an 8 block fall: " + player.getHealth());
            AdvancementHolder nineLives = server.getAdvancements().get(CatgirlMod.id("nine_lives"));
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(nineLives);
            check(progress.getCompletedCriteria().iterator().hasNext(), "the landing did not count toward Nine Lives");
        });
    }

    private static void houseCat(ClientGameTestContext context, TestSingleplayerContext world) {
        // ears + collar, no paws or tail
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            for (CatSlot slot : CatSlot.values()) {
                CosmeticsManager.set(player, slot, ItemStack.EMPTY);
            }
            CosmeticsManager.set(player, CatSlot.EARS, CatgirlItems.withCoat(CatgirlItems.CAT_EARS.get(), Coat.WHITE));
            CosmeticsManager.set(player, CatSlot.NECK, new ItemStack(CatgirlItems.BELL_COLLAR.get()));
        });
        // a stray cat a few blocks away walks over (and doesn't run off)
        UUID stray = world.getServer().computeOnServer(server -> {
            ServerPlayer player = player(server);
            check(CatSets.has(player, CatSet.HOUSE_CAT), "House Cat not active in ears + collar");
            Cat cat = EntityTypes.CAT.create(player.level(), EntitySpawnReason.COMMAND);
            cat.snapTo(player.getX() + CAT_START, player.getY(), player.getZ(), 0.0F, 0.0F);
            player.level().addFreshEntity(cat);
            return cat.getUUID();
        });
        context.waitTicks(FOLLOW_TICKS);
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            Cat cat = (Cat) player.level().getEntity(stray);
            check(cat.distanceTo(player) < CAT_CLOSE, "the stray cat didn't come to a House Cat wearer: " + cat.distanceTo(player));
        });
        context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        context.takeScreenshot(TestScreenshotOptions.of("catgirl_set_house_cat_follow").withSize(WIDTH, HEIGHT));

        // a tamed cat nearby counts as rest: no phantoms
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            Cat cat = (Cat) player.level().getEntity(stray);
            cat.tame(player);
            player.getStats().setValue(player, Stats.CUSTOM.get(Stats.TIME_SINCE_REST), SLEEPLESS);
        });
        context.waitTicks(SETTLE + 2);
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            int sleepless = player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
            check(sleepless < SLEEPLESS, "a tamed cat nearby did not count as rest: " + sleepless);
        });

        // the inventory's cosmetics panel lights the worn set's star
        context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        context.waitTicks(SETTLE / 2);
        context.runOnClient(mc -> mc.gui.toastManager().clear());
        context.takeScreenshot("catgirl_set_inventory");
        context.setScreen(() -> null);
    }

    /** The full outfit: ears, tail, collar, leg warmers, slippers, and paws on the hands if {@code paws}. */
    private static void wear(TestSingleplayerContext world, Coat coat, boolean paws) {
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            CosmeticsManager.set(player, CatSlot.EARS, CatgirlItems.withCoat(CatgirlItems.CAT_EARS.get(), coat));
            CosmeticsManager.set(player, CatSlot.TAIL, CatgirlItems.withCoat(CatgirlItems.CAT_TAIL.get(), coat));
            CosmeticsManager.set(player, CatSlot.NECK, new ItemStack(CatgirlItems.BELL_COLLAR.get()));
            CosmeticsManager.set(player, CatSlot.LEGS, CatgirlItems.withCoat(CatgirlItems.LEG_WARMERS.get(), coat));
            CosmeticsManager.set(player, CatSlot.FEET, CatgirlItems.withCoat(CatgirlItems.PAW_SLIPPERS.get(), coat));
            CosmeticsManager.set(player, CatSlot.HANDS, paws ? CatgirlItems.withCoat(CatgirlItems.PAW_GLOVES.get(), coat) : ItemStack.EMPTY);
        });
    }

    private static boolean noBonus(ServerPlayer player, Holder<Attribute> attribute) {
        AttributeInstance instance = player.getAttribute(attribute);
        return Math.abs(instance.getValue() - instance.getBaseValue()) < EPSILON;
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
