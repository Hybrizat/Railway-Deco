package hybrizat.raildeco;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(Registries.SOUND_EVENT, RailDeco.MOD_ID);

    /** 踏切警铃（自定义音效 fumigiri.ogg） */
    public static final DeferredHolder<SoundEvent, SoundEvent> FUMIGIRI =
        SOUND_EVENTS.register("fumigiri", () -> SoundEvent.createVariableRangeEvent(RailDeco.id("fumigiri")));

    private ModSounds() {
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
