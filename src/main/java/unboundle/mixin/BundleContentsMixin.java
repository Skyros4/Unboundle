package unboundle.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.serialization.DataResult;
//? if >=26.1 {
/*import net.minecraft.world.item.ItemInstance;
*///?}
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import unboundle.BundleTooltipContext;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.*;
import unboundle.UnboundleConfig;

@Mixin(BundleContents.class)
public class BundleContentsMixin {

    //? if >= 26.1 {
    /*@Inject(
            method = "getWeight(Lnet/minecraft/world/item/ItemInstance;)Lcom/mojang/serialization/DataResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemInstance;getOrDefault(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"
            ),
            cancellable = true
    ) // Changes unstackables' weight to 16/64 for convenience
    private static void getWeight(ItemInstance item, CallbackInfoReturnable<DataResult<Fraction>> cir) {
        if (item.getMaxStackSize() == 1) {
            cir.setReturnValue(DataResult.success(Fraction.getFraction(1, 4)));
        }
    }
    *///?} else {
    @Inject(
            method = "getWeight(Lnet/minecraft/world/item/ItemStack;)Lorg/apache/commons/lang3/math/Fraction;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getOrDefault(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"
            ),
            cancellable = true
    ) // Changes unstackables' weight to 16/64 for convenience
    private static void getWeight(ItemStack itemStack, CallbackInfoReturnable<Fraction> cir) {
        if (itemStack.getMaxStackSize() == 1) {
            cir.setReturnValue(Fraction.getFraction(1, 4));
        }
    }
     //?}

//    Show however many items are in the bundle, until maxSlots().
//    Once the bundle starts overflowing, show either maxSlots() - 1 or maxSlots() - 2 items, depending on counters
    @WrapMethod(method = "getNumberOfItemsToShow()I")
    public int getNumberOfItemsToShow(Operation<Integer> original) {
        int totalSize = this.size();
        int windowEnd = BundleTooltipContext.rowOffset * UnboundleConfig.config().columns + UnboundleConfig.config().maxSlots();
        boolean hasAbove = BundleTooltipContext.rowOffset > 0;
        boolean hasBelow = windowEnd < totalSize;
        int counterSlots = (hasBelow ? 1 : 0) + (hasAbove ? 1 : 0);
        int amountOfItemsShown = UnboundleConfig.config().maxSlots() - counterSlots;
        int partialRowItems = totalSize % UnboundleConfig.config().columns;
        int emptySlotsInPartialRow = partialRowItems > 0 && !hasAbove ? UnboundleConfig.config().columns - partialRowItems : 0;
        return Math.min(totalSize, amountOfItemsShown - emptySlotsInPartialRow);
    }

    @Shadow
    public int size() {
        return 0;
    }

}