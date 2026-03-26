package unboundle.mixin;

import unboundle.BundleConfig;
import unboundle.BundleRenderContext;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Bees;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.*;

import java.util.List;

@Mixin(BundleContents.class)
abstract class BundleContentsMixin {

    @Shadow @Final
    List<ItemStack> items;
    @Shadow @Final
    private static final Fraction BUNDLE_IN_BUNDLE_WEIGHT = Fraction.getFraction(1, 16);

//    @Unique
//    private BundleConfig config() {
//        return AutoConfig.getConfigHolder(BundleConfig.class).getConfig();
//    }


    /**
     * @author Skyros4
     * @reason Show however many items are in the bundle, until 12. Once the bundle starts overflowing, show 11 items.
     */
    @Overwrite
    public int getNumberOfItemsToShow() {
        int totalSize = this.size();
        int windowEnd = BundleRenderContext.rowOffset * BundleRenderContext.config().columns + BundleRenderContext.config().maxSlots();
        boolean hasAbove = BundleRenderContext.rowOffset > 0;
        boolean hasBelow = windowEnd < totalSize;
        int counterSlots = (hasBelow ? 1 : 0) + (hasAbove ? 1 : 0);
        int amountOfItemsShown = BundleRenderContext.config().maxSlots() - counterSlots;
        int partialRowItems = totalSize % BundleRenderContext.config().columns;
        int emptySlotsInPartialRow = partialRowItems > 0 && !hasAbove ? BundleRenderContext.config().columns - partialRowItems : 0;
//        System.out.println(Math.min(totalSize, amountOfItemsShown - emptySlotsInPartialRow));
        return Math.min(totalSize, amountOfItemsShown - emptySlotsInPartialRow);
    }

    @Shadow
    public int size() {
        return 0;
    }

//    @Shadow
//    static Fraction getWeight(ItemStack itemStack) {
//        return null;
//    }
//
//    @Unique
//    public Fraction getWeightNonStatic(ItemStack itemStack) {
//        BundleContents bundleContents = itemStack.get(DataComponents.BUNDLE_CONTENTS);
//        if (bundleContents != null) {
//            return BUNDLE_IN_BUNDLE_WEIGHT.add(bundleContents.weight());
//        } else {
//            List<BeehiveBlockEntity.Occupant> list = itemStack.getOrDefault(DataComponents.BEES, Bees.EMPTY).bees();
//            return !list.isEmpty() ? Fraction.ONE : Fraction.getFraction(1, itemStack.getMaxStackSize());
//        }
//    }
}