package unboundle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

// Every public field here is written into unboundle.json under .minecraft/config
public class UnboundleConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("unboundle.json");
    private static final Logger LOGGER = LoggerFactory.getLogger(Unboundle.MOD_ID);

    private static UnboundleConfig instance = new UnboundleConfig();

    public int rows = 3;
    public int columns = 4;
    // Toggles the random item usage out of the bundle.
    public enum ItemUsageMode {
        SEQUENTIAL,
        RANDOM;
        public ItemUsageMode toggle() {
            return this == SEQUENTIAL ? RANDOM : SEQUENTIAL;
        }
    }
    public ItemUsageMode itemUsageMode = ItemUsageMode.SEQUENTIAL;
    // Toggles between vanilla, (left click -> slot and right click -> contents) and (left click -> contents and right click -> slot)
    public enum ClickBehaviour {
        VANILLA,
        PRIMARY_BUNDLE,
        PRIMARY_CONTENTS
    }
    public ClickBehaviour clickBehaviour = ClickBehaviour.VANILLA;

    public int maxSlots(){
        return rows * columns;
    }

    // Allows access to the rows and columns variables
    public static UnboundleConfig config() {
        return instance;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                instance = GSON.fromJson(reader, UnboundleConfig.class);
                instance.validate();
            } catch (IOException e) { // Uses defaults on failure
                LOGGER.error("Failed to load UnboundleConfig, using defaults...", e);
                instance = new UnboundleConfig();
            }
        } else {
            instance = new UnboundleConfig();
            save();
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save UnboundleConfig", e);
        }
    }

    // Makes sure the settings cannot be changed manually to exceed bounds
    // Less than 2 rows breaks the scrolling feature. Less than 4 columns no longer matches up with the width of the empty tooltip
    public void validate() {
        rows = Math.max(2, Math.min(8, rows));
        columns = Math.max(4, Math.min(8, columns));
    }
}