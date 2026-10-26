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
 * Cardboard box: right-click to sit inside. Mobs stop seeing you (and forget you) while you sit
 * there, but you can't move. Funny first, then useful. Sneak to climb out.
 */
public class CardboardBoxBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0.5, 0.0, 0.5, 15.5, 11.0, 15.5);
    /** On the box's floor: only the head pokes out. */
    public static final double SEAT_HEIGHT = -0.35;

    public CardboardBoxBlock(Properties properties) {
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
