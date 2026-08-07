package com.hybri.raildeco.block;

import com.hybri.raildeco.block.entity.CrossingGateBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * 踏切遮断机（红石驱动，不检测列车，与 Create 等模组完全解耦）。
 *
 * <p>方块属性：
 * <ul>
 *   <li>{@code FACING} —— 遮断杆伸出方向（朝向路中心）；</li>
 *   <li>{@code POWERED} —— 红石信号：通电时遮断杆落下、警灯闪烁、警铃响起；</li>
 *   <li>{@code LENGTH} —— 遮断杆长度（1~8 格），手持方块右键摆放好的遮断机循环切换。</li>
 * </ul>
 */
public class CrossingGateBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<CrossingGateBlock> CODEC = simpleCodec(CrossingGateBlock::new);

    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final IntegerProperty LENGTH = IntegerProperty.create("length", 1, 8);

    public CrossingGateBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
            .setValue(FACING, Direction.NORTH)
            .setValue(POWERED, false)
            .setValue(LENGTH, 2));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, LENGTH);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // 整个断面（立柱、遮断杆、警灯）由方块实体渲染器绘制
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            updatePowered(level, pos, state);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(state.getBlock()) && !level.isClientSide) {
            updatePowered(level, pos, state);
        }
    }

    private void updatePowered(Level level, BlockPos pos, BlockState state) {
        boolean powered = level.getBestNeighborSignal(pos) > 0;
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), 3);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            int length = state.getValue(LENGTH);
            length = length >= 8 ? 1 : length + 1;
            level.setBlock(pos, state.setValue(LENGTH, length), 3);
            level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("block.rail_deco.crossing_gate.tooltip"));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrossingGateBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, blockEntity) -> {
            if (blockEntity instanceof CrossingGateBlockEntity gate) {
                CrossingGateBlockEntity.serverTick(lvl, pos, st, gate);
            }
        };
    }
}