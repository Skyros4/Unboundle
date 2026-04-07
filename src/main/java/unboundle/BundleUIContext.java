package unboundle;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Bees;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.apache.commons.lang3.math.Fraction;

import java.util.List;

public class BundleUIContext {
    // Represents how many rows are currently being scrolled down from the initial item grid window.
    public static int rowOffset = 0;
    // Used for the shadows of 16-stackables and unstackables, to determine whether they should render darker or brighter
    public static boolean highContrast = false;
    // Tracks whether shift has been held on insertion
    public static boolean shiftClick = false;

    // Allows access to the rows and columns variables
    public static BundleConfig config() {
        return AutoConfig.getConfigHolder(BundleConfig.class).getConfig();
    }

    // Computes the earliest row offset the index can be on, resulting in the item itself showing on the bottom row of the current window.
    public static int getEarliestRowOffsetFromIndex(int itemsSize, int index) {
        // If all slots fit onto the tooltip
        if (itemsSize <= config().maxSlots()) return 0;

        // Calculates the index of the last item on the window with rowOffset == 0.
        // This one is variable, for all other windows it's just + config().columns on top,
        // with an additional +1 for the very last item where the bottom right counter is usually.
        // int lastIndexOfInitialWindow = (Items in very first row) + (items in all other rows of the initial window)
        int lastIndexOfInitialWindow = ((itemsSize - 1) % config().columns) + (config().maxSlots() - config().columns) - 1;

        // If not all slots fit onto the tooltip, but we're on the initial window still
        if (index <= lastIndexOfInitialWindow) return 0;

        // If we're on the very last item (when there's the bottom right counter on the other windows)
        if (index >= itemsSize - 1) return getMaxRowOffset(itemsSize);

        // Starting from the end of the initial window, we increase by 1 every config().columns steps.
        return (Mth.positiveCeilDiv(index - lastIndexOfInitialWindow, config().columns));
    }

    // Computes the latest row offset the index can be on, resulting in the item itself showing on the top row of the current window.
    public static int getLatestRowOffsetFromIndex(int itemsSize, int index) {
        // If all slots fit onto the tooltip
        if (itemsSize <= config().maxSlots()) return 0;

        // If we're on the very last window. -1 is for the top left counter.
        if (index >= itemsSize - (config().maxSlots() - 1)) return getMaxRowOffset(itemsSize);

        // Calculates how many items there are on the first row, including the first item of the second row, where the top left counter would be usually
        int firstRowItems = ((itemsSize - 1) % config().columns) + 2;

        // If we are in that first row, we must be on the topmost window only
        if (index < firstRowItems) return 0;

        // Starting from the end of the first row, we increase by 1 every config().columns steps. (firstRowItems is 1-based)
        return Mth.positiveCeilDiv(index - (firstRowItems - 1), config().columns);
    }

    // Computes the most amount of row offsets that can occur in a given bundle
    public static int getMaxRowOffset(int totalItems) {
        return Mth.positiveCeilDiv(Math.max(0, totalItems - config().maxSlots()), config().columns);
    }

    // The following two getter methods calculate the start and end index of the currently visible window on the fly, preventing stale values.

    public static int getItemsToShowStart(int bundleContentsSize) {
        // Always 0 if no scroll has been performed yet
        if (rowOffset <= 0) {
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
            return (((bundleContentsSize - 1) % config().columns) + 2) + Math.max(0, rowOffset - 1) * config().columns;
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

    public static int getItemsToShowEnd(int bundleContentsSize, int numberOfItemsToShow) {
        // Simply takes itemsToShowStart and the offset of the items shown, but reduced by one since the list where Start and End are used in is 0-indexed.
        // That way both values are included in the list.
        return getItemsToShowStart(bundleContentsSize) + numberOfItemsToShow - 1;
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

    public static boolean useAllowed(ItemStack stack) {
        // The class of the current item
        Class<?> currentClass = stack.getItem().getClass();

        // Climb up the superclass tree until we arrive at Item itself, at which point we stop.
        while (currentClass != null && currentClass != Item.class) {
            String name = currentClass.getSimpleName();

            // Check what unboundle.json has to say about our class. If not listed, move on.
            if (config().usableOutOfBundle.containsKey(name)) {
                return config().usableOutOfBundle.get(name);
            }

            // Move up one step
            currentClass = currentClass.getSuperclass();
        }

        return false;
    }

}