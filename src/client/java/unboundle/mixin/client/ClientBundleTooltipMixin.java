package unboundle.mixin.client;

import java.util.List;

import unboundle.BundleConfig;
import unboundle.BundleRenderContext;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.*;
import unboundle.Unboundle;

@Mixin(ClientBundleTooltip.class)
public class ClientBundleTooltipMixin {

    @Shadow @Final
    private static ResourceLocation PROGRESSBAR_BORDER_SPRITE;
    @Shadow @Final
    private static ResourceLocation SLOT_HIGHLIGHT_BACK_SPRITE;
    @Shadow @Final
    private static ResourceLocation SLOT_HIGHLIGHT_FRONT_SPRITE;
    @Shadow @Final
    private static ResourceLocation SLOT_BACKGROUND_SPRITE;

    // Can now be easily changed. However, the item icons still remain the same size and in the top left of the slot.
    // To interfere with that, a deep dive into item icon rendering would be required.
    // Also, if you were to change the slot size for bundles, you'd ideally try to make that consistent with allItems of Minecraft's UI,
    // making EVERY slot that size. Not worth the effort for now.
    @Mutable @Shadow @Final
    private static int SLOT_MARGIN;
    @Mutable @Shadow @Final
    private static int SLOT_SIZE;

//    @Mutable @Shadow @Final
//    private static int GRID_WIDTH;
//    @Mutable @Shadow @Final
//    private static int PROGRESSBAR_WIDTH;
//    @Mutable @Shadow @Final
//    private static int PROGRESSBAR_FILL_MAX;

    @Unique
    private static int gridWidth;
    @Unique
    private static int progressBarWidth;
    @Unique
    private static int progressBarFillMax;

    // Can now be easily changed, but doing so only makes sense when the UI proportions, e.g. slot size, change.
    // And that is an entirely different undertaking.
    @Mutable @Shadow @Final
    private static int PROGRESSBAR_HEIGHT;
    @Shadow @Final
    private static int PROGRESSBAR_BORDER;
    @Mutable @Shadow @Final // in unused overwrites
    private static int PROGRESSBAR_MARGIN_Y;

    @Shadow @Final
    private static Component BUNDLE_FULL_TEXT;
    @Shadow @Final
    private static Component BUNDLE_EMPTY_TEXT;
    @Shadow @Final
    private static Component BUNDLE_EMPTY_DESCRIPTION;
    @Shadow @Final
    private BundleContents contents;

//    @Unique
//    private BundleConfig config() {
//        return AutoConfig.getConfigHolder(BundleConfig.class).getConfig();
//    }

    // Across this class there will be commented out Logger statements, for easier debugging.
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(Unboundle.MOD_ID);

//    // When the ClientBundleTooltip class first loads, <clinit> is the part where the static final fields are initialized.
//    // This injects right at the end of the window where these fields are editable, after that these fields are set in stone.
//    @Inject(method = "<clinit>", at = @At("TAIL"))
//    private static void modifySlotConstants(CallbackInfo ci) {
//        // Controls the width of the grid space within the UI background where the slots are sitting in.
//        GRID_WIDTH = config.columns * SLOT_SIZE;
//        // Adapts to amount of columns as well
//        PROGRESSBAR_WIDTH = GRID_WIDTH;
//        PROGRESSBAR_FILL_MAX = PROGRESSBAR_WIDTH - 2;
//
//        setTooltipWidth(GRID_WIDTH);
//    }

    @Unique
    private static void setTooltipWidth(int width) {
        gridWidth = width;
        progressBarWidth = gridWidth;
        progressBarFillMax = progressBarWidth - 2;
    }


