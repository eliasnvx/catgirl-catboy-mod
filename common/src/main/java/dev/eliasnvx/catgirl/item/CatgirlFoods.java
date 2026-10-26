package dev.eliasnvx.catgirl.item;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.catgirl.registry.CatgirlItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.RemoveStatusEffectsConsumeEffect;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.List;

/**
 * Food: crafting chains with cat-themed small buffs. Recipes are JSON, so
 * modpacks can change them. Registered through CatgirlItems, so creative tab order follows declaration order here.
 */
public final class CatgirlFoods {

    // nutrition / saturation modifier, effect lengths
    private static final float FAST = 0.8F;
    private static final int COOKIE_EFFECT_TICKS = 20 * 20;
    private static final int TAKOYAKI_REGEN_TICKS = 5 * 20;
    private static final int SQUID_TENTACLES_MAX = 2;

    /** Tuna onigiri: the mod's everyday food. Wheat + cooked fish + dried kelp. */
    public static final RegistrySupplier<Item> ONIGIRI = CatgirlItems.register("onigiri",
            props -> new Item(props.food(food(6, 0.6F))));
    /** Cod + wheat, x4; cooked into fish sticks. */
    public static final RegistrySupplier<Item> RAW_FISH_STICKS = CatgirlItems.register("raw_fish_sticks", Item::new);
    /** Quick to eat. */
    public static final RegistrySupplier<Item> FISH_STICKS = CatgirlItems.register("fish_sticks",
            props -> new Item(props.food(food(4, 0.5F), Consumables.defaultFood().consumeSeconds(FAST).build())));
    /** One milk bucket pours three saucers; drinking one clears the bad effects (not the good ones, unlike milk). */
    public static final RegistrySupplier<Item> MILK_SAUCER = CatgirlItems.register("milk_saucer",
            props -> new MilkSaucerItem(props.stacksTo(16).usingConvertsTo(Items.BOWL)
                    .component(DataComponents.CONSUMABLE, Consumables.defaultDrink().build())));
    /** 2 food, 8 in any set; cats come for it and can be tamed with it (#minecraft:cat_food). */
    public static final RegistrySupplier<Item> CAT_FOOD = CatgirlItems.register("cat_food",
            props -> new CatFoodItem(props.food(food(CatFoodItem.BASE_FOOD, 0.2F))));
    /** Like catnip, but a snack: Haste and Jump Boost. */
    public static final RegistrySupplier<Item> CATNIP_COOKIE = CatgirlItems.register("catnip_cookie",
            props -> new Item(props.food(alwaysEdible(2, 0.1F), Consumables.defaultFood().consumeSeconds(FAST)
                    .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                            new MobEffectInstance(MobEffects.HASTE, COOKIE_EFFECT_TICKS),
                            new MobEffectInstance(MobEffects.JUMP_BOOST, COOKIE_EFFECT_TICKS)))).build())));
    /** No octopus in Minecraft: squids drop tentacles (SQUID_TENTACLES_MAX, see {@link #register()}). */
    public static final RegistrySupplier<Item> SQUID_TENTACLE = CatgirlItems.register("squid_tentacle",
            props -> new Item(props.food(food(1, 0.1F))));
    public static final RegistrySupplier<Item> TAKOYAKI = CatgirlItems.register("takoyaki",
            props -> new Item(props.food(food(7, 0.6F), Consumables.defaultFood()
                    .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, TAKOYAKI_REGEN_TICKS)))
                    .build())));
    /** Milk tea with berry pearls: shakes off tiredness (Mining Fatigue), gives the bottle back. A screenshot item. */
    public static final RegistrySupplier<Item> BUBBLE_TEA = CatgirlItems.register("bubble_tea",
            props -> new Item(props.stacksTo(16).usingConvertsTo(Items.GLASS_BOTTLE).food(food(4, 0.4F), Consumables.defaultDrink()
                    .onConsume(new RemoveStatusEffectsConsumeEffect(MobEffects.MINING_FATIGUE)).build())));
    /** Canned tuna: dense food for the road. */
    public static final RegistrySupplier<Item> TUNA_CAN = CatgirlItems.register("tuna_can",
            props -> new Item(props.food(food(5, 0.8F))));

    /** Creative tab order. */
    public static final List<RegistrySupplier<Item>> ALL = List.of(ONIGIRI, RAW_FISH_STICKS, FISH_STICKS, MILK_SAUCER, CAT_FOOD,
            CATNIP_COOKIE, SQUID_TENTACLE, TAKOYAKI, BUBBLE_TEA, TUNA_CAN);

    private CatgirlFoods() {
    }

    /** Class-loads the items into CatgirlItems' register and hooks the squid drops. */
    public static void register() {
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            // Squid and glow squid (a subclass) drop tentacles, like any mob loot (respects mobDrops)
            if (entity instanceof Squid && entity.level() instanceof ServerLevel level && level.getGameRules().get(GameRules.MOB_DROPS)) {
                int count = 1 + level.getRandom().nextInt(SQUID_TENTACLES_MAX);
                entity.spawnAtLocation(level, new ItemStack(SQUID_TENTACLE.get(), count));
            }
            return EventResult.pass();
        });
    }

    private static FoodProperties food(int nutrition, float saturation) {
        return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build();
    }

    private static FoodProperties alwaysEdible(int nutrition, float saturation) {
        return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).alwaysEdible().build();
    }
}
