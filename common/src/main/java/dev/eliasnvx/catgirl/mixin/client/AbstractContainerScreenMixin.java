package dev.eliasnvx.catgirl.mixin.client;

import dev.eliasnvx.catgirl.client.CosmeticsPanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Names of empty cosmetic slots and the set stars' tooltips in the inventory panel. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Shadow
    protected @Nullable Slot hoveredSlot;

    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void catgirl$cosmeticsTooltips(GuiGraphics g, int mouseX, int mouseY, CallbackInfo ci) {
        if (CosmeticsPanel.tooltip((AbstractContainerScreen<?>) (Object) this, g, hoveredSlot, menu.getCarried(), mouseX, mouseY)) {
            ci.cancel();
        }
    }
}
