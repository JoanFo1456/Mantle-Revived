package modernmods.hilt.data.loadable.common;

import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.CommonHooks;
import modernmods.hilt.data.loadable.field.ContextKey;
import modernmods.hilt.data.loadable.primitive.ResourceLocationLoadable;
import modernmods.hilt.util.typed.TypedMap;

/** Loadable for dynamic registries that are only available through a runtime lookup. */
public record DynamicRegistryLoadable<T>(ResourceKey<? extends Registry<T>> registryKey) implements ResourceLocationLoadable<T> {
  /** Gets the active lookup for this registry. */
  private HolderLookup.RegistryLookup<T> lookup(String key, TypedMap context) {
    HolderLookup.Provider provider = context.get(ContextKey.REGISTRY_ACCESS);
    HolderLookup.RegistryLookup<T> lookup = provider == null ? CommonHooks.resolveLookup(registryKey) : provider.lookup(registryKey).orElse(null);
    if (lookup == null) {
      throw new JsonSyntaxException("Unable to parse " + key + " as registry " + registryKey.identifier() + " cannot be located");
    }
    return lookup;
  }

  @Override
  public T fromKey(Identifier name, String key, TypedMap context) {
    return lookup(key, context).get(ResourceKey.create(registryKey, name))
      .map(holder -> holder.value())
      .orElseThrow(() -> new JsonSyntaxException("Unable to parse " + key + " as registry " + registryKey.identifier() + " does not contain ID " + name));
  }

  @Override
  public Identifier getKey(T object) {
    return lookup("value", TypedMap.EMPTY).listElements()
      .filter(holder -> holder.value() == object)
      .map(holder -> holder.key().identifier())
      .findFirst()
      .orElseThrow(() -> new EncoderException("Registry " + registryKey.identifier() + " does not contain object " + object));
  }

  @Override
  public T decode(FriendlyByteBuf buffer, TypedMap context) {
    Identifier name = buffer.readIdentifier();
    // The decode context is typically empty on the network thread, and dynamic registries (e.g.
    // minecraft:enchantment) cannot be resolved via CommonHooks there - it only sees static/built-in
    // registries, so the lookup fails while joining a world. Hilt packets use RegistryFriendlyByteBuf,
    // which carries the connection's registry access; prefer that to resolve the registry.
    HolderLookup.Provider provider = context.get(ContextKey.REGISTRY_ACCESS);
    if (provider == null && buffer instanceof RegistryFriendlyByteBuf registryBuffer) {
      provider = registryBuffer.registryAccess();
    }
    try {
      HolderLookup.RegistryLookup<T> lookup = provider == null
        ? CommonHooks.resolveLookup(registryKey)
        : provider.lookup(registryKey).orElse(null);
      if (lookup == null) {
        throw new JsonSyntaxException("Unable to parse packet as registry " + registryKey.identifier() + " cannot be located");
      }
      return lookup.get(ResourceKey.create(registryKey, name))
        .map(Holder::value)
        .orElseThrow(() -> new JsonSyntaxException("Unable to parse packet as registry " + registryKey.identifier() + " does not contain ID " + name));
    } catch (JsonSyntaxException e) {
      throw new DecoderException(e);
    }
  }

  @Override
  public void encode(FriendlyByteBuf buffer, T object) {
    buffer.writeIdentifier(getKey(object));
  }
}
