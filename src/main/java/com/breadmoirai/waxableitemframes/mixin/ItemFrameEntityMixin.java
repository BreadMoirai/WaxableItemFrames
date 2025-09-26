package com.breadmoirai.waxableitemframes.mixin;

import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.WorldEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrameEntity.class)
public class ItemFrameEntityMixin {

   @Shadow
   private boolean fixed;

   @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
   private void onInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
      ItemStack itemStack = player.getStackInHand(hand);
      ItemFrameEntity t = ((ItemFrameEntity) (Object) this);
      if (t.getWorld().isClient) {
         return;
      }
      boolean itemFrameEmpty = t.getHeldItemStack().isEmpty();
      if (!itemFrameEmpty) {
         if (fixed && itemStack.getItem() instanceof AxeItem) {
            t.playSound(SoundEvents.ITEM_AXE_WAX_OFF,1.0F, 1.0F);
            t.getWorld().syncWorldEvent(null, WorldEvents.WAX_REMOVED, t.getBlockPos(), 0);
            fixed = false;
            cir.setReturnValue(ActionResult.SUCCESS);
         }

         if (itemStack.isOf(Items.HONEYCOMB) && !fixed) {
            itemStack.decrement(1);
            t.playSound(SoundEvents.ITEM_HONEYCOMB_WAX_ON, 1.0f, 1.0f);
            t.getWorld().syncWorldEvent(null, WorldEvents.BLOCK_WAXED, t.getBlockPos(), 0);
            fixed = true;
            cir.setReturnValue(ActionResult.SUCCESS);
         }
      }
   }
}
