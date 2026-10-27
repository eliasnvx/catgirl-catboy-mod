package dev.eliasnvx.catgirl.mixin;

import dev.eliasnvx.catgirl.cosmetic.CatSlot;
import dev.eliasnvx.catgirl.cosmetic.CosmeticsManager;
import dev.eliasnvx.catgirl.menu.CosmeticSlot;
import dev.eliasnvx.catgirl.menu.CosmeticsContainer;
import dev.eliasnvx.catgirl.menu.CosmeticsLayout;
import dev.eliasnvx.catgirl.menu.InventoryMenuCosmetics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The cosmetic slots are part of the player's own inventory menu, after the vanilla slots (so vanilla indexes stay
 * put). They are a view over the cosmetics attachment like before: clicks, drags and syncing go through the normal
 * menu code on both sides. Shift-click puts a cosmetic from the inventory into its empty slot.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu implements InventoryMenuCosmetics {

    @Unique
    private int catgirl$cosmeticStart;

    private InventoryMenuMixin() {
        super(null, 0);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void catgirl$addCosmeticSlots(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        CosmeticsContainer cosmetics = new CosmeticsContainer(owner);
        catgirl$cosmeticStart = slots.size();
        for (int i = 0; i < cosmetics.getContainerSize(); i++) {
            addSlot(new CosmeticSlot(cosmetics, i, CosmeticsLayout.slotX(CosmeticsLayout.SURVIVAL_WIDTH, i), CosmeticsLayout.slotY(i)));
        }
    }

    @Override
    public int catgirl$cosmeticStart() {
        return catgirl$cosmeticStart;
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void catgirl$quickMoveIntoCosmeticSlot(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        // from the inventory, hotbar or offhand only; out of a cosmetic slot vanilla's fallback already works
        if (index < InventoryMenu.INV_SLOT_START || index > InventoryMenu.SHIELD_SLOT) {
            return;
        }
        Slot from = slots.get(index);
        ItemStack stack = from.getItem();
        CatSlot target = CosmeticsManager.slotOf(stack);
        if (target == null) {
            return;
        }
        int to = catgirl$cosmeticStart + target.ordinal();
        if (slots.get(to).hasItem()) {
            return; // taken: vanilla moves it between inventory and hotbar as usual
        }
        ItemStack original = stack.copy();
        if (!moveItemStackTo(stack, to, to + 1, false)) {
            return;
        }
        if (stack.isEmpty()) {
            from.setByPlayer(ItemStack.EMPTY, original);
        } else {
            from.setChanged();
        }
        cir.setReturnValue(original);
    }
}
