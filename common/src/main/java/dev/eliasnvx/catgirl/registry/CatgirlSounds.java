package dev.eliasnvx.catgirl.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.catgirl.CatgirlMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

/** Sound events; the files behind them are listed in assets/catgirl/sounds.json and CREDITS.md. */
public final class CatgirlSounds {

    public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(CatgirlMod.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> PLAYER_MEOW = register("player.meow");
    public static final RegistrySupplier<SoundEvent> COLLAR_JINGLE = register("item.bell_collar.jingle");
    public static final RegistrySupplier<SoundEvent> CAT_BED_PURR = register("block.cat_bed.purr");
    public static final RegistrySupplier<SoundEvent> BAKENEKO_HISS = register("entity.bakeneko.hiss");
    public static final RegistrySupplier<SoundEvent> CAT_SPIRIT_FADE = register("entity.cat_spirit.fade");

    private CatgirlSounds() {
    }

    private static RegistrySupplier<SoundEvent> register(String name) {
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(CatgirlMod.id(name)));
    }
}
