package dev.eliasnvx.catgirl.mixin.client;

import dev.eliasnvx.catgirl.client.CosmeticsPanel;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A click on the survival inventory's cosmetics panel is inside the window (else it drops the carried stack). */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {

    @Inject(method = "hasClickedOutside", at = @At("RETURN"), cancellable = true)
    private void catgirl$panelIsInside(double mouseX, double mouseY, int left, int top, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && CosmeticsPanel.contains((AbstractContainerScreen<?>) (Object) this, mouseX, mouseY)) {
            cir.setReturnValue(false);
        }
    }
}
