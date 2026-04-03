package unboundle.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
        // Gets the item the player is currently holding, from *before* the prediction.
        ItemStack heldStack = localPlayer.getItemInHand(interactionHand);

        // Do the vanilla client side prediction of item placement
        ((MultiPlayerGameModeMixinAccessor) instance).invokeStartPrediction(clientLevel, predictiveAction);

        // If the item was a bundle, reset *immediately* back to what it was before the prediction,
        // so that the item within the bundle doesn't flash on the hotbar for a brief second.
        if (heldStack.getItem() instanceof BundleItem) {
            localPlayer.setItemInHand(interactionHand, heldStack);
        }
    }

}