package unboundle.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import unboundle.BundleContext;
import unboundle.UnboundleConfig;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen {

    protected AbstractContainerScreenMixin(Component component) {
        super(component);
    }

    @Shadow @Final
    protected T menu;

    // Prevent the vanilla safeguard of resetting selectedItem on QUICK_MOVE from firing if trying to insert as a separate item.
    // Applies to all screens except for CreativeModeInventoryScreen.
    @WrapWithCondition(
            method = "slotClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;onMouseClickAction(Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/inventory/ClickType;)V"
            )
    )
    private boolean disableSelectedItemResetOnSeparateInsertion(AbstractContainerScreen instance, Slot slot, ClickType clickType,
                                        @Local(ordinal = 0, argsOnly = true) int i,
                                        @Local(ordinal = 1, argsOnly = true) int j) {
        if (slot == null) return true;
        ItemStack slotItem = slot.getItem();
        ItemStack carried = this.menu.getCarried();
        return !(clickType == ClickType.QUICK_MOVE &&
                i >= 0 &&
                (BundleContext.config().clickBehaviour == UnboundleConfig.ClickBehaviour.PRIMARY_BUNDLE ? j == 1 : j == 0) &&
                (slotItem.getItem() instanceof BundleItem && !carried.isEmpty()) ||
                    (!slotItem.isEmpty() && carried.getItem() instanceof BundleItem));
    }

}