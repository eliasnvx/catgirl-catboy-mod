package dev.eliasnvx.catgirl.cosmetic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable snapshot of a player's cosmetic slots, stored as a player attachment. Keyed by slot id, so slots
 * that a later version removes keep their items instead of losing them. Only non-empty slots are kept.
 */
public record CosmeticInventory(Map<ResourceLocation, ItemStack> items) {

    public static final CosmeticInventory EMPTY = new CosmeticInventory(Map.of());

    public static final Codec<CosmeticInventory> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(ResourceLocation.CODEC, ItemStack.CODEC).fieldOf("items").forGetter(CosmeticInventory::items)
    ).apply(i, CosmeticInventory::new));


    public CosmeticInventory {
        Map<ResourceLocation, ItemStack> copy = new LinkedHashMap<>();
        items.forEach((slot, stack) -> {
            if (!stack.isEmpty()) {
                copy.put(slot, stack);
            }
        });
        items = Collections.unmodifiableMap(copy);
    }

    public ItemStack get(CatSlot slot) {
        return items.getOrDefault(slot.id(), ItemStack.EMPTY);
    }

    public CosmeticInventory with(CatSlot slot, ItemStack stack) {
        Map<ResourceLocation, ItemStack> next = new LinkedHashMap<>(items);
        if (stack.isEmpty()) {
            next.remove(slot.id());
        } else {
            next.put(slot.id(), stack);
        }
        return new CosmeticInventory(next);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
