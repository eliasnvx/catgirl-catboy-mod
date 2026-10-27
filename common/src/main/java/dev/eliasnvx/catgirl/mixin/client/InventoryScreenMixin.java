package dev.eliasnvx.catgirl.mixin.client;

import dev.eliasnvx.catgirl.client.CosmeticsPanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The cosmetics panel on the right of the survival inventory; a click on it is inside the window (else it drops the
 * carried stack).
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void catgirl$cosmeticsPanel(GuiGraphics g, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        CosmeticsPanel.extractBackground((AbstractContainerScreen<?>) (Object) this, g);
    }

    @Inject(method = "hasClickedOutside", at = @At("RETURN"), cancellable = true)
    private void catgirl$panelIsInside(double mouseX, double mouseY, int left, int top, int button, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && CosmeticsPanel.contains((AbstractContainerScreen<?>) (Object) this, mouseX, mouseY)) {
            cir.setReturnValue(false);
        }
    }
}
