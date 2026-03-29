package unboundle;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public class ModMenuEntrypoint implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			// Loads the variables from BundleConfig
			BundleConfig config = AutoConfig.getConfigHolder(BundleConfig.class).getConfig();
			// Set up the mod settings menu
			ConfigBuilder builder = ConfigBuilder.create()
					.setParentScreen(parent)
					.setTitle(Component.translatable("text.autoconfig.unboundle.title"));
			ConfigEntryBuilder entryBuilder = builder.entryBuilder();
			ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));

			// Adds the settings
			general.addEntry(entryBuilder.startIntSlider(
							Component.translatable("text.autoconfig.unboundle.option.rows"), config.rows, 2, 8)
					.setDefaultValue(3)
					.setTooltip(Component.translatable("text.autoconfig.unboundle.option.rows.tooltip"))
					.setSaveConsumer(val -> config.rows = val)
					.build());
			general.addEntry(entryBuilder.startIntSlider(
							Component.translatable("text.autoconfig.unboundle.option.columns"), config.columns, 4, 8)
					.setDefaultValue(4)
					.setTooltip(Component.translatable("text.autoconfig.unboundle.option.columns.tooltip"))
					.setSaveConsumer(val -> config.columns = val)
					.build());

			// Links the settings to the variables in BundleConfig
			builder.setSavingRunnable(() ->
					AutoConfig.getConfigHolder(BundleConfig.class).save());

			return builder.build();
		};
	}
}