package unboundle;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "unboundle")
public class BundleConfig implements ConfigData {
    @ConfigEntry.BoundedDiscrete(min = 2, max = 8)
    public int rows = 3;
    @ConfigEntry.BoundedDiscrete(min = 4, max = 8)
    public int columns = 4;

    public int maxSlots(){
        return rows * columns;
    }

    @Override
    public void validatePostLoad() {
        rows = Math.max(2, Math.min(8, rows));
        columns = Math.max(4, Math.min(8, columns));
    }
}