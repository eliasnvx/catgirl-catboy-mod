package dev.eliasnvx.catgirl.block;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.catgirl.CatgirlMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

/** The mechanics' blocks: the cat bed, the cardboard box and catnip. Their items are in CatgirlItems. */
public final class CatgirlBlocks {

    public static final DeferredRegister<Block> REGISTER = DeferredRegister.create(CatgirlMod.MOD_ID, Registries.BLOCK);

    public static final RegistrySupplier<Block> CAT_BED = register("cat_bed", CatBedBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.8F).sound(SoundType.WOOL).noOcclusion().ignitedByLava());
    public static final RegistrySupplier<Block> CARDBOARD_BOX = register("cardboard_box", CardboardBoxBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.5F).sound(SoundType.WOOL).noOcclusion().ignitedByLava());
    public static final RegistrySupplier<Block> CATNIP = register("catnip", CatnipBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).randomTicks().noCollission().noOcclusion().instabreak()
                    .sound(SoundType.SWEET_BERRY_BUSH).pushReaction(PushReaction.DESTROY));

    private CatgirlBlocks() {
    }

    private static RegistrySupplier<Block> register(String name, Function<BlockBehaviour.Properties, Block> factory,
                                                    BlockBehaviour.Properties props) {
        return REGISTER.register(name, () -> factory.apply(props));
    }
}
