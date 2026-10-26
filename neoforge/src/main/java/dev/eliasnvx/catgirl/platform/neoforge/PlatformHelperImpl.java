package dev.eliasnvx.catgirl.platform.neoforge;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.cosmetic.CosmeticInventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class PlatformHelperImpl {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, CatgirlMod.MOD_ID);

    private static final Supplier<AttachmentType<CosmeticInventory>> COSMETICS = ATTACHMENTS.register("cosmetics",
            () -> AttachmentType.builder(() -> CosmeticInventory.EMPTY)
                    .serialize(CosmeticInventory.CODEC.fieldOf("cosmetics"))
                    .copyOnDeath()
                    .build());

    private PlatformHelperImpl() {
    }

    /** Registers loader-side content; must run before {@link CatgirlMod#init()}. */
    public static void init(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }

    public static CosmeticInventory getCosmetics(Player player) {
        return player.getData(COSMETICS);
    }

    public static void setCosmetics(Player player, CosmeticInventory cosmetics) {
        player.setData(COSMETICS, cosmetics);
    }
}
