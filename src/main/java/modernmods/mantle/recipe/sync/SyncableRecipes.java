package modernmods.mantle.recipe.sync;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Public opt-in registry of {@link RecipeType}s that Mantle syncs from the server to the client.
 * <p>
 * In Minecraft 26.1 the vanilla {@code RecipeManager} is server-only and custom recipe types are no
 * longer sent to the client. Mods that need their custom recipes available client-side (for JEI
 * integration, in-book recipe display, etc.) opt those types into this registry. On player join and
 * on {@code /reload}, Mantle collects every recipe of a registered type and ships it to the client,
 * where it is decoded into {@link ClientRecipeCache#getRecipeMap()} and can be queried through the
 * {@code RecipeMap} overloads on {@link modernmods.mantle.recipe.helper.RecipeHelper}.
 * <p>
 * <b>This registry is empty by default.</b> Mantle deliberately does not sync any vanilla recipe type
 * (JEI and vanilla already provide those client-side, and syncing them would inflate the packet).
 * Consumers register exactly the custom types they need and nothing more, keeping the sync payload
 * minimal. A consumer may also opt in a single specific type (even a vanilla one such as
 * {@link RecipeType#CRAFTING}) if it genuinely needs that type outside of JEI.
 * <p>
 * Register during common setup (e.g. from {@code FMLCommonSetupEvent}, via {@code enqueueWork} for
 * thread safety). Example:
 * <pre>{@code
 * event.enqueueWork(() -> {
 *   SyncableRecipes.register(MyRecipeTypes.MELTING.get());
 *   SyncableRecipes.register(MyRecipeTypes.CASTING_TABLE.get());
 * });
 * }</pre>
 * The set is frozen the first time the server builds a sync payload; registering afterwards throws so
 * that the client and server can never disagree on which types are synced.
 */
public final class SyncableRecipes {
  private SyncableRecipes() {}

  private static final Set<RecipeType<?>> TYPES = new LinkedHashSet<>();
  /** True once the first sync payload has been built; further registration is rejected to keep sides in step. */
  private static volatile boolean frozen = false;

  /**
   * Registers a recipe type to be synced to clients.
   * <p>Call during common setup (e.g. {@code FMLCommonSetupEvent}, using {@code enqueueWork}).
   * @param type  Recipe type to sync
   * @throws IllegalStateException  if called after the first sync payload has been built
   */
  public static synchronized void register(RecipeType<? extends Recipe<?>> type) {
    if (frozen) {
      throw new IllegalStateException("Cannot register syncable recipe type after sync has begun: " + type);
    }
    TYPES.add(type);
  }

  /**
   * Immutable view of all registered types. Freezes the registry on first call so registration and
   * syncing cannot race.
   * @return  Unmodifiable set of registered recipe types
   */
  public static synchronized Set<RecipeType<?>> getTypes() {
    frozen = true;
    return Collections.unmodifiableSet(TYPES);
  }

  /**
   * Checks whether the given type is registered for syncing.
   * @param type  Recipe type to check
   * @return  True if the type will be included in the sync
   */
  public static boolean contains(RecipeType<?> type) {
    return TYPES.contains(type);
  }
}
