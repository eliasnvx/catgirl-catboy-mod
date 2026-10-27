package dev.eliasnvx.catgirl.entity;

import dev.eliasnvx.catgirl.registry.CatgirlItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * A yarn ball on the ground is irresistible. Cats run to it and bat it away, again and again, until
 * someone picks it up. The ball is a plain dropped item (YarnBallItem throws it), so it rolls with item physics.
 */
final class ChaseYarnGoal extends Goal {

    private static final double RANGE = 14.0;
    private static final double SPEED = 1.3;
    private static final double BAT_REACH = 1.3;
    private static final double BAT_PUSH = 0.35;
    private static final double BAT_HOP = 0.2;
    private static final int BAT_COOLDOWN = 20;
    private static final int SEARCH_INTERVAL = 10;
    private static final int REPATH_INTERVAL = 10;

    private final PathfinderMob mob;
    private @Nullable ItemEntity ball;
    private int cooldown;
    private int repath;

    ChaseYarnGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.tickCount % SEARCH_INTERVAL != 0 || mob instanceof TamableAnimal tamable && tamable.isOrderedToSit()) {
            return false;
        }
        ball = nearestBall();
        return ball != null;
    }

    @Override
    public boolean canContinueToUse() {
        return ball != null && ball.isAlive() && mob.distanceToSqr(ball) < RANGE * RANGE * 4;
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        ball = null;
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (ball == null) {
            return;
        }
        mob.getLookControl().setLookAt(ball, mob.getMaxHeadYRot(), mob.getMaxHeadXRot());
        if (cooldown > 0) {
            cooldown--;
        }
        if (mob.distanceToSqr(ball) <= BAT_REACH * BAT_REACH) {
            if (cooldown == 0) {
                bat(ball);
            }
        } else if (--repath <= 0) {
            repath = adjustedTickDelay(REPATH_INTERVAL);
            mob.getNavigation().moveTo(ball, SPEED);
        }
    }

    /** A swipe of the paw: the ball rolls off somewhere roughly away from the cat. */
    private void bat(ItemEntity target) {
        cooldown = BAT_COOLDOWN;
        double angle = Math.toRadians(mob.getYRot() + 90) + (mob.getRandom().nextDouble() - 0.5) * Math.PI;
        target.push(Math.cos(angle) * BAT_PUSH, BAT_HOP, Math.sin(angle) * BAT_PUSH);
        target.hurtMarked = true;
        mob.level().playSound(null, target, SoundEvents.WOOL_HIT, SoundSource.NEUTRAL, 0.6F, 1.3F);
    }

    private @Nullable ItemEntity nearestBall() {
        List<ItemEntity> balls = mob.level().getEntitiesOfClass(ItemEntity.class, new AABB(mob.blockPosition()).inflate(RANGE),
                item -> item.isAlive() && item.getItem().is(CatgirlItems.YARN_BALL.get()));
        ItemEntity nearest = null;
        for (ItemEntity item : balls) {
            if (nearest == null || mob.distanceToSqr(item) < mob.distanceToSqr(nearest)) {
                nearest = item;
            }
        }
        return nearest;
    }
}
