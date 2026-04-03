package unboundle.mixin.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin extends AbstractClientPlayer {

    public LocalPlayerMixin(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    @Redirect(
            method = "drop",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Inventory;removeFromSelected(Z)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private ItemStack preventBundleDropPrediction(Inventory inventory, boolean fullStack) {
        // Gets the item that is attempted to be thrown.
        ItemStack heldStack = this.getItemInHand(InteractionHand.MAIN_HAND);

        // If the item attempted to be dropped is not a bundle, or the bundle item is dropped with CTRL & drop, proceed the vanilla way (drop the item itself).
        if (!(heldStack.getItem() instanceof BundleItem) || fullStack) return inventory.removeFromSelected(fullStack);

        // Otherwise, when the bundle is attempted to be dropped with just the drop key,
        // cancel client-side and let the server handle the actual dropping of the item within the bundle.
        // The client-side does not do a prediction here.
        return ItemStack.EMPTY;
    }
}