package com.breadmoirai.waxableitemframes.mixin.v21_8;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Waxing behaviour for MC 1.21.x, where {@code ItemFrame.interact(Player, InteractionHand)} has no
 * hit-location parameter. Selected via the 1.21.x {@code waxableitemframes.mixins.json}. The 26.x
 * counterpart lives in {@code ..mixin.v26_1}.
 */
@Mixin(ItemFrame.class)
public class ItemFrameEntityMixin {

   @Shadow
   private boolean fixed;

   @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
   private void onInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
      ItemStack itemStack = player.getItemInHand(hand);
      ItemFrame t = ((ItemFrame) (Object) this);
      if (t.level().isClientSide()) {
         return;
      }
      boolean itemFrameEmpty = t.getItem().isEmpty();
      if (!itemFrameEmpty) {
         if (fixed && itemStack.getItem() instanceof AxeItem) {
            t.playSound(SoundEvents.AXE_WAX_OFF, 1.0F, 1.0F);
            t.level().levelEvent(null, LevelEvent.PARTICLES_WAX_OFF, t.blockPosition(), 0);
            fixed = false;
            cir.setReturnValue(InteractionResult.SUCCESS);
         }

         if (itemStack.is(Items.HONEYCOMB) && !fixed) {
            itemStack.shrink(1);
            t.playSound(SoundEvents.HONEYCOMB_WAX_ON, 1.0f, 1.0f);
            t.level().levelEvent(null, LevelEvent.PARTICLES_AND_SOUND_WAX_ON, t.blockPosition(), 0);
            fixed = true;
            cir.setReturnValue(InteractionResult.SUCCESS);
         }
      }
   }
}
