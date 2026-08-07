package hybrizat.raildeco.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * JR ??????????????? Metropolis ?????????????
 * ?????? 1 ??????????
 */
public class TicketGateBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<TicketGateBlock> CODEC = simpleCodec(TicketGateBlock::new);

    // 1 ??????????/????? X ????
    private static final VoxelShape SHAPE_NORTH_SOUTH = Shapes.or(
        box(0, 0, 0, 2, 16, 16),  // ????
        box(14, 0, 0, 16, 16, 16) // ????
    );
    // ???/??????? Z ?????? blockstate y ???
    private static final VoxelShape SHAPE_EAST_WEST = Shapes.or(
        box(0, 0, 0, 16, 16, 2),
        box(0, 0, 14, 16, 16, 16)
    );

    public TicketGateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return facing == Direction.NORTH || facing == Direction.SOUTH ? SHAPE_NORTH_SOUTH : SHAPE_EAST_WEST;
    }
}
