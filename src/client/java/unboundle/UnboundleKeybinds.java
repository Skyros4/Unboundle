package unboundle;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public class UnboundleKeybinds {
	public static KeyMapping toggleRandomizer;
	// Creates a new category in the Key Binds menu for the setting. lang reads this as key.category.unboundle.general
	private static final KeyMapping.Category UNBOUNDLE_CATEGORY = KeyMapping.Category.register(
			ResourceLocation.fromNamespaceAndPath("unboundle", "general")
	);

	// Reads the key to be used from Minecraft vanilla's Key Binds settings
	public static void register() {
		toggleRandomizer = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.unboundle.toggleRandomizer",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				UNBOUNDLE_CATEGORY
		));
	}
}