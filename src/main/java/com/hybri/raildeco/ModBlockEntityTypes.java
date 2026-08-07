package com.hybri.raildeco;

import com.hybri.raildeco.block.entity.CrossingGateBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RailDeco.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrossingGateBlockEntity>> CROSSING_GATE =
        BLOCK_ENTITY_TYPES.register("crossing_gate",
            () -> BlockEntityType.Builder.of(CrossingGateBlockEntity::new, ModBlocks.CROSSING_GATE.get()).build(null));

    private ModBlockEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
