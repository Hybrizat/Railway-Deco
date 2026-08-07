package hybrizat.raildeco.block.entity;

import hybrizat.raildeco.ModBlockEntityTypes;
import hybrizat.raildeco.ModSounds;
import hybrizat.raildeco.block.CrossingGateFullBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** ??????????????????? */
public class CrossingGateFullBlockEntity extends BlockEntity {
    public static final int FLASH_PERIOD = 14;

    public CrossingGateFullBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.CROSSING_GATE_FULL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrossingGateFullBlockEntity blockEntity) {
        // ????????????????????????
        boolean powered = level.getBestNeighborSignal(pos) > 0;
        if (powered != state.getValue(CrossingGateFullBlock.POWERED)) {
            level.setBlock(pos, state.setValue(CrossingGateFullBlock.POWERED, powered), 3);
            return;
        }
        if (powered && level.getGameTime() % FLASH_PERIOD == 0) {
            level.playSound(null, pos, ModSounds.FUMIGIRI.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
        }
    }

}
