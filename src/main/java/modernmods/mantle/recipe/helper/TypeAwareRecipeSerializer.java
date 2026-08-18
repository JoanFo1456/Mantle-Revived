package modernmods.mantle.recipe.helper;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

/** Type aware recipe serializer, for the sake of recipes that serializers to swap out the type. */
public interface TypeAwareRecipeSerializer<T extends Recipe<?>> extends LoggingRecipeSerializer<T> {
  /** Gets the type of this recipe */
  RecipeType<?> getType();
}
