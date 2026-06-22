package unboundle;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;

import java.util.Map;
import java.util.TreeMap;

import static java.util.Map.entry;

public class BundleUsageAllowedContext {

    public enum UsageAllowedState {
        ALLOWED,
        DISALLOWED,
        NOT_INCLUDED
    }

    public static boolean isUsageAllowed(ItemStack stack) {
        // Components are checked first, to account for instances of Item (that don't have a unique class) with special components.
        if (getComponentAllowed(stack) == UsageAllowedState.DISALLOWED) return false;
        if (getComponentAllowed(stack) == UsageAllowedState.ALLOWED) return true;
        // DISALLOWED returns false as it should, and NOT_INCLUDED doesn't have use() implementations anyway
        return getClassAllowed(stack) == UsageAllowedState.ALLOWED;
    }

    public static UsageAllowedState getComponentAllowed(ItemStack stack) {
        // Get all components currently on the item
        DataComponentMap components = stack.getComponents();

        // If an item has both black- and whitelisted components (such as shields), restrict the item.

        for (TypedDataComponent<?> component : components) {
            // Get the registry name of the component (e.g., "minecraft:consumable")
            String componentId = getComponentName(component.type());
            // Any blacklist hit wins immediately
            if (USABLE_BY_COMPONENT.containsKey(componentId)) {
                if (!USABLE_BY_COMPONENT.get(componentId)) return UsageAllowedState.DISALLOWED;
            }
        }

        // No blacklisted components found. Now check if anything is whitelisted
        for (TypedDataComponent<?> component : components) {
            String componentId = getComponentName(component.type());
            if (USABLE_BY_COMPONENT.containsKey(componentId)) {
                if (USABLE_BY_COMPONENT.get(componentId)) return UsageAllowedState.ALLOWED;
            }
        }

        return UsageAllowedState.NOT_INCLUDED;
    }

    public static String getComponentName(DataComponentType<?> type) {
        // This returns the full ID, e.g., "minecraft:consumable"
        return BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type).toString();
    }


    public static UsageAllowedState getClassAllowed(ItemStack stack) {
        // The class of the current item
        Class<?> currentClass = stack.getItem().getClass();

        // Climb up the superclass tree until we arrive at Item itself, at which point we stop.
        while (currentClass != null && currentClass != Item.class) {
            String name = currentClass.getSimpleName();

            // if in the USABLE_BY_CLASS, return the value in there.
            // Contrary to components, items can belong to 1 class at most, which makes this single loop possible.
            if (USABLE_BY_CLASS.containsKey(name)) {
                return USABLE_BY_CLASS.get(name) ? UsageAllowedState.ALLOWED : UsageAllowedState.DISALLOWED;
            }

            // Move up one step
            currentClass = currentClass.getSuperclass();
        }

        return UsageAllowedState.NOT_INCLUDED;
    }

    // Contains all item classes that have a right-click functionality for documentation purposes.
    // General rule of thumb: Items with any kind of prolonged usage do not work, for both design and technical reasons.
    // Any entries omitted are items that either don't do anything on use, or that are subclasses of items listed here.
    public static final Map<String, Boolean> USABLE_BY_CLASS = new TreeMap<>(Map.<String, Boolean>ofEntries(
            entry(ArmorStandItem.class.getSimpleName(), true),
            entry(AxeItem.class.getSimpleName(), true),
            entry(BlockItem.class.getSimpleName(), true),
            entry(BoatItem.class.getSimpleName(), true),
            entry(BoneMealItem.class.getSimpleName(), true),
            entry(BottleItem.class.getSimpleName(), true),
            entry(BrushItem.class.getSimpleName(), false),
            entry(BucketItem.class.getSimpleName(), true),
            entry(BundleItem.class.getSimpleName(), true), // Unleashing recursion madness.
            entry(CompassItem.class.getSimpleName(), true),
            entry(DebugStickItem.class.getSimpleName(), true),
            entry(DyeItem.class.getSimpleName(), true),
            entry(EggItem.class.getSimpleName(), true),
            entry(EmptyMapItem.class.getSimpleName(), true),
            entry(EndCrystalItem.class.getSimpleName(), true),
            entry(EnderEyeItem.class.getSimpleName(), true),
            entry(EnderpearlItem.class.getSimpleName(), true),
            entry(ExperienceBottleItem.class.getSimpleName(), true),
            entry(FireChargeItem.class.getSimpleName(), true),
            entry(FireworkRocketItem.class.getSimpleName(), true),
            entry(FishingRodItem.class.getSimpleName(), false), // Requires the item itself to be held visibly for the line to be held.
            entry(FlintAndSteelItem.class.getSimpleName(), true),
            entry(FoodOnAStickItem.class.getSimpleName(), false), // Requires the item itself to be held visibly for the mob to recognize the food.
            entry(GlowInkSacItem.class.getSimpleName(), true),
            entry(HangingEntityItem.class.getSimpleName(), true),
            entry(HoeItem.class.getSimpleName(), true),
            entry(HoneycombItem.class.getSimpleName(), true),
            entry(InkSacItem.class.getSimpleName(), true),
            entry(InstrumentItem.class.getSimpleName(), true),
            entry(KnowledgeBookItem.class.getSimpleName(), true),
            entry(LeadItem.class.getSimpleName(), true),
            entry(MapItem.class.getSimpleName(), true),
            entry(MinecartItem.class.getSimpleName(), true),
            entry(MobBucketItem.class.getSimpleName(), true),
            entry(NameTagItem.class.getSimpleName(), true),
            entry(PotionItem.class.getSimpleName(), true), // Consumption is excluded, see components. Only applies to useOn for water bottles.
            entry(ProjectileWeaponItem.class.getSimpleName(), false),
            entry(ShearsItem.class.getSimpleName(), true),
            entry(ShieldItem.class.getSimpleName(), false), // Requires the item itself to be held visibly for protection.
            entry(ShovelItem.class.getSimpleName(), true),
            entry(SnowballItem.class.getSimpleName(), true),
            entry(SolidBucketItem.class.getSimpleName(), true),
            entry(SpawnEggItem.class.getSimpleName(), true),
            entry(SpyglassItem.class.getSimpleName(), false),
            entry(ThrowablePotionItem.class.getSimpleName(), true),
            entry(TridentItem.class.getSimpleName(), false),
            entry(WindChargeItem.class.getSimpleName(), true),
            // The below two require a direct reference to the stack in the player's hand, making them inherently incompatible with this mod's general usage handlers.
            entry(WritableBookItem.class.getSimpleName(), false),
            entry(WrittenBookItem.class.getSimpleName(), false)
    ));

    // Contains all item components that have a right-click functionality for documentation purposes. Acts as a blacklist.
    // Just like with the classes, items with any kind of prolonged usage generally do not work
    public static final Map<String, Boolean> USABLE_BY_COMPONENT = new TreeMap<>(Map.<String, Boolean>ofEntries(
            //? if >= 1.21.5 {
            entry(BundleUsageAllowedContext.getComponentName(DataComponents.BLOCKS_ATTACKS), false), // equal to ShieldItem, for compatibility
            //?}
            entry(BundleUsageAllowedContext.getComponentName(DataComponents.CONSUMABLE), false),
            entry(BundleUsageAllowedContext.getComponentName(DataComponents.EQUIPPABLE), true)
    ));

}