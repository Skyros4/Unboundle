package unboundle.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import unboundle.BundleUsageAllowedContext;
import unboundle.BundleUsageContext;

@Mixin(BlockItem.class)
public class BlockItemMixin {

    @WrapOperation(
            method = "useOn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;")
    )
    private InteractionResult preventConsumableFallback(BlockItem instance, Level level, Player player, InteractionHand interactionHand, Operation<InteractionResult> original) {
        // Items that are plantable on farmland such as carrots are BlockItems with the CONSUMABLE component.
        // Vanilla checks for these specifically and calls use() *from useOn()*,
        // instead of returning InteractionResult.PASS and letting the pipeline handle use() naturally.
        // This prevents plantables from bypassing the isUsageAllowed check.
        return !BundleUsageContext.usingFromBundle || BundleUsageAllowedContext.isUsageAllowed(player.getItemInHand(interactionHand))
                ? original.call(instance, level, player, interactionHand)
                : InteractionResult.PASS;
    }
}