package unboundle;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.jetbrains.annotations.NotNull;

public class BundleDispenseItemBehavior extends DefaultDispenseItemBehavior {
	@NotNull
	public ItemStack execute(BlockSource blockSource, ItemStack itemStack) {
		BundleContents contents = itemStack.get(DataComponents.BUNDLE_CONTENTS);
		if (contents == null || contents.isEmpty()) return super.execute(blockSource, itemStack);

		// Extract the first item in the bundle
		BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
		ItemStack extracted = contents.getItemUnsafe(0).copy();
		mutable.toggleSelectedItem(0);
		mutable.removeOne();

		// Use recursion to let that item handle dispensing.
		// Nested bundles have their contents dispensed.
		// Empty bundles or non-bundle items will hit super.execute() and fall through to DefaultDispenseItemBehavior, which is just dropping the item.
		ItemStack remainder = execute(blockSource, extracted);

		// The dispensing result (usually stack - 1) is inserted back into the bundle
		if (!remainder.isEmpty()) mutable.tryInsert(remainder);
		itemStack.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());

		return itemStack;
	}
}