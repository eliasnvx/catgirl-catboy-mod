package dev.eliasnvx.catgirl.mixin.client;

import dev.eliasnvx.catgirl.client.CosmeticsPanel;
import dev.eliasnvx.catgirl.menu.CosmeticsLayout;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Status effects are listed right of the inventory; with the cosmetics panel there, they move right of the panel. */
@Mixin(EffectRenderingInventoryScreen.class)
public abstract class EffectRenderingInventoryScreenMixin {

    @Redirect(method = {"canSeeEffects", "renderEffects"}, at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/client/gui/screens/inventory/EffectRenderingInventoryScreen;imageWidth:I"))
    private int catgirl$besidePanel(EffectRenderingInventoryScreen<?> screen) {
        int width = ((AbstractContainerScreenAccessor) screen).catgirl$imageWidth();
        return CosmeticsPanel.visible(screen) ? width + CosmeticsLayout.GAP + CosmeticsLayout.WIDTH : width;
    }
}
