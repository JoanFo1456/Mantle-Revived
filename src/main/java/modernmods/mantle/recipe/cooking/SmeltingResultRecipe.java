package modernmods.mantle.recipe.cooking;

import lombok.Getter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import modernmods.mantle.data.loadable.common.IngredientLoadable;
import modernmods.mantle.data.loadable.field.LoadableField;
import modernmods.mantle.data.loadable.primitive.IntLoadable;
import modernmods.mantle.data.loadable.record.RecordLoadable;
import modernmods.mantle.recipe.MantleRecipes;
import modernmods.mantle.recipe.helper.ItemOutput;
import modernmods.mantle.recipe.helper.LoadableRecipeSerializer;

/** Extension of {@link SmeltingRecipe} to support {@link ItemOutput} */
@Getter
public class SmeltingResultRecipe extends SmeltingRecipe implements CookingResultRecipe {
  public static LoadableField<Integer, AbstractCookingRecipe> COOKING_TIME_FIELD = IntLoadable.FROM_ONE.defaultField("cooking_time", 200, true, AbstractCookingRecipe::cookingTime);
  public static final RecordLoadable<SmeltingResultRecipe> LOADABLE = RecordLoadable.create(
    LoadableRecipeSerializer.RECIPE_GROUP, CookingResultRecipe.CATEGORY_FIELD,
    IngredientLoadable.DISALLOW_EMPTY.requiredField("ingredient", AbstractCookingRecipe::input),
    RESULT_FIELD, EXPERIENCE_FIELD, COOKING_TIME_FIELD,
    SmeltingResultRecipe::new);

  private final ItemOutput result;
  @Override
  public ItemOutput getResult() {
    return result;
  }
  public SmeltingResultRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemOutput result, float experience, int cookingTime) {
    super(new Recipe.CommonInfo(false), new AbstractCookingRecipe.CookingBookInfo(category, group), ingredient, ItemStackTemplate.fromNonEmptyStack(result.get()), experience, cookingTime);
    this.result = result;
  }

  @SuppressWarnings("unchecked")
  @Override
  public RecipeSerializer<SmeltingRecipe> getSerializer() {
    return (RecipeSerializer<SmeltingRecipe>) (RecipeSerializer<?>) MantleRecipes.SMELTING.get();
  }

  @Override
  public ItemStack assemble(SingleRecipeInput input) {
    return result.copy();
  }
}
