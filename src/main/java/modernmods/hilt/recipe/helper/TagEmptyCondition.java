package modernmods.hilt.recipe.helper;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;

/** @deprecated use {@link modernmods.hilt.recipe.condition.TagEmptyCondition} */
@Deprecated(forRemoval = true)
public class TagEmptyCondition<T> extends modernmods.hilt.recipe.condition.TagEmptyCondition<T> {
  public TagEmptyCondition(TagKey<T> tag) {
    super(tag);
  }

  public TagEmptyCondition(ResourceKey<? extends Registry<T>> registry, Identifier name) {
    super(registry, name);
  }
}
