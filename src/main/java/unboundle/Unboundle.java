package unboundle;

import net.fabricmc.api.ModInitializer;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

public class Unboundle implements ModInitializer {
	public static final String MOD_ID = "unboundle";

	@Override
	public void onInitialize() {
		// Tells the game to write the settings to a JSON (to persist) from the BundleConfig class.
		AutoConfig.register(BundleConfig.class, GsonConfigSerializer::new);
	}
}