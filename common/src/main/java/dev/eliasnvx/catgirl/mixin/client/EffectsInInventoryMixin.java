package dev.eliasnvx.catgirl.mixin.client;

import dev.eliasnvx.catgirl.client.CosmeticsPanel;
import dev.eliasnvx.catgirl.menu.CosmeticsLayout;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Status effects are listed right of the inventory; with the cosmetics panel there, they move right of the panel. */
@Mixin(EffectsInInventory.class)
public abstract class EffectsInInventoryMixin {

    @Redirect(method = {"canSeeEffects", "extractRenderState"}, at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;imageWidth:I"))
    private int catgirl$besidePanel(AbstractContainerScreen<?> screen) {
        int width = ((AbstractContainerScreenAccessor) screen).catgirl$imageWidth();
        return CosmeticsPanel.visible(screen) ? width + CosmeticsLayout.GAP + CosmeticsLayout.WIDTH : width;
    }
}
