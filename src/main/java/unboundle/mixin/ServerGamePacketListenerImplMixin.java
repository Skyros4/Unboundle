package unboundle.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import unboundle.BundleRenderContext;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Redirect(
            method = "handlePlayerAction",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;drop(Z)Z",
                    ordinal = 0
            )
    )
    private boolean dropBundleContents(ServerPlayer instance, boolean bl) {
        // Gets the item that is attempted to be thrown.
        ItemStack heldStack = player.getItemInHand(InteractionHand.MAIN_HAND);

        // If not a bundle, proceed with the vanilla dropping
        Item heldItem = heldStack.getItem();
        if (!(heldItem instanceof BundleItem)) return this.player.drop(bl);

        // Empty bundles don't do anything on drop
        BundleContents contents = heldStack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return false;

        // Executes the actual drop itself, using OnUseTick from BundleItem, which is what fires on right click in vanilla
        heldItem.onUseTick(player.level(), player, heldStack, 0);
        // Sends a packet to the client for the arm animation
        player.connection.send(new ClientboundAnimatePacket(player, 0));
        // The drop succeeded
        return true;
    }

    @Redirect(
            method = "handleUseItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItem(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"
            )
    ) // This method fires on use while not looking at a block
    private InteractionResult preventVanillaBundleUse(ServerPlayerGameMode instance, ServerPlayer serverPlayer, Level level, ItemStack itemStack, InteractionHand interactionHand) {
        // The normal useItem action is cancelled for bundles, preventing item drops server side.
        if ((itemStack.getItem() instanceof BundleItem)) return InteractionResult.FAIL;

        // Otherwise, just proceed the vanilla way.
        return serverPlayer.gameMode.useItem(serverPlayer, level, itemStack, interactionHand);
    }

    @Redirect(
            method = "handleUseItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"
            )
    )
    private InteractionResult useItemFromBundle(ServerPlayerGameMode gameMode, ServerPlayer serverPlayer, Level level, ItemStack itemStack, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        // If the item to be used is not a bundle, just proceed the vanilla way.
        if (!(itemStack.getItem() instanceof BundleItem)) return gameMode.useItemOn(player, level, itemStack, interactionHand, blockHitResult);

        // Extracts the first item out of the bundle and performs a few validation checks
        BundleContents contents = itemStack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return gameMode.useItemOn(player, level, itemStack, interactionHand, blockHitResult);
        ItemStack selectedItem = contents.getItemUnsafe(0);
        if (selectedItem.isEmpty() || !BundleRenderContext.useAllowed(selectedItem)) return gameMode.useItemOn(player, level, itemStack, interactionHand, blockHitResult);
        // As selectedItem is going to be hampered with, create a copy.
        player.setItemInHand(interactionHand, selectedItem.copy());

        // As the original bundle item will not persist through the placement, a copy is created.
        ItemStack bundleBackup = itemStack.copy();

        // Uses the item within the bundle
        InteractionResult result = gameMode.useItemOn(player, level, selectedItem.copy(), interactionHand, blockHitResult);

        // If the usage failed, do not proceed with removing an item in Survival
        if (!result.consumesAction()) {
            player.setItemInHand(interactionHand, bundleBackup);
            return result;
        }

        BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
        // Takes the item out of the bundle, decrease its count by 1 if in survival, and insert it back at the end if not empty.
        ItemStack toRemove = mutable.toImmutable().getItemUnsafe(0).copy();
        mutable.removeOne();
        if (!serverPlayer.getAbilities().instabuild) toRemove.shrink(1);
        if (!toRemove.isEmpty()) {
            mutable.toggleSelectedItem(mutable.toImmutable().size() - 1);
            mutable.tryInsert(toRemove);
            mutable.toggleSelectedItem(-1);
        }

        // Writes these changes back into the player's hand
        bundleBackup.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
        player.setItemInHand(interactionHand, bundleBackup);

        // Moves the player's main or offhand accordingly, depending on which used the bundle item
        player.connection.send(new ClientboundAnimatePacket(player, interactionHand == InteractionHand.MAIN_HAND ? 0 : 3));

        return result;
    }
}