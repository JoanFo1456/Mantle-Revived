package slimeknights.mantle.plugin.jei;

import com.google.common.collect.Streams;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.recipe.crafting.ShapedRetexturedRecipe;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * JEI crafting extension to properly show, animate, and focus {@link ShapedRetexturedRecipe} instances.
 * <p>26.1.2 reworked {@link ICraftingCategoryExtension} to be stateless, passing a {@link RecipeHolder} to each
 * method rather than storing a single recipe, so this no longer caches per-recipe data.
 */
public class RetexturableRecipeExtension implements ICraftingCategoryExtension<ShapedRetexturedRecipe> {
  @Override
  public List<SlotDisplay> getIngredients(RecipeHolder<ShapedRetexturedRecipe> holder) {
    return holder.value().getBase().pattern.ingredients().stream()
                 .map(Ingredient::optionalIngredientToDisplay)
                 .toList();
  }

  @Override
  public int getWidth(RecipeHolder<ShapedRetexturedRecipe> holder) {
    return holder.value().getBase().getWidth();
  }

  @Override
  public int getHeight(RecipeHolder<ShapedRetexturedRecipe> holder) {
    return holder.value().getBase().getHeight();
  }

  @Override
  public void setRecipe(RecipeHolder<ShapedRetexturedRecipe> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
    ShapedRetexturedRecipe recipe = holder.value();
    int width = recipe.getBase().getWidth();
    int height = recipe.getBase().getHeight();

    // set the output to display all variants from the texture ingredient
    // fetch all stacks from the ingredient, note any variants that are not blocks will get a blank look
    List<ItemStack> displayOutputs = recipe.getTexture().items()
                                           .map(item -> recipe.getResultItem(item.value()))
                                           .filter(stack -> !stack.isEmpty())
                                           .toList();
    // empty display means the tag found nothing, so just use the original output
    if (displayOutputs.isEmpty()) {
      displayOutputs = List.of(recipe.getResult());
    }

    // we need the blank version for the sake of recipe lookup due to the subtype interpreter making it not the same
    builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(recipe.getResult());

    // add the itemstacks to the grid
    List<Optional<Ingredient>> ingredients = recipe.getBase().pattern.ingredients();
    List<List<ItemStack>> inputStacks = ingredients.stream()
                                                   .map(RetexturableRecipeExtension::ingredientStacks)
                                                   .toList();
    List<IRecipeSlotBuilder> inputs = craftingGridHelper.createAndSetInputs(builder, VanillaTypes.ITEM_STACK, inputStacks, width, height);
    IRecipeSlotBuilder output = craftingGridHelper.createAndSetOutputs(builder, displayOutputs);

    // find out which inputs match the texture, we will need to use those for the focus link
    Ingredient texture = recipe.getTexture();
    int[] textureSlots = IntStream.range(0, ingredients.size())
                                  .filter(i -> ingredients.get(i).map(ing -> ingredientsMatch(texture, ing)).orElse(false))
                                  .toArray();

    if (inputs.size() != 9) {
      Mantle.logger.error("Failed to create focus link for {} as the layout {} is not 3x3", holder.id(), builder.getClass().getName());
    } else {
      // link the output to all inputs that match the texture
      builder.createFocusLink(Streams.concat(Stream.of(output), IntStream.of(textureSlots).mapToObj(i -> inputs.get(MantleJEIConstants.getCraftingIndex(i, width, height)))).toArray(IRecipeSlotBuilder[]::new));
    }
  }

  /** Gets the list of display stacks matching an optional ingredient */
  private static List<ItemStack> ingredientStacks(Optional<Ingredient> ingredient) {
    return ingredient.map(ing -> ing.items().map(ItemStack::new).toList()).orElseGet(List::of);
  }

  /** Checks if two ingredients match based on their display items */
  private static boolean ingredientsMatch(Ingredient left, Ingredient right) {
    List<ItemStack> leftStacks = left.items().map(ItemStack::new).toList();
    List<ItemStack> rightStacks = right.items().map(ItemStack::new).toList();
    if (leftStacks.size() != rightStacks.size()) {
      return false;
    }
    for (int i = 0; i < leftStacks.size(); i++) {
      if (!ItemStack.isSameItemSameComponents(leftStacks.get(i), rightStacks.get(i))) {
        return false;
      }
    }
    return true;
  }
}
