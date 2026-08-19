package modernmods.hilt.util;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/** Condition context to use when data has already been loaded, used in books for processing their conditions for instance. */
public enum DataLoadedConditionContext implements ICondition.IContext {
  INSTANCE;

  @Override
  public <T> boolean isTagLoaded(TagKey<T> key) {
    Registry<T> registry = RegistryHelper.getRegistry(key.registry());
    return registry != null && registry.get(key).isPresent();
  }

  @Override
  public <T> Collection<Holder<T>> getTag(TagKey<T> key) {
    Registry<T> registry = RegistryHelper.getRegistry(key.registry());
    if (registry != null) {
      Optional<HolderSet.Named<T>> tag = registry.get(key);
      if (tag.isPresent()) {
        return tag.get().stream().toList();
      }
    }
    return Set.of();
  }
}
