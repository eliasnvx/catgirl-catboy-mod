package dev.eliasnvx.catgirl.platform.forge;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.cosmetic.CosmeticInventory;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The mod's data on a player, as a Forge capability (Forge 47 has no data attachments; adapted from Femboy Mod). Saved
 * with the player; copied to the new player on respawn and on return from the End, like the later versions' copyOnDeath
 * attachment.
 */
public final class CatgirlPlayerData {

    /** Capability id on the player ({@code catgirl:player_data}). */
    public static final ResourceLocation ID = CatgirlMod.id("player_data");

    public static final Capability<CatgirlPlayerData> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });

    private static final String COSMETICS_KEY = "cosmetics";

    CosmeticInventory cosmetics = CosmeticInventory.EMPTY;

    void copyFrom(CatgirlPlayerData old) {
        cosmetics = old.cosmetics;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CosmeticInventory.CODEC.encodeStart(NbtOps.INSTANCE, cosmetics)
                .resultOrPartial(error -> CatgirlMod.LOGGER.error("Failed to save player cosmetics: {}", error))
                .ifPresent(encoded -> tag.put(COSMETICS_KEY, encoded));
        return tag;
    }

    void load(CompoundTag tag) {
        cosmetics = !tag.contains(COSMETICS_KEY) ? CosmeticInventory.EMPTY
                : CosmeticInventory.CODEC.parse(NbtOps.INSTANCE, tag.get(COSMETICS_KEY))
                        .resultOrPartial(error -> CatgirlMod.LOGGER.error("Failed to load player cosmetics: {}", error))
                        .orElse(CosmeticInventory.EMPTY);
    }

    /** Attached to every player (both logical sides); saved with the player NBT. */
    static final class Provider implements ICapabilitySerializable<CompoundTag> {

        private final CatgirlPlayerData data = new CatgirlPlayerData();
        // Not invalidated on removal: PlayerEvent.Clone revives the old player's caps to read them
        private final LazyOptional<CatgirlPlayerData> optional = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return CAPABILITY.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.save();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            data.load(tag);
        }
    }
}
