package dev.eliasnvx.catgirl.cosmetic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Data component {@code catgirl:cosmetic}: the item is worn in this slot. A component, not an item class, so
 * modpacks can make any item wearable with a default-component tweak.
 */
public record Cosmetic(CatSlot slot) {

    public static final Codec<Cosmetic> CODEC = RecordCodecBuilder.create(i -> i.group(
            CatSlot.CODEC.fieldOf("slot").forGetter(Cosmetic::slot)
    ).apply(i, Cosmetic::new));

}
