package slimeknights.mantle.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import slimeknights.mantle.block.entity.MantleBlockEntity;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

/**
 * Item handler containing exactly one item.
 *
 * <p>In MC 26.1 this is backed by the {@link net.neoforged.neoforge.transfer.ResourceHandler} API (via
 * {@link ItemStacksResourceHandler}) so instances can be registered as {@code ResourceHandler<ItemResource>} capabilities,
 * while still implementing the legacy {@link IItemHandlerModifiable} so existing callers keep working unchanged.
 */
@SuppressWarnings("unused")
public abstract class SingleItemHandler<T extends MantleBlockEntity> extends ItemStacksResourceHandler implements IItemHandlerModifiable {
  protected final T parent;
  private final int maxStackSize;

  protected SingleItemHandler(T parent, int maxStackSize) {
    super(1);
    this.parent = parent;
    this.maxStackSize = maxStackSize;
  }

  /** Gets the current item in this slot */
  @Nonnull
  public ItemStack getStack() {
    return this.stacks.get(0);
  }

  /**
   * Sets the stack in this duct
   * @param newStack  New stack
   */
  public void setStack(ItemStack newStack) {
    this.stacks.set(0, newStack);
    parent.setChangedFast();
  }

  /**
   * Checks if the given stack is valid for this slot
   * @param stack  Stack
   * @return  True if valid
   */
  protected abstract boolean isItemValid(ItemStack stack);


  /* ResourceHandler override points */

  @Override
  public boolean isValid(int index, ItemResource resource) {
    return index == 0 && isItemValid(resource.toStack(1));
  }

  @Override
  protected int getCapacity(int index, ItemResource resource) {
    return maxStackSize;
  }

  @Override
  protected void onContentsChanged(int index, ItemStack previousContents) {
    parent.setChangedFast();
  }


  /* Legacy IItemHandlerModifiable properties */

  @Override
  public boolean isItemValid(int slot, ItemStack stack) {
    return slot == 0 && isItemValid(stack);
  }

  @Override
  public int getSlots() {
    return 1;
  }

  @Override
  public int getSlotLimit(int slot) {
    return maxStackSize;
  }

  @Nonnull
  @Override
  public ItemStack getStackInSlot(int slot) {
    if (slot == 0) {
      return getStack();
    }
    return ItemStack.EMPTY;
  }


  /* Interaction */

  @Override
  public void setStackInSlot(int slot, ItemStack stack) {
    if (slot == 0) {
      setStack(stack);
    }
  }

  @Nonnull
  @Override
  public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
    if (stack.isEmpty()) {
      return ItemStack.EMPTY;
    }
    if (slot != 0) {
      return stack;
    }
    try (Transaction tx = Transaction.openRoot()) {
      int inserted = insert(0, ItemResource.of(stack), stack.getCount(), tx);
      if (!simulate) {
        tx.commit();
      }
      return stack.copyWithCount(stack.getCount() - inserted);
    }
  }

  @Nonnull
  @Override
  public ItemStack extractItem(int slot, int amount, boolean simulate) {
    if (amount <= 0 || slot != 0) {
      return ItemStack.EMPTY;
    }
    ItemStack current = getStack();
    if (current.isEmpty()) {
      return ItemStack.EMPTY;
    }
    try (Transaction tx = Transaction.openRoot()) {
      int extracted = extract(0, ItemResource.of(current), amount, tx);
      if (!simulate) {
        tx.commit();
      }
      return extracted > 0 ? current.copyWithCount(extracted) : ItemStack.EMPTY;
    }
  }

  /**
   * Writes this module to NBT
   * @return  Module in NBT
   */
  public CompoundTag writeToNBT() {
    CompoundTag nbt = new CompoundTag();
    ItemStack stack = getStack();
    if (!stack.isEmpty()) {
      HolderLookup.Provider registries = parent.getLevel() != null ? parent.getLevel().registryAccess() : HolderLookup.Provider.create(Stream.empty());
      Tag saved = ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
      nbt = (CompoundTag) saved;
    }
    return nbt;
  }

  /**
   * Reads this module from NBT
   * @param nbt  NBT
   */
  public void readFromNBT(CompoundTag nbt) {
    HolderLookup.Provider registries = parent.getLevel() != null ? parent.getLevel().registryAccess() : HolderLookup.Provider.create(Stream.empty());
    this.stacks.set(0, ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), nbt).result().orElse(ItemStack.EMPTY));
  }
}
