package unboundle.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import unboundle.BundleUIContext;
import unboundle.BundleUsageAllowedContext;
import unboundle.BundleUsageContext;
import unboundle.UnboundleConfig;

import java.util.Random;

// This class runs on both client and server side.
// The client predicts everything by running the code themselves, then the server does a validation run and informs the client accordingly.
@Mixin(BundleItem.class)
public abstract class BundleItemMixin extends Item implements SignApplicator {

    public BundleItemMixin(Properties properties) {
        super(properties);
    }

    // Bundle A is on cursor, bundle B is in slot.
    // Resets B's state when putting B into A.
    @Inject(
            method = "overrideStackedOnOther",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;",
                    shift = At.Shift.AFTER
            )

    )
    private void overrideStackedOnOther$resetBundleState(
            ItemStack itemStack, Slot slot, ClickAction clickAction, Player player,
            CallbackInfoReturnable<Boolean> cir) {
        ItemStack slotItem = slot.getItem();
        if (slotItem.getItem() instanceof BundleItem) {
            BundleContents contents = slotItem.get(DataComponents.BUNDLE_CONTENTS);
            if (contents == null) return;
            BundleContents.Mutable mutable = new BundleContents.Mutable(contents);

            mutable.toggleSelectedItem(-1);
            slotItem.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
            BundleUIContext.rowOffset = 0;
        }
    }

    // Bundle is on cursor, item is in slot.
    // If PRIMARY_BUNDLE, right click inserts items, otherwise left click.
    @ModifyExpressionValue(
            method = "overrideStackedOnOther(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/inventory/ClickAction;Lnet/minecraft/world/entity/player/Player;)Z",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/inventory/ClickAction;PRIMARY:Lnet/minecraft/world/inventory/ClickAction;",
                    ordinal = 0,
                    opcode = Opcodes.GETSTATIC
            )
    )
    private ClickAction overrideStackedOnOther$dynamicClickActionPrimary(ClickAction original) {
        return UnboundleConfig.config().clickBehaviour == UnboundleConfig.ClickBehaviour.PRIMARY_BUNDLE
                ? ClickAction.SECONDARY
                : original; // ClickAction.PRIMARY
    }
    // Bundle is on cursor, slot is empty.
    // If PRIMARY_CONTENTS, left click drops items into the slot, otherwise right click.
    @ModifyExpressionValue(
            method = "overrideStackedOnOther(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/inventory/ClickAction;Lnet/minecraft/world/entity/player/Player;)Z",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/inventory/ClickAction;SECONDARY:Lnet/minecraft/world/inventory/ClickAction;",
                    ordinal = 0,
                    opcode = Opcodes.GETSTATIC
            )
    )
    private ClickAction overrideStackedOnOther$dynamicClickActionSecondary(ClickAction original) {
        return UnboundleConfig.config().clickBehaviour == UnboundleConfig.ClickBehaviour.PRIMARY_CONTENTS
                ? ClickAction.PRIMARY
                : original; // ClickAction.SECONDARY
    }

    // Bundle is in slot, cursor is empty.
    // If PRIMARY_CONTENTS, right click now picks up the bundle, otherwise left click.
    @ModifyExpressionValue(
            method = "overrideOtherStackedOnMe(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/inventory/ClickAction;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/SlotAccess;)Z",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/inventory/ClickAction;PRIMARY:Lnet/minecraft/world/inventory/ClickAction;",
                    ordinal = 0,
                    opcode = Opcodes.GETSTATIC
            )
    )
    private ClickAction overrideOtherStackedOnMe$dynamicClickActionPrimary1(ClickAction original) {
        return UnboundleConfig.config().clickBehaviour == UnboundleConfig.ClickBehaviour.PRIMARY_CONTENTS
                ? ClickAction.SECONDARY
                : original; // ClickAction.PRIMARY
    }
    // Bundle is in slot, item is on cursor.
    // If PRIMARY_BUNDLE, right click now inserts items, otherwise left click.
    @ModifyExpressionValue(
            method = "overrideOtherStackedOnMe(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/inventory/ClickAction;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/SlotAccess;)Z",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/inventory/ClickAction;PRIMARY:Lnet/minecraft/world/inventory/ClickAction;",
                    ordinal = 1,
                    opcode = Opcodes.GETSTATIC
            )
    )
    private ClickAction overrideOtherStackedOnMe$dynamicClickActionPrimary2(ClickAction original) {
        return UnboundleConfig.config().clickBehaviour == UnboundleConfig.ClickBehaviour.PRIMARY_BUNDLE
                ? ClickAction.SECONDARY
                : original; // ClickAction.PRIMARY
    }
    // Bundle is in slot, cursor is empty.
    // If PRIMARY_CONTENTS, left click now takes out items, otherwise right click.
    @ModifyExpressionValue(
            method = "overrideOtherStackedOnMe(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/inventory/ClickAction;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/SlotAccess;)Z",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/inventory/ClickAction;SECONDARY:Lnet/minecraft/world/inventory/ClickAction;",
                    ordinal = 0,
                    opcode = Opcodes.GETSTATIC
            )
    )
    private ClickAction overrideOtherStackedOnMe$dynamicClickActionSecondary(ClickAction original) {
        return UnboundleConfig.config().clickBehaviour == UnboundleConfig.ClickBehaviour.PRIMARY_CONTENTS
                ? ClickAction.PRIMARY
                : original; // ClickAction.SECONDARY
    }

    // This method handles general block interaction, with the item being the handler.
    // If that fails, the pipeline proceeds with useItemOn() in BlockStateBase.class.
    @Override @NotNull
    public InteractionResult useOn(UseOnContext useOnContext) {
        // Applies the general "take item out of bundle, use item, put item back in" pattern.
        return BundleUsageContext.applyAsSelectedItem(useOnContext.getPlayer(), useOnContext.getHand(),
                (selectedItem) -> {
                    // Creates a new context for the item to be used in, namely replacing the item to be used with the item within the bundle.
                    return selectedItem.useOn(new UseOnContext(
                            useOnContext.getLevel(),
                            useOnContext.getPlayer(),
                            useOnContext.getHand(),
                            selectedItem,
                            new BlockHitResult(
                                    useOnContext.getClickLocation(),
                                    useOnContext.getClickedFace(),
                                    useOnContext.getClickedPos(),
                                    useOnContext.isInside()
                            )
                    ));
                },
                (stack) -> true // None of the problematic interactions (prolonged use) occur here
        );
    }

    // This method handles general item interaction unrelated to blocks with the item being the handler.
    @WrapMethod(method = "use")
    private InteractionResult use(Level level, Player player, InteractionHand interactionHand, Operation<InteractionResult> original) {
        // Applies the general "take item out of bundle, use item, put item back in" pattern.
        return BundleUsageContext.applyAsSelectedItem(player, interactionHand,
                (selectedItem) -> selectedItem.use(level, player, interactionHand),
                BundleUsageAllowedContext::isUsageAllowed // This is the one case where the problematic interactions (prolonged use) can occur
        );
    }

    // Implements SignApplicator to be able to use the bundle's contents on signs.
    @Override
    public boolean tryApplyToSign(Level level, SignBlockEntity signBlockEntity, boolean bl, Player player) {
        // Determine the player's hand that just used the bundle. We know a bundle was used because we're here in BundleItem.class.
        InteractionHand interactionHand = player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof BundleItem
                ? InteractionHand.MAIN_HAND
                : InteractionHand.OFF_HAND;

        // Slightly hackier way to make tryApplyToSign adapt to the general "take item out of bundle, use item, put item back in" pattern.
        // If selectedItem.tryApplyToSign() succeeds, we set the InteractionResult to SUCCESS, and test against that when applyAsSelectedItem() finishes.
        // That way we can carry the boolean result through the InteractionResult pipeline up to this point.
        return BundleUsageContext.applyAsSelectedItem(player, interactionHand,
                (selectedItem) -> ((SignApplicator)selectedItem.getItem()).tryApplyToSign(level, signBlockEntity, bl, player)
                        ? InteractionResult.SUCCESS
                        : InteractionResult.PASS,
                (stack) -> stack.getItem() instanceof SignApplicator
        ) == InteractionResult.SUCCESS;
    }

    // Part of SignApplicator. Reads the selectedItem from the bundle and applies its canApplyToSign()
    @Override
    public boolean canApplyToSign(SignText signText, Player player) {
        // Determine the player's hand that just used the bundle. We know a bundle was used because we're here in BundleItem.class.
        // Then, look only at non-empty bundles.
        InteractionHand hand = player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof BundleItem
                ? InteractionHand.MAIN_HAND
                : InteractionHand.OFF_HAND;
        ItemStack bundleItem = player.getItemInHand(hand);
        BundleContents contents = bundleItem.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return false;

        // Because we're just getting the selectedItem and not actually using it,
        // the selectedItemIndex value is just read and not written, contrary to what happens in BundleUsageContext.getSelectedItemIndex().
        long randomHash = bundleItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLong("randomHash").orElse(0L);
        int selectedItemIndex = UnboundleConfig.config().randomizedUsage ? new Random(randomHash).nextInt(contents.size()) : 0;
        ItemStack selectedItem = contents.getItemUnsafe(selectedItemIndex);

        // canApplyToSign now applies to selectedItem rather than the bundle.
        if (selectedItem.getItem() instanceof SignApplicator applicator) {
            return applicator.canApplyToSign(signText, player);
        }
        return false;
    }
}