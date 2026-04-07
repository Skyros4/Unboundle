package unboundle;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.Map;
import java.util.TreeMap;

import static java.util.Map.entry;

// Every field here is written into unboundle.json under .minecraft/config
@Config(name = "unboundle")
public class BundleConfig implements ConfigData {
    // 3 rows, 4 columns are vanilla settings. Less than 2 rows breaks the scrolling feature.
    // Less than 4 columns no longer matches up with the width of the empty tooltip
    @ConfigEntry.BoundedDiscrete(min = 2, max = 8)
    public int rows = 3;
    @ConfigEntry.BoundedDiscrete(min = 4, max = 8)
    public int columns = 4;
    // Toggles between vanilla and (left click -> slot and right click -> contents)
    public boolean clickBehaviourSeparate = false;

    // Determines which items are allowed to be used out of a bundle.
    // Gives the user the ability to toy around with this themselves.
    // **WARNING** Some items might break.
    public Map<String, Boolean> usableOutOfBundle = new TreeMap<>(Map.<String, Boolean>ofEntries(
            entry("AirItem", false),
            entry("ArmorStandItem", false),
            entry("ArrowItem", false),
            entry("AxeItem", false),
            entry("BannerItem", false), // StandingAndWallBlockItem, BlockItem parent
            entry("BedItem", false), // BlockItem parent
            entry("BlockItem", true),
            entry("BoatItem", false),
            entry("BoneMealItem", false),
            entry("BottleItem", false),
            entry("BowItem", false), // ProjectileWeaponItem parent
            entry("BrushItem", false),
            entry("BucketItem", false),
            entry("BundleItem", false),
            entry("CompassItem", false),
            entry("CrossbowItem", false), // ProjectileWeaponItem parent
            entry("DebugStickItem", false),
            entry("DiscFragmentItem", false),
            entry("DoubleHighBlockItem", false), // BlockItem parent
            entry("DyeItem", false),
            entry("EggItem", false),
            entry("EmptyMapItem", false),
            entry("EndCrystalItem", false),
            entry("EnderEyeItem", false),
            entry("EnderpearlItem", false),
            entry("ExperienceBottleItem", false),
            entry("FireChargeItem", false),
            entry("FireworkRocketItem", false),
            entry("FishingRodItem", false),
            entry("FlintAndSteelItem", false),
            entry("FoodOnAStickItem", false),
            entry("GameMasterBlockItem", false), // BlockItem parent
            entry("GlowInkSacItem", false),
            entry("HangingEntityItem", false),
            entry("HangingSignItem", false), // SignItem parent
            entry("HoeItem", false),
            entry("HoneycombItem", false),
            entry("InkSacItem", false),
            entry("InstrumentItem", false),
            entry("ItemFrameItem", false), // HangingEntityItem parent
            entry("KnowledgeBookItem", false),
            entry("LeadItem", false),
            entry("LingeringPotionItem", false), // ThrowablePotionItem, PotionItem parent
            entry("MaceItem", false),
            entry("MapItem", false),
            entry("MinecartItem", false),
            entry("MobBucketItem", false), // BucketItem parent
            entry("NameTagItem", false),
            entry("PlaceOnWaterBlockItem", false), // BlockItem parent
            entry("PlayerHeadItem", false), // StandingAndWallBlockItem, BlockItem parent
            entry("PotionItem", false),
            entry("ProjectileWeaponItem", false),
            entry("ScaffoldingBlockItem", false), // BlockItem parent
            entry("ShearsItem", false),
            entry("ShieldItem", false),
            entry("ShovelItem", false),
            entry("SignItem", false), // StandingAndWallBlockItem, BlockItem parent
            entry("SmithingTemplateItem", false),
            entry("SnowballItem", false),
            entry("SolidBucketItem", false), // BlockItem parent
            entry("SpawnEggItem", false),
            entry("SpectralArrowItem", false), // ArrowItem parent
            entry("SplashPotionItem", false), // ThrowablePotionItem, PotionItem parent
            entry("SpyglassItem", false),
            entry("StandingAndWallBlockItem", false), // BlockItem parent
            entry("ThrowablePotionItem", false), // PotionItem parent
            entry("TippedArrowItem", false), // ArrowItem parent
            entry("TridentItem", false),
            entry("WindChargeItem", false),
            entry("WritableBookItem", false),
            entry("WrittenBookItem", false)
    ));

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