package slimeknights.mantle.recipe.sync;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import slimeknights.mantle.network.MantleNetwork;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Server-side send logic for the recipe sync. Listens to {@link OnDatapackSyncEvent} (NeoForge game
 * bus, fired on player join and {@code /reload}), collects every recipe of a {@link SyncableRecipes}
 * registered type from the server recipe manager, and sends one {@link RecipeSyncPacket} to each
 * relevant player.
 */
public final class RecipeSyncHandler {
  private RecipeSyncHandler() {}

  /** Listener for {@link OnDatapackSyncEvent}; registered on the NeoForge game bus from {@code Mantle}. */
  public static void onDatapackSync(OnDatapackSyncEvent event) {
    Set<RecipeType<?>> types = SyncableRecipes.getTypes();
    if (types.isEmpty()) {
      return; // nothing opted in, skip the packet entirely
    }
    MinecraftServer server = event.getPlayerList().getServer();
    RecipeManager manager = server.getRecipeManager();
    RecipeMap recipeMap = manager.recipeMap();

    // build the filtered holder list once and reuse it for every relevant player. Trial-encode each recipe and drop any
    // that throws (e.g. an ingredient over a missing/unbound item tag): a single un-encodable recipe would otherwise
    // corrupt the whole sync packet and disconnect the joining client. Skipped recipes simply do not appear client-side.
    net.minecraft.core.RegistryAccess access = server.registryAccess();
    List<RecipeHolder<?>> holders = new ArrayList<>();
    for (RecipeType<?> type : types) {
      for (RecipeHolder<?> holder : byType(recipeMap, type)) {
        if (canEncode(holder, access)) {
          holders.add(holder);
        }
      }
    }

    RecipeSyncPacket packet = new RecipeSyncPacket(holders);
    event.getRelevantPlayers().forEach((ServerPlayer player) -> MantleNetwork.INSTANCE.sendTo(packet, player));
  }

  /**
   * Fetches all holders of a wildcard recipe type. {@link RecipeMap#byType} requires a fully-typed
   * {@code RecipeType<T extends Recipe<I>>}, which a {@code RecipeType<?>} cannot satisfy through
   * wildcard capture; a raw cast bridges that safely since the returned holders are only upcast to
   * {@code RecipeHolder<?>}.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Collection<RecipeHolder<?>> byType(RecipeMap map, RecipeType<?> type) {
    return (Collection<RecipeHolder<?>>) (Collection<? extends RecipeHolder<? extends Recipe<?>>>) map.byType((RecipeType) type);
  }

  /**
   * Checks a recipe holder round-trips through {@link RecipeHolder#STREAM_CODEC} into a throwaway buffer. Returns false
   * (and logs) if encoding throws -- typically an ingredient resolving a missing/unbound item tag -- so the recipe is
   * dropped from the sync rather than corrupting the packet for the whole player.
   */
  private static boolean canEncode(RecipeHolder<?> holder, net.minecraft.core.RegistryAccess access) {
    net.minecraft.network.RegistryFriendlyByteBuf buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), access);
    try {
      RecipeHolder.STREAM_CODEC.encode(buffer, holder);
      return true;
    } catch (RuntimeException e) {
      slimeknights.mantle.Mantle.logger.warn("Skipping recipe {} from client sync (unresolvable): {}", holder.id(), e.getMessage());
      return false;
    } finally {
      buffer.release();
    }
  }
}
