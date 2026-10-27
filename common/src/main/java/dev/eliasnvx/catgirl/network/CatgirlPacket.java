package dev.eliasnvx.catgirl.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** A packet of the mod: an id and its bytes (1.20.1 has no custom payload types; later versions use those). */
public interface CatgirlPacket {

    ResourceLocation id();

    void write(FriendlyByteBuf buf);
}
