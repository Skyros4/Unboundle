package unboundle.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import unboundle.BundleUIContext;
import unboundle.Unboundle;
import unboundle.UnboundleConfig;

import java.util.List;

@Mixin(BundleContents.Mutable.class)
public class BundleContentsMutableMixin {

    @Mutable
    @Shadow @Final
    private List<ItemStack> items;
    @Shadow
    private int selectedItem;

    // Across this class there will be commented out Logger statements, for easier debugging.
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(Unboundle.MOD_ID);

    // If the item on the cursor is already present in the bundle,
    // holding Shift no longer adds the new item to the existing stack, resulting in it being added as a new stack.
    @Definition(id = "j", local = @Local(type = int.class, ordinal = 1))
    @Expression("j != -1")
    @ModifyExpressionValue(
            method = "tryInsert(Lnet/minecraft/world/item/ItemStack;)I",
            at = @At("MIXINEXTRAS:EXPRESSION")
    )
    private boolean disableOriginalBehaviourForSeparateInsertion(boolean original) {
        return original && !BundleUIContext.shiftClick;
    }

    // When adding an item to the bundle, and there's already an item of the same type already present in there,
    // preserve the position of the latter instead of moving that item to the front.
    @ModifyArg(
            method = "tryInsert(Lnet/minecraft/world/item/ItemStack;)I",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(ILjava/lang/Object;)V",
                    ordinal = 0
            ),
            index = 0
    )
    private int tryInsert$undoResetPositionForExistingItem(int zero, @Local(ordinal = 1) int indexOfSameItem // == j in deobfuscated code
    ) {
        return indexOfSameItem;
    }
    // When adding an item to the bundle, and there's already an item of the same type already present in there,
    // automatically select the slot of the latter.
    // Also make sure that that slot becomes visible in the current window.
    @Inject(
            method = "tryInsert(Lnet/minecraft/world/item/ItemStack;)I",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(ILjava/lang/Object;)V",
                    ordinal = 0, // First instance of this.items.add
                    shift = At.Shift.AFTER // after the method
            )
    )
    public void tryInsert$goToExistingItem(ItemStack itemStack, CallbackInfoReturnable<Integer> cir,
        @Local(ordinal = 1) int indexOfSameItem  // == j in deobfuscated code
    ) {
        if (this.selectedItem != -1) {
            // If the item stack the new item is supposed to be added to is...

            // ... after the current window, update the window so that the earliest window is shown where the item is visible,
            // making it look like it was automatically scrolled down to
            if (BundleUIContext.getItemsToShowEnd(this.items.size(), this.toImmutable().getNumberOfItemsToShow()) < indexOfSameItem) {
                BundleUIContext.rowOffset = BundleUIContext.getEarliestRowOffsetFromIndex(items.size(), indexOfSameItem);
            }
            // ... before the current window, update the window so that the latest window is shown where the item is visible,
            // making it look like it was automatically scrolled up to
            else if (BundleUIContext.getItemsToShowStart(this.items.size()) > indexOfSameItem) {
                BundleUIContext.rowOffset = BundleUIContext.getLatestRowOffsetFromIndex(items.size(), indexOfSameItem);
            }
            // ... otherwise just stay in the current window if the targeted slot is visible already

            this.toggleSelectedItem(indexOfSameItem);
        }
    }
    // When adding a new item to the bundle, add it right where the selectedItem cursor pointed to, not at the beginning.
    @ModifyArg(
            method = "tryInsert(Lnet/minecraft/world/item/ItemStack;)I",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(ILjava/lang/Object;)V",
                    ordinal = 1
            ),
            index = 0
    )
    private int tryInsert$undoResetPositionForNewItem(int zero) {
        return Math.min(selectedItem + 1, this.items.size());
    }
    // When adding a new item to the bundle, automatically select the slot of the item just added.
    // Also, if inserting causes a new topmost row with just 1 item in it to appear, shift the rowOffset accordingly.
    @Inject(
            method = "tryInsert(Lnet/minecraft/world/item/ItemStack;)I",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(ILjava/lang/Object;)V",
                    ordinal = 1, // Second instance of this.items.add
                    shift = At.Shift.AFTER // after the method
            )
    )
    public void tryInsert$goToNewItem(ItemStack itemStack, CallbackInfoReturnable<Integer> cir) {
        int itemInsertedIndex = Math.min(selectedItem + 1, this.items.size());
        // Usually when inserting a new item, the rowOffset is not changed because you are already on the correct window as you insert the item.
        // However, here this is done because you can change the amount of rowOffsets by creating a new top row with just 1 item in it, in which case we just increase the rowOffset by 1.
        if(this.items.size() % UnboundleConfig.config().columns == 1 && selectedItem > 0) {
            BundleUIContext.rowOffset = Math.min(BundleUIContext.rowOffset + 1, BundleUIContext.getMaxRowOffset(this.items.size()));
        }
        if (this.selectedItem != -1) {
            this.toggleSelectedItem(itemInsertedIndex);
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
            BundleUIContext.rowOffset = Math.max(BundleUIContext.rowOffset - 1, 0);
        }
    }

}