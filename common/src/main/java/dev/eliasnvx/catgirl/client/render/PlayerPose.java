package dev.eliasnvx.catgirl.client.render;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * What the cosmetic models read about the wearer this frame. 1.20.1 has no render states, so the cosmetic layer fills
 * one reusable instance per draw (same field names as the 26.x render state, so the models stay alike across versions).
 */
public final class PlayerPose {

    /** {@code AbstractClientPlayer#getModelName} of slim-armed skins. */
    public static final String SLIM = "slim";

    public int id;
    public float ageInTicks;
    public float walkAnimationPos;
    public float walkAnimationSpeed;
    public float bodyRot;
    public boolean hasRedOverlay;
    public boolean isInvisible;
    public boolean slim;
    public ItemStack headEquipment = ItemStack.EMPTY;
    public CosmeticRenderData cosmetics = CosmeticRenderData.STILL;

    public void capture(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks) {
        this.id = player.getId();
        this.ageInTicks = ageInTicks;
        this.walkAnimationPos = limbSwing;
        this.walkAnimationSpeed = limbSwingAmount;
        this.bodyRot = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        this.hasRedOverlay = player.hurtTime > 0 || player.deathTime > 0;
        this.isInvisible = player.isInvisible();
        this.slim = SLIM.equals(player.getModelName());
        this.headEquipment = player.getItemBySlot(EquipmentSlot.HEAD);
    }
}
