package dev.eliasnvx.catgirl.entity;

import dev.eliasnvx.catgirl.effect.CatgirlEffects;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Cats around a catnip eater go wild. A cat under catnip gets the zoomies: it dashes from spot to spot and hops now and
 * then. Even a sitting cat can't resist it.
 */
final class CatnipZoomiesGoal extends Goal {

    private static final double SPEED = 1.6;
    private static final int RANGE = 6;
    private static final int HEIGHT = 2;
    private static final int DASH_INTERVAL = 15;
    private static final float HOP_CHANCE = 0.3F;

    private final PathfinderMob mob;
    private int nextDash;

    CatnipZoomiesGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return mob.hasEffect(CatgirlEffects.CATNIP.get());
    }

    @Override
    public void start() {
        if (mob instanceof TamableAnimal tamable) {
            tamable.setOrderedToSit(false);
            tamable.setInSittingPose(false);
        }
        nextDash = 0;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (--nextDash > 0) {
            return;
        }
        nextDash = DASH_INTERVAL + mob.getRandom().nextInt(DASH_INTERVAL);
        Vec3 target = DefaultRandomPos.getPos(mob, RANGE, HEIGHT);
        if (target != null) {
            mob.getNavigation().moveTo(target.x, target.y, target.z, SPEED);
        }
        if (mob.onGround() && mob.getRandom().nextFloat() < HOP_CHANCE) {
            mob.getJumpControl().jump();
        }
    }
}
