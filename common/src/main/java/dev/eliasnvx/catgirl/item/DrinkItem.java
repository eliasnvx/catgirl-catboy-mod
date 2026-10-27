package dev.eliasnvx.catgirl.item;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** A food that is drunk (drinking animation and sound), optionally clearing one effect. */
public class DrinkItem extends Item {

    private final @Nullable Holder<MobEffect> clears;

    public DrinkItem(Properties properties) {
        this(properties, null);
    }

    public DrinkItem(Properties properties, @Nullable Holder<MobEffect> clears) {
        super(properties);
        this.clears = clears;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (clears != null && !level.isClientSide()) {
            entity.removeEffect(clears);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_DRINK;
    }
}
