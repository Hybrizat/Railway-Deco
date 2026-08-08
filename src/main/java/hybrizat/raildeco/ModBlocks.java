package hybrizat.raildeco;

import hybrizat.raildeco.block.CrossingGateBlock;
import hybrizat.raildeco.block.PlatformEdgeBlock;
import hybrizat.raildeco.block.TicketGateBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RailDeco.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RailDeco.MOD_ID);

    /** 闸机：纯装饰，保持通行开启造型。 */
    public static final DeferredBlock<Block> TICKET_GATE = BLOCKS.register("ticket_gate",
        () -> new TicketGateBlock(BlockBehaviour.Properties.of()
            .strength(2.0F)
            .sound(SoundType.METAL)
            .noOcclusion()));

    /** 踏切遮断机：红石控制，警灯闪烁 + 警铃。 */
    public static final DeferredBlock<Block> CROSSING_GATE = BLOCKS.register("crossing_gate",
        () -> new CrossingGateBlock(BlockBehaviour.Properties.of()
            .strength(2.0F)
            .sound(SoundType.METAL)
            .noOcclusion()));

    /** 整体踏切（参考 cr1）：约 3.8 格高，红石通电警灯闪烁、警铃响起 */
    public static final DeferredBlock<Block> CROSSING_GATE_FULL = BLOCKS.register("crossing_signal",
        () -> new CrossingGateFullBlock(BlockBehaviour.Properties.of()
            .strength(2.0F)
            .sound(SoundType.METAL)
            .noOcclusion()));

    /** 站台边缘：低矮的黄色盲道/边界方块。 */
    public static final DeferredBlock<Block> PLATFORM_EDGE = BLOCKS.register("platform_edge",
        () -> new PlatformEdgeBlock(BlockBehaviour.Properties.of()
            .strength(1.5F)
            .sound(SoundType.STONE)));

    public static final DeferredItem<BlockItem> TICKET_GATE_ITEM = ITEMS.registerSimpleBlockItem(TICKET_GATE);
    public static final DeferredItem<BlockItem> CROSSING_GATE_ITEM = ITEMS.registerSimpleBlockItem(CROSSING_GATE);
    public static final DeferredItem<BlockItem> PLATFORM_EDGE_ITEM = ITEMS.registerSimpleBlockItem(PLATFORM_EDGE);

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
