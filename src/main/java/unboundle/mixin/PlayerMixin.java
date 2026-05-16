package unboundle.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import unboundle.BundleUsageContext;

@Mixin(Player.class)
public class PlayerMixin extends Avatar {

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    // This method controls what happens when a player interacts with an entity. Inside it's determined whether the entity or the item handles the interaction.
    // We intercept at this level so that the handlers only ever see the selectedItem and never the bundle.
    // That way all entity-specific interactions can be covered without entity-specific mixins.
    @WrapMethod(method = "interactOn")
    private InteractionResult interactOn$handleBundle(
            Entity entity, InteractionHand interactionHand, Operation<InteractionResult> original) {
        // Items other than non-empty bundles proceed as normal. Also when interacting with item frames (HangingEntity), the bundle itself should be put in there.
        ItemStack bundleItem = this.getItemInHand(interactionHand).copy();
        if (!(bundleItem.getItem() instanceof BundleItem) || entity instanceof HangingEntity) return original.call(entity, interactionHand);
        BundleContents contents = bundleItem.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return original.call(entity, interactionHand);

        // Applies the general "take item out of bundle, use item, put item back in" pattern.
        return BundleUsageContext.applyAsSelectedItem(((Player)(Object)this), interactionHand,
                (selectedItem) -> original.call(entity, interactionHand),
                (stack) -> true // None of the problematic interactions (prolonged use) occur here
        );
    }
}