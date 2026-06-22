package unboundle.mixin.client;

import java.util.List;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import unboundle.BundleTooltipContext;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
//? if >= 1.21.6 {
import net.minecraft.client.renderer.RenderPipelines;
//?} else {
/*import net.minecraft.client.renderer.RenderType;
*///?}
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.*;
import unboundle.Unboundle;
import unboundle.UnboundleConfig;
import unboundle.mixin.BundleContentsAccessor;

@Mixin(ClientBundleTooltip.class)
public class ClientBundleTooltipMixin {

    @Shadow @Final
    private static ResourceLocation SLOT_HIGHLIGHT_BACK_SPRITE;
    @Shadow @Final
    private static ResourceLocation SLOT_HIGHLIGHT_FRONT_SPRITE;
    @Shadow @Final
    private static ResourceLocation SLOT_BACKGROUND_SPRITE;
    @Shadow @Final
    private static int SLOT_MARGIN;
    @Shadow @Final
    private static int SLOT_SIZE;

    @Unique
    private static int gridWidth;
    @Unique
    private static int progressBarWidth;
    @Unique
    private static int progressBarFillMax;

    @Shadow @Final
    private BundleContents contents;

    // Across this class there will be commented out Logger statements, for easier debugging.
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(Unboundle.MOD_ID);

    @Unique
    private static void setTooltipWidth(int width) {
        gridWidth = width;
        progressBarWidth = gridWidth;
        progressBarFillMax = progressBarWidth - 2; // accounts for the one pixel wide border on both sides
    }

    // Replaces hardcoded value to provide support for a flexible configuration of the grid width.
    // Also allows the tooltip width to dynamically adapt to the current amount of unique items in the bundle,
    // from 4 (default value needed for the empty bundle) to COLUMNS.
    @ModifyConstant(method = "getWidth(Lnet/minecraft/client/gui/Font;)I", constant = @Constant(intValue = 96))
    public int dynamicWidth(int original) {
        int size = this.contents.size();
        if (size == 0) {
            setTooltipWidth(4 * SLOT_SIZE);
        } else if (size < UnboundleConfig.config().columns) {
            setTooltipWidth(Math.max(size, 4) * SLOT_SIZE);
        } else {
            setTooltipWidth(UnboundleConfig.config().columns * SLOT_SIZE);
        }
        return gridWidth;
    }

    @Shadow
    private int itemGridHeight() { return 0; }

    @Shadow
    private int getContentXOffset(int i) { return 0; }
    // Replaces the hardcoded value to provide support for a flexible configuration of the grid width.
    @ModifyConstant(method = "getContentXOffset(I)I", constant = @Constant(intValue = 96))
    private static int getContentXOffset$dynamicGridWidth(int original) {
        return gridWidth;
    }

    @Shadow
    private int gridSizeY() { return 0; }
    // Replaces the hardcoded value to provide support for a flexible configuration of the amount of columns.
    @ModifyConstant(method = "gridSizeY()I", constant = @Constant(intValue = 4))
    private static int dynamicColumns(int original) {
        return UnboundleConfig.config().columns;
    }

    // Replaces the hardcoded value to provide support for a flexible configuration of the maximum amount of slots.
    @ModifyConstant(method = "slotCount()I", constant = @Constant(intValue = 12))
    private static int dynamicMaxSlots(int original) {
        return UnboundleConfig.config().maxSlots();
    }

