package dev.eliasnvx.catgirl.item;

import dev.eliasnvx.catgirl.CatgirlAdvancements;
import dev.eliasnvx.catgirl.effect.CatgirlEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Catnip: planted on grass like sweet berries, or eaten. Eating it gives Haste and the catnip
 * tumbles (the food component's effects), and sends every cat around into the zoomies. Eating more while still under
 * its spell is "Catnip Overdose".
 */
public class CatnipItem extends BlockItem {

    // Balance (move to catgirl-common config once the config exists)
    public static final int EFFECT_TICKS = 30 * 20;
    static final double CAT_RADIUS = 12.0;
    static final int CAT_ZOOMIES_TICKS = 10 * 20;

    public CatnipItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** "Catnip" as an item, not the bush's block name. */
    @Override
    public String getDescriptionId() {
        return getOrCreateDescriptionId();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        boolean already = entity.hasEffect(CatgirlEffects.CATNIP.get());
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide()) {
            for (LivingEntity cat : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(CAT_RADIUS),
                    e -> e instanceof Cat || e instanceof Ocelot)) {
                cat.addEffect(new MobEffectInstance(CatgirlEffects.CATNIP.get(), CAT_ZOOMIES_TICKS));
            }
            if (already && entity instanceof ServerPlayer player) {
                CatgirlAdvancements.award(player, CatgirlAdvancements.CATNIP_OVERDOSE);
            }
        }
        return result;
    }
}
