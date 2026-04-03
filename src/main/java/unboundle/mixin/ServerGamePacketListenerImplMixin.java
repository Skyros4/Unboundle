package unboundle.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

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
    ) // This controls what happens when the drop key is pressed. Does not include CTRL & drop.
    private boolean dropBundleContents(ServerPlayer instance, boolean bl) {
        // Gets the item that is attempted to be thrown.
        ItemStack heldStack = player.getItemInHand(InteractionHand.MAIN_HAND);

        // If not a bundle, proceed with the vanilla dropping
        Item heldItem = heldStack.getItem();
        if (!(heldItem instanceof BundleItem)) return this.player.drop(bl);

        // Empty bundles don't do anything on drop. That way holding the drop key gets rid of only all items within the bundle, and not the bundle itself.
        BundleContents contents = heldStack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return false;

        // Executes the actual drop itself, using OnUseTick from BundleItem, which is what fires on right click in vanilla
        heldItem.onUseTick(player.level(), player, heldStack, 0);
        // Sends a packet to the client for the arm animation
        player.connection.send(new ClientboundAnimatePacket(player, 0));
        // The drop succeeded
        return true;
    }
}