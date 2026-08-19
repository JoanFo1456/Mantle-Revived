package modernmods.hilt.recipe.cooking;

import lombok.Getter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmokingRecipe;
import modernmods.hilt.data.loadable.common.IngredientLoadable;
import modernmods.hilt.data.loadable.field.LoadableField;
import modernmods.hilt.data.loadable.primitive.IntLoadable;
import modernmods.hilt.data.loadable.record.RecordLoadable;
import modernmods.hilt.recipe.HiltRecipes;
import modernmods.hilt.recipe.helper.ItemOutput;
import modernmods.hilt.recipe.helper.LoadableRecipeSerializer;

/** Extension of {@link SmokingRecipe} to support {@link ItemOutput} */
@Getter
public class SmokingResultRecipe extends SmokingRecipe implements CookingResultRecipe {
  public static LoadableField<Integer, AbstractCookingRecipe> COOKING_TIME_FIELD = IntLoadable.FROM_ONE.defaultField("cooking_time", 100, true, AbstractCookingRecipe::cookingTime);
  public static final RecordLoadable<SmokingResultRecipe> LOADABLE = RecordLoadable.create(
    LoadableRecipeSerializer.RECIPE_GROUP, CookingResultRecipe.CATEGORY_FIELD,
    IngredientLoadable.DISALLOW_EMPTY.requiredField("ingredient", AbstractCookingRecipe::input),
    RESULT_FIELD, EXPERIENCE_FIELD, COOKING_TIME_FIELD,
    SmokingResultRecipe::new);

  private final ItemOutput result;
  @Override
  public ItemOutput getResult() {
    return result;
  }
  public SmokingResultRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemOutput result, float experience, int cookingTime) {
    super(new Recipe.CommonInfo(false), new AbstractCookingRecipe.CookingBookInfo(category, group), ingredient, ItemStackTemplate.fromNonEmptyStack(result.get()), experience, cookingTime);
    this.result = result;
  }

  @SuppressWarnings("unchecked")
  @Override
  public RecipeSerializer<SmokingRecipe> getSerializer() {
    return (RecipeSerializer<SmokingRecipe>) (RecipeSerializer<?>) HiltRecipes.SMOKING.get();
  }

  @Override
  public ItemStack assemble(SingleRecipeInput input) {
    return result.copy();
  }
}
