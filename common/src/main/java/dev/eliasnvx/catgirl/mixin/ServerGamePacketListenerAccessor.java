package dev.eliasnvx.catgirl.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A player's network connection (GameTest mock players have one without a channel). */
@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerAccessor {

    @Accessor("connection")
    Connection catgirl$connection();
}
