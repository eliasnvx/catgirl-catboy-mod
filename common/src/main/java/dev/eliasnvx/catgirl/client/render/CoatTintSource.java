package dev.eliasnvx.catgirl.client.render;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.registry.CatgirlComponents;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Item model tint {@code catgirl:coat}: colors a grayscale icon layer with one part of the item's coat.
 * {@code {"type": "catgirl:coat", "part": "inner"}}
 */
public record CoatTintSource(Coat.Part part) implements ItemTintSource {

    public static final Identifier ID = CatgirlMod.id("coat");

    public static final MapCodec<CoatTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Coat.Part.CODEC.fieldOf("part").forGetter(CoatTintSource::part)
    ).apply(i, CoatTintSource::new));

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        return ARGB.opaque(stack.getOrDefault(CatgirlComponents.COAT.get(), Coat.DEFAULT).color(part));
    }

    @Override
    public MapCodec<CoatTintSource> type() {
        return MAP_CODEC;
    }
}
