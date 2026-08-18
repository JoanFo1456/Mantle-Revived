package modernmods.mantle.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * Recipe that has an output other than an {@link ItemStack}
 * @param <C>  Inventory type
 */
public interface ICustomOutputRecipe<C extends RecipeInput> extends ICommonRecipe<C> {
  // assemble(C) defaulting to empty is inherited from ICommonRecipe.
  // getResultItem was removed from Recipe in 26.1.2, so there is nothing to override here.
}
