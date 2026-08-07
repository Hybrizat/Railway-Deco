package hybrizat.raildeco.block.entity;

import hybrizat.raildeco.ModBlockEntityTypes;
import hybrizat.raildeco.block.CrossingGateBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 踏切遮断机方块实体：服务端负责警铃，渲染器在客户端负责动画与警灯。
 */
public class CrossingGateBlockEntity extends BlockEntity {
    /** 遮断杆落下耗时（tick）：参考大宫铁道踏切 Addon 的 4.0 秒。 */
    public static final int DOWN_TICKS = 80;
    /** 遮断杆抬起耗时（tick）：参考 Addon 的 6.1 秒（抬起比落下慢）。 */
    public static final int UP_TICKS = 122;
    /** 警灯闪烁/警铃节拍（tick）：参考 Addon 的 1.4 秒周期。 */
    public static final int FLASH_PERIOD = 14;

    public CrossingGateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.CROSSING_GATE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrossingGateBlockEntity blockEntity) {
        // ????????????????????????
        boolean powered = level.getBestNeighborSignal(pos) > 0;
        if (powered != state.getValue(CrossingGateBlock.POWERED)) {
            level.setBlock(pos, state.setValue(CrossingGateBlock.POWERED, powered), 3);
            return;
        }
        if (powered && level.getGameTime() % FLASH_PERIOD == 0) {
            level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 0.6F, 1.0F);
        }
    }
}