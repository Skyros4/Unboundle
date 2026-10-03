package unboundle.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import unboundle.BundleTooltipContext;
import unboundle.Unboundle;
import unboundle.UnboundleConfig;
//? if >= 26.3 {
/*import net.minecraft.world.item.component.GrowableMutableContainer;
*///?}

import java.util.List;

@Mixin(BundleContents.Mutable.class)
//? if >= 26.3 {
/*public abstract class BundleContentsMutableMixin extends GrowableMutableContainer<BundleContents> {
    public BundleContentsMutableMixin(List<ItemStack> items) {
        super(items);
    }
*///?} else {
public class BundleContentsMutableMixin {
    @Mutable
    @Shadow @Final
    private List<ItemStack> items;
 //?}

    @Shadow
    private int selectedItem;

    // Across this class there will be commented out Logger statements, for easier debugging.
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(Unboundle.MOD_ID);

    // If the item on the cursor is already present in the bundle,
    // holding Shift no longer adds the new item to the existing stack, resulting in it being added as a new stack.
    @WrapOperation(
            method = "tryInsert(Lnet/minecraft/world/item/ItemStack;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/BundleContents$Mutable;findStackIndex(Lnet/minecraft/world/item/ItemStack;)I")
    )
    private int disableOriginalBehaviourForSeparateInsertion(BundleContents.Mutable instance, ItemStack itemStack, Operation<Integer> original) {
        if (BundleTooltipContext.shiftClick) return -1;
        return original.call(instance, itemStack);
    }

    // When adding an item to the bundle, and there's already an item of the same type already present in there,
    // preserve the position of the latter instead of moving that item to the front.
    // Also make the slot visible in the current window, and select it.
    @Redirect(
            method = "tryInsert(Lnet/minecraft/world/item/ItemStack;)I",
            at = @At(
                    value = "INVOKE",
                    //? if >= 26.3 {
                    /*target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V",
                    *///?} else {
                    target = "Ljava/util/List;add(ILjava/lang/Object;)V",
                    //?}
                    ordinal = 0
            )
    )
    private void tryInsert$preserveExistingItemPosition(
            List<ItemStack> items,
            //? if < 26.3 {
            int i,
             //?}
            Object item,
            @Local(ordinal = 1) int indexOfSameItem
    ) {
        // Instead of adding it to the start, add the merged items back to where they used to be.
        items.add(indexOfSameItem, (ItemStack) item);

        if (this.selectedItem != -1) {
            // If the item stack the new item is supposed to be added to is...

            // ... after the current window, update the window so that the earliest window is shown where the item is visible,
            // making it look like it was automatically scrolled down to
            if (BundleTooltipContext.getItemsToShowEnd(items.size(), this.toImmutable().getNumberOfItemsToShow()) < indexOfSameItem) {
                BundleTooltipContext.rowOffset = BundleTooltipContext.getEarliestRowOffsetFromIndex(items.size(), indexOfSameItem);
            }
            // ... before the current window, update the window so that the latest window is shown where the item is visible,
            // making it look like it was automatically scrolled up to
            else if (BundleTooltipContext.getItemsToShowStart(items.size()) > indexOfSameItem) {
                BundleTooltipContext.rowOffset = BundleTooltipContext.getLatestRowOffsetFromIndex(items.size(), indexOfSameItem);
            }
            // ... otherwise just stay in the current window if the targeted slot is visible already

            this.toggleSelectedItem(indexOfSameItem);
        }
    }

    // When adding a new item to the bundle, add it right where the selectedItem cursor pointed to, not at the beginning.
    // Also, automatically select the slot of the item just added.
    // If insertion causes a new topmost row with just 1 item in it to appear, shift the rowOffset accordingly.
    @Redirect(
            method = "tryInsert(Lnet/minecraft/world/item/ItemStack;)I",
            at = @At(
                    value = "INVOKE",
                    //? if >= 26.3 {
                    /*target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V",
                    *///?} else {
                    target = "Ljava/util/List;add(ILjava/lang/Object;)V",
                    //?}
                    ordinal = 1
            )
    )
    private void tryInsert$preserveNewItemPosition(
            List<ItemStack> items,
            //? if < 26.3 {
            int i,
            //?}
            Object item
    ) {
        // Instead of adding it to the start, add the new items to the right of where the cursor pointed to.
        int newItemIndex = Math.min(selectedItem + 1, items.size());
        items.add(newItemIndex, (ItemStack) item);

        // Usually when inserting a new item, the rowOffset is not changed because you are already on the correct window as you insert the item.
        // However, here this is done because you can change the amount of rowOffsets by creating a new top row with just 1 item in it, in which case we just increase the rowOffset by 1.
        if(items.size() % UnboundleConfig.config().columns == 1 && selectedItem > 0) {
            BundleTooltipContext.rowOffset = Math.min(BundleTooltipContext.rowOffset + 1, BundleTooltipContext.getMaxRowOffset(items.size()));
        }
        if (this.selectedItem != -1) {
            this.toggleSelectedItem(newItemIndex);
        }
    }

    @Shadow
    public void toggleSelectedItem(int i) {

    }

    @Shadow
    public BundleContents toImmutable() {
        return null;
    }

    // Leaves the selection cursor where it was after removing an item
    @Redirect(
            method = "removeOne()Lnet/minecraft/world/item/ItemStack;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/component/BundleContents$Mutable;toggleSelectedItem(I)V"
            )
    )
    public void removeOne$toggleSelectedItem(BundleContents.Mutable instance, int i) {
        if (this.selectedItem >= 0) {
            this.toggleSelectedItem(this.selectedItem - 1);
        }
    }
    // If removing causes a row to disappear, update the rowOffset accordingly
    @Inject(
            method = "removeOne()Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN")
    )
    private void removeOne$handleOneLessRow(CallbackInfoReturnable<Component> cir) {
        if(this.items.size() % UnboundleConfig.config().columns == 0 && selectedItem != -1){
            BundleTooltipContext.rowOffset = Math.max(BundleTooltipContext.rowOffset - 1, 0);
        }
    }

}