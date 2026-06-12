# Design

My general vision for this mod was to make it feel very much like Vanilla, as if it's always been part of the game. 
In order to achieve that, a surprising amount of concrete design decisions have been made, as detailed below.

## HUD Display

I considered showing the bundle's contents or at least the currently selected item when holding the bundle, outside the inventory as a tooltip above the hotbar, and respectively changing the hand model and hotbar icon.
But I decided against that idea for the following reasons: 
- If the tooltip element were to show the entire contents, it would obscure a relatively big part of the screen, which is not desirable during gameplay. 
Also, that would take away from the inventory tooltip's function.
- If the tooltip were to show just the currently selected item or a small subset of the contents, it wouldn't really provide essential information to know in advance. 
For one, the usage itself provides immediate feedback, and for another the bundle's contents can be checked in the inventory at any time anyway.
- No other item in the game has such a tooltip. 
Adding it specifically for the bundle would break with vanilla's HUD design philosophy. 
- If the bundle's model in the player's hand and/or icon in the hotbar did change visually, that would lead to flashes, as the duration of item usage is generally short.
- Not revealing any information about the bundle's contents on the HUD, outside of GUIs, is more in line with the random item usage mode, one of the two item usage modes. 
Trying to make it work otherwise would be difficult as the random mode's very nature means you cannot predict which item is going to come out of the bundle.
- Furthermore, not revealing the items inside the bundle through the model in the player's hand is a mechanic that could find use cases in multiplayer. 
That would allow players to conceal their item usage from others.

## Bundle tooltip

Different items in the bundle can have different weights, notably 64-stackables, 16-stackables, unstackables and other bundles.
To make that visually clear, a shadow was added to item slots with higher weights. It represents the item's imagined "heaviness" inside the bundle. 
Minecraft's native High-Contrast resource pack was considered in the process, for which the shadows are made much brighter.

Furthermore, after careful consideration, unstackables were given a weight of 16, so that four of them can fit in one bundle.
This weight allows for saving a few inventory slots, integrating unstackables in the sequential/random item placement feature, but also still ensures that storing unstackables comes at a meaningful cost.

## Item insertion

### ... on the first slot

In the bundle tooltip, players cannot insert items at the very first slot anymore once they started scrolling and selecting items.
This is because there are n item slots and n+1 positions between them, if you consider the position before the first and after the last item as well.
However, on first hover with no item selected, you can still insert at the first slot.
Therefore, if you are mid-scroll and wanted to insert at the first slot again, the cost of hovering away and back on the bundle slot is relatively small.
The alternative would have been that scrolling through all items in the bundle once deselects at the end, which breaks continuity.

### ... as a separate stack

This mod includes the ability to insert items as a separate stack, if that item was already present in the bundle.
That way, by inserting the same item in multiple slots, players can give some items more weight than others. 
In other words, item distribution inside the bundle can now be fine-tuned. 
This can come in handy for builders, for example.

At first, I thought that CTRL & left-click would fit nicely for this feature, as there is a small parallel to CTRL & drop thinking in stacks rather than single items.
However, CTRL & mouse-click in GUIs is not used anywhere in Minecraft Vanilla. 
As GUI interaction is encoded through a ClickType enum, this would have required me to add CTRL & left-click (or CTRL & mouse-click in general) as a new click type to that enum, and then go through Minecraft's entire GUI pipeline to support this, only for it to be used on one item, the bundle.

So using CTRL was not worth the effort. 
This prompted me to think more about the different functions of mouse clicks in bundle interaction in general. 
Left and right click do separate things when interacting with a bundle, except for when SHIFT is held. 
In that case, the shift-click functionality is applied onto the slot.
This one exception is what I've been looking for, an unused or redundant mouse action, so that I can use it for the separate stack insertion feature. 
This is why SHIFT & mouse-click inserts items as a separate stack in my mod. 
If no item is held on the cursor, it shift-clicks just like in vanilla.

## Click behaviour

