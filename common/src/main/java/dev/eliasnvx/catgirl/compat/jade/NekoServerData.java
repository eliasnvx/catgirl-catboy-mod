package dev.eliasnvx.catgirl.compat.jade;

import dev.eliasnvx.catgirl.CatgirlMod;
import dev.eliasnvx.catgirl.entity.neko.FisherNeko;
import dev.eliasnvx.catgirl.entity.neko.Kitten;
import dev.eliasnvx.catgirl.entity.neko.StrayNeko;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;

import java.util.UUID;

/** Server side of {@link NekoInfo}: what only the server knows (owner, quest progress, time left to grow). */
public enum NekoServerData implements IServerDataProvider<EntityAccessor> {
    INSTANCE;

    static final ResourceLocation UID = CatgirlMod.id("neko");
    static final String OWNER = "catgirl_owner";
    static final String OWNER_IS_YOU = "catgirl_owner_is_you";
    static final String TRUSTS = "catgirl_trusts";
    static final String FISH = "catgirl_fish";
    static final String GROW_TICKS = "catgirl_grow";

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        Entity entity = accessor.getEntity();
        UUID viewer = accessor.getPlayer().getUUID();
        if (entity instanceof StrayNeko neko && neko.owner() != null) {
            UUID owner = neko.owner();
            data.putBoolean(TRUSTS, true);
            data.putBoolean(OWNER_IS_YOU, owner.equals(viewer));
            ServerPlayer player = ((ServerLevel) accessor.getLevel()).getServer().getPlayerList().getPlayer(owner);
            if (player != null) {
                data.putString(OWNER, player.getGameProfile().getName());
            }
        } else if (entity instanceof FisherNeko fisher) {
            data.putInt(FISH, fisher.delivered(viewer));
        } else if (entity instanceof Kitten kitten && kitten.getAge() < 0) {
            data.putInt(GROW_TICKS, -kitten.getAge());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
