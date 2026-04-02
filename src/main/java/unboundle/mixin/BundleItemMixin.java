package unboundle.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BundleItem.class)
public class BundleItemMixin {

    @Inject(
            method = "use",
            at = @At("HEAD"),
            cancellable = true
    ) // This method fires on use while not looking at a block
    private void preventVanillaBundleUse(Level level, Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        // If the item is a bundle, prevent the vanilla dropping behaviour
        ItemStack bundleStack = player.getItemInHand(interactionHand);
        if (bundleStack.getItem() instanceof BundleItem) {
            cir.setReturnValue(InteractionResult.PASS);
            cir.cancel();
        }
    }
}