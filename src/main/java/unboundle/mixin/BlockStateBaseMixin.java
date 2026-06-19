package unboundle.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import unboundle.BundleUsageContext;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

    // useOn() in BundleItem handles general block interaction, with the item being the handler.
    // If that fails, the pipeline proceeds with this - handling block-specific interaction where the block is the handler.
    // Usually that applies to block entities.
    @WrapMethod(method = "useItemOn")
    private InteractionResult useItemOn$delegateToContents(
            ItemStack itemStack, Level level, Player player, InteractionHand interactionHand,
            BlockHitResult blockHitResult, Operation<InteractionResult> original) {
        // Items other than non-empty bundles proceed as normal.
        // If the vanilla bundle would have had an interaction (for example with shelves), perform that one instead.
        ItemStack bundleItem = player.getItemInHand(interactionHand).copy();
        if (!(bundleItem.getItem() instanceof BundleItem)) return original.call(itemStack, level, player, interactionHand, blockHitResult);
        BundleContents contents = bundleItem.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return original.call(itemStack, level, player, interactionHand, blockHitResult);
        InteractionResult vanillaResult = original.call(itemStack, level, player, interactionHand, blockHitResult);
        if (vanillaResult.consumesAction()) return vanillaResult;

        // Applies the general "take item out of bundle, use item, put item back in" pattern.
        return BundleUsageContext.applyAsSelectedItem(player, interactionHand,
                (selectedItem) -> original.call(selectedItem, level, player, interactionHand, blockHitResult),
                (stack) -> true // None of the problematic interactions (prolonged use) occur here
        );
    }
}