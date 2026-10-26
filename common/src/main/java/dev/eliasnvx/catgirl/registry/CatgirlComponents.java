package dev.eliasnvx.catgirl.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.cosmetic.Cosmetic;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

public final class CatgirlComponents {

    public static final DeferredRegister<DataComponentType<?>> REGISTER =
            DeferredRegister.create(CatgirlMod.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<DataComponentType<Cosmetic>> COSMETIC = REGISTER.register("cosmetic",
            () -> DataComponentType.<Cosmetic>builder()
                    .persistent(Cosmetic.CODEC)
                    .networkSynchronized(Cosmetic.STREAM_CODEC)
                    .cacheEncoding()
                    .build());

    public static final RegistrySupplier<DataComponentType<Coat>> COAT = REGISTER.register("coat",
            () -> DataComponentType.<Coat>builder()
                    .persistent(Coat.CODEC)
                    .networkSynchronized(Coat.STREAM_CODEC)
                    .build());

    private CatgirlComponents() {
    }
}
