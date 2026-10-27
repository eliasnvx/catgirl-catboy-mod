package dev.eliasnvx.catgirl.meow;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.network.CatgirlPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** C2S: the meow key was pressed. Carries no data; the server decides everything (cooldown, who hears it). */
public record MeowPayload() implements CatgirlPacket {

    public static final MeowPayload INSTANCE = new MeowPayload();

    public static final ResourceLocation ID = CatgirlMod.id("meow");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
    }

    public static void handle(MeowPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) {
                Meow.meow(player);
            }
        });
    }
}
