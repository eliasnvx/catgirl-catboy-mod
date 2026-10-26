package dev.eliasnvx.catgirl.client;

import dev.eliasnvx.catgirl.cosmetic.CatSlot;
import dev.eliasnvx.catgirl.cosmetic.CosmeticInventory;
import dev.eliasnvx.catgirl.cosmetic.CosmeticsManager;
import dev.eliasnvx.catgirl.menu.CosmeticSlot;
import dev.eliasnvx.catgirl.menu.CosmeticsContainer;
import dev.eliasnvx.catgirl.menu.CosmeticsLayout;
import dev.eliasnvx.catgirl.mixin.client.AbstractContainerScreenAccessor;
import dev.eliasnvx.catgirl.set.CatSet;
import dev.eliasnvx.catgirl.set.CatSets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The cosmetic slots' panel on the right of the survival inventory and the creative inventory tab
 * ({@link CosmeticsLayout}): background, slot frames, a star per set (lit while worn) with the set's pieces and
 * bonus in its tooltip, and the slot's name on empty slots. The slots themselves belong to the inventory menu.
 */
public final class CosmeticsPanel {

    private static final Component STAR = Component.literal("✦");
    private static final int STAR_ON = 0xFFF5A9D0;
    private static final int STAR_OFF = 0xFF8B8B8B;
    private static final CatSet[] SETS = CatSet.values();
    private static final int STAR_STEP = CosmeticsLayout.SLOT * 2 / SETS.length;

    private CosmeticsPanel() {
    }

    public static boolean visible(AbstractContainerScreen<?> screen) {
        return screen instanceof InventoryScreen
                || screen instanceof CreativeModeInventoryScreen creative && creative.isInventoryOpen();
    }

    private static int left(AbstractContainerScreen<?> screen) {
        AbstractContainerScreenAccessor access = (AbstractContainerScreenAccessor) screen;
        return access.catgirl$leftPos() + CosmeticsLayout.panelX(access.catgirl$imageWidth());
    }

    private static int top(AbstractContainerScreen<?> screen) {
        return ((AbstractContainerScreenAccessor) screen).catgirl$topPos();
    }

    /** The panel's screen area, or null while it isn't shown (JEI keeps its item list out of it). */
    public static @Nullable Rect2i area(AbstractContainerScreen<?> screen) {
        return visible(screen) ? new Rect2i(left(screen), top(screen), CosmeticsLayout.WIDTH, CosmeticsLayout.HEIGHT) : null;
    }

    /** Clicks on the panel are not "outside the window" (they would drop the carried stack). */
    public static boolean contains(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        if (!visible(screen)) {
            return false;
        }
        int x = left(screen);
        int y = top(screen);
        return mouseX >= x && mouseX < x + CosmeticsLayout.WIDTH && mouseY >= y && mouseY < y + CosmeticsLayout.HEIGHT;
    }

    public static void extractBackground(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g) {
        if (!visible(screen)) {
            return;
        }
        int x = left(screen);
        int y = top(screen);
        CatGui.panel(g, x, y, CosmeticsLayout.WIDTH, CosmeticsLayout.HEIGHT);
        int slotLeft = x - CosmeticsLayout.panelX(0);
        for (int i = 0; i < CatSlot.values().length; i++) {
            CatGui.slotFrame(g, slotLeft + CosmeticsLayout.slotX(0, i) - 1, y + CosmeticsLayout.slotY(i) - 1);
        }
        Player player = Minecraft.getInstance().player;
        int mask = player == null ? 0 : CatSets.mask(player);
        Font font = Minecraft.getInstance().font;
        int starY = y + CosmeticsLayout.setsY();
        for (CatSet set : SETS) {
            boolean worn = (mask & (1 << set.ordinal())) != 0;
            g.text(font, STAR, starX(x, set) - font.width(STAR) / 2, starY, worn ? STAR_ON : STAR_OFF, worn);
        }
    }

    private static int starX(int panelX, CatSet set) {
        return panelX + CosmeticsLayout.PADDING + STAR_STEP / 2 + set.ordinal() * STAR_STEP;
    }

    /** Tooltips of empty cosmetic slots and of the set stars; true when one was shown. */
    public static boolean tooltip(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, @Nullable Slot hovered,
                                  ItemStack carried, int mouseX, int mouseY) {
        if (!visible(screen) || !carried.isEmpty()) {
            return false;
        }
        Font font = Minecraft.getInstance().font;
        if (hovered != null && !hovered.hasItem() && hovered.container instanceof CosmeticsContainer) {
            CatSlot slot = CosmeticSlot.slotOfIcon(hovered.getNoItemIcon());
            if (slot != null) {
                g.setTooltipForNextFrame(font, Component.translatable(slot.translationKey()), mouseX, mouseY);
                return true;
            }
        }
        int x = left(screen);
        int starY = top(screen) + CosmeticsLayout.setsY();
        if (mouseY < starY - 1 || mouseY >= starY + font.lineHeight) {
            return false;
        }
        Player player = Minecraft.getInstance().player;
        for (CatSet set : SETS) {
            int starX = starX(x, set);
            if (player != null && mouseX >= starX - STAR_STEP / 2 && mouseX < starX + STAR_STEP / 2) {
                g.setComponentTooltipForNextFrame(font, setLines(player, set), mouseX, mouseY);
                return true;
            }
        }
        return false;
    }

    private static List<Component> setLines(Player player, CatSet set) {
        CosmeticInventory worn = CosmeticsManager.worn(player);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(set.translationKey())
                .withStyle(CatSets.has(player, set) ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
        for (CatSet.Piece piece : set.pieces()) {
            boolean has = worn.get(piece.slot()).is(piece.items());
            lines.add(Component.literal(has ? "✔ " : "✘ ").append(Component.translatable(piece.slot().translationKey()))
                    .withStyle(has ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
        lines.add(Component.translatable(set.translationKey() + ".bonus").withStyle(ChatFormatting.GRAY));
        return lines;
    }
}
