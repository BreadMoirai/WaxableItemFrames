package com.breadmoirai.waxableitemframes.mixin;

import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the inherited {@code HangingEntity.fixed} flag (set when an item frame is waxed) so the
 * game tests can assert the waxed state. {@code fixed} is package-private/non-public in vanilla.
 */
@Mixin(ItemFrame.class)
public interface ItemFrameAccessor {

   @Accessor("fixed")
   boolean waxableitemframes$isFixed();
}