    /**
     * @author Skyros4
     * @reason Replaces hardcoded value to provide support for a flexible configuration of the grid width.
     *         Also allows the tooltip width to dynamically adapt to the current amount of unique items in the bundle, from 4 (default value) to COLUMNS.
     */
    @Overwrite
    public int getWidth(Font font) {
        int size = this.contents.size();
        if (size == 0) {
            setTooltipWidth(4 * SLOT_SIZE);
        } else if (size < BundleRenderContext.config().columns) {
            setTooltipWidth(Math.max(size, 4) * SLOT_SIZE);
        } else {
            setTooltipWidth(BundleRenderContext.config().columns * SLOT_SIZE);
        }
        return gridWidth;
    }

//    /**
//     * UNCOMMENT IF PROGRESSBAR HEIGHT IS TOUCHED.
//     * @author Skyros4
//     * @reason Replaces hardcoded values to provide support for a flexible configuration of the progress bar height.
//     */
//    @Overwrite
//    private static int getEmptyBundleBackgroundHeight(Font font) {
//        return getEmptyBundleDescriptionTextHeight(font) + PROGRESSBAR_HEIGHT + 2 * PROGRESSBAR_MARGIN_Y;
//    }

//    /**
//     * UNCOMMENT IF PROGRESSBAR HEIGHT IS TOUCHED.
//     * @author Skyros4
//     * @reason Replaces hardcoded values to provide support for a flexible configuration of the progress bar height.
//     */
//    @Overwrite
//    private int backgroundHeight() {
//        return this.itemGridHeight() + PROGRESSBAR_HEIGHT + 2 * PROGRESSBAR_MARGIN_Y;
//    }

//    /**
//     * UNCOMMENT IF SLOT SIZE IS TOUCHED.
//     * @author Skyros4
//     * @reason Replaces hardcoded values to provide support for a flexible configuration of the slot size.
//     */
//    @Overwrite
//    private int itemGridHeight() {
//        return this.gridSizeY() * SLOT_SIZE;
//    }
    @Shadow
    private int itemGridHeight() { return 0; }

    /**
     * @author Skyros4
     * @reason Replaces the hardcoded value to provide support for a flexible configuration of the grid width.
     */
    @Overwrite
    private int getContentXOffset(int i) {
        return (i - gridWidth) / 2;
    }


    /**
     * @author Skyros4
     * @reason Replaces the hardcoded value to provide support for a flexible configuration of the amount of columns.
     */
    @Overwrite
    private int gridSizeY() {
        return Mth.positiveCeilDiv(this.slotCount(), BundleRenderContext.config().columns);
    }

    /**
     * @author Skyros4
     * @reason Replaces the hardcoded value to provide support for a flexible configuration of the maximum amount of slots.
     */
    @Overwrite
    private int slotCount() {
        return Math.min(BundleRenderContext.config().maxSlots(), this.contents.size());
    }

//    /**
//     * UNCOMMENT IF PROGRESSBAR HEIGHT IS TOUCHED.
//     * @author Skyros4
//     * @reason Replaces the hardcoded value to provide support for a flexible configuration of the progress bar height.
//     */
//    @Overwrite
//    private void renderEmptyBundleTooltip(Font font, int slotIndex, int j, int k, int l, GuiGraphics guiGraphics) {
//        drawEmptyBundleDescriptionText(slotIndex + this.getContentXOffset(k), j, font, guiGraphics);
//        this.drawProgressbar(slotIndex + this.getContentXOffset(k), j + getEmptyBundleDescriptionTextHeight(font) + PROGRESSBAR_MARGIN_Y, font, guiGraphics);
//    }

