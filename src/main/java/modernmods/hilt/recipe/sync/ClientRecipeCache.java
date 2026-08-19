package modernmods.hilt.recipe.sync;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

import java.util.List;

/**
 * Client-side store of the recipes synced from the server via {@link RecipeSyncPacket}.
 * <p>
 * Populated on player join and on {@code /reload} (see {@link RecipeSyncHandler}); cleared when the
 * client disconnects. The recipes it holds are exactly those whose {@link net.minecraft.world.item.crafting.RecipeType}
 * a mod opted into through {@link SyncableRecipes}. Query the cache through the {@code RecipeMap}
 * overloads on {@link modernmods.hilt.recipe.helper.RecipeHelper}, for example:
 * <pre>{@code
 * RecipeMap map = ClientRecipeCache.getRecipeMap();
 * List<MeltingRecipe> recipes = RecipeHelper.getRecipes(map, MyRecipeTypes.MELTING.get(), MeltingRecipe.class);
 * }</pre>
 * Note that simply receiving the sync also re-runs each recipe's constructor client-side (recipe
 * decoding invokes the registered serializer's stream codec), so any static caches those constructors
 * populate are (re)populated as a side effect.
 */
public final class ClientRecipeCache {
  private ClientRecipeCache() {}

  /** Written and read on the client main thread; volatile guarantees visibility across the network-thread hop. */
  private static volatile RecipeMap map = RecipeMap.EMPTY;

  /**
   * Gets the current client recipe map. Never null; returns {@link RecipeMap#EMPTY} before the first
   * sync arrives, so callers need no null guard.
   * @return  Client-side recipe map of the synced types
   */
  public static RecipeMap getRecipeMap() {
    return map;
  }

  /**
   * Rebuilds the cache from a freshly decoded recipe list. Called by {@link RecipeSyncPacket} on the
   * client thread when a sync payload arrives.
   * @param recipes  Decoded recipe holders
   */
  static void receive(List<RecipeHolder<?>> recipes) {
    // RecipeMap.create builds a brand-new immutable map, replacing any prior sync wholesale.
    // Deserialization already happened during decode, so consumer static caches are repopulated by now.
    map = RecipeMap.create(recipes);
  }

  /**
   * Clears the cache. Called on client disconnect so a subsequent world/server join starts fresh.
   */
  public static void clear() {
    map = RecipeMap.EMPTY;
  }
}
