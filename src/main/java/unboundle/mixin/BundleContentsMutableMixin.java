package unboundle.mixin;

import unboundle.BundleConfig;
import unboundle.BundleRenderContext;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.*;
import unboundle.Unboundle;

import java.util.List;

@Mixin(BundleContents.Mutable.class)
public class BundleContentsMutableMixin {

    @Mutable
    @Shadow @Final
    private List<ItemStack> items;
    @Shadow
    private Fraction weight;
    @Shadow
    private int selectedItem;

    @Unique
    private boolean insertedWithBundleHeld = false;

//    @Unique
//    private BundleConfig config() {
//        return AutoConfig.getConfigHolder(BundleConfig.class).getConfig();
//    }

    // Across this class there will be commented out Logger statements, for easier debugging.
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(Unboundle.MOD_ID);

    @Shadow
    private int findStackIndex(ItemStack itemStack) {
        return 0;
    }

    @Shadow
    private int getMaxAmountToAdd(ItemStack itemStack) {
        return 0;
    }

    /**
     * @author Skyros4
     * @reason ToDo
     */
    @Overwrite
    public int tryInsert(ItemStack itemStackToInsert) {

        if (!BundleContents.canItemBeInBundle(itemStackToInsert)) return 0;

        // The
        int countToBeAdded = Math.min(itemStackToInsert.getCount(), this.getMaxAmountToAdd(itemStackToInsert));
        if (countToBeAdded == 0) return 0;

        this.weight = this.weight.add(BundleRenderContext.getWeight(itemStackToInsert).multiplyBy(Fraction.getFraction(countToBeAdded, 1)));
        // This gets the index of the same item type in the bundle, if the item is present there already.
        int indexOfSameItem = this.findStackIndex(itemStackToInsert);
        if (indexOfSameItem != -1) {
            // Takes the items of the same type out of the bundle
            ItemStack itemStack2 = (ItemStack) this.items.remove(indexOfSameItem);
            // Create a new item of the same type that adds the two counts together
            ItemStack itemStack3 = itemStack2.copyWithCount(itemStack2.getCount() + countToBeAdded);
            // Removes from the mouse pointer how much of the item fits into the bundle, and the rest is left over at the mouse pointer.
            itemStackToInsert.shrink(countToBeAdded);
            // Adds the updated item stack back to right where it was
            this.items.add(indexOfSameItem, itemStack3);
            if (this.selectedItem != -1 && !insertedWithBundleHeld) {
//                System.out.println(BundleRenderContext.getRowOffsetFromIndex(items.size(), indexOfSameItem));
                BundleRenderContext.rowOffset = BundleRenderContext.getRowOffsetFromIndex(items.size(), indexOfSameItem);
                if (this.items.size() % BundleRenderContext.config().columns == 1) BundleRenderContext.rowOffset--;
                this.toggleSelectedItem(indexOfSameItem);
            }
        } else {
            int itemInsertedIndex = Math.min(selectedItem + 1, this.items.size());
            this.items.add(itemInsertedIndex, itemStackToInsert.split(countToBeAdded));
            if (this.selectedItem != -1 && !insertedWithBundleHeld) {
                this.toggleSelectedItem(itemInsertedIndex);
            }
        }

        if(this.items.size() % BundleRenderContext.config().columns == 1 && selectedItem > 1) {
//            BundleRenderContext.rowOffset = Math.min(BundleRenderContext.rowOffset, BundleRenderContext.getMaxRowOffset(this.items.size()));
            BundleRenderContext.rowOffset = Math.min(BundleRenderContext.rowOffset + 1, BundleRenderContext.getMaxRowOffset(this.items.size()));
//            System.out.println(BundleRenderContext.rowOffset + " " + BundleRenderContext.getMaxRowOffset(this.items.size()));
        }

//        LOGGER.info("tryInsert | selectedItem: {} | itemInsertedIndex: {} | getRowOffsetFromIndex: {}", selectedItem, Math.min(selectedItem + 1, this.items.size()),
//                BundleRenderContext.getRowOffsetFromIndex(items, indexOfSameItem));

        return countToBeAdded;
    }

    /**
     * @author Skyros4
     * @reason ToDo
     */
    @Overwrite
    public int tryTransfer(Slot slot, Player player) {
        ItemStack itemStack = slot.getItem();
        int i = this.getMaxAmountToAdd(itemStack);
        if (!BundleContents.canItemBeInBundle(itemStack)) return 0;
//        System.out.println("TRANSFER");
        insertedWithBundleHeld = true;
        int result = this.tryInsert(slot.safeTake(itemStack.getCount(), i, player));
        insertedWithBundleHeld = false;
        return result;
    }

    @Shadow
    public void toggleSelectedItem(int i) {

    }

    @Shadow
    private boolean indexIsOutsideAllowedBounds(int i) {
        return false;
    }

    /**
     * @author Skyros4
     * @reason ToDo
     */
    @Overwrite @Nullable
    public ItemStack removeOne() {
        if (this.items.isEmpty()) {
            return null;
        } else {
            int i = this.indexIsOutsideAllowedBounds(this.selectedItem) ? 0 : this.selectedItem;
            ItemStack itemStack = ((ItemStack)this.items.remove(i)).copy();
            this.weight = this.weight.subtract(BundleRenderContext.getWeight(itemStack).multiplyBy(Fraction.getFraction(itemStack.getCount(), 1)));
            if (this.selectedItem > 0) {
                this.toggleSelectedItem(this.selectedItem - 1);
            }

            if(this.items.size() % BundleRenderContext.config().columns == 0 && selectedItem != -1){
//                BundleRenderContext.rowOffset = Math.min(BundleRenderContext.rowOffset, BundleRenderContext.getMaxRowOffset(this.items.size()));
                BundleRenderContext.rowOffset = Math.max(BundleRenderContext.rowOffset - 1, 0);
//                BundleRenderContext.rowOffset = Math.min(BundleRenderContext.rowOffset - 1, BundleRenderContext.getMaxRowOffset(this.items.size()));
//                System.out.println(BundleRenderContext.rowOffset);
            }

            return itemStack;
        }
    }
}