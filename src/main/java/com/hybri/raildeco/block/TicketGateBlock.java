package com.hybri.raildeco.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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
 * JR 风格闸机（纯装饰）。
 * 两个闸机底座分列左右，中间为通行通道，闸门保持开启状态。
 */
public class TicketGateBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<TicketGateBlock> CODEC = simpleCodec(TicketGateBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
        box(0, 0, 0, 3, 6, 16),    // 左侧票箱
        box(13, 0, 0, 16, 6, 16),  // 右侧票箱
        box(0, 6, 1, 3, 14, 15),   // 左侧立柱
        box(13, 6, 1, 16, 14, 15), // 右侧立柱
        box(0, 14, 1, 16, 16, 15)  // 顶部横梁
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
        return SHAPE;
    }
}