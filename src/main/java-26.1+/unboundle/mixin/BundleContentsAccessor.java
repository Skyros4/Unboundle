package unboundle.mixin;

import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import com.mojang.serialization.DataResult;
import net.minecraft.world.item.ItemInstance;

@Mixin(BundleContents.class)
public interface BundleContentsAccessor {
    @Invoker("getWeight")
    DataResult<Fraction> invokeGetWeight(ItemInstance stack);
}