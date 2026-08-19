package modernmods.hilt.recipe;

import net.minecraft.world.item.crafting.RecipeSerializer;
import modernmods.hilt.Hilt;

import static modernmods.hilt.registration.RegistrationHelper.injected;

/** @deprecated use {@link HiltRecipes} */
@Deprecated(forRemoval = true)
public class HiltRecipeSerializers {
  private HiltRecipeSerializers() {}

  /** @deprecated use {@link HiltRecipes#CRAFTING_SHAPED_FALLBACK} */
  @Deprecated(forRemoval = true)
  public static final RecipeSerializer<?> CRAFTING_SHAPED_FALLBACK = injected();
  /** @deprecated use {@link HiltRecipes#CRAFTING_SHAPED_RETEXTURED} */
  @Deprecated(forRemoval = true)
  public static final RecipeSerializer<?> CRAFTING_SHAPED_RETEXTURED = injected();
}
