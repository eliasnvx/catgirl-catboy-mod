package dev.eliasnvx.catgirl.item;

import dev.eliasnvx.catgirl.set.CatSets;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Cat food: 2 food for people, 8 for someone wearing a set (they're basically a cat). Cats are tempted by
 * it and take it for taming, through the vanilla tag {@code #minecraft:cat_food}.
 */
public class CatFoodItem extends Item {

    public static final int BASE_FOOD = 2;
    /** Total 8 in a set: the base plus this. */
    static final int SET_BONUS_FOOD = 6;
    static final float SET_BONUS_SATURATION = 0.5F;

    public CatFoodItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player && CatSets.mask(player) != 0) {
            player.getFoodData().eat(SET_BONUS_FOOD, SET_BONUS_SATURATION);
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
