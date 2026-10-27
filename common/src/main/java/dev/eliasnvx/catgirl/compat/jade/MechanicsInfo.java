package dev.eliasnvx.catgirl.compat.jade;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.block.CardboardBoxBlock;
import dev.eliasnvx.catgirl.block.CatBedBlock;
import dev.eliasnvx.catgirl.block.CatnipBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** What the cat bed and the cardboard box do, and the catnip's growth (client side, from the block state). */
public enum MechanicsInfo implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = CatgirlMod.id("mechanics");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        Block block = accessor.getBlock();
        if (block instanceof CatBedBlock) {
            tooltip.add(Component.translatable("tooltip.catgirl.cat_bed").withStyle(ChatFormatting.GRAY));
        } else if (block instanceof CardboardBoxBlock) {
            tooltip.add(Component.translatable("tooltip.catgirl.cardboard_box").withStyle(ChatFormatting.GRAY));
        } else if (block instanceof CatnipBlock) {
            int age = accessor.getBlockState().getValue(CatnipBlock.AGE);
            tooltip.add(age >= CatnipBlock.MAX_AGE
                    ? Component.translatable("jade.catgirl.catnip.ready").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("jade.catgirl.catnip.growth", age * 100 / CatnipBlock.MAX_AGE));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
