package dev.eliasnvx.catgirl.compat.jade;

import dev.eliasnvx.catgirl.block.CardboardBoxBlock;
import dev.eliasnvx.catgirl.block.CatBedBlock;
import dev.eliasnvx.catgirl.block.CatnipBlock;
import dev.eliasnvx.catgirl.entity.neko.Bakeneko;
import dev.eliasnvx.catgirl.entity.neko.CatSpirit;
import dev.eliasnvx.catgirl.entity.neko.FisherNeko;
import dev.eliasnvx.catgirl.entity.neko.Kitten;
import dev.eliasnvx.catgirl.entity.neko.StrayNeko;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade (soft dependency): what the mechanics' blocks do and how far the catnip has grown; for the mobs, whom a Stray
 * Neko trusts, how many fish the Fisher Neko still wants from you, when a kitten grows up, what a Bakeneko carries.
 * Loaded by Jade only (Forge: {@link WailaPlugin}, Fabric: the {@code jade} entrypoint). The client providers are
 * only touched in {@link #registerClient}, so a dedicated server never loads them.
 */
@WailaPlugin
public final class CatgirlJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(NekoServerData.INSTANCE, StrayNeko.class);
        registration.registerEntityDataProvider(NekoServerData.INSTANCE, FisherNeko.class);
        registration.registerEntityDataProvider(NekoServerData.INSTANCE, Kitten.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(MechanicsInfo.INSTANCE, CatBedBlock.class);
        registration.registerBlockComponent(MechanicsInfo.INSTANCE, CardboardBoxBlock.class);
        registration.registerBlockComponent(MechanicsInfo.INSTANCE, CatnipBlock.class);
        registration.registerEntityComponent(NekoInfo.INSTANCE, StrayNeko.class);
        registration.registerEntityComponent(NekoInfo.INSTANCE, FisherNeko.class);
        registration.registerEntityComponent(NekoInfo.INSTANCE, Kitten.class);
        registration.registerEntityComponent(NekoInfo.INSTANCE, Bakeneko.class);
        registration.registerEntityComponent(NekoInfo.INSTANCE, CatSpirit.class);
    }
}
