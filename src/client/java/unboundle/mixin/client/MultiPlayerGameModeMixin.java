package unboundle.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import unboundle.BundleRenderContext;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Redirect(
            method = "useItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startPrediction(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/multiplayer/prediction/PredictiveAction;)V"
            )
    ) // This method fires on placement onto a block
    public void swapBundleToItemInHand(MultiPlayerGameMode instance, ClientLevel clientLevel, PredictiveAction predictiveAction, // Parameters of startPrediction
                          LocalPlayer localPlayer, InteractionHand interactionHand, BlockHitResult blockHitResult) { // Parameters of useItemOn
        // Checking if the held item is a bundle, which contains placeable items in the first slot
        ItemStack heldStack = localPlayer.getItemInHand(interactionHand);
        if (heldStack.getItem() instanceof BundleItem) {
            BundleContents contents = heldStack.get(DataComponents.BUNDLE_CONTENTS);
            if (contents == null || contents.isEmpty()) return;
            ItemStack selectedItem = contents.getItemUnsafe(0);
            if (selectedItem.isEmpty() || !BundleRenderContext.useAllowed(selectedItem)) return;
            // If so, put the item inside the bundle in the player's hand briefly
            localPlayer.setItemInHand(interactionHand, selectedItem.copy());
        }

        // Either place the item inside the bundle, or do the normal vanilla stuff
        ((MultiPlayerGameModeMixinAccessor) instance).invokeStartPrediction(clientLevel, predictiveAction);

        // And reset the player's hand back to the bundle (if it was one) right after placement, otherwise just reset to itself
        localPlayer.setItemInHand(interactionHand, heldStack);
    }

}