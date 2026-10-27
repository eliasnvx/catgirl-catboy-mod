package dev.eliasnvx.catgirl.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Yarn ball: right-click throws it a few blocks ahead. It lands as an ordinary dropped item that
 * rolls about; cats chase it and bat it around (ChaseYarnGoal) until someone picks it up again.
 */
public class YarnBallItem extends Item {

    private static final double THROW_SPEED = 0.7;
    private static final double THROW_LIFT = 0.15;
    /** Long enough that the thrower doesn't catch it again right away. */
    private static final int PICKUP_DELAY = 40;
    private static final int COOLDOWN = 10;

    public YarnBallItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS,
                0.5F, 0.6F + level.getRandom().nextFloat() * 0.3F);
        if (level instanceof ServerLevel) {
            ItemStack thrown = Stacks.consumeAndReturn(held, 1, player);
            Vec3 look = player.getLookAngle();
            ItemEntity ball = new ItemEntity(level, player.getX(), player.getEyeY() - 0.2, player.getZ(), thrown,
                    look.x * THROW_SPEED, look.y * THROW_SPEED + THROW_LIFT, look.z * THROW_SPEED);
            ball.setPickUpDelay(PICKUP_DELAY);
            ball.setThrower(player.getUUID());
            level.addFreshEntity(ball);
        }
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
    }
}
