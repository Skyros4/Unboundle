package unboundle.mixin.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Unlike BundleItem, LocalPlayer and ServerPlayer are separated. Hence, the same drop logic is in both classes.
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
    private ItemStack predictDropContents(Inventory inventory, boolean fullStack) {
        // if not a bundle OR if a bundle and CTRL & drop was pressed, execute vanilla method. if empty, prevent drop.
        // The latter is done so that players can safely rid the bundle of its contents by holding down the drop key, without also dropping the bundle itself.
        ItemStack heldStack = this.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(heldStack.getItem() instanceof BundleItem) || fullStack) return inventory.removeFromSelected(fullStack);
        BundleContents contents = heldStack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return ItemStack.EMPTY;

        // Remove the item to be dropped from the bundle, and update the player's hand holding the bundle.
        BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
        mutable.toggleSelectedItem(0);
        ItemStack selectedItem = mutable.removeOne();
        heldStack.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
        this.setItemInHand(InteractionHand.MAIN_HAND, heldStack);
        // The entire stack is dropped.
        return selectedItem;
    }
}