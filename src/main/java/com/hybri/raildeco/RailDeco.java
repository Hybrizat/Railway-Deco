package com.hybri.raildeco;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rail Deco —— 铁路装饰方块模组（NeoForge 1.21.1，不依赖 Create 等模组）。
 */
@Mod(RailDeco.MOD_ID)
public class RailDeco {
    public static final String MOD_ID = "rail_deco";
    public static final Logger LOGGER = LoggerFactory.getLogger(RailDeco.class);

    public RailDeco(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModBlockEntityTypes.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
