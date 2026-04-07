package unboundle.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import unboundle.BundleUIContext;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Shadow
    public abstract Slot getSlot(int index);

    // If trying to shift-click on a bundle with an item on the cursor, or shift-clicking on an item with a bundle on the cursor,
    // now use PICKUP to insert as a separate item, instead of executing QUICK_MOVE.
    @WrapMethod(method = "doClick")
    private void disableQuickMoveForSeparateInsertion(int i, int j, ClickType clickType, Player player, Operation<Void> original) {
        if (clickType == ClickType.QUICK_MOVE && i >= 0 && (BundleUIContext.config().clickBehaviourSeparate ? j == 1 : j == 0)) { // j indicates which type of click, here it is ClickAction.SECONDARY.
            ItemStack slotItem = this.getSlot(i).getItem();
            ItemStack carried = this.getCarried();
            if ((slotItem.getItem() instanceof BundleItem && !carried.isEmpty()) || (!slotItem.isEmpty() && carried.getItem() instanceof BundleItem)) {
                BundleUIContext.shiftClick = true;
                original.call(i, j, ClickType.PICKUP, player);
                BundleUIContext.shiftClick = false;
                return;
            }
        }
        original.call(i, j, clickType, player);
    }

    // This fires whenever an item enters the cursor, from a container slot, excluding the bundle GUI.
    // RETURN signals that the below code is injected right before returns, and in this void method here, at the end.
    @Inject(method = "setCarried", at = @At("RETURN"))
    private void unboundle$setCarried(ItemStack stack, CallbackInfo ci) {
        // On every instance a bundle item is picked up and stuck to the cursor, reset the rowOffset so that the item window is reset to the topmost one.
        // Covers 95% of bundle item interaction cases.
        if (!stack.isEmpty() && stack.getItem() instanceof BundleItem) {
            BundleUIContext.rowOffset = 0;
        }
    }

    @Shadow
    public abstract ItemStack getCarried();

}