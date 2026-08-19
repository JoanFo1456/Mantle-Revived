package modernmods.hilt.data.loadable.common;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Utility for building tag-based {@link Ingredient}s that resolve the tag lazily.
 * <p>
 * The vanilla {@code Ingredient.of(TagKey)} overload was removed in 26.1, and the common workaround
 * {@code Ingredient.of(registry.getOrThrow(tag))} resolves the tag eagerly at call time. When that happens while
 * loot/recipe data is decoded (before the reload binds tags) {@link net.minecraft.core.Registry#getOrThrow(TagKey)}
 * throws {@code "Missing tag"}. This wraps the tag in a {@link HolderSet} that does not touch the registry's tag
 * contents until first use. {@link Ingredient}'s constructor only calls {@link HolderSet#isImmediatelyResolvable()}
 * (which returns {@code false} here), so no tag access happens until the ingredient is actually tested/iterated/synced.
 */
public final class LazyTagIngredient {
  private LazyTagIngredient() {}

  /** Creates an {@link Ingredient} matching the given item tag, resolving the tag lazily on first use. */
  public static Ingredient of(TagKey<Item> tag) {
    return Ingredient.of(holderSet(tag));
  }

  /** Creates a lazily-resolving {@link HolderSet} for the given item tag. */
  public static HolderSet<Item> holderSet(TagKey<Item> tag) {
    return new LazyTagHolderSet(tag);
  }

  /** A {@link HolderSet} view over an item tag that defers resolving the registry-backed {@link HolderSet.Named}. */
  private static final class LazyTagHolderSet implements HolderSet<Item> {
    private final TagKey<Item> tag;
    private HolderSet.Named<Item> resolved;

    private LazyTagHolderSet(TagKey<Item> tag) {
      this.tag = tag;
    }

    private HolderSet.Named<Item> resolve() {
      HolderSet.Named<Item> set = this.resolved;
      if (set == null) {
        set = BuiltInRegistries.ITEM.getOrThrow(this.tag);
        this.resolved = set;
      }
      return set;
    }

    @Override
    public boolean isImmediatelyResolvable() {
      return false;
    }

    @Override
    public Optional<TagKey<Item>> unwrapKey() {
      return Optional.of(this.tag);
    }

    @Override
    public Either<TagKey<Item>, List<Holder<Item>>> unwrap() {
      return Either.left(this.tag);
    }

    @Override
    public boolean isBound() {
      return this.resolved != null && this.resolved.isBound();
    }

    @Override
    public Stream<Holder<Item>> stream() {
      return resolve().stream();
    }

    @Override
    public int size() {
      return resolve().size();
    }

    @Override
    public java.util.Iterator<Holder<Item>> iterator() {
      return resolve().iterator();
    }

    @Override
    public Optional<Holder<Item>> getRandomElement(RandomSource random) {
      return resolve().getRandomElement(random);
    }

    @Override
    public Holder<Item> get(int index) {
      return resolve().get(index);
    }

    @Override
    public boolean contains(Holder<Item> value) {
      return resolve().contains(value);
    }

    @Override
    public boolean canSerializeIn(HolderOwner<Item> owner) {
      // a tag serializes as a name reference (via unwrapKey), so do NOT resolve contents here -- resolving during
      // serialization throws "Missing tag" at datagen time (tags not yet bound). Serialization is always valid.
      return true;
    }

    @Override
    public void addInvalidationListener(Runnable runnable) {
      resolve().addInvalidationListener(runnable);
    }

    @Override
    public boolean equals(Object o) {
      return this == o || (o instanceof LazyTagHolderSet other && this.tag.equals(other.tag));
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(this.tag);
    }

    @Override
    public String toString() {
      return "LazyTagHolderSet(" + this.tag + ")";
    }
  }
}
