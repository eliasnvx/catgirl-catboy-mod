package dev.eliasnvx.catgirl.client.render;

import dev.eliasnvx.catgirl.cosmetic.CatSlot;
import dev.eliasnvx.catgirl.cosmetic.Coat;
import dev.eliasnvx.catgirl.cosmetic.CosmeticInventory;
import dev.eliasnvx.catgirl.cosmetic.CosmeticsManager;
import dev.eliasnvx.catgirl.registry.CatgirlComponents;
import dev.eliasnvx.catgirl.registry.CatgirlTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeableLeatherItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Per-player render data, captured by the cosmetic layer each frame. One instance per entity is reused across frames:
 * the worn list is rebuilt only when the (immutable) inventory snapshot changes, and the smoothed motion values are
 * updated in place, so capturing does not allocate.
 */
public final class CosmeticRenderData {

    /** Radians of tail sway per degree/tick of body turn. */
    private static final float TURN_GAIN = 0.035F;
    private static final float MAX_TURN_SWAY = 0.9F;
    /** Higher = snappier smoothing (per tick). */
    private static final float SMOOTHING = 0.25F;
    /** Ears flatten fast when hurt and come back up slowly. */
    private static final float FLATTEN_IN = 1.2F;
    private static final float FLATTEN_OUT = 0.12F;
    private static final float GOLDEN_ANGLE = 2.39996F;

    /** Neutral values for states without data (inventory doll of a player that wears nothing). */
    public static final CosmeticRenderData STILL = new CosmeticRenderData(0);

    /** @param dye vanilla dyed color (collars), or -1 if undyed */
    public record Worn(CatSlot slot, Item item, Coat coat, int dye) {
    }

    private static final Map<Entity, CosmeticRenderData> CACHE = new WeakHashMap<>();

    private CosmeticInventory source;
    private List<Worn> worn = List.of();
    private boolean earsCovered;
    private final float phase;
    private float turnSway;
    private float walkAmount;
    private float flatten;
    private float lastAge = Float.NaN;
    private float lastBodyRot;

    private CosmeticRenderData(int entityId) {
        this.phase = (entityId * GOLDEN_ANGLE) % Mth.TWO_PI;
    }

    /** Called from the cosmetic layer once per frame per player. Returns null if nothing is worn. */
    public static @Nullable CosmeticRenderData capture(Player player, PlayerPose state) {
        CosmeticInventory inventory = CosmeticsManager.worn(player);
        if (inventory.isEmpty()) {
            return null;
        }
        CosmeticRenderData data = CACHE.computeIfAbsent(player, e -> new CosmeticRenderData(e.getId()));
        if (data.source != inventory) {
            data.rebuild(inventory);
        }
        data.updateMotion(state);
        return data;
    }

    /** The data a model's {@code setupAnim} reads; never null. */
    public static CosmeticRenderData of(PlayerPose state) {
        return state.cosmetics;
    }

    private void rebuild(CosmeticInventory inventory) {
        List<Worn> list = new ArrayList<>();
        for (CatSlot slot : CatSlot.values()) {
            ItemStack stack = inventory.get(slot);
            if (!stack.isEmpty()) {
                list.add(new Worn(slot, stack.getItem(), CatgirlComponents.COAT.getOrDefault(stack, Coat.DEFAULT),
                        dyeOf(stack)));
            }
        }
        this.worn = List.copyOf(list);
        this.earsCovered = inventory.get(CatSlot.HEAD).is(CatgirlTags.COVERS_EARS);
        this.source = inventory;
    }

    /** 24-bit dyed color, or -1 if undyed. */
    private static int dyeOf(ItemStack stack) {
        return stack.getItem() instanceof DyeableLeatherItem dyeable && dyeable.hasCustomColor(stack)
                ? dyeable.getColor(stack) & 0xFFFFFF : -1;
    }

    private void updateMotion(PlayerPose state) {
        if (Float.isNaN(lastAge)) {
            lastAge = state.ageInTicks;
            lastBodyRot = state.bodyRot;
            return;
        }
        float dt = state.ageInTicks - lastAge;
        if (dt <= 0) {
            return; // same frame (inventory doll) or paused
        }
        float turnRate = Mth.wrapDegrees(state.bodyRot - lastBodyRot) / dt;
        float targetSway = Mth.clamp(-turnRate * TURN_GAIN, -MAX_TURN_SWAY, MAX_TURN_SWAY);
        float alpha = 1.0F - (float) Math.exp(-dt * SMOOTHING);
        turnSway += (targetSway - turnSway) * alpha;
        walkAmount += (Math.min(state.walkAnimationSpeed, 1.0F) - walkAmount) * alpha;
        float flattenTarget = state.hasRedOverlay ? 1.0F : 0.0F;
        float flattenRate = state.hasRedOverlay ? FLATTEN_IN : FLATTEN_OUT;
        flatten += (flattenTarget - flatten) * (1.0F - (float) Math.exp(-dt * flattenRate));
        lastAge = state.ageInTicks;
        lastBodyRot = state.bodyRot;
    }

    public List<Worn> worn() {
        return worn;
    }

    /** Whether the head piece has ears of its own ({@code #catgirl:covers_ears}), so the ears slot isn't drawn. */
    public boolean earsCovered() {
        return earsCovered;
    }

    /** Sideways swing in radians caused by body turning (negative = left). */
    public float turnSway() {
        return turnSway;
    }

    /** 0..1 how fast the player is walking, smoothed. */
    public float walkAmount() {
        return walkAmount;
    }

    /** 0..1 how flat the ears are pressed back (just took damage). */
    public float flatten() {
        return flatten;
    }

    /** Per-player phase offset in radians, so idle animations of different players do not sync. */
    public float phase() {
        return phase;
    }
}
