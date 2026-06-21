package unboundle;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.network.chat.Component;

public class UnboundleClientEvents {
	public static void register() {
		// Listens to the key presses
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (UnboundleKeybinds.toggleItemUsageMode.consumeClick()) {
				// if the configured key is pressed, flip and save the setting, and show a text
				UnboundleConfig.config().itemUsageMode = UnboundleConfig.config().itemUsageMode.toggle();
				UnboundleConfig.save();
				//? if >= 26.1 {
				/*client.gui.setOverlayMessage(
						Component.translatable(UnboundleConfig.config().itemUsageMode == UnboundleConfig.ItemUsageMode.SEQUENTIAL
								? "key.unboundle.toggleItemUsageMode.sequential"
								: "key.unboundle.toggleItemUsageMode.random"),
						false
				);
				*///?} else {
				client.player.displayClientMessage(
						Component.translatable(UnboundleConfig.config().itemUsageMode == UnboundleConfig.ItemUsageMode.SEQUENTIAL
							? "key.unboundle.toggleItemUsageMode.sequential"
							: "key.unboundle.toggleItemUsageMode.random"),
						true // above the hotbar (overlay). False would be in chat
				);
				 //?}
			}
		});
	}
}