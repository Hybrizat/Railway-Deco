package com.hybri.raildeco.client;

import com.hybri.raildeco.ModBlockEntityTypes;
import com.hybri.raildeco.RailDeco;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * 客户端注册：方块实体渲染器 + 额外加载的模型。
 * 由 {@link RailDeco} 主类在客户端分发时手动注册。
 */
public final class ClientModEvents {
    private ClientModEvents() {
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntityTypes.CROSSING_GATE.get(), CrossingGateBlockEntityRenderer::new);
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(standaloneModel("block/crossing_gate_pole"));
        event.register(standaloneModel("block/crossing_gate_pole_top"));
        event.register(standaloneModel("block/crossing_gate_lamp_unit"));
        event.register(standaloneModel("block/crossing_gate_arm"));
        event.register(standaloneModel("block/crossing_gate_counterweight"));
        event.register(standaloneModel("block/crossing_gate_lamp_glow"));
    }

    private static ModelResourceLocation standaloneModel(String path) {
        return new ModelResourceLocation(RailDeco.id(path), ModelResourceLocation.STANDALONE_VARIANT);
    }
}