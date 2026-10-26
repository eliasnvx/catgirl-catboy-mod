package dev.eliasnvx.catgirl.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** House Cat adds a follow goal to vanilla cats and ocelots; neither loader exposes a mob's goals. */
@Mixin(Mob.class)
public interface MobAccessor {

    @Accessor("goalSelector")
    GoalSelector catgirl$goalSelector();
}