    // Replaces the hardcoded values to provide support for a flexible configuration of the slot size and the amount of columns.
    // Furthermore, adds support for the top left counter when scrolling rows.
    // WrapMethod is used as I haven't found a clean way to add a new elseif for shouldRenderSurplusTextTopLeft otherwise.
    @WrapMethod(method = "renderBundleWithItemsTooltip(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/client/gui/GuiGraphics;)V")
    private void addTopLeftCounter(Font font, int x, int y, int w, int h, GuiGraphics guiGraphics, Operation<Void> original) {
        // Contains a sublist of this.contents, with the items to be currently displayed
        List<ItemStack> list = this.getShownItems(this.contents.getNumberOfItemsToShow());
        int itemsToShowStart = BundleTooltipContext.getItemsToShowStart(this.contents.size());
        int itemsToShowEnd = BundleTooltipContext.getItemsToShowEnd(this.contents.size(), this.contents.getNumberOfItemsToShow());
        // These two booleans control whether the top left and bottom right counter should be rendered
        boolean hiddenAbove = itemsToShowStart > 0;
        // + 1 because the former is 0-indexed and the latter 1-indexed
        boolean hiddenBelow = itemsToShowEnd + 1 < this.contents.size();

//        LOGGER.info("getNumberOfItemsToShow(): {} | itemsToShowStart: {} | itemsToShowEnd: {} | rowOffset: {}",
//                this.contents.getNumberOfItemsToShow(), itemsToShowStart, itemsToShowEnd, BundleTooltipContext.rowOffset);

        // (slotIndex, y) is the top left edge of the tooltip. (startX, startY) is the bottom right edge
        // w is the total width of the grid. So it is effectively == gridWidth, which means this.getContentXOffset(k) always returns 0.
        // h is unused.
        int startX = x + this.getContentXOffset(w) + gridWidth;
        int startY = y + this.gridSizeY() * SLOT_SIZE;
        // slotIndex is used to keep track of where in the list we are
        int slotIndex = 1;
        // ... because we want to iterate over the shown items from bottom right to top left,
        // so that the oldest items are shown in the bottom right and the newest items in the top left.
        for (int row = 1; row <= this.gridSizeY(); row++) {
            for (int col = 1; col <= UnboundleConfig.config().columns; col++) {
                // (loopX, loopY) is the top left edge of the individual slot to render.
                int loopX = startX - col * SLOT_SIZE;
                int loopY = startY - row * SLOT_SIZE;
                // Checks if we're at the bottom right slot, and if we need to draw a counter there.
                if (shouldRenderSurplusText(hiddenBelow, col, row)) {
                    renderCount(loopX, loopY, this.getAmountOfHiddenItems(list), font, guiGraphics);
                }
                // Checks if we're at the top left slot, and if we need to draw a counter there.
                else if (shouldRenderSurplusTextTopLeft(hiddenAbove, col, row)) {
                    renderCount(loopX, loopY, this.getAmountOfHiddenItemsForTopLeft(), font, guiGraphics);
                }
                // Otherwise, draw item if we're not at the end of the list already
                else if (shouldRenderItemSlot(list, slotIndex)){
                    this.renderSlot(slotIndex, loopX, loopY, list, slotIndex, font, guiGraphics);
                    slotIndex++;
                }
            }
        }

        // Draws the item name above the bundle tooltip, and the progress bar below the item grid.
        this.drawSelectedItemTooltip(font, guiGraphics, x, y, w);
        this.drawProgressbar(x + this.getContentXOffset(w), y + this.itemGridHeight() + SLOT_MARGIN, font, guiGraphics);
    }

    @Shadow
    private List<ItemStack> getShownItems(int numberOfItemsToShow) { return null;}
    // Now dynamically moves the subsection of items to show through the entire list of contents of the bundle as you scroll through it
    @WrapMethod(method = "getShownItems(I)Ljava/util/List;")
    private List<ItemStack> dynamicItemWindows(int numberOfItemsToShow, Operation<List<ItemStack>> original) {
        int itemsToShowStart = BundleTooltipContext.getItemsToShowStart(this.contents.size());
        int itemsToShowEnd = BundleTooltipContext.getItemsToShowEnd(this.contents.size(), numberOfItemsToShow);

//        LOGGER.info("numberOfItemsToShow: {} | itemsToShowStart: {} | itemsToShowEnd: {}",
//                    numberOfItemsToShow, itemsToShowStart, itemsToShowEnd);

        // Because itemsToShowEnd + 1 is excluded in subList, the 1 has been added in order for itemsToShowEnd itself to still make it in.
        return this.contents.itemCopyStream().toList().subList(itemsToShowStart, itemsToShowEnd + 1);
    }

    @Shadow
    private static boolean shouldRenderSurplusText(boolean bl, int i, int j) { return false; }