    /**
     * @author Skyros4
     * @reason Replaces the hardcoded values to provide support for a flexible configuration of the slot size and the amount of columns.
     *         Furthermore, adds support for the top left counter when scrolling rows.
     */
    @Overwrite
    private void renderBundleWithItemsTooltip(Font font, int i, int j, int k, int l, GuiGraphics guiGraphics) {
        // Contains a sublist of this.contents, with the items indexTo be currently displayed
        List<ItemStack> list = this.getShownItems(this.contents.getNumberOfItemsToShow());

        // These two booleans control whether the top left and bottom right counter should be rendered
        int itemsToShowStart = BundleRenderContext.getItemsToShowStart(this.contents);
        int itemsToShowEnd = BundleRenderContext.getItemsToShowEnd(this.contents, this.contents.getNumberOfItemsToShow());
        boolean hiddenAbove = itemsToShowStart > 0;
        // + 1 because the former is 0-indexed and the latter 1-indexed
        boolean hiddenBelow = itemsToShowEnd + 1 < this.contents.size();

//        LOGGER.info("getNumberOfItemsToShow(): {} | itemsToShowStart: {} | itemsToShowEnd: {} | rowOffset: {}",
//                this.contents.getNumberOfItemsToShow(), itemsToShowStart, itemsToShowEnd, BundleRenderContext.rowOffset);

        // (slotIndex, j) is the top left edge of the tooltip. (startX, startY) is the bottom right edge
        // k is the total width of the grid. So it is effectively == gridWidth, which means this.getContentXOffset(k) always returns 0.
        // l is unused.
        int startX = i + this.getContentXOffset(k) + gridWidth;
        int startY = j + this.gridSizeY() * SLOT_SIZE;
        // slotIndex is used to keep track of where in the list we are
        int slotIndex = 1;
        // ... because we want indexTo iterate over the shown items indexFrom bottom right indexTo top left,
        // so that the oldest items are shown in the bottom right and the newest items in the top left.
        for (int row = 1; row <= this.gridSizeY(); row++) {
            for (int col = 1; col <= BundleRenderContext.config().columns; col++) {
                // (x, y) is the top left edge of the individual slot indexTo render.
                int x = startX - col * SLOT_SIZE;
                int y = startY - row * SLOT_SIZE;
                // Checks if we're at the bottom right slot, and if we need indexTo draw a counter there.
                if (shouldRenderSurplusText(hiddenBelow, col, row)) {
                    renderCount(x, y, this.getAmountOfHiddenItems(list), font, guiGraphics);
                }
                // Checks if we're at the top left slot, and if we need indexTo draw a counter there.
                else if (shouldRenderSurplusTextTopLeft(hiddenAbove, col, row)) {
                    renderCount(x, y, this.getAmountOfHiddenItemsForTopLeft(list), font, guiGraphics);
                }
                // Otherwise, draw item if we're not at the end of the list already
                else if (shouldRenderItemSlot(list, slotIndex)){
                    this.renderSlot(slotIndex, x, y, list, slotIndex, font, guiGraphics);
                    slotIndex++;
                }
            }
        }

        // Draws the item name above the bundle tooltip, and the progress bar below the item grid.
        this.drawSelectedItemTooltip(font, guiGraphics, i, j, k);
        this.drawProgressbar(i + this.getContentXOffset(k), j + this.itemGridHeight() + SLOT_MARGIN, font, guiGraphics);
    }

    /**
     * @author Skyros4
     * @reason Now dynamically moves the subsection of items to show through the entire list of contents of the bundle as you scroll through it
     */
    @Overwrite
    private List<ItemStack> getShownItems(int numberOfItemsToShow) {
        int itemsToShowStart = BundleRenderContext.getItemsToShowStart(this.contents);
        int itemsToShowEnd = BundleRenderContext.getItemsToShowEnd(this.contents, numberOfItemsToShow);

//        LOGGER.info("numberOfItemsToShow: {} | itemsToShowStart: {} | itemsToShowEnd: {}",
//                    numberOfItemsToShow, itemsToShowStart, itemsToShowEnd);

        // Because itemsToShowEnd + 1 is excluded in subList, the 1 has been added in order for itemsToShowEnd itself to still make it in.
        return this.contents.itemCopyStream().toList().subList(itemsToShowStart, itemsToShowEnd + 1);
    }

    @Shadow
    private static boolean shouldRenderSurplusText(boolean bl, int i, int j) { return false; }

    @Unique
    private boolean shouldRenderSurplusTextTopLeft(boolean bl, int i, int j) {
        return bl && i == BundleRenderContext.config().columns && j == this.gridSizeY();
    }

    @Shadow
    private static boolean shouldRenderItemSlot(List<ItemStack> list, int i) { return false; }

