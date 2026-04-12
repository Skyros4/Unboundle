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
			// Loads the variables from UnboundleConfig
			UnboundleConfig config = AutoConfig.getConfigHolder(UnboundleConfig.class).getConfig();
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
					.setTooltip(Component.translatable("text.autoconfig.unboundle.option.rows.tooltip"),
								Component.translatable("text.autoconfig.unboundle.option.rows.tooltip2"))
					.setTextGetter(val -> Component.literal(String.valueOf(val)))
					.setSaveConsumer(val -> config.rows = val)
					.build());
			general.addEntry(entryBuilder.startIntSlider(
							Component.translatable("text.autoconfig.unboundle.option.columns"), config.columns, 4, 8)
					.setDefaultValue(4)
					.setTooltip(Component.translatable("text.autoconfig.unboundle.option.columns.tooltip"),
								Component.translatable("text.autoconfig.unboundle.option.columns.tooltip2"))
					.setTextGetter(val -> Component.literal(String.valueOf(val)))
					.setSaveConsumer(val -> config.columns = val)
					.build());
			general.addEntry(entryBuilder.startBooleanToggle(
							Component.translatable("text.autoconfig.unboundle.option.randomizer"), config.randomizedUsage)
					.setDefaultValue(false)
					.setTooltip(Component.translatable("text.autoconfig.unboundle.option.randomizer.tooltip"),
							Component.translatable("text.autoconfig.unboundle.option.randomizer.tooltip2"),
							Component.translatable("text.autoconfig.unboundle.option.randomizer.tooltip3"))
					.setSaveConsumer(val -> config.randomizedUsage = val)
					.build());
			general.addEntry(entryBuilder.startEnumSelector(
							Component.translatable("text.autoconfig.unboundle.option.ClickBehaviour"),
							UnboundleConfig.ClickBehaviour.class,
							config.clickBehaviour)
					.setDefaultValue(UnboundleConfig.ClickBehaviour.VANILLA)
					.setTooltip(Component.translatable("text.autoconfig.unboundle.option.ClickBehaviour.tooltip"),
								Component.translatable("text.autoconfig.unboundle.option.ClickBehaviour.tooltip2"))
					.setEnumNameProvider(val -> Component.translatable(
							"text.autoconfig.unboundle.option.ClickBehaviour." + val.name().toLowerCase()))
					.setSaveConsumer(val -> config.clickBehaviour = val)
					.build());

			// Links the settings to the variables in UnboundleConfig
			builder.setSavingRunnable(() ->
					AutoConfig.getConfigHolder(UnboundleConfig.class).save());

			return builder.build();
		};
	}
}