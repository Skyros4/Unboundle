package unboundle.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import unboundle.BundleUIContext;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.*;
import unboundle.UnboundleConfig;

@Mixin(BundleContents.class)
public class BundleContentsMixin {

    /**
     * @author Skyros4
     * @reason Show however many items are in the bundle, until maxSlots().
     *         Once the bundle starts overflowing, show either maxSlots() - 1 or maxSlots() - 2 items, depending on counters
     */
    @WrapMethod(method = "getNumberOfItemsToShow()I")
    public int getNumberOfItemsToShow(Operation<Integer> original) {
        int totalSize = this.size();
        int windowEnd = BundleUIContext.rowOffset * UnboundleConfig.config().columns + UnboundleConfig.config().maxSlots();
        boolean hasAbove = BundleUIContext.rowOffset > 0;
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