package dev.eliasnvx.catgirl.set;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/** House Cat: a cat or ocelot walks after the nearest player wearing the set, and stops a couple of blocks away. */
public final class FollowHouseCatGoal extends Goal {

    /** Looking for a wearer is a nearby-players scan: not every tick. */
    private static final int SEARCH_INTERVAL = 10;
    private static final int REPATH_INTERVAL = 10;

    private final PathfinderMob mob;
    private @Nullable Player target;
    private int repath;

    public FollowHouseCatGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.tickCount % SEARCH_INTERVAL != 0 || sitting() || mob.isLeashed()) {
            return false;
        }
        target = mob.level().getNearestPlayer(mob, SetBalance.FOLLOW_RANGE);
        return target != null && !target.isSpectator() && CatSets.has(target, CatSet.HOUSE_CAT)
                && mob.distanceToSqr(target) > SetBalance.FOLLOW_STOP * SetBalance.FOLLOW_STOP;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && !sitting() && CatSets.has(target, CatSet.HOUSE_CAT)
                && mob.distanceToSqr(target) > SetBalance.FOLLOW_STOP * SetBalance.FOLLOW_STOP
                && mob.distanceToSqr(target) < SetBalance.FOLLOW_RANGE * SetBalance.FOLLOW_RANGE * 4;
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        target = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        mob.getLookControl().setLookAt(target, mob.getMaxHeadYRot(), mob.getMaxHeadXRot());
        if (--repath <= 0) {
            repath = adjustedTickDelay(REPATH_INTERVAL);
            mob.getNavigation().moveTo(target, SetBalance.FOLLOW_SPEED);
        }
    }

    /** A tamed cat told to sit stays put. */
    private boolean sitting() {
        return mob instanceof TamableAnimal tamable && tamable.isOrderedToSit();
    }
}
