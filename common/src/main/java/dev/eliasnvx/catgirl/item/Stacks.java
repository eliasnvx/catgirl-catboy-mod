package dev.eliasnvx.catgirl.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Later versions' ItemStack helpers: creative players keep what they use. */
public final class Stacks {

    private Stacks() {
    }

    public static boolean keepsItems(@Nullable LivingEntity entity) {
        return entity instanceof Player player && player.getAbilities().instabuild;
    }

    public static void consume(ItemStack stack, int amount, @Nullable LivingEntity entity) {
        if (!keepsItems(entity)) {
            stack.shrink(amount);
        }
    }

    /** Takes {@code amount} off the stack and returns them; a creative player's stack stays as it is. */
    public static ItemStack consumeAndReturn(ItemStack stack, int amount, @Nullable LivingEntity entity) {
        ItemStack taken = stack.copyWithCount(amount);
        consume(stack, amount, entity);
        return taken;
    }
}
