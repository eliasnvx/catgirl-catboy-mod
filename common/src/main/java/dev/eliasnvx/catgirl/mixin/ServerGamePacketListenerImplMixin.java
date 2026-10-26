package dev.eliasnvx.catgirl.mixin;

import dev.eliasnvx.catgirl.cosmetic.CatSlot;
import dev.eliasnvx.catgirl.menu.InventoryMenuCosmetics;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The creative inventory edits the player's inventory menu on the client and sends every changed slot; vanilla only
 * accepts slots 1-45. This accepts the cosmetic slots too, with the same checks a click would get.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleSetCreativeModeSlot", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V"))
    private void catgirl$creativeCosmeticSlot(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        int index = packet.slotNum();
        int start = ((InventoryMenuCosmetics) player.inventoryMenu).catgirl$cosmeticStart();
        if (index < start || index >= start + CatSlot.values().length) {
            return;
        }
        ci.cancel();
        ItemStack stack = packet.itemStack();
        Slot slot = player.inventoryMenu.getSlot(index);
        if (player.hasInfiniteMaterials() && (stack.isEmpty() || stack.getCount() == 1 && slot.mayPlace(stack))) {
            slot.setByPlayer(stack);
            player.inventoryMenu.setRemoteSlot(index, stack);
            player.inventoryMenu.broadcastChanges();
        }
    }
}
