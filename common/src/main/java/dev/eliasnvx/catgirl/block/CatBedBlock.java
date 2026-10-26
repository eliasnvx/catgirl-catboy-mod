package dev.eliasnvx.catgirl.block;

import dev.eliasnvx.catgirl.entity.CatSeat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cat bed: a wicker basket with a cushion. Right-click to curl up in it, day or night (no time skip,
 * no spawn point): resting refills food and, after a short nap, gives "Well Rested". Sneak to get up.
 */
public class CatBedBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 6.0, 15.0);
    /** Lying on the cushion. */
    public static final double SEAT_HEIGHT = 0.15;

    public CatBedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive() || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            CatSeat.sit(serverLevel, pos, player, SEAT_HEIGHT);
        }
        return InteractionResult.SUCCESS;
    }
}
