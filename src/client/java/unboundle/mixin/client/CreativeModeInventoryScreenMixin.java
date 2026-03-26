package unboundle.mixin.client;

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
import unboundle.BundleRenderContext;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends AbstractContainerScreen<ItemPickerMenu> {

    public CreativeModeInventoryScreenMixin(ItemPickerMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    // Injecting BEFORE the game is able to create a copy, so that the original and the copied bundle are both properly reset.
    // Covers the last 5% of bundle item interaction cases, as copying items in Creative completely bypasses the AbstractContainerMenu logic.
    @Inject(
            method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V",
            at = @At("HEAD")
    )
    private void unboundle$slotClicked(Slot slot, int i, int j, ClickType clickType, CallbackInfo ci) {
        if (clickType == ClickType.CLONE && slot.getItem().getItem() instanceof BundleItem) {
            ItemStack stack = slot.getItem();
            // 1. Reset the UI state
            BundleRenderContext.rowOffset = 0;

            // 2. Get the current component
            BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);

            // 3. Use the Mutable builder (Option 2)
            // This creates a temporary editable version of the bundle contents
            BundleContents.Mutable mutable = new BundleContents.Mutable(contents);

            // Set the selected item index to -1 (none selected)
            // Note: The method name is usually .setSelected() or .toggleSelection()
            // depending on exact mappings, but -1 is the standard 'empty' index.
            mutable.toggleSelectedItem(-1);

            // 4. Set the updated component back to the stack
            // .toImmutable() converts the Mutable builder back into the BundleContents record
            stack.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
        }
    }
}