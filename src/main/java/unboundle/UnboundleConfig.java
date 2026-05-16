package unboundle;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

// Every field here is written into unboundle.json under .minecraft/config
@Config(name = "unboundle")
public class UnboundleConfig implements ConfigData {
    // 3 rows, 4 columns are vanilla settings. Less than 2 rows breaks the scrolling feature.
    // Less than 4 columns no longer matches up with the width of the empty tooltip
    @ConfigEntry.BoundedDiscrete(min = 2, max = 8)
    public int rows = 3;
    @ConfigEntry.BoundedDiscrete(min = 4, max = 8)
    public int columns = 4;
    // Toggles the random item usage out of the bundle.
    public boolean randomizedUsage = false;
    // Toggles between vanilla, (left click -> slot and right click -> contents) and (left click -> contents and right click -> slot)
    public enum ClickBehaviour {
        VANILLA,
        PRIMARY_BUNDLE,
        PRIMARY_CONTENTS
    }
    public ClickBehaviour clickBehaviour = ClickBehaviour.VANILLA;

    // Allows access to the rows and columns variables
    public static UnboundleConfig config() {
        return AutoConfig.getConfigHolder(UnboundleConfig.class).getConfig();
    }

    public int maxSlots(){
        return rows * columns;
    }

    // Makes sure the settings cannot be changed manually to exceed bounds
    @Override
    public void validatePostLoad() {
        rows = Math.max(2, Math.min(8, rows));
        columns = Math.max(4, Math.min(8, columns));
    }
}