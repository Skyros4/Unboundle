package unboundle.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.ItemPickerMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import unboundle.BundleTooltipContext;
import unboundle.UnboundleConfig;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends AbstractContainerScreen<ItemPickerMenu> {

    public CreativeModeInventoryScreenMixin(ItemPickerMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    // Injecting BEFORE the game is able to create a copy, so that the original and the copied bundle are both properly reset.
    // Covers the last 5% of bundle item interaction cases, as copying items in Creative completely bypasses the AbstractContainerMenu logic.
    @Inject(
            method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/EditBox;moveCursorToEnd(Z)V"
            )
    )
    private void resetScrollState(Slot slot, int i, int j, ClickType clickType, CallbackInfo ci) {
        // If doing the middle click on a bundle item
        if (clickType == ClickType.CLONE && slot.getItem().getItem() instanceof BundleItem) {
            // Resets the row offset
            BundleTooltipContext.rowOffset = 0;
            // Get an editable version of the bundle stack
            ItemStack stack = slot.getItem();
            BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
            BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
            // Reset the selected item
            mutable.toggleSelectedItem(-1);
            // Write the changes into the current bundle
            stack.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
        }
    }

    // Prevent the vanilla safeguard of resetting selectedItem on QUICK_MOVE from firing if trying to insert as a separate item.
    // Applies to the inventory screen in Creative Mode.
    @WrapWithCondition(
            method = "slotClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen;onMouseClickAction(Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/inventory/ClickType;)V"
            )
    )
    private boolean disableSelectedItemResetOnSeparateInsertion(CreativeModeInventoryScreen instance, Slot slot, ClickType clickType,
                                        @Local(ordinal = 0, argsOnly = true) int i,
                                        @Local(ordinal = 1, argsOnly = true) int j) {
        if (slot == null) return true;
        ItemStack slotItem = slot.getItem();
        ItemStack carried = this.menu.getCarried();
        return !(clickType == ClickType.QUICK_MOVE &&
                i >= 0 &&
                (UnboundleConfig.config().clickBehaviour == UnboundleConfig.ClickBehaviour.PRIMARY_BUNDLE ? j == 1 : j == 0) &&
                (slotItem.getItem() instanceof BundleItem && !carried.isEmpty()) ||
                (!slotItem.isEmpty() && carried.getItem() instanceof BundleItem));
    }

    // When the bundle is on the cursor, and the item inside is attempted to be dropped onto the X icon in the creative mode inventory,
    // the first item inside the bundle is deleted, rather than the entire bundle.
    @Inject(
            method = "slotClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$ItemPickerMenu;setCarried(Lnet/minecraft/world/item/ItemStack;)V"
            ),
            cancellable = true
    )
    private void deleteFirstBundleItemOnTrash(Slot slot, int i, int j, ClickType clickType, CallbackInfo ci) {
        // Fall through to vanilla, which is deleting the entire item, if the item is not a bundle, or the bundle itself is attempted to be placed on the X icon.
        // If the bundle is empty and its contents are attempted to be placed on the X icon, do nothing, just like with other slots.
        // That way, you can quickly empty bundles without also deleting the bundle itself, mirroring drop behaviour.
        ItemStack stack = this.menu.getCarried();
        if (!(stack.getItem() instanceof BundleItem)) return;
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (UnboundleConfig.config().clickBehaviour == UnboundleConfig.ClickBehaviour.PRIMARY_CONTENTS ? j == 1 : j == 0) return;
        if (contents == null) return;
        if (contents.isEmpty()) {
            ci.cancel();
            return;
        }

        // Remove the first stack inside the bundle
        BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
        mutable.toggleSelectedItem(0);
        mutable.removeOne();
        stack.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());

        // Prevents vanilla from deleting the bundle itself right afterward.
        ci.cancel();
    }
}