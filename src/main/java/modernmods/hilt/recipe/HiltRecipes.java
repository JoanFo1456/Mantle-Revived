package modernmods.hilt.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import modernmods.hilt.Hilt;
import modernmods.hilt.recipe.cooking.BlastingResultRecipe;
import modernmods.hilt.recipe.cooking.CampfireResultRecipe;
import modernmods.hilt.recipe.cooking.SmeltingResultRecipe;
import modernmods.hilt.recipe.cooking.SmokingResultRecipe;
import modernmods.hilt.recipe.crafting.ShapedFallbackRecipe;
import modernmods.hilt.recipe.crafting.ShapedRetexturedRecipe;
import modernmods.hilt.recipe.helper.LoadableRecipeSerializer;
import modernmods.hilt.recipe.ingredient.FluidContainerIngredient;
import modernmods.hilt.recipe.ingredient.PotionDisplayIngredient;
import modernmods.hilt.recipe.ingredient.PotionIngredient;

/** Handles any custom recipes added by Hilt */
public class HiltRecipes {
  private static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Hilt.modId);
  private static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, Hilt.modId);

  private HiltRecipes() {}

  /** Registers this to the bus */
  public static void init(IEventBus bus) {
    RECIPES.register(bus);
    INGREDIENT_TYPES.register(bus);
  }

  // crafting
  public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<ShapedFallbackRecipe>> CRAFTING_SHAPED_FALLBACK = RECIPES.register("crafting_shaped_fallback", () -> ShapedFallbackRecipe.SERIALIZER);
  public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<ShapedRetexturedRecipe>> CRAFTING_SHAPED_RETEXTURED = RECIPES.register("crafting_shaped_retextured", () -> ShapedRetexturedRecipe.SERIALIZER);
  // cooking
  public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<SmeltingResultRecipe>> SMELTING = RECIPES.register("smelting", () -> LoadableRecipeSerializer.of(SmeltingResultRecipe.LOADABLE));
  public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<BlastingResultRecipe>> BLASTING = RECIPES.register("blasting", () -> LoadableRecipeSerializer.of(BlastingResultRecipe.LOADABLE));
  public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<SmokingResultRecipe>> SMOKING = RECIPES.register("smoking", () -> LoadableRecipeSerializer.of(SmokingResultRecipe.LOADABLE));
  public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<CampfireResultRecipe>> CAMPFIRE = RECIPES.register("campfire", () -> LoadableRecipeSerializer.of(CampfireResultRecipe.LOADABLE));

  // ingredients
  public static final DeferredHolder<IngredientType<?>,IngredientType<PotionIngredient>> POTION_INGREDIENT = INGREDIENT_TYPES.register("potion", () -> new IngredientType<>(PotionIngredient.SERIALIZER.codec(), PotionIngredient.SERIALIZER.streamCodec()));
  public static final DeferredHolder<IngredientType<?>,IngredientType<PotionDisplayIngredient>> POTION_DISPLAY_INGREDIENT = INGREDIENT_TYPES.register("potion_display", () -> new IngredientType<>(PotionDisplayIngredient.SERIALIZER.codec(), PotionDisplayIngredient.SERIALIZER.streamCodec()));
  public static final DeferredHolder<IngredientType<?>,IngredientType<FluidContainerIngredient>> FLUID_CONTAINER_INGREDIENT = INGREDIENT_TYPES.register("fluid_container", () -> new IngredientType<>(FluidContainerIngredient.SERIALIZER.codec(), FluidContainerIngredient.SERIALIZER.streamCodec()));
}
