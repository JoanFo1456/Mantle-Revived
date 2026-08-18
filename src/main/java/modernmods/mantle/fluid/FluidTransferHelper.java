package modernmods.mantle.fluid;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.SoundAction;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import modernmods.mantle.Mantle;
import modernmods.mantle.fluid.transfer.FluidContainerTransferManager;
import modernmods.mantle.fluid.transfer.IFluidContainerTransfer;
import modernmods.mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection;
import modernmods.mantle.fluid.transfer.IFluidContainerTransfer.TransferResult;

import javax.annotation.Nullable;

import static modernmods.mantle.util.TranslationHelper.COMMA_FORMAT;

/**
 * Alternative to {@link net.neoforged.neoforge.fluids.FluidUtil} since no one has time to make the forge util not a buggy mess.
 * <p>In 26.1.2 fluid capabilities are {@link ResourceHandler} of {@link FluidResource} operated through {@link Transaction}s,
 * so all the tank/item handlers here are {@code ResourceHandler<FluidResource>}.
 */
@SuppressWarnings("unused")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FluidTransferHelper {
  private static final String KEY_FILLED = Mantle.makeDescriptionId("block", "tank.filled");
  private static final String KEY_DRAINED = Mantle.makeDescriptionId("block", "tank.drained");

  /** Gets the given sound from the fluid */
  public static SoundEvent getSound(FluidStack fluid, SoundAction action, SoundEvent fallback) {
    SoundEvent event = fluid.getFluid().getFluidType().getSound(fluid, action);
    if (event == null) {
      return fallback;
    }
    return event;
  }

  /** Gets the empty sound for a fluid */
  public static SoundEvent getEmptySound(FluidStack fluid) {
    return getSound(fluid, SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY);
  }

  /** Gets the fill sound for a fluid */
  public static SoundEvent getFillSound(FluidStack fluid) {
    return getSound(fluid, SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL);
  }


  /* Resource handler helpers - bridge the FluidStack API to ResourceHandler<FluidResource> */

  /**
   * Inserts the given fluid into the handler
   * @param handler  Fluid handler
   * @param fluid    Fluid to insert
   * @param execute  If true, commits the change; if false simulates
   * @return  Amount inserted
   */
  public static int fill(ResourceHandler<FluidResource> handler, FluidStack fluid, boolean execute) {
    if (fluid.isEmpty()) {
      return 0;
    }
    try (Transaction tx = Transaction.openRoot()) {
      int inserted = handler.insert(FluidResource.of(fluid), fluid.getAmount(), tx);
      if (execute) {
        tx.commit();
      }
      return inserted;
    }
  }

  /**
   * Extracts the given fluid from the handler
   * @param handler  Fluid handler
   * @param filter   Fluid and maximum amount to extract
   * @param execute  If true, commits the change; if false simulates
   * @return  Fluid extracted, empty if none
   */
  public static FluidStack drain(ResourceHandler<FluidResource> handler, FluidStack filter, boolean execute) {
    if (filter.isEmpty()) {
      return FluidStack.EMPTY;
    }
    FluidResource resource = FluidResource.of(filter);
    try (Transaction tx = Transaction.openRoot()) {
      int extracted = handler.extract(resource, filter.getAmount(), tx);
      if (extracted <= 0) {
        return FluidStack.EMPTY;
      }
      if (execute) {
        tx.commit();
      }
      return resource.toStack(extracted);
    }
  }

  /**
   * Extracts up to the given amount of any fluid found in the handler, checking tanks in order
   * @param handler    Fluid handler
   * @param maxAmount  Maximum amount to extract
   * @param execute    If true, commits the change; if false simulates
   * @return  Fluid extracted, empty if none
   */
  public static FluidStack drainAny(ResourceHandler<FluidResource> handler, int maxAmount, boolean execute) {
    for (int i = 0; i < handler.size(); i++) {
      FluidResource resource = handler.getResource(i);
      if (!resource.isEmpty()) {
        try (Transaction tx = Transaction.openRoot()) {
          int extracted = handler.extract(resource, maxAmount, tx);
          if (extracted > 0) {
            if (execute) {
              tx.commit();
            }
            return resource.toStack(extracted);
          }
        }
      }
    }
    return FluidStack.EMPTY;
  }

  /**
   * Attempts to transfer fluid, moving whatever the input contains
   * @param input    Fluid source
   * @param output   Fluid destination
   * @param maxFill  Maximum to transfer
   * @return  Fluid transferred, empty if none
   */
  public static FluidStack tryTransfer(ResourceHandler<FluidResource> input, ResourceHandler<FluidResource> output, int maxFill) {
    for (int i = 0; i < input.size(); i++) {
      FluidResource resource = input.getResource(i);
      if (!resource.isEmpty()) {
        FluidStack moved = tryTransfer(input, output, resource.toStack(maxFill));
        if (!moved.isEmpty()) {
          return moved;
        }
      }
    }
    return FluidStack.EMPTY;
  }

  /**
   * Attempts to transfer a specific fluid between two handlers
   * @param input    Fluid source
   * @param output   Fluid destination
   * @param fluid    Fluid to transfer, will not be modified. Amount is the maximum to move.
   * @return  Fluid transferred, empty if none
   */
  public static FluidStack tryTransfer(ResourceHandler<FluidResource> input, ResourceHandler<FluidResource> output, FluidStack fluid) {
    if (fluid.isEmpty()) {
      return FluidStack.EMPTY;
    }
    FluidResource resource = FluidResource.of(fluid);
    // first find out how much we can extract
    int extractable;
    try (Transaction tx = Transaction.openRoot()) {
      extractable = input.extract(resource, fluid.getAmount(), tx);
    }
    if (extractable <= 0) {
      return FluidStack.EMPTY;
    }
    // next find out how much of that we can insert
    int insertable;
    try (Transaction tx = Transaction.openRoot()) {
      insertable = output.insert(resource, extractable, tx);
    }
    // All-or-nothing containers (a vanilla bucket must end up at exactly one bucket) REJECT an over-capacity insert amount
    // instead of clamping it, so filling a bucket from a tank that holds more than a bucket fails the insert above. Retry
    // offering only the output's free capacity. (Tinkers' own tank items accept partial fills, which is why those worked.)
    if (insertable <= 0) {
      long freeSpace = 0;
      for (int i = 0; i < output.size(); i++) {
        freeSpace += Math.max(0, output.getCapacityAsLong(i, resource) - output.getAmountAsLong(i));
      }
      int offer = (int) Math.min(freeSpace, extractable);
      if (offer > 0 && offer < extractable) {
        try (Transaction tx = Transaction.openRoot()) {
          insertable = output.insert(resource, offer, tx);
        }
      }
    }
    if (insertable <= 0) {
      return FluidStack.EMPTY;
    }
    // finally, move exactly the insertable amount atomically
    try (Transaction tx = Transaction.openRoot()) {
      int extracted = input.extract(resource, insertable, tx);
      int inserted = output.insert(resource, extracted, tx);
      if (extracted == insertable && inserted == extracted) {
        tx.commit();
        return resource.toStack(inserted);
      }
    }
    return FluidStack.EMPTY;
  }

  /** Return options for interaction methods */
  public enum FluidInteractionResult {
    /** Indicates fluid filled the item stack, draining the block entity */
    FILLED_STACK,
    /** Indicates fluid drained the stack, filling the block entity */
    DRAINED_STACK,
    /** Indicates there was a fluid container, but no fluid was transferred. Note that client side will never attempt transfer */
    CONTAINER,
    /** Indicates there was no block entity or the player was not holding a fluid container */
    MISSING;

    /** Returns true if fluid did move */
    public boolean didTransfer() {
      return this == FILLED_STACK || this == DRAINED_STACK;
    }

    /** Returns true if a container is present */
    public boolean hasContainer() {
      return this != MISSING;
    }
  }

  /** @deprecated use {@link #interactWithFilledBucket(Level, BlockPos, ResourceHandler, Player, InteractionHand, Direction)} or {@link #interactWithTank(Level, BlockPos, Player, InteractionHand, Direction, Direction)} */
  @Deprecated(forRemoval = true)
  public static boolean interactWithBucket(Level world, BlockPos pos, Player player, InteractionHand hand, Direction hit, Direction offset) {
    if (player.getItemInHand(hand).getItem() instanceof BucketItem) {
      BlockEntity te = world.getBlockEntity(pos);
      if (te != null) {
        ResourceHandler<FluidResource> handler = world.getCapability(Capabilities.Fluid.BLOCK, pos, hit);
        if (handler != null) {
          return interactWithFilledBucket(world, pos, handler, player, hand, offset).hasContainer();
        }
      }
    }
    return false;
  }

  /**
   * Attempts to interact with a flilled bucket on a fluid tank. This is unique as it handles fish buckets, which don't expose fluid capabilities
   * @param world     World instance
   * @param pos       Block position
   * @param handler   Fluid handler in the block entity
   * @param player    Player
   * @param hand      Hand
   * @param offset    Direction to place fish
   * @return {@link FluidInteractionResult} indicating the type of interaction that happened.
   */
  public static FluidInteractionResult interactWithFilledBucket(Level world, BlockPos pos, ResourceHandler<FluidResource> handler, Player player, InteractionHand hand, Direction offset) {
    ItemStack held = player.getItemInHand(hand);
    if (held.getItem() instanceof BucketItem bucket) {
      Fluid fluid = bucket.content;
      if (fluid != Fluids.EMPTY) {
        if (!world.isClientSide()) {
          FluidStack fluidStack = new FluidStack(bucket.content, FluidType.BUCKET_VOLUME);
          // must empty the whole bucket
          if (fill(handler, fluidStack, false) == FluidType.BUCKET_VOLUME) {
            SoundEvent sound = getEmptySound(fluidStack);
            fill(handler, fluidStack, true);
            bucket.checkExtraContent(player, world, held, pos.relative(offset));
            world.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.sendOverlayMessage(Component.translatable(KEY_FILLED, COMMA_FORMAT.format(FluidType.BUCKET_VOLUME), fluidStack.getHoverName()));
            if (!player.isCreative()) {
              player.setItemInHand(hand, held.getItem().getCraftingRemainder().create());
            }
            return FluidInteractionResult.DRAINED_STACK;
          }
        }
        return FluidInteractionResult.CONTAINER;
      }
    }
    return FluidInteractionResult.MISSING;
  }

  /** Plays the sound from filling a TE */
  public static void playEmptySound(Level world, BlockPos pos, Player player, FluidStack transferred) {
    world.playSound(null, pos, getEmptySound(transferred), SoundSource.BLOCKS, 1.0F, 1.0F);
    player.sendOverlayMessage(Component.translatable(KEY_FILLED, COMMA_FORMAT.format(transferred.getAmount()), transferred.getHoverName()));
  }

  /** Plays the sound from draining a TE */
  public static void playFillSound(Level world, BlockPos pos, Player player, FluidStack transferred) {
    world.playSound(null, pos, getFillSound(transferred), SoundSource.BLOCKS, 1.0F, 1.0F);
    player.sendOverlayMessage(Component.translatable(KEY_DRAINED, COMMA_FORMAT.format(transferred.getAmount()), transferred.getHoverName()));
  }

  /** @deprecated use {@link #interactWithContainer(Level, BlockPos, Player, InteractionHand, BlockHitResult)} */
  @Deprecated(forRemoval = true)
  public static boolean interactWithFluidItem(Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    return interactWithContainer(world, pos, player, hand, hit).hasContainer();
  }

  /**
   * Base logic to interact with a tank by fetching it from the block entity.
   * @param world   World instance
   * @param pos     Tank position
   * @param player  Player instance
   * @param hand    Hand used
   * @param hit     Hit position
   * @return {@link FluidInteractionResult} indicating the type of interaction that happened.
   * @see #interactWithTank(Level, BlockPos, Player, InteractionHand, BlockHitResult)
   */
  public static FluidInteractionResult interactWithContainer(Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    if (!player.getItemInHand(hand).isEmpty()) {
      BlockEntity te = world.getBlockEntity(pos);
      if (te != null) {
        // TE must have a capability
        ResourceHandler<FluidResource> handler = world.getCapability(Capabilities.Fluid.BLOCK, pos, hit.getDirection());
        if (handler != null) {
          return interactWithContainer(world, pos, handler, player, hand);
        }
      }
    }
    return FluidInteractionResult.MISSING;
  }

  /**
   * Base logic to interact with a tank within a block entity.
   * @param world     World instance
   * @param pos       Tank position
   * @param teHandler Fluid handler in the block entity
   * @param player    Player instance
   * @param hand      Hand used
   * @return {@link FluidInteractionResult} indicating the type of interaction that happened.
   * @see #interactWithContainer(Level, BlockPos, ResourceHandler, Player, InteractionHand)
   */
  public static FluidInteractionResult interactWithContainer(Level world, BlockPos pos, ResourceHandler<FluidResource> teHandler, Player player, InteractionHand hand) {
    // fallback to JSON based transfer
    ItemStack stack = player.getItemInHand(hand);
    // an empty hand can never transfer fluid; bail before ItemAccess.forStack below, which rejects empty stacks in 26.1
    if (stack.isEmpty()) {
      return FluidInteractionResult.MISSING;
    }
    if (FluidContainerTransferManager.INSTANCE.mayHaveTransfer(stack)) {
      // only actually transfer on the serverside, client just has items
      if (!world.isClientSide()) {
        FluidStack currentFluid = drainAny(teHandler, Integer.MAX_VALUE, false);
        IFluidContainerTransfer transfer = FluidContainerTransferManager.INSTANCE.getTransfer(stack, currentFluid);
        if (transfer != null) {
          TransferResult result = transfer.transfer(stack, currentFluid, teHandler, TransferDirection.AUTO);
          if (result != null) {
            if (result.didFill()) {
              playFillSound(world, pos, player, result.fluid());
            } else {
              playEmptySound(world, pos, player, result.fluid());
            }
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, result.stack()));
            return result.didFill() ? FluidInteractionResult.FILLED_STACK : FluidInteractionResult.DRAINED_STACK;
          }
        }
      }
      return FluidInteractionResult.CONTAINER;
    }

    // if the item has a capability, do a direct transfer
    ItemAccess itemAccess = ItemAccess.forStack(stack.copyWithCount(1));
    ResourceHandler<FluidResource> itemHandler = itemAccess.getCapability(Capabilities.Fluid.ITEM);
    if (itemHandler != null) {
      FluidInteractionResult result = FluidInteractionResult.CONTAINER;
      if (!world.isClientSide()) {
        // first, try filling the TE from the item
        FluidStack transferred = tryTransfer(itemHandler, teHandler, Integer.MAX_VALUE);
        if (!transferred.isEmpty()) {
          playEmptySound(world, pos, player, transferred);
          result = FluidInteractionResult.DRAINED_STACK;
        } else {
          // if that failed, try filling the item handler from the TE
          transferred = tryTransfer(teHandler, itemHandler, Integer.MAX_VALUE);
          if (!transferred.isEmpty()) {
            playFillSound(world, pos, player, transferred);
            result = FluidInteractionResult.FILLED_STACK;
          }
        }
        // if either worked, update the player's inventory
        if (!transferred.isEmpty()) {
          player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, itemAccess.getResource().toStack(1)));
        }
      }
      return result;
    }
    return FluidInteractionResult.MISSING;
  }

  /**
   * Utility to try fluid item then bucket.
   * @param world   World instance
   * @param pos     Tank position
   * @param player  Player instance
   * @param hand    Hand used
   * @param hit     Hit position
   * @return  True if interacted
   * @see #interactWithTank(Level, BlockPos, Player, InteractionHand, Direction, Direction)
   * @see #interactWithContainer(Level, BlockPos, Player, InteractionHand, BlockHitResult)
   */
  public static boolean interactWithTank(Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    Direction direction = hit.getDirection();
    return interactWithTank(world, pos, player, hand, direction, direction);
  }

  /**
   * Utility to try fluid item then bucket
   * @param world   World instance
   * @param pos     Tank position
   * @param player  Player instance
   * @param hand    Hand used
   * @param hit     Hit direction
   * @param offset  Offset to spawn the mob in the bucket, if present
   * @return  True if interacted
   * @see #interactWithTank(Level, BlockPos, Player, InteractionHand, BlockHitResult)
   * @see #interactWithContainer(Level, BlockPos, Player, InteractionHand, BlockHitResult)
   */
  public static boolean interactWithTank(Level world, BlockPos pos, Player player, InteractionHand hand, Direction hit, Direction offset) {
    if (!player.getItemInHand(hand).isEmpty()) {
      BlockEntity te = world.getBlockEntity(pos);
      if (te != null) {
        ResourceHandler<FluidResource> handler = world.getCapability(Capabilities.Fluid.BLOCK, pos, hit);
        if (handler != null) {
          // Try the generic container path first; if it actually moved fluid we are done.
          FluidInteractionResult container = interactWithContainer(world, pos, handler, player, hand);
          if (container.didTransfer()) {
            return true;
          }
          // The container path can report CONTAINER (an item fluid capability was present) without moving anything,
          // e.g. vanilla buckets whose item handler refuses the simulated drain. Always give the deterministic bucket
          // path a chance rather than letting '||' short-circuit on that non-transferring CONTAINER result.
          FluidInteractionResult bucket = interactWithFilledBucket(world, pos, handler, player, hand, offset);
          if (bucket.didTransfer()) {
            return true;
          }
          // neither moved fluid; treat the click as handled only if a container was actually present
          return container.hasContainer() || bucket.hasContainer();
        }
      }
    }
    return false;
  }

  /**
   * Attempts to transfer fluid from the passed stack into a tank.
   * @param teHandler  Tank handler
   * @param stack      Input stack, may be modified
   * @param direction  Determines whether we may empty the item, fill, or both
   * @return  Resulting stack after transfer
   */
  public static ItemStack interactWithTankSlot(ResourceHandler<FluidResource> teHandler, ItemStack stack, TransferDirection direction) {
    TransferResult result = interactWithStack(teHandler, stack, direction);
    return result != null ? result.stack() : ItemStack.EMPTY;
  }

  /**
   * Attempts to transfer fluid from the passed stack into a tank.
   * @param teHandler  Tank handler
   * @param stack      Input stack, may be modified
   * @param direction  Determines whether we may empty the item, fill, or both
   * @return  What was transferred and the resulting stack, or null if no transfer happened.
   */
  @Nullable
  public static TransferResult interactWithStack(ResourceHandler<FluidResource> teHandler, ItemStack stack, TransferDirection direction) {
    if (!stack.isEmpty()) {
      // fallback to JSON based transfer
      if (FluidContainerTransferManager.INSTANCE.mayHaveTransfer(stack)) {
        // only actually transfer on the serverside, client just has items
        FluidStack currentFluid = drainAny(teHandler, Integer.MAX_VALUE, false);
        IFluidContainerTransfer transfer = FluidContainerTransferManager.INSTANCE.getTransfer(stack, currentFluid);
        if (transfer != null) {
          TransferResult result = transfer.transfer(stack, currentFluid, teHandler, direction);
          if (result != null) {
            stack.shrink(1);
            return result;
          }
        }
      }

      // if the item has a capability, do a direct transfer
      ItemAccess itemAccess = ItemAccess.forStack(stack.copyWithCount(1));
      ResourceHandler<FluidResource> itemHandler = itemAccess.getCapability(Capabilities.Fluid.ITEM);
      if (itemHandler != null) {
        // first, try filling the TE from the item
        FluidStack transferred = FluidStack.EMPTY;
        // reverse means try TE to item first
        boolean didFill = true;
        if (direction == TransferDirection.REVERSE) {
          transferred = tryTransfer(teHandler, itemHandler, Integer.MAX_VALUE);
        }
        // if not reverse or reverse failed, try filling TE from item
        if (direction.canEmpty() && transferred.isEmpty()) {
          transferred = tryTransfer(itemHandler, teHandler, Integer.MAX_VALUE);
          if (!transferred.isEmpty()) {
            didFill = false;
          }
        }
        // if that failed, try filling the item handler from the TE
        if (direction != TransferDirection.REVERSE && direction.canFill() && transferred.isEmpty()) {
          transferred = tryTransfer(teHandler, itemHandler, Integer.MAX_VALUE);
        }
        // if either worked, update the player's inventory
        if (!transferred.isEmpty()) {
          stack.shrink(1);
          return new TransferResult(itemAccess.getResource().toStack(1), transferred, didFill);
        }
      }
    }
    return null;
  }

  /**
   * Attempts to transfer fluid into the passed stack from the given handler.
   * Similar to {@link #interactWithTankSlot(ResourceHandler, ItemStack, TransferDirection)} except filtered and unable to set direction.
   * @param teHandler  Tank handler
   * @param stack      Input stack, may be modified
   * @param fluid      Determines the fluid used to fill the item
   * @return  Resulting stack after transfer
   */
  public static ItemStack fillFromTankSlot(ResourceHandler<FluidResource> teHandler, ItemStack stack, FluidStack fluid) {
    TransferResult result = fillStack(teHandler, stack, fluid);
    return result != null ? result.stack() : ItemStack.EMPTY;
  }

  /**
   * Attempts to transfer fluid into the passed stack from the given handler.
   * Similar to {@link #interactWithTankSlot(ResourceHandler, ItemStack, TransferDirection)} except filtered and unable to set direction.
   * @param teHandler  Tank handler
   * @param stack      Input stack, may be modified
   * @param fluid      Determines the fluid used to fill the item
   * @return  Resulting stack after transfer
   */
  @Nullable
  public static TransferResult fillStack(ResourceHandler<FluidResource> teHandler, ItemStack stack, FluidStack fluid) {
    if (!stack.isEmpty()) {
      // fallback to JSON based transfer
      if (FluidContainerTransferManager.INSTANCE.mayHaveTransfer(stack)) {
        // only actually transfer on the serverside, client just has items
        IFluidContainerTransfer transfer = FluidContainerTransferManager.INSTANCE.getTransfer(stack, fluid);
        if (transfer != null) {
          TransferResult result = transfer.transfer(stack, fluid, teHandler, TransferDirection.FILL_ITEM);
          if (result != null) {
            stack.shrink(1);
            return result;
          }
        }
      }

      // if the item has a capability, do a direct transfer
      ItemAccess itemAccess = ItemAccess.forStack(stack.copyWithCount(1));
      ResourceHandler<FluidResource> itemHandler = itemAccess.getCapability(Capabilities.Fluid.ITEM);
      if (itemHandler != null) {
        // first, try filling the item from the TE
        FluidStack transferred = tryTransfer(teHandler, itemHandler, fluid.copy());
        if (!transferred.isEmpty()) {
          stack.shrink(1);
          return new TransferResult(itemAccess.getResource().toStack(1), transferred, true);
        }
      }
    }
    return null;
  }

  /**
   * Same as {@link net.minecraft.world.item.ItemUtils#createFilledResult(ItemStack, Player, ItemStack)} but doesn't shrink results or check creative.
   * Useful in UIs along {@link #interactWithTankSlot(ResourceHandler, ItemStack, TransferDirection)} or {@link #fillFromTankSlot(ResourceHandler, ItemStack, FluidStack)}
   */
  public static ItemStack getOrTransferFilled(Player player, ItemStack emptyStack, ItemStack filledStack) {
    // if no more helpd
    if (emptyStack.isEmpty()) {
      return filledStack;
    }
    if (!player.getInventory().add(filledStack)) {
      player.drop(filledStack, false);
    }
    return emptyStack;
  }

  /** Plays sound only to the targeted player. Works by sending a targeted packet to server players, or a local packet to client. */
  @SuppressWarnings("deprecation")
  public static void playUISound(Player player, SoundEvent sound) {
    if (player.level().isClientSide()) {
      player.playSound(sound);
    } else if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), player.getSoundSource(), player.getX(), player.getY(), player.getZ(), 1, 1, player.getRandom().nextLong()));
    }
  }

  /**
   * Combination of {@link #getOrTransferFilled(Player, ItemStack, ItemStack)} and {@link #playUISound(Player, SoundEvent)}.
   * For working with {@link #interactWithStack(ResourceHandler, ItemStack, TransferDirection)} and {@link #fillStack(ResourceHandler, ItemStack, FluidStack)} in UIs.
   */
  public static ItemStack handleUIResult(Player player, ItemStack emptyStack, @Nullable TransferResult result) {
    if (result == null) {
      return emptyStack;
    }
    playUISound(player, result.getSound());
    return getOrTransferFilled(player, emptyStack, result.stack());
  }
}
