package unboundle;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Bees;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.apache.commons.lang3.math.Fraction;

import java.util.List;

public class BundleRenderContext {
    // Represents how many rows are currently being scrolled down from the initial item grid window.
    public static int rowOffset = 0;
    // Used for the shadows of 16-stackables and unstackables, to determine whether they should render darker or brighter
    public static boolean highContrast = false;

    public static BundleConfig config() {
        return AutoConfig.getConfigHolder(BundleConfig.class).getConfig();
    }

    // These getter methods allow the start and end index of the currently visible window to be always calculated on the fly, preventing stale values.

    // Computes the earliest row offset the index can be on, resulting in the item itself showing on the bottom row of the current window.
    public static int getRowOffsetFromIndex(int itemsSize, int index) {
        if(itemsSize <= config().maxSlots()) return 0;


        int lastIndexOfInitialWindow = ((itemsSize - 1) % config().columns) + (config().maxSlots() - config().columns) - 1;

//        System.out.println(lastIndexOfInitialWindow);
        if (index <= lastIndexOfInitialWindow) return 0;

//        System.out.println(Mth.positiveCeilDiv(Math.min(index, itemsSize - 2) - lastIndexOfInitialWindow, BundleConfig.COLUMNS));

        if (index >= itemsSize - 1) return Mth.positiveCeilDiv(Math.min(index, itemsSize - 2) - lastIndexOfInitialWindow, config().columns);

        // rowOffset 1 maps to base, rowOffset 2 maps to base + COLUMNS, etc.
        return (Mth.positiveCeilDiv(index - lastIndexOfInitialWindow, config().columns));
    }

    public static int getMaxRowOffset(int totalItems) {
        return Mth.positiveCeilDiv(Math.max(0, totalItems - config().maxSlots()), config().columns);
    }

    public static int getItemsToShowStart(BundleContents bundleContents) {
        // Always 0 if no scroll has been performed yet
        if (rowOffset <= 0 ) {
            return 0;
        }
        // Formula to determine the index offset on subsequent rowOffsets aka rows scrolled
        else {
            /*
            (bundleContents.size() - 1)
                shifts input to end on a multiple of COLUMNS, instead of start.
                Example: from 16-19 to 17-20
            (( [...] % BundleConfig.COLUMNS) + 2)
                shifts output by 2, one to pass from the very top row to the next row, and one to account for the top left counter.
                Example: from 0-3 to 2-5
            [...] + Math.max(0, rowOffset - 1) * BundleConfig.COLUMNS)
                starts adding multiples of COLUMNS to this initial output,
                starting on rowOffset == 2 aka on the second row scrolled down,
                The initial offset above already accounts for the first row scrolled down.
                Example: from 2-5 to 6-9 with rowOffset == 2, and 10-13 with rowOffset == 3
            */
            return (((bundleContents.size() - 1) % config().columns) + 2) + Math.max(0, rowOffset - 1) * config().columns;
            /*
            What itemsToShowStart can look like, with an example with 4 Rows and 4 Columns:
            Items | Modulo | rowOffset | Result    |
            1-16  | 2-5    | 0         | 0 |       |
            17    | 2      | 0 | 1     | 0 | 2     |
            18    | 3      | 0 | 1     | 0 | 3     |
            19    | 4      | 0 | 1     | 0 | 4     |
            20    | 5      | 0 | 1     | 0 | 5     |
            21    | 2      | 0 | 1 | 2 | 0 | 2 | 6 |
            22    | 3      | 0 | 1 | 2 | 0 | 3 | 7 |
            23    | 4      | 0 | 1 | 2 | 0 | 4 | 8 |
            24    | 5      | 0 | 1 | 2 | 0 | 5 | 9 |
            ...   | ...    | ...       | ...       |
            */
        }
    }

    public static int getItemsToShowEnd(BundleContents bundleContents, int numberOfItemsToShow){
        // Simply takes itemsToShowStart and the offset of the items shown, but reduced by one since the list where Start and End are used in is 0-indexed.
        // That way both values are included in the list.
        return getItemsToShowStart(bundleContents) + numberOfItemsToShow - 1;
    }

    // Manual BundleContents' static getWeight replication since overwritten static method calls across mixin classes don't work. So putting it here instead.
    // assign list of items IF the item within the slot we're looking at contains more items (which only happens if it's a bundle). Otherwise, null.
    public static Fraction getWeight(ItemStack itemStack) {
        BundleContents bundleContents = itemStack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundleContents != null) {
            return Fraction.getFraction(1, 16).add(bundleContents.weight());
        } else {
            List<BeehiveBlockEntity.Occupant> list = itemStack.getOrDefault(DataComponents.BEES, Bees.EMPTY).bees();
            return !list.isEmpty() ? Fraction.ONE : Fraction.getFraction(1, itemStack.getMaxStackSize());
        }
    }
}