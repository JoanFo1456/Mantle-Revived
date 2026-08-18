package modernmods.mantle.data.predicate;

import modernmods.mantle.data.loadable.record.RecordLoadable;
import modernmods.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;

/** Generic interface for predicate based JSON loaders */
public interface IJsonPredicate<I> extends IHaveLoader {
  /** Returns true if this json predicate matches the given input */
  boolean matches(I input);

  /** Inverts the given predicate */
  IJsonPredicate<I> inverted();

  @Override
  RecordLoadable<? extends IJsonPredicate<I>> getLoader();
}
