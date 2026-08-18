package modernmods.mantle.data.loadable.common;

import com.google.gson.JsonSyntaxException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import modernmods.mantle.data.loadable.Loadable;
import modernmods.mantle.data.loadable.mapping.EnumMapLoadable;
import modernmods.mantle.data.loadable.primitive.ResourceLocationLoadable;
import modernmods.mantle.util.typed.TypedMap;

import java.util.Map;

/** Special loadable for display contexts due to the Forge weirdness in {@link ItemDisplayContext} */
public enum DisplayContextLoadable implements ResourceLocationLoadable<ItemDisplayContext> {
  INSTANCE;

  @Override
  public ItemDisplayContext fromKey(Identifier name, String key, TypedMap context) {
    for (ItemDisplayContext value : ItemDisplayContext.values()) {
      if (name.getPath().equals(value.getSerializedName())) {
        return value;
      }
    }
    throw new JsonSyntaxException("Unable to parse " + key + " as an ItemDisplayContext: " + name);
  }

  @Override
  public Identifier getKey(ItemDisplayContext object) {
    return Identifier.withDefaultNamespace(object.getSerializedName());
  }

  @Override
  public ItemDisplayContext decode(FriendlyByteBuf buffer, TypedMap context) {
    return buffer.readEnum(ItemDisplayContext.class);
  }

  @Override
  public void encode(FriendlyByteBuf buffer, ItemDisplayContext value) {
    buffer.writeEnum(value);
  }

  @Override
  public <V> Loadable<Map<ItemDisplayContext,V>> mapWithValues(Loadable<V> valueLoadable, int minSize) {
    return new EnumMapLoadable<>(ItemDisplayContext.class, this, valueLoadable, minSize);
  }
}
