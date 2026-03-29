package unboundle;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "unboundle")
public class BundleConfig implements ConfigData {
    // 3 rows, 4 columns are vanilla settings. Less than 2 rows breaks the scrolling feature.
    // Less than 4 columns no longer matches up with the width of the empty tooltip
    @ConfigEntry.BoundedDiscrete(min = 2, max = 8)
    public int rows = 3;
    @ConfigEntry.BoundedDiscrete(min = 4, max = 8)
    public int columns = 4;

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