# Technical

The following document details the software architecture of this mod. Concepts behind the implementation as well as technical challenges are explained.

## Bundle Tooltip

### Rendering

In vanilla Minecraft, the items inside the bundle are sorted from oldest to newest.
So new items would be appended at the end of the list, which visually would be the top and leftmost slot.
In order to render these items, Mojang decided to iterate over the slot coordinates from bottom right to top left, so that the items are drawn in the order they appear in the list.

If the bundle contains more unique items than the initial 12 slots available, the sublist of the items that would actually get rendered is used.
In Mojang's initial implementation, this means that on "overflows", the rendering starts on the 11-th-to-last item in the list (accounting for the counter too). 
So the starting point is somewhere in the middle of the full list, visually to the left of the counter (not truly bottom right), and the starting item is highly variable as it depends on the contents' size.

These two things combined required some mental gymnastics to visually imagine the rendering process.

The shadows for items with higher weights are simply reused, darker versions of the slot sprite layered behind the actual slot sprites.
That reused sprite has been made larger, so that it peeks out on the right and bottom, giving the effect of a shadow.

Additionally, the static fields in ClientBundleTooltipMixin controlling the tooltip dimensions were originally in-lined through the compiler.
Therefore, a re-embedding was necessary, so that changing these fields would actually affect the tooltip dimensions.

### Scrolling

In order to allow for scrolling past the initial visible items to reveal the hidden items, the mentioned sublist of items to be rendered would need to change dynamically on mouse wheel inputs.
For that, the mouse wheel inputs update the current window of visible items over the list using a global variable.
Using that variable and some math, the indexes of the first and last visible item relative to the full list are determined dynamically, which is then used to construct the sublist.

As moving the window of visible items down hides the topmost items, another counter was implemented that behaves just like the original bottom right counter, except for the top left.
That means based on setup, the number of visible items can either be the amount of slots, one less, or two less, which also needed to be accounted for.

On top of this, when inserting an item that was present in the bundle already, but not on the current window of visible items, the window would need to be set remotely to show the position of the item just inserted.
For that, some more math was done to cover the following two cases:
- When the current window is before the inserted item's stack position, the earliest window showing that item is calculated.
- When the current window is after the inserted item's stack position, the latest window showing that item is calculated.

This makes it look like the game automatically scrolled to the item's position on insert.

### Separate stack item insertion

Minecraft uses a few very high-level classes to deal with all containers, which was handy.
Injecting just in these would handle all container GUI screens to prevent vanilla behaviour from overwriting the mod's.
But as it turns out, the inventory in Creative mode is an outlier and uses a different structure.
Therefore, it needed separate handling, especially for the middle-click copy feature.
That one was covered already in other containers by the general checks in the above classes.

## Bundle Contents

### Dropping items from within bundles

In terms of the client-server network, Minecraft generally operates on shared classes for any mechanics where players and the world interact with each other. 
For example, a block placement is executed by both the client and server, which runs through the same block placement method twice.
The client can execute placement instantly, which ensures smooth and responsive gameplay. 
And the server verifies that the placement was actually valid.
If not, the placement is reverted retroactively. 
In this sense, the client predicts the actual placement.
Dropping items is the exception here, as that is handled generically for items by two different classes, LocalPlayer and ServerPlayer, hence the two mixins.

### Item usage from within bundles

The method applyAsSelectedItem() sits at the center of this mod's bundle item usage pipeline. 
The core principle is as follows:
1. Check whether the item can be used according to BundleUsageAllowedContext. If not, return a failed item usage.
2. Selected item is taken out of the bundle and replaces that bundle in the player's hand.
3. Vanilla pipeline handles usage for that item.
4. Result of the item usage is retrieved from the player's hand, put back into the bundle, and the bundle is restored to the player's hand.

All of this happens during one frame, so the player does not ever see the bundle in their hand change.

Originally this was done manually for just useOn() in BundleItem. 
But as more and more cases needing separate handling crept up, it was decided that this workflow is best extracted into its own method.
The cases include:
- useOn() from BundleItem, which includes any interaction with a block. 
Flower placement, hoe tilling, water bottle mudding and other things fall under that.
- use() from BundleItem, which includes any item usage without a target. 
This is what other methods fall back on if their usage fails. 
These are the kind that can be performed looking into the air, such as throwing snowballs, swapping armor or launching ender eyes.
- tryApplyToSign() and canApplyToSign(), as part of BundleItem now implementing SignApplicator. 
This allows dyes, ink sacs and honey comb to be applied to signs from within a bundle.
- useItemOn() from BlockStateBase, for block-specific interaction that is handled from the block's side and not the item's. 
The fallback for useOn(). 
Examples would be campfires, cauldrons and TNT.
- interactOn() from Player, for any entity interactions. 
The game naturally distinguishes between item-side and entity-side handling here. 
Examples include dyeing sheep, leashing mobs and feeding dogs.
- interactAt() for Armor Stands, which is the exception here. 
Corresponds to interactOn(), except with additional information for the exact position clicked on the entity. 
Technically exists for any entity with Entity.interactAt(), but Armor Stands are the only one implementing this, and @Override the parent method. 



