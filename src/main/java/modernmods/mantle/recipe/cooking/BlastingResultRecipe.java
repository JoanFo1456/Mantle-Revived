package slimeknights.mantle.recipe.cooking;

import lombok.Getter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.MantleRecipes;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;

/** Extension of {@link BlastingRecipe} to support {@link ItemOutput} */
@Getter
public class BlastingResultRecipe extends BlastingRecipe implements CookingResultRecipe {
  public static LoadableField<Integer, AbstractCookingRecipe> COOKING_TIME_FIELD = IntLoadable.FROM_ONE.defaultField("cooking_time", 100, true, AbstractCookingRecipe::cookingTime);
  public static final RecordLoadable<BlastingResultRecipe> LOADABLE = RecordLoadable.create(
    LoadableRecipeSerializer.RECIPE_GROUP, CookingResultRecipe.CATEGORY_FIELD,
    IngredientLoadable.DISALLOW_EMPTY.requiredField("ingredient", AbstractCookingRecipe::input),
    RESULT_FIELD, EXPERIENCE_FIELD, COOKING_TIME_FIELD,
    BlastingResultRecipe::new);

  private final ItemOutput result;
  @Override
  public ItemOutput getResult() {
    return result;
  }
  public BlastingResultRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemOutput result, float experience, int cookingTime) {
    super(new Recipe.CommonInfo(false), new AbstractCookingRecipe.CookingBookInfo(category, group), ingredient, ItemStackTemplate.fromNonEmptyStack(result.get()), experience, cookingTime);
    this.result = result;
  }

  @SuppressWarnings("unchecked")
  @Override
  public RecipeSerializer<BlastingRecipe> getSerializer() {
    return (RecipeSerializer<BlastingRecipe>) (RecipeSerializer<?>) MantleRecipes.BLASTING.get();
  }

  @Override
  public ItemStack assemble(SingleRecipeInput input) {
    return result.copy();
  }
}
