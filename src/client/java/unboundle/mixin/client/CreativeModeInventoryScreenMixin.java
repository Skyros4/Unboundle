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
}