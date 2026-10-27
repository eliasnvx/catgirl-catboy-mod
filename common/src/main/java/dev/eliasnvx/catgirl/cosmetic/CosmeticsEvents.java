package dev.eliasnvx.catgirl.cosmetic;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.eliasnvx.catgirl.network.CosmeticsSyncPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;

/** Game hooks for cosmetic slots: sync, right-click equip, death drops. */
public final class CosmeticsEvents {

    private CosmeticsEvents() {
    }

    public static void register() {
        PlayerEvent.PLAYER_JOIN.register(CosmeticsSyncPayload::sendToTrackingAndSelf);
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd) -> CosmeticsSyncPayload.sendToTrackingAndSelf(player));
        PlayerEvent.CHANGE_DIMENSION.register((player, from, to) -> CosmeticsSyncPayload.sendToTrackingAndSelf(player));
        // Start tracking: Architectury 13 has no event for it, each loader calls onStartTracking
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                dropOnDeath(player);
            }
            return EventResult.pass();
        });
        // Tell vanilla what the hand holds now: without it, the use code sees the emptied stack and clears the hand,
        // deleting the swapped-out cosmetic
        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> equipFromHand(player, hand)
                ? CompoundEventResult.interruptTrue(player.getItemInHand(hand))
                : CompoundEventResult.pass());
    }

    /** {@code watcher} starts seeing {@code entity}: send the outfit if it is a player. Called by the loader entrypoints. */
    public static void onStartTracking(Entity entity, ServerPlayer watcher) {
        if (entity instanceof Player tracked) {
            CosmeticsSyncPayload.sendTo(watcher, tracked);
        }
    }

    private static void dropOnDeath(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return; // the attachment is copyOnDeath, so cosmetics carry over to the respawned player
        }
        for (ItemStack stack : CosmeticsManager.clear(player)) {
            player.spawnAtLocation(stack);
        }
    }

    /** Right-click a cosmetic in hand to wear it; swaps with whatever was in the slot. True if it was worn. */
    private static boolean equipFromHand(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        CatSlot slot = CosmeticsManager.slotOf(held);
        if (slot == null || !CosmeticsManager.canEquip(slot, held)) {
            return false;
        }
        if (!(player instanceof ServerPlayer)) {
            return true;
        }
        ItemStack worn = CosmeticsManager.get(player).get(slot).copy();
        ItemStack toWear = held.split(1);
        CosmeticsManager.set(player, slot, toWear);
        if (!worn.isEmpty()) {
            if (held.isEmpty()) {
                player.setItemInHand(hand, worn);
            } else if (!player.getInventory().add(worn)) {
                player.spawnAtLocation(worn);
            }
        }
        return true;
    }
}