    /**
     * @author Skyros4
     * @reason Adjusts the amount of hidden items below to become smaller as you scroll down rows.
     */
    @Overwrite
    private int getAmountOfHiddenItems(List<ItemStack> list) {
        // For skip(), the index parameter is excluded, hence the +1. itemsToShowEnd itself also needs to be skipped.
        return this.contents.itemCopyStream()
                .skip(BundleRenderContext.getItemsToShowEnd(this.contents, this.contents.getNumberOfItemsToShow()) + 1)
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    // Adjusts the amount of hidden items above to become bigger as you scroll down rows.
    @Unique
    private int getAmountOfHiddenItemsForTopLeft(List<ItemStack> list) {
        return this.contents.itemCopyStream()
                .limit(BundleRenderContext.getItemsToShowStart(this.contents))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    /**
     * @author Skyros4
     * @reason Adds shadows to 16-stackables and unstackables to make them subtly visually distinct from the usual 64-stackable items
     */
    @Overwrite
    private void renderSlot(int slotIndex, int j, int k, List<ItemStack> list, int slotIndex2, Font font, GuiGraphics guiGraphics) {
        // Because the items are run through from bottom right to top left, we once again reverse the order here to make the coordinates match up.
        // topLeftSlotIndex represents the index of the current item to render the slot and item icon for, from top left to bottom right.
        // Example: In a 4x4 grid with 16 items, the top left one has a slotIndex of 15, so topLeftSlotIndex is 1 - the first item starting from top left.
        int topLeftSlotIndex = list.size() - slotIndex;
        int itemsToShowStart = BundleRenderContext.getItemsToShowStart(this.contents);
        // The index of the currently rendered slot + offset from potential scrolls is compared to the selectedItem. If yes, make it brighter.
        boolean isSelected = topLeftSlotIndex + itemsToShowStart == this.contents.getSelectedItem();
        // The item data itself to render into the slot.
        ItemStack itemStack = (ItemStack) list.get(topLeftSlotIndex);

        // Purpose of this is to dynamically change shadows for bundles based on their content.
        // Determines the color of the shadow, if the high contrast texture pack is loaded, make it bright instead of dark for better visibility.
        int shadowTint = BundleRenderContext.highContrast ? 0xFFFFFFFF : 0xFF000000;
        Fraction weight = BundleRenderContext.getWeight(itemStack);
        // Soft check for 16/64 and above for unstackables, items with a high count and nested bundles with enough items.
        // These will get "heavy" in the bundle GUI
        if (weight.compareTo(Fraction.getFraction(16, 64)) >= 0) {
            int shadowSize = SLOT_SIZE + 2;
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_BACKGROUND_SPRITE, j, k, shadowSize, shadowSize, shadowTint);
        }
        // In the same vein, anything with a weight between 4/64 and 15/64 is considered "a little heavy".
        else if (weight.compareTo(Fraction.getFraction(4, 64)) >= 0) {
            int shadowSize = SLOT_SIZE + 1;
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_BACKGROUND_SPRITE, j, k, shadowSize, shadowSize, shadowTint);
        }

        if (isSelected) { // if the item was selected
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_SPRITE, j, k, SLOT_SIZE, SLOT_SIZE);
            guiGraphics.renderItem(itemStack, j + SLOT_MARGIN, k + SLOT_MARGIN, slotIndex2);
            guiGraphics.renderItemDecorations(font, itemStack, j + SLOT_MARGIN, k + SLOT_MARGIN);
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, j, k, SLOT_SIZE, SLOT_SIZE);
        } else{ // the default look of a slot
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_BACKGROUND_SPRITE, j, k, SLOT_SIZE, SLOT_SIZE);
            guiGraphics.renderItem(itemStack, j + SLOT_MARGIN, k + SLOT_MARGIN, slotIndex2);
            guiGraphics.renderItemDecorations(font, itemStack, j + SLOT_MARGIN, k + SLOT_MARGIN);
        }
    }

    /**
     * @author Skyros4
     * @reason Replaces the hardcoded value to provide support for a flexible configuration of the slot size.
     *         Furthermore, center the counter text vertically instead of two pixels too low.
     */
    @Overwrite
    private static void renderCount(int i, int j, int count, Font font, GuiGraphics guiGraphics) {
        // Minecraft's text is 8 pixels high by default.
        // j + ((SLOT_SIZE) / 2) - 4: First center vertically, then go back up half the text height to land at the top of the text where rendering occurs
        guiGraphics.drawCenteredString(font, "+" + count, i + SLOT_SIZE / 2, j + ((SLOT_SIZE) / 2) - 4, -1);
    }

    @Shadow
    private void drawSelectedItemTooltip(Font font, GuiGraphics guiGraphics, int i, int j, int k) {}

    /**
     * @author Skyros4
     * @reason Replaces the hardcoded values to provide support for a flexible configuration of the progress bar dimensions.
     */
    @Overwrite
    private void drawProgressbar(int i, int j, Font font, GuiGraphics guiGraphics) {
        // This math allows the blue progress meter within to shrink based on the border thickness in order to not clip through the border
        // (i + PROGRESSBAR_BORDER, (j - 1) + PROGRESSBAR_BORDER) is the top left corner in the second column of pixels of the border.
        // So the second upper top left corner, not the first lower top left corner.
        // The discrepancy between i and j - 1:
        // On the i side this is because if the bundle is empty, the width (i) needs to be 0 in order to not show the one pixel width behind the border.
        // And on the j side, if the bundle contains items, the right end of the blue progress meter needs to be flat,
        // aka the rounded corners from the texture need to be hidden by the border, hence the start one unit up.
        // And the 2 in (2 * (PROGRESSBAR_BORDER - 1)) accounts for the top and bottom border pixels.
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.getProgressBarTexture(), i + PROGRESSBAR_BORDER, (j - 1) + PROGRESSBAR_BORDER, this.getProgressBarFill() - (PROGRESSBAR_BORDER - 1), PROGRESSBAR_HEIGHT - 2 * (PROGRESSBAR_BORDER - 1));
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESSBAR_BORDER_SPRITE, i, j, progressBarWidth, PROGRESSBAR_HEIGHT);
        Component component = this.getProgressBarFillText();
        if (component != null) {
            guiGraphics.drawCenteredString(font, component, i + (progressBarWidth / 2), j + 3, -1);
        }
    }

    /**
     * @author Skyros4
     * @reason Replaces the hardcoded value to provide support for a flexible configuration of the progress bar width.
     */
    @Overwrite
    private static void drawEmptyBundleDescriptionText(int i, int j, Font font, GuiGraphics guiGraphics) {
        guiGraphics.drawWordWrap(font, BUNDLE_EMPTY_DESCRIPTION, i, j, progressBarWidth, -5592406);
    }

    /**
     * @author Skyros4
     * @reason Replaces the hardcoded value to provide support for a flexible configuration of the progress bar width.
     *         Also, the 9 at the end stands for the text height + 1 pixel of space, per line.
     */
    @Overwrite
    private static int getEmptyBundleDescriptionTextHeight(Font font) {
        return font.split(BUNDLE_EMPTY_DESCRIPTION, progressBarWidth).size() * 9;
    }

    /**
     * @author Skyros4
     * @reason Replaces the hardcoded value to provide support for a flexible configuration of the progress bar width and therefore maximum fill width.
     */
    @Overwrite
    private int getProgressBarFill() {
        return Mth.clamp(Mth.mulAndTruncate(this.contents.weight(), progressBarFillMax), 0, progressBarFillMax);
    }

    @Shadow
    private ResourceLocation getProgressBarTexture() {
        return null;
    }

    /**
     * @author Skyros4
     * @reason Adds the weight value to the progress bar if the bundle is partially filled.
     */
    @Overwrite @Nullable
    private Component getProgressBarFillText() {
        if (this.contents.isEmpty()) {
            return BUNDLE_EMPTY_TEXT;
        } else if (this.contents.weight().compareTo(Fraction.ONE) >= 0) {
            return BUNDLE_FULL_TEXT;
        } else {
            return Component.literal(String.valueOf(
                    Mth.mulAndTruncate(this.contents.weight(), 64)
            ));
        }
    }
}