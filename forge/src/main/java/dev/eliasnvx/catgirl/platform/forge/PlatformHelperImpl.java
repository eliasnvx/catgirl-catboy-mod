package dev.eliasnvx.catgirl.platform.forge;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.cosmetic.CosmeticInventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class PlatformHelperImpl {

    private PlatformHelperImpl() {
    }

    /** Registers loader-side content; must run before {@link CatgirlMod#init()}. */
    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterCapabilitiesEvent event) -> event.register(CatgirlPlayerData.class));
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, (AttachCapabilitiesEvent<Entity> event) -> {
            if (event.getObject() instanceof Player) {
                event.addCapability(CatgirlPlayerData.ID, new CatgirlPlayerData.Provider());
            }
        });
        MinecraftForge.EVENT_BUS.addListener(PlatformHelperImpl::onClone);
    }

    /**
     * Copies the cosmetics to the new player on respawn and on return from the End. The old player's caps are already
     * invalidated here, so they are revived for the copy and invalidated again afterwards.
     */
    private static void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        try {
            original.getCapability(CatgirlPlayerData.CAPABILITY).ifPresent(old -> event.getEntity()
                    .getCapability(CatgirlPlayerData.CAPABILITY)
                    .ifPresent(data -> data.copyFrom(old)));
        } finally {
            original.invalidateCaps();
        }
    }

    public static CosmeticInventory getCosmetics(Player player) {
        // orElse instead of map: read every frame by the cosmetic layer, no Optional allocation
        CatgirlPlayerData data = player.getCapability(CatgirlPlayerData.CAPABILITY).orElse(null);
        return data == null ? CosmeticInventory.EMPTY : data.cosmetics;
    }

    public static void setCosmetics(Player player, CosmeticInventory cosmetics) {
        CatgirlPlayerData data = player.getCapability(CatgirlPlayerData.CAPABILITY).orElse(null);
        if (data != null) {
            data.cosmetics = cosmetics;
        }
    }
}
