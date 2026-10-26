package dev.eliasnvx.catgirl.cosmetic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data component {@code catgirl:cosmetic}: the item is worn in this slot. A component, not an item class, so
 * modpacks can make any item wearable with a default-component tweak.
 */
public record Cosmetic(CatSlot slot) {

    public static final Codec<Cosmetic> CODEC = RecordCodecBuilder.create(i -> i.group(
            CatSlot.CODEC.fieldOf("slot").forGetter(Cosmetic::slot)
    ).apply(i, Cosmetic::new));

    public static final StreamCodec<ByteBuf, Cosmetic> STREAM_CODEC =
            CatSlot.STREAM_CODEC.map(Cosmetic::new, Cosmetic::slot);
}
