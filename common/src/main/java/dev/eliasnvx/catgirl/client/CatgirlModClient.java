package dev.eliasnvx.catgirl.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientTooltipEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.eliasnvx.catgirl.block.CatgirlBlocks;
import net.minecraft.client.renderer.RenderType;
import dev.eliasnvx.catgirl.entity.CatgirlEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.client.render.ItemTints;
import dev.eliasnvx.catgirl.client.render.entity.KittenRenderer;
import dev.eliasnvx.catgirl.client.render.entity.NekoRenderer;
import dev.eliasnvx.catgirl.client.render.model.CatModels;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.cosmetic.Cosmetic;
import dev.eliasnvx.catgirl.item.CatgirlFoods;
import dev.eliasnvx.catgirl.meow.MeowPayload;
import dev.eliasnvx.catgirl.set.CatSet;
import dev.eliasnvx.catgirl.set.LandingPayload;
import dev.eliasnvx.catgirl.set.NightVision;
import dev.eliasnvx.catgirl.registry.CatgirlComponents;
import dev.eliasnvx.catgirl.registry.CatgirlItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Client entry point, called by each loader's client initializer after {@link CatgirlMod#init()}. */
public final class CatgirlModClient {

    /** Z by default: M is Xaero's World Map, which most modpacks have. */
    public static final KeyMapping MEOW = new KeyMapping(
            "key.catgirl.meow", InputConstants.Type.KEYSYM, InputConstants.KEY_Z, "key.categories.multiplayer");

    /** N by default: Full Neko's night vision. */
    public static final KeyMapping NIGHT_VISION = new KeyMapping(
            "key.catgirl.night_vision", InputConstants.Type.KEYSYM, InputConstants.KEY_N, "key.categories.gameplay");

    private CatgirlModClient() {
    }

    public static void init() {
        CatModels.registerLayers();
        ItemTints.register();
        // the catnip bush's textures have see-through pixels; 1.21.1 picks a block's render layer in code (as soon as the
        // block exists: Fabric has it by now, NeoForge registers it later)
        CatgirlBlocks.CATNIP.listen(catnip -> RenderTypeRegistry.register(RenderType.cutout(), catnip));
        EntityRendererRegistry.register(CatgirlEntities.SEAT, NoopRenderer::new);
        EntityRendererRegistry.register(CatgirlEntities.STRAY_NEKO, context -> new NekoRenderer<>(context, "stray_neko", false));
        EntityRendererRegistry.register(CatgirlEntities.FISHER_NEKO, context -> new NekoRenderer<>(context, "fisher_neko", false));
        EntityRendererRegistry.register(CatgirlEntities.BAKENEKO, context -> new NekoRenderer<>(context, "bakeneko", false));
        EntityRendererRegistry.register(CatgirlEntities.CAT_SPIRIT, context -> new NekoRenderer<>(context, "cat_spirit", true));
        EntityRendererRegistry.register(CatgirlEntities.KITTEN, KittenRenderer::new);

        KeyMappingRegistry.register(MEOW);
        KeyMappingRegistry.register(NIGHT_VISION);
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            while (NIGHT_VISION.consumeClick()) {
                if (minecraft.player != null && minecraft.screen == null) {
                    NetworkManager.sendToServer(NightVision.Payload.INSTANCE);
                }
            }
            while (MEOW.consumeClick()) {
                if (minecraft.player != null && minecraft.screen == null) {
                    NetworkManager.sendToServer(MeowPayload.INSTANCE);
                }
            }
        });
        ClientTooltipEvent.ITEM.register(CatgirlModClient::appendTooltip);
        LandingPayload.onClient = (entityId, ticks) -> {
            var level = Minecraft.getInstance().level;
            Entity entity = level == null ? null : level.getEntity(entityId);
            if (entity != null) {
                LandingPose.start(entity, ticks);
            }
        };
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> LandingPose.clear());

        CatgirlMod.LOGGER.info("catgirl client initialized");
    }

    private static void appendTooltip(ItemStack stack, List<Component> lines, Item.TooltipContext context, TooltipFlag flag) {
        // how-to lines for the mechanics' items
        if (stack.is(CatgirlItems.CAT_BED.get()) || stack.is(CatgirlItems.CARDBOARD_BOX.get()) || stack.is(CatgirlItems.YARN_BALL.get())
                || stack.is(CatgirlFoods.CAT_FOOD.get()) || stack.is(CatgirlFoods.MILK_SAUCER.get())) {
            lines.add(Component.translatable("tooltip." + stack.getItem().getDescriptionId().substring(stack.getItem().getDescriptionId().indexOf('.') + 1))
                    .withStyle(ChatFormatting.GRAY));
        }
        Cosmetic cosmetic = stack.get(CatgirlComponents.COSMETIC.get());
        if (cosmetic == null) {
            return;
        }
        Coat coat = stack.get(CatgirlComponents.COAT.get());
        if (coat != null) {
            lines.add(Component.translatable("tooltip.catgirl.coat", Component.translatable(coat.translationKey()))
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("tooltip.catgirl.slot", Component.translatable(cosmetic.slot().translationKey()))
                .withStyle(ChatFormatting.GRAY));
        for (CatSet set : CatSet.values()) {
            if (set.includes(stack)) {
                lines.add(Component.translatable("tooltip.catgirl.set_piece", Component.translatable(set.translationKey()))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }
}
