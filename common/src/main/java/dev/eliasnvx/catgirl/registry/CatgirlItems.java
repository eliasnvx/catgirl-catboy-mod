package dev.eliasnvx.catgirl.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.block.CatgirlBlocks;
import dev.eliasnvx.catgirl.effect.CatgirlEffects;
import dev.eliasnvx.catgirl.item.CatgirlFoods;
import dev.eliasnvx.catgirl.item.CatnipItem;
import dev.eliasnvx.catgirl.item.DyeableCosmeticItem;
import dev.eliasnvx.catgirl.item.YarnBallItem;
import dev.eliasnvx.catgirl.cosmetic.CatSlot;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.cosmetic.Cosmetic;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import dev.architectury.core.item.ArchitecturySpawnEggItem;
import dev.eliasnvx.catgirl.entity.CatgirlEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class CatgirlItems {

    public static final DeferredRegister<Item> REGISTER = DeferredRegister.create(CatgirlMod.MOD_ID, Registries.ITEM);
    private static final int SPAWN_EGG_TINT = 0xFFFFFF;
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(CatgirlMod.MOD_ID, Registries.CREATIVE_MODE_TAB);
    /** Items whose creative tab entry is one stack per {@link Coat}. */
    private static final List<RegistrySupplier<Item>> COATED = new ArrayList<>();
    private static final List<RegistrySupplier<Item>> DYEABLE = new ArrayList<>();

    public static final RegistrySupplier<Item> CAT_EARS = coated("cat_ears", CatSlot.EARS);
    public static final RegistrySupplier<Item> CAT_TAIL = coated("cat_tail", CatSlot.TAIL);
    public static final RegistrySupplier<Item> PAW_GLOVES = coated("paw_gloves", CatSlot.HANDS);
    public static final RegistrySupplier<Item> PAW_MITTENS = coated("paw_mittens", CatSlot.HANDS);
    public static final RegistrySupplier<Item> LEG_WARMERS = coated("leg_warmers", CatSlot.LEGS);
    public static final RegistrySupplier<Item> PAW_SLIPPERS = coated("paw_slippers", CatSlot.FEET);

    /** Undyed colors of the collars; the icons and the renderer use the same ones. */
    public static final int BELL_COLLAR_COLOR = 0xC8323C;
    public static final int VELVET_CHOKER_COLOR = 0x2A2A33;
    public static final int NECK_RIBBON_COLOR = 0xF291BE;
    /** Dyeable with any dye (vanilla crafting_dye recipes); the bell jingles when the wearer jumps. */
    public static final RegistrySupplier<Item> BELL_COLLAR = dyeable("bell_collar", CatSlot.NECK);
    public static final RegistrySupplier<Item> VELVET_CHOKER = dyeable("velvet_choker", CatSlot.NECK);
    public static final RegistrySupplier<Item> NECK_RIBBON = dyeable("neck_ribbon", CatSlot.NECK);

    public static final int WHISKERS_COLOR = 0x2A2328;
    public static final int BLUSH_COLOR = 0xF06292;
    public static final int CAT_EAR_HEADPHONES_COLOR = 0xF7A8CC;
    public static final int EAR_BOWS_COLOR = 0xE0405A;
    public static final int BELL_HEADBAND_COLOR = 0x2A2A33;
    /** Face pieces, also dyeable (white whiskers on a dark skin, a different blush). */
    public static final RegistrySupplier<Item> WHISKERS = dyeable("whiskers", CatSlot.FACE);
    public static final RegistrySupplier<Item> BLUSH = dyeable("blush", CatSlot.FACE);
    public static final RegistrySupplier<Item> CAT_EAR_HEADPHONES = dyeable("cat_ear_headphones", CatSlot.HEAD);
    public static final RegistrySupplier<Item> EAR_BOWS = dyeable("ear_bows", CatSlot.HEAD);
    /** Jingles on jumps like the bell collar (#catgirl:bells). */
    public static final RegistrySupplier<Item> BELL_HEADBAND = dyeable("bell_headband", CatSlot.HEAD);

    // mechanics
    public static final RegistrySupplier<Item> CAT_BED = register("cat_bed",
            props -> new BlockItem(CatgirlBlocks.CAT_BED.get(), props));
    public static final RegistrySupplier<Item> CARDBOARD_BOX = register("cardboard_box",
            props -> new BlockItem(CatgirlBlocks.CARDBOARD_BOX.get(), props));
    /** Plants the catnip bush, or is eaten: Haste and the catnip tumbles (CatnipItem). */
    public static final RegistrySupplier<Item> CATNIP = register("catnip", props -> new CatnipItem(CatgirlBlocks.CATNIP.get(),
            props.food(new FoodProperties.Builder().nutrition(1).saturationMod(0.1F).alwaysEat().fast()
                    .effect(new MobEffectInstance(MobEffects.DIG_SPEED, CatnipItem.EFFECT_TICKS), 1.0F)
                    .effect(new MobEffectInstance(CatgirlEffects.CATNIP.get(), CatnipItem.EFFECT_TICKS), 1.0F).build())));
    public static final RegistrySupplier<Item> YARN_BALL = register("yarn_ball", props -> new YarnBallItem(props.stacksTo(16)));

    public static final RegistrySupplier<Item> STRAY_NEKO_SPAWN_EGG = spawnEgg("stray_neko_spawn_egg", CatgirlEntities.STRAY_NEKO);
    public static final RegistrySupplier<Item> FISHER_NEKO_SPAWN_EGG = spawnEgg("fisher_neko_spawn_egg", CatgirlEntities.FISHER_NEKO);
    public static final RegistrySupplier<Item> BAKENEKO_SPAWN_EGG = spawnEgg("bakeneko_spawn_egg", CatgirlEntities.BAKENEKO);
    public static final RegistrySupplier<Item> CAT_SPIRIT_SPAWN_EGG = spawnEgg("cat_spirit_spawn_egg", CatgirlEntities.CAT_SPIRIT);
    public static final RegistrySupplier<Item> KITTEN_SPAWN_EGG = spawnEgg("kitten_spawn_egg", CatgirlEntities.KITTEN);
    private static final List<RegistrySupplier<Item>> SPAWN_EGGS = List.of(STRAY_NEKO_SPAWN_EGG, FISHER_NEKO_SPAWN_EGG,
            BAKENEKO_SPAWN_EGG, CAT_SPIRIT_SPAWN_EGG, KITTEN_SPAWN_EGG);

    public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register("catgirl", () -> CreativeTabRegistry.create(builder -> builder
            .title(Component.translatable("itemGroup.catgirl"))
            .icon(() -> withCoat(CAT_EARS.get(), Coat.GINGER))
            .displayItems((parameters, output) -> {
                for (RegistrySupplier<Item> item : COATED) {
                    for (Coat coat : Coat.values()) {
                        output.accept(withCoat(item.get(), coat));
                    }
                }
                for (RegistrySupplier<Item> item : DYEABLE) {
                    output.accept(item.get());
                }
                output.accept(CAT_BED.get());
                output.accept(CARDBOARD_BOX.get());
                output.accept(CATNIP.get());
                output.accept(YARN_BALL.get());
                for (RegistrySupplier<Item> food : CatgirlFoods.ALL) {
                    output.accept(food.get());
                }
                for (RegistrySupplier<Item> egg : SPAWN_EGGS) {
                    output.accept(egg.get());
                }
            })));

    private CatgirlItems() {
    }

    /** Fur pieces with a {@link Coat} (one creative/JEI entry per coat). */
    public static List<RegistrySupplier<Item>> coated() {
        return List.copyOf(COATED);
    }

    /** Accessories dyed like leather armor. */
    public static List<RegistrySupplier<Item>> dyeable() {
        return List.copyOf(DYEABLE);
    }

    public static List<RegistrySupplier<Item>> spawnEggs() {
        return SPAWN_EGGS;
    }

    public static ItemStack withCoat(Item item, Coat coat) {
        ItemStack stack = new ItemStack(item);
        CatgirlComponents.COAT.set(stack, coat);
        return stack;
    }

    /** A wearable fur piece; the coat comes from crafting (wool color) or trades, white if missing. */
    private static RegistrySupplier<Item> coated(String name, CatSlot slot) {
        RegistrySupplier<Item> item = register(name, props -> {
            Item piece = new Item(props.stacksTo(1));
            CatgirlComponents.COSMETIC.setDefault(piece, new Cosmetic(slot));
            CatgirlComponents.COAT.setDefault(piece, Coat.DEFAULT);
            return piece;
        });
        COATED.add(item);
        return item;
    }

    /** An accessory colored by vanilla dyeing, like leather armor. */
    private static RegistrySupplier<Item> dyeable(String name, CatSlot slot) {
        RegistrySupplier<Item> item = register(name, props -> {
            Item piece = new DyeableCosmeticItem(props.stacksTo(1));
            CatgirlComponents.COSMETIC.setDefault(piece, new Cosmetic(slot));
            return piece;
        });
        DYEABLE.add(item);
        return item;
    }

    /** The egg's texture shows the mob itself (the newer vanilla style), so the tint colors are white. */
    private static RegistrySupplier<Item> spawnEgg(String name, RegistrySupplier<? extends EntityType<? extends Mob>> type) {
        return register(name, props -> new ArchitecturySpawnEggItem(type, SPAWN_EGG_TINT, SPAWN_EGG_TINT, props));
    }

    /** Also used by CatgirlFoods. */
    public static RegistrySupplier<Item> register(String name, Function<Item.Properties, Item> factory) {
        return REGISTER.register(name, () -> factory.apply(new Item.Properties()));
    }
}
