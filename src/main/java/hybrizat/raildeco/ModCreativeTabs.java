package hybrizat.raildeco;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RailDeco.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("rail_deco", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.rail_deco"))
            .icon(() -> new ItemStack(ModBlocks.TICKET_GATE.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModBlocks.TICKET_GATE.get());
                output.accept(ModBlocks.CROSSING_GATE.get());
                output.accept(ModBlocks.PLATFORM_EDGE.get());
            })
            .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }
}