The SHIFT key is not the only thing though that makes left and right click interaction with the bundle slightly weird. 
For example, left click picks up the bundle (interaction with the slot), but it also inserts items into the bundle (interaction with the bundle's contents). 
So this means functionality-wise, they're swapped sometimes.
Because of this realization, I decided to add an optional setting that isolates these functionalities, so that one click always interacts with the bundle, and one with its contents.

## Dropping items

Pressing the drop key on an empty bundle does nothing, so that the player can hold down the drop key to quickly empty the bundle onto the ground without also throwing the bundle itself after the items.
CTRL & drop then allows for dropping the entire bundle itself (including empty ones).

In vanilla Minecraft, there is no way to automate emptying bundles. 
The contents of a bundle can only be removed through player interaction.
That is why dispensers were made to drop a bundle's contents, one by one. 
Even items that normally have a different behaviour inside dispensers, for example water buckets, are dropped when inside a bundle.
The reason for this is that every single item should be extractable out of bundles. 
If the player wanted to, say, place water out of a bundle out of a dispenser, they can chain multiple dispensers together to achieve their desired effect.

Dispensers were chosen as the way to empty bundles because it is a block with very specific interactions with items. 
In existing redstone contraptions, only a select few items ever find their way into dispensers, so that their specific interaction can be taken advantage of.
This means bundles wouldn't normally interact with dispensers, and therefore existing redstone contraptions would be unlikely to break with this change.
For the general use case of dropping items, including bundles, the dropper would have come into play, which remains unchanged in dropping the bundle itself.

## Functional items in bundles

At first only blocks were intended to be supported to be used out of bundles, but as the technical architecture gradually developed, more and more items came along. 
The technical feasibility really was the only thing holding many of them back.

In the end, the only items remaining unsupported are the ones with any kind of prolonged usage, which actually are justifiable from a design perspective.
Using something out of a bundle can be imagined as physically opening the bundle, holding it sideways and tapping the items out in a controlled manner, for any items that are placed or applied.
Throwables can similarly be imagined as being slung out of a bundle, and tools as being grabbed on the shaft with the bundle's leather around it while the blade peeks out.
The unsupported items would then clash with this mental picture.

Among the supported items, items with cooldowns are handled as a special case.
If the item just used out of a bundle would have had a cooldown applied, the cooldown is now applied to all bundles too.
This prevents players from using bundles to bypass cooldowns.

## Non-functional items in bundles

When trying to use an item out of the bundle that doesn't have a use functionality in the first place, for example sticks, nothing happens, and the item is not cycled. 
Once such an item is selected, the bundle is stuck on that item.
I thought about cycling such items as well, for one to get those items unstuck and for another to allow for a Russian-Roulette-like mechanic.

However, that proved problematic because there is no simple way to detect whether an item has a use functionality in the first place.
The game considers the interaction results of a stick used at a wall and a block used at your feet to be the same, nothing happens.
Rather than deal with the complexity required to detect items without use functionalities, and making sure it doesn't disrupt gameplay, I decided to scrap this feature.

Also, it would feel arbitrary to the player when on a failed usage some items cycle through the bundle, and others don't.
The Russian-Roulette-like mechanic can still be achieved with items that have a harmless use functionality, snowballs for example.
Furthermore, the player is free and therefore responsible for what they decide to put in the bundle. 
If a stick disrupts cycling, it can simply be taken out of the bundle.

## Random item usage mode

There was a considerate amount of reflection about how to integrate the random mode into the gameplay so that it feels like a natural part of Minecraft, and came up with a few ways to implement this:
 
- As an enchantment,
- as a crafting recipe consisting of the bundle item and a rabbit foot symbolizing luck and randomness, or 
- as a global toggle when trying to use an empty bundle the random mode could be incorporated into the bundle.

In the end, I've found that they either require more effort in survival than a simple random mode feature is worth, are unintuitive and unclear to the average player, or can only be used in certain contexts.
If Mojang were to add a feature like this, I believe they would only pick one of the two placement modes to implement. 
That is why the random mode is now a toggle, with an optional keybind to switch between modes quickly.