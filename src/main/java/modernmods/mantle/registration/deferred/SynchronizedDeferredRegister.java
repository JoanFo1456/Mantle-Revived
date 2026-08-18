package modernmods.mantle.registration.deferred;

import lombok.RequiredArgsConstructor;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import modernmods.mantle.registration.RegistrationIdContext;

import java.util.function.Supplier;

/** Deferred register instance that synchronizes register calls */
@RequiredArgsConstructor(staticName = "create")
public class SynchronizedDeferredRegister<T> {
  private final DeferredRegister<T> internal;

  /** Creates a new instance for the given resource key */
  public static <T> SynchronizedDeferredRegister<T> create(ResourceKey<? extends Registry<T>> key, String modid) {
    return create(DeferredRegister.create(key, modid));
  }

  /**
   * Registers the given object, synchronized over the internal register.
   * <p>
   * Registers through the key aware form so the registration id is exposed via {@link RegistrationIdContext} while the
   * supplier runs. As of Minecraft 26.1, block and item constructors eagerly require the id on their properties, which
   * the supplier bakes in and thus cannot be set from here; the block/item constructor mixins read the context back to
   * assign the id before that eager access.
   */
  public <I extends T> DeferredHolder<T,I> register(final String name, final Supplier<? extends I> sup) {
    synchronized (internal) {
      return internal.register(name, id -> {
        RegistrationIdContext.push(id);
        try {
          return sup.get();
        } finally {
          RegistrationIdContext.pop();
        }
      });
    }
  }

  /**
   * Registers the internal register with the event bus
   */
  public void register(IEventBus bus) {
    internal.register(bus);
  }
}
