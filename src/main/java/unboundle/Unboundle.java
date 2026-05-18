package unboundle;

import net.fabricmc.api.ModInitializer;

public class Unboundle implements ModInitializer {
	public static final String MOD_ID = "unboundle";

	@Override
	public void onInitialize() {
		UnboundleConfig.load();
	}
}