package unboundle;

import com.mojang.blaze3d.platform.InputConstants;
//? if >= 26.1 {
/*import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
*///?} else {
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
 //?}
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public class UnboundleKeybinds {
	public static KeyMapping toggleItemUsageMode;
	// Creates a new category in the Key Binds menu for the setting. lang reads this as key.category.unboundle.general
	private static final KeyMapping.Category UNBOUNDLE_CATEGORY = KeyMapping.Category.register(
			ResourceLocation.fromNamespaceAndPath("unboundle", "general")
	);

	// Reads the key to be used from Minecraft vanilla's Key Binds settings
	public static void register() {
		//? if >= 26.1 {
		/*toggleItemUsageMode = KeyMappingHelper.registerKeyMapping(new KeyMapping(
		 *///?} else {
		toggleItemUsageMode = KeyBindingHelper.registerKeyBinding(new KeyMapping(
		//?}
				"key.unboundle.toggleItemUsageMode",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				UNBOUNDLE_CATEGORY
		));
	}
}