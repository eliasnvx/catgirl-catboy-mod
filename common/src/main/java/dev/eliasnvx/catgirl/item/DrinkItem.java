package dev.eliasnvx.catgirl.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A food that is drunk (drinking animation and sound), optionally clearing one effect, and gives its container back
 * (1.20.1 foods have no "converts to").
 */
public class DrinkItem extends Item {

    private final @Nullable MobEffect clears;
    private final Item container;

    public DrinkItem(Properties properties, Item container) {
        this(properties, null, container);
    }

    public DrinkItem(Properties properties, @Nullable MobEffect clears, Item container) {
        super(properties);
        this.clears = clears;
        this.container = container;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (clears != null && !level.isClientSide()) {
            entity.removeEffect(clears);
        }
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        if (Stacks.keepsItems(entity)) {
            return rest;
        }
        if (rest.isEmpty()) {
            return new ItemStack(container);
        }
        if (entity instanceof Player player && !player.getInventory().add(new ItemStack(container))) {
            player.drop(new ItemStack(container), false);
        }
        return rest;
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
