package dev.eliasnvx.catgirl.cosmetic;

import com.mojang.serialization.Codec;
import dev.eliasnvx.catgirl.CatgirlMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * Cosmetic slots. They sit on top of armor: armor is never removed or hidden.
 * Declaration order = screen order. Stored by id, so slots can be added later without breaking saves.
 */
public enum CatSlot implements StringRepresentable {
    EARS,
    HEAD,
    FACE,
    NECK,
    HANDS,
    LEGS,
    FEET,
    TAIL;

    public static final Codec<CatSlot> CODEC = StringRepresentable.fromEnum(CatSlot::values);

    private final String name = name().toLowerCase(Locale.ROOT);
    private final ResourceLocation id = CatgirlMod.id(name);

    public ResourceLocation id() {
        return id;
    }

    /** {@code cosmetic_slot.catgirl.<slot>} */
    public String translationKey() {
        return id.toLanguageKey("cosmetic_slot");
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