    @Unique
    private boolean shouldRenderSurplusTextTopLeft(boolean bl, int i, int j) {
        return bl && i == UnboundleConfig.config().columns && j == this.gridSizeY();
    }

    @Shadow
    private static boolean shouldRenderItemSlot(List<ItemStack> list, int i) { return false; }

    @Shadow
    private int getAmountOfHiddenItems(List<ItemStack> list) { return 0; }

    // Adjusts the amount of hidden items below to become smaller as you scroll down rows.
    @ModifyReturnValue(
            method = "getAmountOfHiddenItems(Ljava/util/List;)I",
            at = @At("RETURN")
    )
    private int getAmountOfHiddenItems$considerRowScrolls(int original) {
        return this.contents.itemCopyStream()
                .skip(BundleTooltipContext.getItemsToShowEnd(this.contents.size(), this.contents.getNumberOfItemsToShow()) + 1)
                .mapToInt(ItemStack::getCount)
                .sum();
    }
    // Adjusts the amount of hidden items above to become bigger as you scroll down rows.
    @Unique
    private int getAmountOfHiddenItemsForTopLeft() {
        return this.contents.itemCopyStream()
                .limit(BundleTooltipContext.getItemsToShowStart(this.contents.size()))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    @Shadow
    private void renderSlot(int slotIndex, int j, int k, List<ItemStack> list, int slotIndex2, Font font, GuiGraphics guiGraphics) {}

    // Adds shadows to 16-stackables and unstackables to make them subtly visually distinct from the usual 64-stackable items.
    // Also replaces the hardcoded values to provide support for a flexible configuration of the bundle tooltip.
    @WrapMethod(method = "renderSlot(IIILjava/util/List;ILnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/GuiGraphics;)V")
    private void addShadows(int slotIndex, int j, int k, List<ItemStack> list, int slotIndex2, Font font, GuiGraphics guiGraphics, Operation<Void> original) {
        // Because the items are run through from bottom right to top left, we once again reverse the order here to make the coordinates match up.
        // topLeftSlotIndex represents the index of the current item to render the slot and item icon for, from top left to bottom right.
        // Example: In a 4x4 grid with 16 items, the top left one has a slotIndex of 15, so topLeftSlotIndex is 1 - the first item starting from top left.
        int topLeftSlotIndex = list.size() - slotIndex;
        int itemsToShowStart = BundleTooltipContext.getItemsToShowStart(this.contents.size());
        // The index of the currently rendered slot + offset from potential scrolls is compared to the selectedItem. If yes, make it brighter.
        boolean isSelected = topLeftSlotIndex + itemsToShowStart == this.contents.getSelectedItem();
        // The item data itself to render into the slot.
        ItemStack itemStack = list.get(topLeftSlotIndex);

        // Purpose of this is to dynamically change shadows for bundles based on their content.
        // Determines the color of the shadow. If the high contrast texture pack is loaded, make it bright instead of dark for better visibility.
        int shadowTint = BundleTooltipContext.highContrast ? 0xFFFFFFFF : 0xFF000000;
        Fraction weight = ((BundleContentsAccessor)(Object) contents).invokeGetWeight(itemStack);
        //? if >= 1.21.6 {
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
         //?} else {
        /*// Soft check for 16/64 and above for unstackables, items with a high count and nested bundles with enough items.
        // These will get "heavy" in the bundle GUI
        if (weight.compareTo(Fraction.getFraction(16, 64)) >= 0) {
            int shadowSize = SLOT_SIZE + 2;
            guiGraphics.blitSprite(RenderType::guiTextured, SLOT_BACKGROUND_SPRITE, j, k, shadowSize, shadowSize, shadowTint);
        }
        // In the same vein, anything with a weight between 4/64 and 15/64 is considered "a little heavy".
        else if (weight.compareTo(Fraction.getFraction(4, 64)) >= 0) {
            int shadowSize = SLOT_SIZE + 1;
            guiGraphics.blitSprite(RenderType::guiTextured, SLOT_BACKGROUND_SPRITE, j, k, shadowSize, shadowSize, shadowTint);
        }

        if (isSelected) { // if the item was selected
            guiGraphics.blitSprite(RenderType::guiTextured, SLOT_HIGHLIGHT_BACK_SPRITE, j, k, SLOT_SIZE, SLOT_SIZE);
            guiGraphics.renderItem(itemStack, j + SLOT_MARGIN, k + SLOT_MARGIN, slotIndex2);
            guiGraphics.renderItemDecorations(font, itemStack, j + SLOT_MARGIN, k + SLOT_MARGIN);
            guiGraphics.blitSprite(RenderType::guiTexturedOverlay, SLOT_HIGHLIGHT_FRONT_SPRITE, j, k, SLOT_SIZE, SLOT_SIZE);
        } else{ // the default look of a slot
            guiGraphics.blitSprite(RenderType::guiTextured, SLOT_BACKGROUND_SPRITE, j, k, SLOT_SIZE, SLOT_SIZE);
            guiGraphics.renderItem(itemStack, j + SLOT_MARGIN, k + SLOT_MARGIN, slotIndex2);
            guiGraphics.renderItemDecorations(font, itemStack, j + SLOT_MARGIN, k + SLOT_MARGIN);
        }
        *///?}
    }

    @Shadow
    private static void renderCount(int i, int j, int count, Font font, GuiGraphics guiGraphics) {}

    @Shadow
    private void drawSelectedItemTooltip(Font font, GuiGraphics guiGraphics, int i, int j, int k) {}

    @Shadow
    private void drawProgressbar(int i, int j, Font font, GuiGraphics guiGraphics) {}

    // Replaces the hardcoded values to provide support for a flexible configuration of the progress bar width.
    @ModifyArg(
            method = "drawProgressbar(IILnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At(
                    value = "INVOKE",
                    //? if >= 1.21.6 {
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V",
                    //?} else {
                    /*target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V",
                    *///?}
                    ordinal = 1
            ),
            index = 4
    )
    private static int drawProgressbar$BlitSprite2$dynamicProgressBarWidth(int ninetySix) {
        return progressBarWidth;
    }
    @ModifyArg(
            method = "drawProgressbar(IILnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V",
                    ordinal = 0
            ),
            index = 2
    )
    private static int drawProgressbar$CenteredString$dynamicI(int i) {
        // undoing the hardcoded width "+ 48" before adding half the actual width
        return i - 48 + (progressBarWidth / 2);
    }

    // Replaces the hardcoded value to provide support for a flexible configuration of the progress bar width.
    @ModifyConstant(method = "drawEmptyBundleDescriptionText(IILnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/GuiGraphics;)V", constant = @Constant(intValue = 96))
    private static int drawEmptyBundleDescriptionText$dynamicProgressBarWidth(int original) {
        return progressBarWidth;
    }

    // Replaces the hardcoded value to provide support for a flexible configuration of the progress bar width and therefore maximum fill width.
    @ModifyConstant(method = "getProgressBarFill()I", constant = @Constant(intValue = 94))
    private static int getProgressBarFill$dynamicProgressBarMargin(int original) {
        return progressBarFillMax;
    }

    // Adds the weight value to the progress bar if the bundle is partially filled.
    @Inject(
            method = "getProgressBarFillText()Lnet/minecraft/network/chat/Component;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void getProgressBarFillText$unboundle(CallbackInfoReturnable<Component> cir) {
        // Vanilla returns null if no text should be shown (when the bundle is neither completely empty nor full).
        // Here we intercept this null and replace it with the weight value in string form instead.
        if (cir.getReturnValue() == null) {
            cir.setReturnValue(Component.literal(String.valueOf(
                    Mth.mulAndTruncate(this.contents.weight(), 64)
            )));
        }
    }

    // Replaces the hardcoded value to provide support for a flexible configuration of the progress bar width.
    @ModifyConstant(method = "getEmptyBundleDescriptionTextHeight(Lnet/minecraft/client/gui/Font;)I", constant = @Constant(intValue = 96))
    private static int getEmptyBundleDescriptionTextHeight$dynamicProgressBarWidth(int original) {
        return progressBarWidth;
    }
}