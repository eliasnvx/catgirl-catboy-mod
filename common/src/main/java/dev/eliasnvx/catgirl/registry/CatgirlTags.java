package dev.eliasnvx.catgirl.registry;

import dev.eliasnvx.catgirl.CatgirlMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Item tags for addons, modpacks and resource packs. The sets are defined by these tags, so a
 * pack can make its own items count toward a set by adding them here.
 */
public final class CatgirlTags {

    public static final TagKey<Item> COSMETICS = tag("cosmetics");
    public static final TagKey<Item> EARS = tag("ears");
    public static final TagKey<Item> TAILS = tag("tails");
    public static final TagKey<Item> COLLARS = tag("collars");
    /** Paw gloves and mittens. */
    public static final TagKey<Item> HAND_PAWS = tag("hand_paws");
    /** Paw slippers; they also make steps quieter on their own. */
    public static final TagKey<Item> FOOT_PAWS = tag("foot_paws");
    public static final TagKey<Item> LEG_WARMERS = tag("leg_warmers");
    /** Raw fish nekos take: taming the Stray Neko, the Fisher Neko's quest, cat food. */
    public static final TagKey<Item> RAW_FISH = tag("raw_fish");
    /** What the Cat Spirit takes for Nine Lives (small flowers). */
    public static final TagKey<Item> SPIRIT_GIFTS = tag("spirit_gifts");
    /** Jingle when the wearer jumps (neck or head slot). */
    public static final TagKey<Item> BELLS = tag("bells");
    /** Head pieces with ears of their own (cat-ear headphones): the ears slot isn't drawn under them. */
    public static final TagKey<Item> COVERS_EARS = tag("covers_ears");

    private CatgirlTags() {
    }

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, CatgirlMod.id(name));
    }
}
