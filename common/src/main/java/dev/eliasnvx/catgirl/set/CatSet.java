package dev.eliasnvx.catgirl.set;

import dev.eliasnvx.catgirl.cosmetic.CatSlot;
import dev.eliasnvx.catgirl.cosmetic.CosmeticInventory;
import dev.eliasnvx.catgirl.registry.CatgirlTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * The three sets. A set is active while every one of its slots holds an item of the given tag.
 * Bonuses are applied by {@link SetBonuses}.
 */
public enum CatSet {
    /** Ears + tail + collar + hand paws: quieter steps, a higher jump, night vision on a key, hearts when meowing. */
    FULL_NEKO(new Piece(CatSlot.EARS, CatgirlTags.EARS), new Piece(CatSlot.TAIL, CatgirlTags.TAILS),
            new Piece(CatSlot.NECK, CatgirlTags.COLLARS), new Piece(CatSlot.HANDS, CatgirlTags.HAND_PAWS)),
    /** Ears + collar: cats and ocelots don't run away and follow you; a cat nearby counts as rest (no phantoms). */
    HOUSE_CAT(new Piece(CatSlot.EARS, CatgirlTags.EARS), new Piece(CatSlot.NECK, CatgirlTags.COLLARS)),
    /** Tail + slippers + leg warmers: no fall damage up to 10 blocks, landing on all fours. */
    ALLEY_CAT(new Piece(CatSlot.TAIL, CatgirlTags.TAILS), new Piece(CatSlot.FEET, CatgirlTags.FOOT_PAWS),
            new Piece(CatSlot.LEGS, CatgirlTags.LEG_WARMERS));

    public record Piece(CatSlot slot, TagKey<Item> items) {
    }

    private final List<Piece> pieces;
    private final String name = name().toLowerCase(Locale.ROOT);

    CatSet(Piece... pieces) {
        this.pieces = List.of(pieces);
    }

    public List<Piece> pieces() {
        return pieces;
    }

    public boolean isWorn(CosmeticInventory inventory) {
        for (Piece piece : pieces) {
            if (!inventory.get(piece.slot()).is(piece.items())) {
                return false;
            }
        }
        return true;
    }

    /** Whether this item counts toward the set (item tooltips). */
    public boolean includes(ItemStack stack) {
        for (Piece piece : pieces) {
            if (stack.is(piece.items())) {
                return true;
            }
        }
        return false;
    }

    /** {@code set.catgirl.<set>} */
    public String translationKey() {
        return "set.catgirl." + name;
    }

    public String id() {
        return name;
    }
}
