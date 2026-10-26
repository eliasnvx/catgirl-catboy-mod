package dev.eliasnvx.catgirl.cosmetic;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * Data component {@code catgirl:coat}: one of the 8 fixed fur colors (decision 2026-10-24: fixed coats, no dyeing).
 * Models and icons are grayscale and tinted per {@link Part}, so a coat is five colors, not a texture.
 */
public enum Coat implements StringRepresentable {
    //               main      inner     points    patch     tuft
    BLACK(0x2B2A30, 0xE8A0B4, 0x2B2A30, 0x1C1B20, 0x8E8A94),
    WHITE(0xF4F1EE, 0xF2A7BD, 0xF4F1EE, 0xE2DDD8, 0xFFFFFF),
    GINGER(0xE38B3D, 0xF2B39A, 0xE38B3D, 0xA8541C, 0xF7DDB8),
    GRAY(0x8C8D96, 0xE3A6B6, 0x8C8D96, 0x5E5F68, 0xD8D8DE),
    SIAMESE(0xEEE0C8, 0xE9B3B9, 0x4A3628, 0xEEE0C8, 0xF8F1E4),
    CALICO(0xF4F1EE, 0xF2A7BD, 0xE38B3D, 0x2B2A30, 0xFFFFFF),
    PINK(0xF6A9C2, 0xFFD6E4, 0xF6A9C2, 0xE88BAA, 0xFFF0F5),
    NEON(0x4FF0D8, 0xFF6EC7, 0xFF6EC7, 0x2CCDB6, 0xC8FFF6);

    public static final Codec<Coat> CODEC = StringRepresentable.fromEnum(Coat::values);
    public static final StreamCodec<ByteBuf, Coat> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);

    /** Undyed fallback for items without the component. */
    public static final Coat DEFAULT = WHITE;

    /**
     * Color areas of fur.
     * MAIN body color, INNER inner ear and paw pads, POINTS ear tips and tail tip (Siamese points, Calico orange),
     * PATCH stripes and patches (tabby stripes, Calico black), TUFT light undercoat (fur tufts in the ears, tail belly).
     */
    public enum Part implements StringRepresentable {
        MAIN, INNER, POINTS, PATCH, TUFT;

        public static final Codec<Part> CODEC = StringRepresentable.fromEnum(Part::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final int[] colors;
    private final String name = name().toLowerCase(Locale.ROOT);

    Coat(int main, int inner, int points, int patch, int tuft) {
        this.colors = new int[]{main, inner, points, patch, tuft};
    }

    /** All part colors indexed by {@link Part#ordinal()}; shared, do not modify. */
    public int[] colors() {
        return colors;
    }

    /** 24-bit RGB of one part. */
    public int color(Part part) {
        return colors[part.ordinal()];
    }

    /** {@code coat.catgirl.<coat>} */
    public String translationKey() {
        return "coat.catgirl." + name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
