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
 * JR 风格闸机（纯装饰）。两侧立柱与 Metropolis 开放形态一致，中间可通行。
 * 碰撞箱高度仅 1 格，随方块朝向旋转。
 */
public class TicketGateBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<TicketGateBlock> CODEC = simpleCodec(TicketGateBlock::new);

    // 1 格高的侧立柱（面朝南/北时分布在 X 轴两侧）
    private static final VoxelShape SHAPE_NORTH_SOUTH = Shapes.or(
        box(0, 0, 0, 2, 16, 16),  // 左侧立柱
        box(14, 0, 0, 16, 16, 16) // 右侧立柱
    );
    // 面朝东/西时立柱旋转到 Z 轴两侧（对应 blockstate y 旋转）
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
