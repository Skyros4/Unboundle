package unboundle.mixin.client;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import unboundle.BundleRenderContext;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    // This fires whenever an item enters the cursor, from a container slot, excluding the bundle GUI.
    // RETURN signals that the below code is injected right before returns, and in this void method here, at the end.
    @Inject(method = "setCarried", at = @At("RETURN"))
    private void unboundle$setCarried(ItemStack stack, CallbackInfo ci) {
        // On every instance a bundle item is picked up and stuck to the cursor, reset the rowOffset so that the item window is reset to the topmost one.
        // Covers 95% of bundle item interaction cases.
        if (!stack.isEmpty() && stack.getItem() instanceof BundleItem) {
            BundleRenderContext.rowOffset = 0;
        }
    }
}