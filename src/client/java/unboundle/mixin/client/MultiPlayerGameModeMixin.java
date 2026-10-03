package unboundle.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import unboundle.BundleDropPredictionContext;

// Unlike BundleItem, LocalPlayer and ServerPlayer are separated. LocalPlayer predicts the item disappearing from the inventory.
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    //? if >= 26.3 {
    /*@Shadow @Final
    private Minecraft minecraft;

    @Redirect(
            method = "dropItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Inventory;removeFromSelected(Z)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private ItemStack predictDropContents(Inventory inventory, boolean fullStack) {
        return BundleDropPredictionContext.predict(this.minecraft.player, inventory, fullStack);
    }
    *///?}
}

