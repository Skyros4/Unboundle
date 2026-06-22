package unboundle.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import unboundle.UnboundleConfig;

import java.util.Random;

// Unlike BundleItem, LocalPlayer and ServerPlayer are separated. ServerPlayer executes the actual drop, including the item being removed from the inventory.
@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Shadow
    public ItemEntity drop(ItemStack itemStack, boolean bl, boolean bl2) {
        return null;
    }

    //? if >= 1.21.11 {
    /*@WrapMethod(method = "drop(Z)V")
    private void validateDropContents(boolean isStackDropped, Operation<Void> original) {
        // if not a bundle OR if a bundle and CTRL & drop was pressed, execute vanilla method. if empty, prevent drop.
        // The latter is done so that players can safely rid the bundle of its contents by holding down the drop key, without also dropping the bundle itself.
        ServerPlayer serverPlayer = (ServerPlayer) (Object) this;
        ItemStack heldStack = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(heldStack.getItem() instanceof BundleItem) || isStackDropped) { original.call(isStackDropped); return; }
        BundleContents contents = heldStack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return;
    *///?} else {
    @WrapMethod(method = "drop(Z)Z")
    private boolean validateDropContents(boolean isStackDropped, Operation<Boolean> original) {
        // if not a bundle OR if a bundle and CTRL & drop was pressed, execute vanilla method. if empty, prevent drop.
        // The latter is done so that players can safely rid the bundle of its contents by holding down the drop key, without also dropping the bundle itself.
        ServerPlayer serverPlayer = (ServerPlayer) (Object) this;
        ItemStack heldStack = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(heldStack.getItem() instanceof BundleItem) || isStackDropped) return original.call(isStackDropped);
        BundleContents contents = heldStack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return false;
    //?}
        BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
        // If itemUsageMode == RANDOM, use a field in the bundle's DataComponents to determine randomness, then use that random value to toggle the selected item.
        // Done with DataComponents so that client and server can individually generate their own random value,
        // which is the same for both because they pull the seed from one shared location. Then they both generate a new seed, equal on both sides.
        if(UnboundleConfig.config().itemUsageMode == UnboundleConfig.ItemUsageMode.RANDOM) {
            // Read
            //? if >= 1.21.5 {
            long randomHash = heldStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLong("randomHash").orElse(0L);
             //?} else {
            /*long randomHash = heldStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLong("randomHash");
            *///?}
            int randomIndex = new Random(randomHash).nextInt(contents.size());
            // Write
            CompoundTag tag = heldStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.putLong("randomHash", new Random(randomHash).nextLong());
            heldStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            mutable.toggleSelectedItem(randomIndex);
        }
        // Otherwise, if not random, just drop the first item in the bundle
        else{
            mutable.toggleSelectedItem(0);
        }
        // Remove the item to be dropped from the bundle, and update the player's hand holding the bundle.
        // This updating includes the random value saved in the DataComponents. So after every placement, the server syncs the value back to the client.
        ItemStack selectedItem = mutable.removeOne();
        heldStack.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
        serverPlayer.setItemInHand(InteractionHand.MAIN_HAND, heldStack);
        //? if >= 1.21.11 {
        /*this.drop(selectedItem, false, true);
        *///?} else {
        return this.drop(selectedItem, false, true) != null;
        //?}
    }
}