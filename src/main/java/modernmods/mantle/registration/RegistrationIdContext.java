package slimeknights.mantle.registration;

import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Thread local holding the registry name currently being constructed through one of Mantle's deferred registers.
 * <p>
 * As of Minecraft 26.1, {@link net.minecraft.world.level.block.state.BlockBehaviour.Properties} and
 * {@link net.minecraft.world.item.Item.Properties} carry a mandatory registry id that is dereferenced eagerly in the
 * block/item constructor (via {@code effectiveDrops()} / {@code effectiveDescriptionId()}). Mantle registers most
 * blocks and items through opaque {@link java.util.function.Supplier} lambdas that bake the properties inside, so the
 * id cannot be set on the properties before construction from the register itself.
 * <p>
 * To keep the existing register API (and every downstream call site) unchanged, the deferred register pushes the
 * registration name here for the duration of the supplier invocation, and the block/item constructor mixins read it
 * back to assign the id before the eager access happens.
 */
public final class RegistrationIdContext {
  private RegistrationIdContext() {}

  private static final ThreadLocal<Deque<Identifier>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

  /** Marks the given id as the one currently being constructed. Must be paired with {@link #pop()}. */
  public static void push(Identifier id) {
    STACK.get().push(id);
  }

  /** Clears the most recently pushed id */
  public static void pop() {
    Deque<Identifier> deque = STACK.get();
    if (!deque.isEmpty()) {
      deque.pop();
    }
    if (deque.isEmpty()) {
      STACK.remove();
    }
  }

  /** Gets the id currently being constructed, or null if construction is not happening through a Mantle register */
  @Nullable
  public static Identifier current() {
    return STACK.get().peek();
  }
}
