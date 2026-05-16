package unboundle;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class UnboundleClientEvents {
	public static void register() {
		// Listens to the key presses
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (UnboundleKeybinds.toggleRandomizer.consumeClick()) {
				// if the configured key is pressed, flip and save the setting, and show a text
				UnboundleConfig.config().randomizedUsage = !UnboundleConfig.config().randomizedUsage;
				AutoConfig.getConfigHolder(UnboundleConfig.class).save();
				client.player.displayClientMessage(
					net.minecraft.network.chat.Component.translatable(UnboundleConfig.config().randomizedUsage
							? "key.unboundle.toggleRandomizer.on"
							: "key.unboundle.toggleRandomizer.off"),
						true // above the hotbar (overlay). False would be in chat
				);
			}
		});
	}
}