package unboundle.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import unboundle.BundleUsageContext;

@Mixin(ArmorStand.class)
public class ArmorStandMixin {

    // Since Armor Stands require the exact position clicked (Vec3), they use a different pipeline than other entity interactions.
    // Originally I intended to intercept at Entity.interactAt().
    // But because Armor Stands are the only entity that implements this, they @Override and do not call super.interactAt().
    // That made a mixin on ArmorStand.class necessary.
    @WrapMethod(method = "interactAt")
    private InteractionResult interactAt$delegateToContents(
            Player player, Vec3 vec3, InteractionHand interactionHand, Operation<InteractionResult> original) {
        // Items other than non-empty bundles proceed as normal.
        ItemStack bundleItem = player.getItemInHand(interactionHand).copy();
        if (!(bundleItem.getItem() instanceof BundleItem)) return original.call(player, vec3, interactionHand);
        BundleContents contents = bundleItem.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return original.call(player, vec3, interactionHand);

        // Applies the general "take item out of bundle, use item, put item back in" pattern.
        return BundleUsageContext.applyAsSelectedItem(player, interactionHand,
                (selectedItem) -> original.call(player, vec3, interactionHand),
                (stack) -> true // None of the problematic interactions (prolonged use) occur here
        );
    }
}