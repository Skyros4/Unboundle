package unboundle.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import unboundle.BundleTooltipContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.gui.BundleMouseActions;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.joml.Vector2i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.*;
import unboundle.Unboundle;

@Mixin(BundleMouseActions.class)
public class BundleMouseActionsMixin {

    @Shadow @Final
    private ScrollWheelHandler scrollWheelHandler;

    // Across this class there will be commented out Logger statements, for easier debugging.
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(Unboundle.MOD_ID);

    // General rule of thumb: Recompute values whenever you can, as @Local values are unstable.
    // Only use @Local variables if you have no other way of obtaining them.

    // What we're doing here: We let the game run the checks for when you should be able to scroll.
    // Then we replace the one line where something actually happens, and handle the 5 new cases
    @Redirect(
        method = "onMouseScrolled(DDILnet/minecraft/world/item/ItemStack;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/BundleMouseActions;toggleSelectedBundleItem(Lnet/minecraft/world/item/ItemStack;II)V"
        )
    )
    private void handleRowScrolls(BundleMouseActions instance,
        ItemStack bundleItemStack, int i, int originalNewSelected,
        @Local(argsOnly = true, ordinal = 0) double d,
        @Local(argsOnly = true, ordinal = 1) double e
    ) {
        Vector2i vector2i = this.scrollWheelHandler.onMouseScroll(d, e);
        // Determines the direction of the scroll, considering mouses that scroll horizontally if no vertical scroll is detected.
        // The change here is: Cap the scrolling speed. Math.signum detects the sign of vector2i, and returns either 1.0, 0.0 or -1.0 respectively.
        // That way scrolling past the currently shown window by traveling multiple slots in 1 frame is not possible.
        // Both directions are reversed so that down and right are 1, and up and left are -1.
        int scrollDirection = vector2i.y != 0 ? (int) Math.signum(-vector2i.y) : (int) Math.signum(vector2i.x);

        // ***get the current state of the bundle UI***
        BundleContents bundleContents = bundleItemStack.get(DataComponents.BUNDLE_CONTENTS);
        int totalItems = bundleContents.size();
        // The total amount of rows we can scroll down by at most.
        int maxRowOffset = BundleTooltipContext.getMaxRowOffset(totalItems);
        // These indexes are relative to entire list, not the current window
        int currentItemsToShowStart = BundleTooltipContext.getItemsToShowStart(bundleContents.size());
        int currentItemsToShowEnd = BundleTooltipContext.getItemsToShowEnd(bundleContents.size(), bundleContents.getNumberOfItemsToShow());
        //? if >= 26.1 {
        /*int currentlySelected = BundleItem.getSelectedItemIndex(bundleItemStack);
        *///?} else {
        int currentlySelected = BundleItem.getSelectedItem(bundleItemStack);
         //?}
        // scrollDirection is negated because renderBundleWithItemsTooltip within ClientBundleTooltip renders the items from bottom right to top left,
        // so forward (the index increases) would actually be up left, when we want it to be down right.
        int newSelected = ScrollWheelHandler.getNextScrollWheelSelection(-scrollDirection, currentlySelected, totalItems);

        // ***The four special cases where either a row scroll or a wrap around occurs***
        // We're at the very last slot, and scroll forwards
        boolean atActualLast = currentlySelected == totalItems - 1 && scrollDirection > 0;
        // We're at the very first slot, and scroll backwards
        boolean atActualFirst = currentlySelected <= 0 && scrollDirection < 0;
        // We're at the last slot of the current window, excluding the actual last slot, and scroll forwards
        boolean atVisibleLast = !atActualLast && currentlySelected == currentItemsToShowEnd && scrollDirection > 0;
        // We're at the first slot of the current window, excluding the actual first slot, and scroll backwards
        boolean atVisibleFirst = !atActualFirst && newSelected < currentItemsToShowStart && scrollDirection < 0;
        if (atActualLast) {
            // This flips to the topmost window for the items
            BundleTooltipContext.rowOffset = 0;
            this.toggleSelectedBundleItem(bundleItemStack, i, 0);
        }
        else if (atActualFirst) {
            // This flips to the bottommost window for the items
            BundleTooltipContext.rowOffset = maxRowOffset;
            this.toggleSelectedBundleItem(bundleItemStack, i, totalItems - 1);
        }
        else if (atVisibleLast) {
            // Proceed down one row
            BundleTooltipContext.rowOffset = BundleTooltipContext.rowOffset + 1;
            this.toggleSelectedBundleItem(bundleItemStack, i, currentItemsToShowEnd + 1);
        }
        else if (atVisibleFirst) {
            // Proceed up one row
            BundleTooltipContext.rowOffset = BundleTooltipContext.rowOffset - 1;
            this.toggleSelectedBundleItem(bundleItemStack, i, currentItemsToShowStart - 1);
        }
        else {
            // In all other cases, just change whatever was the selected item, without the slots themselves as part of the window moving.
            this.toggleSelectedBundleItem(bundleItemStack, i, newSelected);
        }

//        // the amount of items to be displayed in the current window. For Logging purposes, see below
//        int numberOfItemsToShow = BundleItem.getNumberOfItemsToShow(bundleItemStack);
//        // The only difference to currentItemsToShow is that these are computed after the rowOffset has been updated.
//        // So these represent the new indexes for the window after a row scroll.
//        int newItemsToShowStart = BundleTooltipContext.getItemsToShowStart(bundleContents.size());
//        int newItemsToShowEnd = BundleTooltipContext.getItemsToShowEnd(bundleContents.size(), bundleContents.getNumberOfItemsToShow());
//        LOGGER.info("numberOfItemsToShow: {}->{} | itemsToShowStart: {}->{} | itemsToShowEnd: {}->{} | selected: {}->{} | " +
//                    "maxRowOffset: {} | aAL: {}, aVL: {}, aAF: {}, aVF: {}",
//                    numberOfItemsToShow, BundleItem.getNumberOfItemsToShow(bundleItemStack),
//                    currentItemsToShowStart, newItemsToShowStart, currentItemsToShowEnd, newItemsToShowEnd, currentlySelected, newSelected,
//                    maxRowOffset, atActualLast ? "1" : "0", atVisibleLast ? "1" : "0", atActualFirst ? "1" : "0", atVisibleFirst ? "1" : "0");
    }

    // Resets rowOffset to 0 when you stop hovering so that the initial window for items is the topmost one
    @Inject(
        method = "onStopHovering(Lnet/minecraft/world/inventory/Slot;)V",
        at = @At(value = "HEAD")
    )
    public void onStopHovering$resetRowOffset(Slot slot, CallbackInfo cir) {
        BundleTooltipContext.rowOffset = 0;
    }

    // Resets rowOffset to 0 when you shift-click or move with keybinds so that the initial window for items is the topmost one
    @Inject(
        method = "onSlotClicked(Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/inventory/ClickType;)V",
        at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/gui/BundleMouseActions;unselectedBundleItem(Lnet/minecraft/world/item/ItemStack;I)V"
        )
    )
    public void onSlotClicked$resetRowOffset(Slot slot, ClickType clickType, CallbackInfo ci) {
        BundleTooltipContext.rowOffset = 0;
    }

    @Shadow @Final
    private void toggleSelectedBundleItem(ItemStack bundleItemStack, int i, int j) {}

    // Now has the index j be bound by the total amount of slots, not just the visible ones, to allow for the scrolling ability
    @Redirect(
            method = "toggleSelectedBundleItem(Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/BundleItem;getNumberOfItemsToShow(Lnet/minecraft/world/item/ItemStack;)I"
            )
    )
    private int allowScrollsPastInitialWindow(ItemStack itemStack) {
        BundleContents bundleContents = itemStack.get(DataComponents.BUNDLE_CONTENTS);
        return bundleContents.size();
    }

}