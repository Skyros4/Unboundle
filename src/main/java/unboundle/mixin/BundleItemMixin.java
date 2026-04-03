package unboundle.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import unboundle.BundleRenderContext;

// This class runs on both client and server side.
// The client predicts everything by running the code themselves, then the server does a validation runs and informs the client accordingly.
@Mixin(BundleItem.class)
public class BundleItemMixin extends Item{

    public BundleItemMixin(Properties properties) {
        super(properties);
    }

    // This method fires on use while looking at a block
    @Override @NotNull
    public InteractionResult useOn(UseOnContext useOnContext) {
        // Extract the first item out of the bundle and perform a validation check
        // copy() avoids potential stale references
        ItemStack bundleItem = useOnContext.getItemInHand().copy();
        BundleContents contents = bundleItem.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return InteractionResult.FAIL; // does not allow for the use() fallback
        ItemStack selectedItem = contents.getItemUnsafe(0).copy();

        // Creates a new context for the item to be used in, namely replacing the item to be used with the item within the bundle.
        UseOnContext selectedItemUseOnContext = new UseOnContext(
                useOnContext.getLevel(),
                useOnContext.getPlayer(),
                useOnContext.getHand(),
                selectedItem,
                new BlockHitResult(
                        useOnContext.getClickLocation(),
                        useOnContext.getClickedFace(),
                        useOnContext.getClickedPos(),
                        useOnContext.isInside()
                )
        );

        // Only items from the whitelist actually get used, the other items are rejected.
        InteractionResult result;
        if (BundleRenderContext.useAllowed(selectedItem)) {
            result = selectedItem.useOn(selectedItemUseOnContext);
            // If the item placement failed, return without cycling through the items in the bundle
            if (!result.consumesAction()) return result;
        } else {
            result = InteractionResult.PASS; // allows for the use() fallback
        }

        // Takes the item out of the bundle, decrease its count by 1 if in survival, and insert it back at the end if not empty.
        BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
        mutable.toggleSelectedItem(0);
        ItemStack toRemove = mutable.removeOne();
        if (!useOnContext.getPlayer().getAbilities().instabuild) toRemove.shrink(1);
        if (!toRemove.isEmpty()) {
            mutable.toggleSelectedItem(mutable.toImmutable().size() - 1);
            mutable.tryInsert(toRemove);
        }
        // Resets the UI after cycling
        mutable.toggleSelectedItem(-1);
        BundleRenderContext.rowOffset = 0;

        // The bundle appears to be playing the "pick up" animation precisely because the items have been successfully cycled through,
        // and a new bundle is written into the player's hand.
        bundleItem.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
        useOnContext.getPlayer().setItemInHand(useOnContext.getHand(), bundleItem);

        // Returns whether the item usage succeeded.
        return result;
    }

    @WrapMethod(method = "use") // This method fires on use while *not* looking at a block
    private InteractionResult preventVanillaBundleUse(Level level, Player player, InteractionHand interactionHand, Operation<InteractionResult> original) {
        // Prevent the vanilla dropping behaviour
        return InteractionResult.PASS;
    }
}