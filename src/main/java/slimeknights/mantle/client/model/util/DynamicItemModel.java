package slimeknights.mantle.client.model.util;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Reusable base for an {@link ItemModel} that resolves the rendered model dynamically per {@link ItemStack}, replacing the
 * removed {@code ItemOverrides} system.
 * <p>
 * In 26.1 an item resolves its geometry through {@link ItemModel#update} rather than {@code ItemOverrides#resolve} on a
 * {@code BakedModel}. This base derives a cache key from the stack (typically material/fluid ids stored in NBT), lazily
 * bakes an {@link ItemModel} for that key using the {@link ItemModel.BakingContext} captured at bake time, caches it, and
 * delegates {@link ItemModel#update} to it. Subclasses implement {@link #getCacheKey(ItemStack)} and
 * {@link #bakeModel(Object)}.
 * <p>
 * Lazy baking during {@link #update} reuses the captured baking context, matching how the pre-26.1 override system rebaked
 * on demand; the exact model-baker lifecycle across reloads should be validated visually in-game.
 * @param <K>  Cache key type (e.g. a material id list)
 */
public abstract class DynamicItemModel<K> implements ItemModel {
  /** Item model that renders nothing, used as the default fallback */
  protected static final ItemModel EMPTY = (output, item, resolver, displayContext, level, owner, seed) -> {};
  /** Baking context captured at bake time, used to bake variants lazily */
  protected final ItemModel.BakingContext context;
  /** Root transform to compose with baked children */
  protected final Matrix4fc transform;
  /** Cache of key to baked model */
  private final Map<K, ItemModel> cache = new ConcurrentHashMap<>();
  private final Function<K, ItemModel> baker = this::bakeModel;

  protected DynamicItemModel(ItemModel.BakingContext context, Matrix4fc transform) {
    this.context = context;
    this.transform = transform;
  }

  /** Derives the cache key for the given stack, e.g. the material list read from NBT. Return null to render the fallback. */
  @Nullable
  protected abstract K getCacheKey(ItemStack stack);

  /** Bakes the item model for the given key using {@link #context} and {@link #transform}. */
  protected abstract ItemModel bakeModel(K key);

  /** Model to render when the stack has no key (e.g. missing NBT); defaults to an empty render state. */
  protected ItemModel getFallback() {
    return EMPTY;
  }

  /** Gets the cached model for the key, baking it if needed */
  protected ItemModel getModel(K key) {
    return cache.computeIfAbsent(key, baker);
  }

  @Override
  public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
    output.appendModelIdentityElement(this);
    K key = getCacheKey(item);
    ItemModel model = key == null ? getFallback() : getModel(key);
    model.update(output, item, resolver, displayContext, level, owner, seed);
  }
}
