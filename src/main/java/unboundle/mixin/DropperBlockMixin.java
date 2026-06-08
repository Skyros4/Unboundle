package unboundle.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DropperBlock.class)
public class DropperBlockMixin {

    @WrapMethod(method = "dispenseFrom")
    private void dispenseFrom(ServerLevel serverLevel, BlockState blockState, BlockPos blockPos, Operation<Void> original) {
        // Only non-empty bundles receive special treatment, all other items fall through to vanilla.
        DispenserBlockEntity dispenserBlockEntity = serverLevel.getBlockEntity(blockPos, BlockEntityType.DROPPER).orElse(null);
        if (dispenserBlockEntity == null) {
            original.call(serverLevel, blockState, blockPos);
            return;
        }
        int slot = dispenserBlockEntity.getRandomSlot(serverLevel.random);
        if (slot < 0) {
            original.call(serverLevel, blockState, blockPos);
            return;
        }
        ItemStack stack = dispenserBlockEntity.getItem(slot);
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) {
            original.call(serverLevel, blockState, blockPos);
            return;
        }

        // Extract the first item in the bundle, put it in the slot,
        // let that item handle dispensing (which in all cases but nested bundles falls through to vanilla)
        // then restore the bundle.
        BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
        ItemStack extracted = contents.getItemUnsafe(0).copy();
        mutable.toggleSelectedItem(0);
        mutable.removeOne();

        dispenserBlockEntity.setItem(slot, extracted);
        dispenseFrom(serverLevel, blockState, blockPos, original);

        ItemStack remainder = dispenserBlockEntity.getItem(slot);
        if (!remainder.isEmpty()) mutable.tryInsert(remainder);
        stack.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
        dispenserBlockEntity.setItem(slot, stack);
    }
}