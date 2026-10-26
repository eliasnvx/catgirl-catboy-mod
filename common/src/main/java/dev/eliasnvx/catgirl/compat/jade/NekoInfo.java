package dev.eliasnvx.catgirl.compat.jade;

import dev.eliasnvx.catgirl.entity.neko.Bakeneko;
import dev.eliasnvx.catgirl.entity.neko.CatSpirit;
import dev.eliasnvx.catgirl.entity.neko.FisherNeko;
import dev.eliasnvx.catgirl.entity.neko.Kitten;
import dev.eliasnvx.catgirl.entity.neko.StrayNeko;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Tooltip lines for the cat-people and the kitten (client side; server data from {@link NekoServerData}). */
public enum NekoInfo implements IEntityComponentProvider {
    INSTANCE;

    private static final int TICKS_PER_SECOND = 20;

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        Entity entity = accessor.getEntity();
        CompoundTag data = accessor.getServerData();
        if (entity instanceof StrayNeko) {
            if (data.getBooleanOr(NekoServerData.OWNER_IS_YOU, false)) {
                tooltip.add(Component.translatable("jade.catgirl.stray_neko.trusts_you").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else if (data.getBooleanOr(NekoServerData.TRUSTS, false)) {
                String owner = data.getStringOr(NekoServerData.OWNER, "");
                tooltip.add(owner.isEmpty() ? Component.translatable("jade.catgirl.stray_neko.trusts_someone")
                        : Component.translatable("jade.catgirl.stray_neko.trusts", owner));
            } else {
                tooltip.add(Component.translatable("jade.catgirl.stray_neko.wary").withStyle(ChatFormatting.GRAY));
            }
        } else if (entity instanceof FisherNeko) {
            tooltip.add(Component.translatable("jade.catgirl.fisher_neko.quest",
                    data.getIntOr(NekoServerData.FISH, 0), FisherNeko.QUEST_FISH));
        } else if (entity instanceof Kitten) {
            int seconds = data.getIntOr(NekoServerData.GROW_TICKS, 0) / TICKS_PER_SECOND;
            if (seconds > 0) {
                tooltip.add(Component.translatable("jade.catgirl.kitten.grows_up",
                        String.format("%d:%02d", seconds / 60, seconds % 60)));
            }
        } else if (entity instanceof Bakeneko bakeneko) {
            if (bakeneko.hasLoot()) {
                tooltip.add(Component.translatable("jade.catgirl.bakeneko.carrying", bakeneko.getMainHandItem().getHoverName())
                        .withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.translatable("jade.catgirl.bakeneko.hint").withStyle(ChatFormatting.GRAY));
            }
        } else if (entity instanceof CatSpirit) {
            tooltip.add(Component.translatable("jade.catgirl.cat_spirit.hint").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public Identifier getUid() {
        return NekoServerData.UID;
    }
}
