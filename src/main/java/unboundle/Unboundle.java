package unboundle;

import net.fabricmc.api.ModInitializer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;

public class Unboundle implements ModInitializer {
	public static final String MOD_ID = "unboundle";

	@Override
	public void onInitialize() {
		UnboundleConfig.load();
		//? if >= 26.2 {
		/*DispenserBlock.registerBehavior(Items.BUNDLE, new BundleDispenseItemBehavior());
		for (DyeColor color : DyeColor.values()) {
			DispenserBlock.registerBehavior(Items.DYED_BUNDLE.pick(color), new BundleDispenseItemBehavior());
		}
		*///?} else {
		DispenserBlock.registerBehavior(Items.BUNDLE, new BundleDispenseItemBehavior());
		DispenserBlock.registerBehavior(Items.WHITE_BUNDLE, new BundleDispenseItemBehavior());
		DispenserBlock.registerBehavior(Items.ORANGE_BUNDLE, new BundleDispenseItemBehavior());
		DispenserBlock.registerBehavior(Items.MAGENTA_BUNDLE, new BundleDispenseItemBehavior());
		DispenserBlock.registerBehavior(Items.LIGHT_BLUE_BUNDLE, new BundleDispenseItemBehavior());
		DispenserBlock.registerBehavior(Items.YELLOW_BUNDLE, new BundleDispenseItemBehavior());
		DispenserBlock.registerBehavior(Items.LIME_BUNDLE, new BundleDispenseItemBehavior());
		DispenserBlock.registerBehavior(Items.PINK_BUNDLE, new BundleDispenseItemBehavior());
		DispenserBlock.registerBehavior(Items.GRAY_BUNDLE, new BundleDispenseItemBehavior());
		DispenserBlock.registerBehavior(Items.LIGHT_GRAY_BUNDLE, new BundleDispenseItemBehavior());
		DispenserBlock.registerBehavior(Items.CYAN_BUNDLE, new BundleDispenseItemBehavior());
		DispenserBlock.registerBehavior(Items.PURPLE_BUNDLE, new BundleDispenseItemBehavior());
		DispenserBlock.registerBehavior(Items.BLUE_BUNDLE, new BundleDispenseItemBehavior());
		DispenserBlock.registerBehavior(Items.BROWN_BUNDLE, new BundleDispenseItemBehavior());
		DispenserBlock.registerBehavior(Items.GREEN_BUNDLE, new BundleDispenseItemBehavior());
		DispenserBlock.registerBehavior(Items.RED_BUNDLE, new BundleDispenseItemBehavior());
		DispenserBlock.registerBehavior(Items.BLACK_BUNDLE, new BundleDispenseItemBehavior());
		 //?}

	}
}