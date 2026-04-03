package unboundle.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Unlike BundleItem, LocalPlayer and ServerPlayer are separated. Hence, the same drop logic is in both classes.
@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Shadow
    public ItemEntity drop(ItemStack itemStack, boolean bl, boolean bl2) {
        return null;
    }

    @Inject(
            method = "drop(Z)Z",
            at = @At("HEAD"),
            cancellable = true
    ) // Injecting at HEAD bypasses the server doing setRemoteSlot, as we handle that ourselves for the bundle
    private void validateDropContents(boolean fullStack, CallbackInfoReturnable<Boolean> cir) {
        // if not a bundle OR if a bundle and CTRL & drop was pressed, execute vanilla method. if empty, prevent drop.
        // The latter is done so that players can safely rid the bundle of its contents by holding down the drop key, without also dropping the bundle itself.
        ServerPlayer serverPlayer = (ServerPlayer) (Object) this;
        ItemStack itemStack = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(itemStack.getItem() instanceof BundleItem) || fullStack) return;
        BundleContents contents = itemStack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        // Remove the item to be dropped from the bundle, and update the player's hand holding the bundle.
        BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
        mutable.toggleSelectedItem(0);
        ItemStack selectedItem = mutable.removeOne();
        itemStack.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
        serverPlayer.setItemInHand(InteractionHand.MAIN_HAND, itemStack);

        cir.setReturnValue(this.drop(selectedItem, false, true) != null);
        cir.cancel();
    }
}