package unboundle.mixin.client;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import unboundle.BundleRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.gui.BundleMouseActions;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
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
    private Minecraft minecraft;
    @Shadow @Final
    private ScrollWheelHandler scrollWheelHandler;
    @Unique // Across this class there will be commented out Logger statements, for easier debugging.
    private static final Logger LOGGER = LoggerFactory.getLogger(Unboundle.MOD_ID);

    /**
     * @author Skyros4
     * @reason Now also updates BundleRenderContext.rowOffset on a scroll, which controls the visual display of a row scrolled
     */
    @Overwrite
    public boolean onMouseScrolled(double d, double e, int i, ItemStack bundleItemStack) {
        // the amount of items to be displayed in te current window
        int numberOfItemsToShow = BundleItem.getNumberOfItemsToShow(bundleItemStack);
        // Scrolling doesn't make sense if there's nothing to scroll over
        if (numberOfItemsToShow == 0) return false;

        Vector2i vector2i = this.scrollWheelHandler.onMouseScroll(d, e);
        // Determines the direction of the scroll, considering mouses that scroll horizontally if no vertical scroll is detected.
        // Also caps the scrolling speed. Math.signum detects the sign of vector2i, and returns either 1.0, 0.0 or -1.0 respectively.
        // That way scrolling past the currently shown window is not possible.
        // The x direction is reversed so that up and right are 1, and down and left are -1.
        int scrollDirection = vector2i.y != 0 ? (int) Math.signum(vector2i.y) : (int) Math.signum(-vector2i.x);
        if (scrollDirection == 0) return false; // Just in case

        // get the current state of the bundle UI and the goal index of the selection
        BundleContents bundleContents = bundleItemStack.get(DataComponents.BUNDLE_CONTENTS);
        int totalItems = bundleContents.size();
        // The total amount of rows we can scroll down by at most.
        int maxRowOffset = BundleRenderContext.getMaxRowOffset(totalItems);
        // These indexes are relative to entire list, not the current window
        int currentItemsToShowStart = BundleRenderContext.getItemsToShowStart(bundleContents);
        int currentItemsToShowEnd = BundleRenderContext.getItemsToShowEnd(bundleContents, bundleContents.getNumberOfItemsToShow());
        int currentlySelected = BundleItem.getSelectedItem(bundleItemStack);
        // scrollDirection is negated because renderBundleWithItemsTooltip within ClientBundleTooltip renders the items from bottom right to top left,
        // so forward would actually be up left, when we want it to be down right.
        int newSelected = ScrollWheelHandler.getNextScrollWheelSelection(-scrollDirection, currentlySelected, totalItems);

        // If you scroll once on a bundle with 1 item, you go from -1 to 0.
        // Once the one item and index 0 is selected, cancel the scroll command since there are no other items to scroll to.
        if (currentlySelected == newSelected) return false;

        // The four special cases where either a row scroll or a wrap around occurs.
        // We're at the very last slot, and scroll forwards
        boolean atActualLast = currentlySelected == totalItems - 1 && scrollDirection > 0;
        // We're at the last slot of the current window, excluding the actual last slot, and scroll forwards
        boolean atVisibleLast = !atActualLast && currentlySelected == currentItemsToShowEnd && scrollDirection > 0;
        // We're at the very first slot, and scroll backwards
        boolean atActualFirst = currentlySelected <= 0 && scrollDirection < 0;
        // We're at the first slot of the current window, excluding the actual first slot, and scroll backwards
        boolean atVisibleFirst = !atActualFirst && newSelected < currentItemsToShowStart && scrollDirection < 0;
        if (atActualLast) {
            // This flips to the topmost window for the items
            BundleRenderContext.rowOffset = 0;
            this.toggleSelectedBundleItem(bundleItemStack, i, 0);
        }
        else if (atVisibleLast) {
            // Proceed down one row
            BundleRenderContext.rowOffset = BundleRenderContext.rowOffset + 1;
            this.toggleSelectedBundleItem(bundleItemStack, i, currentItemsToShowEnd + 1);
        }
        else if (atActualFirst) {
            // This flips to the bottommost window for the items
            BundleRenderContext.rowOffset = maxRowOffset;
            this.toggleSelectedBundleItem(bundleItemStack, i, totalItems - 1);
        }
        else if (atVisibleFirst) {
            // Proceed up one row
            BundleRenderContext.rowOffset = BundleRenderContext.rowOffset - 1;
            this.toggleSelectedBundleItem(bundleItemStack, i, currentItemsToShowStart - 1);
        }
        else {
            // In all other cases, just change whatever was the selected item, without the slots moving.
            this.toggleSelectedBundleItem(bundleItemStack, i, newSelected);
        }

//        System.out.println(BundleRenderContext.getRowOffsetFromIndex(totalItems, newSelected));

        // The only difference to currentItemsToShow is that these are computed after the rowOffset has been updated.
        // So these represent the new indexes for the window after a row scroll.
//        int newItemsToShowStart = BundleRenderContext.getItemsToShowStart(bundleContents);
//        int newItemsToShowEnd = BundleRenderContext.getItemsToShowEnd(bundleContents, bundleContents.getNumberOfItemsToShow());
//        LOGGER.info("numberOfItemsToShow: {}->{} | itemsToShowStart: {}->{} | itemsToShowEnd: {}->{} | selected: {}->{} | " +
//                    "maxRowOffset: {} | aAL: {}, aVL: {}, aAF: {}, aVF: {}",
//                    numberOfItemsToShow, BundleItem.getNumberOfItemsToShow(bundleItemStack),
//                    currentItemsToShowStart, newItemsToShowStart, currentItemsToShowEnd, newItemsToShowEnd, currentlySelected, newSelected,
//                    maxRowOffset, atActualLast ? "1" : "0", atVisibleLast ? "1" : "0", atActualFirst ? "1" : "0", atVisibleFirst ? "1" : "0");

        // The scroll must have succeeded if we get to this
        return true;
    }



    /**
     * @author Skyros4
     * @reason Now also resets BundleRenderContext.rowOffset.
     */
    @Overwrite
    public void onStopHovering(Slot slot) {
        // Resets rowOffset to 0 when you stop hovering so that the topmost window for items is the inital one
        BundleRenderContext.rowOffset = 0;
        this.unselectedBundleItem(slot.getItem(), slot.index);
    }

    /**
     * @author Skyros4
     * @reason Now also resets BundleRenderContext.rowOffset.
     */
    @Overwrite
    public void onSlotClicked(Slot slot, ClickType clickType) {
//        if (clickType == ClickType.PICKUP) {
////            boolean bundleIsInSlot = slot.getItem().has(DataComponents.BUNDLE_CONTENTS);
//            boolean bundleCarried = this.minecraft.player.containerMenu.getCarried().has(DataComponents.BUNDLE_CONTENTS);
//            if (bundleCarried) {
////                BundleRenderContext.rowOffset = 0;
//                System.out.println("PICKUP");
//            }
//        } else
        // Shift-clicking or using the 9 number keys or ...
        // ... taking out an item or ...
//                || (clickType == ClickType.PICKUP && this.minecraft.player.containerMenu.getCarried().isEmpty())
        if (clickType == ClickType.QUICK_MOVE || clickType == ClickType.SWAP) {
            BundleRenderContext.rowOffset = 0;
            this.unselectedBundleItem(slot.getItem(), slot.index);
        }
        // ... putting in an item resets the window back to its initial starting position and resets selection.
//        else if((clickType == ClickType.PICKUP && !this.minecraft.player.containerMenu.getCarried().isEmpty())){
//            System.out.println("HERE");
//            BundleRenderContext.insertedWithBundleHeld = this.minecraft.player.containerMenu.getCarried().has(DataComponents.BUNDLE_CONTENTS);
//            this.toggleSelectedBundleItem(slot.getItem(), slot.index, BundleRenderContext.itemInsertedIndex);
//            BundleRenderContext.rowOffset = 0;
//        }
    }

    /**
     * @author Skyros4
     * @reason Now restricts the index j to the total amount of slots in the bundle due to the scrolling ability
     */
    @Overwrite
    private void toggleSelectedBundleItem(ItemStack bundleItemStack, int i, int j) {
        BundleContents bundleContents = bundleItemStack.get(DataComponents.BUNDLE_CONTENTS);
        if (this.minecraft.getConnection() != null && j < bundleContents.size()) {
            ClientPacketListener clientPacketListener = this.minecraft.getConnection();
            BundleItem.toggleSelectedItem(bundleItemStack, j);
            clientPacketListener.send(new ServerboundSelectBundleItemPacket(i, j));
        }
    }

    @Shadow @Final
    public void unselectedBundleItem(ItemStack itemStack, int i) {}

}