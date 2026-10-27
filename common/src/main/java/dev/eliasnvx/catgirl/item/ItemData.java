package dev.eliasnvx.catgirl.item;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Item data stored in a stack's NBT under {@code key} (1.20.1 has no data components; adapted from Femboy Mod's
 * NbtItemData). An item can have a default value, like a default component on later versions. Decoded values are cached
 * by the identity of the stored tag, so reading the same stack every frame doesn't decode again.
 */
public final class ItemData<T> {

    private static final int CACHE_SIZE = 4096;

    private final String key;
    private final Codec<T> codec;
    // Weak identity keys: an entry goes away with the tag; a changed value is a new tag instance
    private final Cache<Tag, Optional<T>> decoded = CacheBuilder.newBuilder().weakKeys().maximumSize(CACHE_SIZE).build();
    private final Map<Item, T> defaults = new ConcurrentHashMap<>();

    public ItemData(String key, Codec<T> codec) {
        this.key = key;
        this.codec = codec;
    }

    /** Every stack of {@code item} has this value unless it stores its own. Call it when the item is created. */
    public void setDefault(ItemLike item, T value) {
        defaults.put(item.asItem(), value);
    }

    public @Nullable T get(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        Tag stored = tag == null ? null : tag.get(key);
        if (stored == null) {
            return stack.isEmpty() ? null : defaults.get(stack.getItem());
        }
        Optional<T> cached = decoded.getIfPresent(stored);
        if (cached == null) {
            cached = codec.parse(NbtOps.INSTANCE, stored).result();
            decoded.put(stored, cached);
        }
        return cached.orElse(null);
    }

    public T getOrDefault(ItemStack stack, T fallback) {
        T value = get(stack);
        return value == null ? fallback : value;
    }

    public void set(ItemStack stack, T value) {
        if (value.equals(defaults.get(stack.getItem()))) {
            stack.removeTagKey(key); // stacks at their default stay stackable with fresh ones
            return;
        }
        codec.encodeStart(NbtOps.INSTANCE, value).result().ifPresent(encoded -> stack.getOrCreateTag().put(key, encoded));
    }
}
