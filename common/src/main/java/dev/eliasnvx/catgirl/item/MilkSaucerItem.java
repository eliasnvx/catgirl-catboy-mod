package dev.eliasnvx.catgirl.item;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Milk in a saucer: clears the harmful effects, like milk, but keeps the good ones, and there's no bucket
 * in your hand. The bowl comes back.
 */
public class MilkSaucerItem extends DrinkItem {

    public MilkSaucerItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            List<MobEffectInstance> harmful = new ArrayList<>();
            for (MobEffectInstance effect : entity.getActiveEffects()) {
                if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    harmful.add(effect);
                }
            }
            for (MobEffectInstance effect : harmful) {
                entity.removeEffect(effect.getEffect());
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
