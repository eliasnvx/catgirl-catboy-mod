package dev.eliasnvx.catgirl.mixin.client;

import dev.eliasnvx.catgirl.client.CosmeticsPanel;
import dev.eliasnvx.catgirl.menu.CosmeticSlot;
import dev.eliasnvx.catgirl.menu.CosmeticsLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The cosmetics panel on the creative inventory tab. That tab wraps every slot of the player's inventory menu and
 * places them by index; the cosmetic slots would land on the hotbar, so they get the panel's positions instead.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {

    private static final String SLOT_WRAPPER =
            "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V";

    @Shadow
    private boolean hasClickedOutside;

    @ModifyArg(method = "selectTab", index = 2, at = @At(value = "INVOKE", target = SLOT_WRAPPER))
    private int catgirl$cosmeticSlotX(Slot target, int index, int x, int y) {
        return target instanceof CosmeticSlot slot
                ? CosmeticsLayout.slotX(((AbstractContainerScreenAccessor) (Object) this).catgirl$imageWidth(), slot.getContainerSlot())
                : x;
    }

    @ModifyArg(method = "selectTab", index = 3, at = @At(value = "INVOKE", target = SLOT_WRAPPER))
    private int catgirl$cosmeticSlotY(Slot target, int index, int x, int y) {
        return target instanceof CosmeticSlot slot ? CosmeticsLayout.slotY(slot.getContainerSlot()) : y;
    }

    @Inject(method = "hasClickedOutside", at = @At("RETURN"), cancellable = true)
    private void catgirl$panelIsInside(double mouseX, double mouseY, int left, int top, int button, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && CosmeticsPanel.contains((AbstractContainerScreen<?>) (Object) this, mouseX, mouseY)) {
            hasClickedOutside = false;
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void catgirl$cosmeticsPanel(GuiGraphics g, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        CosmeticsPanel.extractBackground((AbstractContainerScreen<?>) (Object) this, g);
    }
}
