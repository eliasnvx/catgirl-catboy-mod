package dev.eliasnvx.catgirl.fabric.gametest;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.cosmetic.CatSlot;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.cosmetic.CosmeticsManager;
import dev.eliasnvx.catgirl.item.CatgirlFoods;
import dev.eliasnvx.catgirl.registry.CatgirlItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Food on a real client and integrated server: what each one gives, the containers that come back, the squid drop,
 * recipe-book unlocks, and the icons in the inventory (screenshot). Assertions crash the client with a message.
 */
public final class FoodClientTest implements FabricClientGameTest {

    private static final int SETTLE = 20;
    private static final int HUNGRY = 4;
    private static final int LONG_EFFECT = 1200;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("difficulty peaceful");

            world.getServer().runOnServer(server -> {
                ServerPlayer player = player(server);

                check(gained(player, CatgirlFoods.ONIGIRI.get()) == 6, "onigiri isn't 6 food");
                check(gained(player, CatgirlFoods.TAKOYAKI.get()) == 7, "takoyaki isn't 7 food");
                check(player.hasEffect(MobEffects.REGENERATION), "takoyaki didn't give Regeneration");

                check(gained(player, CatgirlFoods.CAT_FOOD.get()) == 2, "cat food without a set isn't 2 food");
                CosmeticsManager.set(player, CatSlot.TAIL, CatgirlItems.withCoat(CatgirlItems.CAT_TAIL.get(), Coat.GRAY));
                CosmeticsManager.set(player, CatSlot.LEGS, CatgirlItems.withCoat(CatgirlItems.LEG_WARMERS.get(), Coat.GRAY));
                CosmeticsManager.set(player, CatSlot.FEET, CatgirlItems.withCoat(CatgirlItems.PAW_SLIPPERS.get(), Coat.GRAY));
                check(gained(player, CatgirlFoods.CAT_FOOD.get()) == 8, "cat food in a set isn't 8 food");
                check(new ItemStack(CatgirlFoods.CAT_FOOD.get()).is(ItemTags.CAT_FOOD), "cats don't take cat food (#minecraft:cat_food)");
                check(new ItemStack(CatgirlFoods.TUNA_CAN.get()).is(ItemTags.CAT_FOOD), "cats don't take tuna (#minecraft:cat_food)");

                player.removeAllEffects();
                eat(player, CatgirlFoods.CATNIP_COOKIE.get());
                check(player.hasEffect(MobEffects.HASTE) && player.hasEffect(MobEffects.JUMP_BOOST), "the catnip cookie gave no Haste/Jump Boost");

                player.removeAllEffects();
                player.addEffect(new MobEffectInstance(MobEffects.POISON, LONG_EFFECT));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, LONG_EFFECT));
                ItemStack bowl = eat(player, CatgirlFoods.MILK_SAUCER.get());
                check(!player.hasEffect(MobEffects.POISON), "the milk saucer didn't clear Poison");
                check(player.hasEffect(MobEffects.SPEED), "the milk saucer cleared a good effect too");
                check(bowl.is(Items.BOWL), "the milk saucer didn't give the bowl back: " + bowl);

                player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, LONG_EFFECT));
                ItemStack bottle = eat(player, CatgirlFoods.BUBBLE_TEA.get());
                check(!player.hasEffect(MobEffects.MINING_FATIGUE), "bubble tea didn't shake off Mining Fatigue");
                check(bottle.is(Items.GLASS_BOTTLE), "bubble tea didn't give the bottle back: " + bottle);

                // a squid drops tentacles for takoyaki
                ServerLevel level = (ServerLevel) player.level();
                Squid squid = EntityTypes.SQUID.create(level, EntitySpawnReason.COMMAND);
                squid.snapTo(player.getX() + 3, player.getY() + 1, player.getZ(), 0.0F, 0.0F);
                level.addFreshEntity(squid);
                squid.kill(level);
                boolean tentacle = !level.getEntitiesOfClass(ItemEntity.class, squid.getBoundingBox().inflate(3),
                        item -> item.getItem().is(CatgirlFoods.SQUID_TENTACLE.get())).isEmpty();
                check(tentacle, "the squid dropped no tentacle");

                player.getInventory().clearContent();
                for (var food : CatgirlFoods.ALL) {
                    player.getInventory().add(new ItemStack(food.get(), 8));
                }
            });
            context.waitTicks(SETTLE);

            // recipe book: holding an ingredient opens the recipe, like vanilla
            world.getServer().runOnServer(server -> {
                ServerPlayer player = player(server);
                player.getInventory().add(new ItemStack(Items.WOOL.orange()));
                player.getInventory().add(new ItemStack(Items.COD));
            });
            context.waitTicks(SETTLE / 4);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = player(server);
                check(unlocked(player, "cat_ears_ginger"), "orange wool didn't open the ginger ears recipe");
                check(unlocked(player, "raw_fish_sticks"), "cod didn't open the fish sticks recipe");
                check(unlocked(player, "fish_sticks"), "raw fish sticks didn't open the smelting recipe");
                check(!unlocked(player, "cat_ears_black"), "black ears opened without black wool, string or leather");
            });
            context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
            context.waitTicks(SETTLE / 2);
            context.runOnClient(mc -> mc.gui.toastManager().clear());
            context.takeScreenshot("catgirl_food_inventory");
            context.setScreen(() -> null);
        }
    }

    /** Food points gained by eating one, starting hungry. */
    private static int gained(ServerPlayer player, Item food) {
        player.getFoodData().setFoodLevel(HUNGRY);
        eat(player, food);
        return player.getFoodData().getFoodLevel() - HUNGRY;
    }

    /**
     * Eats one as if the use finished; returns what stays in the hand (the container, for drinks). Goes through the
     * stack, like the game does: the stack applies the use remainder (bowl, bottle), the item alone doesn't.
     */
    private static ItemStack eat(ServerPlayer player, Item food) {
        return new ItemStack(food).finishUsingItem(player.level(), player);
    }

    private static boolean unlocked(ServerPlayer player, String recipe) {
        return player.getRecipeBook().contains(ResourceKey.create(Registries.RECIPE, CatgirlMod.id(recipe)));
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
