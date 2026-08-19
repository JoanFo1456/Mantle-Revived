package modernmods.hilt.data.predicate;

import modernmods.hilt.data.loadable.record.RecordLoadable;
import modernmods.hilt.data.registry.GenericLoaderRegistry.IHaveLoader;

/** Generic interface for predicate based JSON loaders */
public interface IJsonPredicate<I> extends IHaveLoader {
  /** Returns true if this json predicate matches the given input */
  boolean matches(I input);

  /** Inverts the given predicate */
  IJsonPredicate<I> inverted();

  @Override
  RecordLoadable<? extends IJsonPredicate<I>> getLoader();
}
