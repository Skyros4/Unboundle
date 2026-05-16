package unboundle;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.UseCooldown;

import java.util.Random;

public class BundleUsageContext {

    // used just for patching plantables bypassing the interaction pipeline
    public static boolean usingFromBundle = false;

    @FunctionalInterface
    public interface UsageOperation {
        InteractionResult apply(ItemStack selectedItem);
    }

    @FunctionalInterface
    public interface UsageAllowedCheck {
        boolean check(ItemStack selectedItem);
    }

    // The entire point of this method is to apply the same pattern (take item out of bundle, use, put back in) to slightly different usage contexts, preventing duplicate logic
    public static InteractionResult applyAsSelectedItem(Player player, InteractionHand interactionHand,
                                                        BundleUsageContext.UsageOperation usageOperation, BundleUsageContext.UsageAllowedCheck usageAllowedCheck) {
        // Extract the first item out of the bundle and perform a validation check
        // copy() avoids potential stale references
        ItemStack bundleItem = player.getItemInHand(interactionHand).copy();
        BundleContents contents = bundleItem.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return InteractionResult.PASS; // allows for the use() fallback

        int selectedItemIndex = getSelectedItemIndex(bundleItem, contents);
        ItemStack selectedItem = contents.getItemUnsafe(selectedItemIndex).copy();

        // Only items from the allowlists actually get used, the other items are rejected.
        InteractionResult result;
        if (usageAllowedCheck.check(selectedItem)) {
            // If the item attempted to be used is currently on a cooldown
            if (player.getCooldowns().isOnCooldown(selectedItem)) return InteractionResult.PASS;

            ItemStack originalHand = player.getItemInHand(interactionHand);
            player.setItemInHand(interactionHand, selectedItem);

            usingFromBundle = true;
            result = usageOperation.apply(selectedItem);
            usingFromBundle = false;

            System.out.println(result);
            selectedItem = player.getItemInHand(interactionHand);

            player.setItemInHand(interactionHand, originalHand);
            player.stopUsingItem(); // Interrupts any dangling item usage, notoriously Ender Eyes.

            // If the item just used (e.g. Ender Pearls) has any cooldowns that would have been applied to them, apply to the bundle item type too.
            applyCooldown(player, bundleItem, selectedItem);

            // If the item placement failed, do nothing, akin to using blocks normally.
            if (!result.consumesAction()) return result;
        } else {
            result = InteractionResult.PASS; // allows for the use() fallback
        }

        return applyItemCycling(result, player, interactionHand, bundleItem,
                selectedItem, contents, selectedItemIndex);
    }

    public static int getSelectedItemIndex(ItemStack bundleItem, BundleContents contents) {
        // If randomizedUsage is enabled, use a field in the bundle's DataComponents to determine randomness, then use that random value to toggle the selected item.
        // Done with DataComponents so that client and server can individually generate their own random value,
        // which is the same for both because they pull the seed from one shared location. Then they both generate a new seed, equal on both sides.
        if(UnboundleConfig.config().randomizedUsage) {
            // Read
            long randomHash = bundleItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLong("randomHash").orElse(0L);

            int selectedItemIndex = new Random(randomHash).nextInt(contents.size());
            // Write
            CompoundTag tag = bundleItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.putLong("randomHash", new Random(randomHash).nextLong());
            bundleItem.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            return selectedItemIndex;
        }
        // Otherwise, if not random, just take out the first item in the bundle
        else{
            return 0;
        }
    }

    public static void applyCooldown(Player player, ItemStack bundleItem, ItemStack selectedItem) {
        if (player.getCooldowns().isOnCooldown(selectedItem)) {
            UseCooldown useCooldown = selectedItem.get(DataComponents.USE_COOLDOWN);
            if (useCooldown != null) {
                useCooldown.apply(bundleItem, player);
            } else {
                // Hackier way to detect cooldowns for items that do not use the data component.
                // Goat Horns as an instance of InstrumentItem are an example.
                // If a cooldown has been applied, the remaining cooldown at frame 0 is 100%, otherwise 0%.
                float percent = player.getCooldowns().getCooldownPercent(selectedItem, 0f);
                if (percent >= 1f) {
                    // Since the cooldown has been applied to the item in the bundle already,
                    // we reverse engineer the total duration by observing one cooldown step (where the duration is slightly less than 100%),
                    // then extrapolating the total amount of steps from that.
                    float step = percent - player.getCooldowns().getCooldownPercent(selectedItem, 1f);
                    int totalTicks = Math.round(1f / step);
                    player.getCooldowns().addCooldown(
                            player.getCooldowns().getCooldownGroup(bundleItem),
                            totalTicks
                    );
                }
            }
        }
    }

    public static InteractionResult applyItemCycling(InteractionResult result, Player player, InteractionHand interactionHand,
                                                      ItemStack bundleItem, ItemStack selectedItem,
                                                      BundleContents contents, int randomIndex) {
        // If item usage did not succeed, do not cycle
        if (!(result instanceof InteractionResult.Success success)) return result;

        // Takes the result of the item usage bundle, and insert it back at the end if not empty.
        ItemStack transformed = success.heldItemTransformedTo();
        BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
        mutable.toggleSelectedItem(UnboundleConfig.config().randomizedUsage ? randomIndex : 0);
        mutable.removeOne();
        ItemStack transformedCopy;
        if(transformed != null) {
            transformedCopy = transformed.copy();
        }
        else {
            // If in Creative Mode, force all items that do not receive transformations to not be consumed.
            // Deals with Firework Rockets and other outliers that use shrink() instead of consume().
            // shrink() actually attempts to reduce the item count even in Creative,
            // but the check for the item's slot in the creative inventory prevents that. Bundles bypass this check by nature.
            transformedCopy = player.getAbilities().instabuild && !(selectedItem.getItem() instanceof BundleItem)
                    ? contents.getItemUnsafe(UnboundleConfig.config().randomizedUsage ? randomIndex : 0).copy()
                    : selectedItem.copy();
        }
        if (!transformedCopy.isEmpty()) {
            // Always inserts as a separate stack. That way, separate stacks are preserved, and unified stacks remain unaffected
            // If not randomizedUsage, insert at the end to simulate cycling
            if (!UnboundleConfig.config().randomizedUsage) mutable.toggleSelectedItem(mutable.toImmutable().size() - 1);
            BundleUIContext.shiftClick = true;
            boolean inserted = mutable.tryInsert(transformedCopy) > 0;
            BundleUIContext.shiftClick = false;
            if (!inserted) {
                if (!player.getInventory().add(transformedCopy)) {
                    player.drop(transformedCopy, false);
                }
            }
        }
        // Resets everything after cycling
        mutable.toggleSelectedItem(-1);
        BundleUIContext.rowOffset = 0;

        // The bundle appears to be playing the "pick up" animation because of this set(),
        // and a new bundle is written into the player's hand.
        bundleItem.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
        player.setItemInHand(interactionHand, bundleItem);

        // Return a Success with no transform, so vanilla doesn't overwrite the hand
        // Otherwise returns whether the item usage succeeded.
        return transformed != null ? success.withoutItem() : result;
    }

}