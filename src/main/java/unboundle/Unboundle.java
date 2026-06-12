package unboundle;

import net.fabricmc.api.ModInitializer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;

public class Unboundle implements ModInitializer {
	public static final String MOD_ID = "unboundle";

	@Override
	public void onInitialize() {
		UnboundleConfig.load();
		DispenserBlock.registerBehavior(Items.BUNDLE, new BundleDispenseItemBehavior());
	}
}