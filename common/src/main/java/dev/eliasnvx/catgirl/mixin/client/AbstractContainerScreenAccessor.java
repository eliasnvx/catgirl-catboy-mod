package dev.eliasnvx.catgirl.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The inventory's position moves with the recipe book; the cosmetics panel sits on its right edge. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("leftPos")
    int catgirl$leftPos();

    @Accessor("topPos")
    int catgirl$topPos();

    @Accessor("imageWidth")
    int catgirl$imageWidth();
}
