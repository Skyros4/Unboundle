package unboundle;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback;
import net.minecraft.client.Minecraft;

public class UnboundleClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// client == Minecraft.getInstance()
		// Sets up event listeners that set a boolean flag indicating whether the high contrast resource pack is loaded, on client start
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			BundleRenderContext.highContrast = client.getResourcePackRepository().getSelectedIds().contains("high_contrast");
		});
		// ... and on resource pack reload. This flag controls the look of the shadows of 16-stackables and unstackables.
		InvalidateRenderStateCallback.EVENT.register(() -> {
			BundleRenderContext.highContrast = Minecraft.getInstance().getResourcePackRepository().getSelectedIds().contains("high_contrast");
		});
	}
}