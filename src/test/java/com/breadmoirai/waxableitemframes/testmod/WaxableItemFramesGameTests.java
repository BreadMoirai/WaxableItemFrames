package com.breadmoirai.waxableitemframes.testmod;

import com.breadmoirai.waxableitemframes.mixin.ItemFrameAccessor;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Server game tests for WaxableItemFrames. The mod injects into {@link ItemFrame#interact} so that
 * right-clicking a <em>filled</em> item frame with honeycomb locks it ({@code fixed = true}) and a
 * subsequent right-click with an axe unlocks it. Tests run headless via {@code runGameTest} on
 * every supported MC version — no client/display required.
 *
 * <p>The waxed state lives in the inherited (non-public) {@code HangingEntity.fixed} field, read
 * here through {@link ItemFrameAccessor}.
 */
public class WaxableItemFramesGameTests {

   private static final BlockPos SUPPORT = new BlockPos(2, 2, 1);
   private static final BlockPos FRAME = new BlockPos(2, 2, 2);
   private static final Direction FACING = Direction.SOUTH; // frame hangs on the block to its north (SUPPORT)

   // Honeycomb on a filled frame waxes it (locks rotation) and consumes one honeycomb.
   @GameTest(skyAccess = true)
   public void honeycombWaxesFilledFrame(GameTestHelper helper) {
      ItemFrame frame = placeFilledFrame(helper, new ItemStack(Items.DIAMOND));
      Player player = giveHeld(helper, new ItemStack(Items.HONEYCOMB));

      InteractionResult result = interact(frame, player);

      if (!result.consumesAction()) {
         fail(helper, "waxing a filled frame with honeycomb should consume the interaction");
      }
      if (!isFixed(frame)) {
         fail(helper, "frame was not waxed (fixed) after honeycomb interaction");
      }
      if (player.getItemInHand(InteractionHand.MAIN_HAND).getCount() != 0) {
         fail(helper, "waxing should have consumed exactly one honeycomb");
      }
      helper.succeed();
   }

   // An axe removes the wax from a previously waxed frame.
   @GameTest(skyAccess = true)
   public void axeRemovesWaxFromFrame(GameTestHelper helper) {
      ItemFrame frame = placeFilledFrame(helper, new ItemStack(Items.DIAMOND));

      // Wax it first.
      Player waxer = giveHeld(helper, new ItemStack(Items.HONEYCOMB));
      interact(frame, waxer);
      if (!isFixed(frame)) {
         fail(helper, "precondition failed: frame should be waxed before testing the axe");
      }

      Player chopper = giveHeld(helper, new ItemStack(Items.IRON_AXE));
      InteractionResult result = interact(frame, chopper);

      if (!result.consumesAction()) {
         fail(helper, "removing wax with an axe should consume the interaction");
      }
      if (isFixed(frame)) {
         fail(helper, "frame was still waxed (fixed) after the axe interaction");
      }
      helper.succeed();
   }

   // The mod must NOT wax an empty frame — only filled frames are lockable.
   @GameTest(skyAccess = true)
   public void honeycombDoesNotWaxEmptyFrame(GameTestHelper helper) {
      ItemFrame frame = placeFilledFrame(helper, ItemStack.EMPTY);
      Player player = giveHeld(helper, new ItemStack(Items.HONEYCOMB));

      interact(frame, player);

      if (isFixed(frame)) {
         fail(helper, "an empty frame must not become waxed");
      }
      helper.succeed();
   }

   // An axe on an unwaxed (but filled) frame must not toggle anything via the mod.
   @GameTest(skyAccess = true)
   public void axeDoesNotWaxUnwaxedFrame(GameTestHelper helper) {
      ItemFrame frame = placeFilledFrame(helper, new ItemStack(Items.DIAMOND));
      Player player = giveHeld(helper, new ItemStack(Items.IRON_AXE));

      interact(frame, player);

      if (isFixed(frame)) {
         fail(helper, "an axe should never wax a frame, only remove existing wax");
      }
      helper.succeed();
   }

   // --- helpers ------------------------------------------------------------------------------

   private static ItemFrame placeFilledFrame(GameTestHelper helper, ItemStack content) {
      helper.setBlock(SUPPORT, Blocks.STONE);
      ServerLevel level = helper.getLevel();
      BlockPos abs = helper.absolutePos(FRAME);
      ItemFrame frame = new ItemFrame(level, abs, FACING);
      if (!content.isEmpty()) {
         frame.setItem(content, false);
      }
      level.addFreshEntity(frame);
      return frame;
   }

   private static Player giveHeld(GameTestHelper helper, ItemStack held) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      player.setItemInHand(InteractionHand.MAIN_HAND, held);
      return player;
   }

   private static boolean isFixed(ItemFrame frame) {
      return ((ItemFrameAccessor) frame).waxableitemframes$isFixed();
   }

   // ItemFrame.interact gained a Vec3 hit-location parameter in MC 26.x.
   private static InteractionResult interact(ItemFrame frame, Player player) {
      //? if >=26.1 {
      return frame.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
      //? } else {
      /*return frame.interact(player, InteractionHand.MAIN_HAND);
      *///? }
   }

   private static void fail(GameTestHelper helper, String message) {
      helper.fail(Component.literal(message));
   }
}
