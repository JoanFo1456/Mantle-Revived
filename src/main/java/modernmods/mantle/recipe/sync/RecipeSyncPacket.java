package modernmods.mantle.recipe.sync;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import modernmods.mantle.network.packet.IThreadsafePacket;

import java.util.List;

/**
 * S2C payload carrying the recipes of every {@link SyncableRecipes}-registered type as a list of
 * {@link RecipeHolder}s. Each holder is serialized via {@link RecipeHolder#STREAM_CODEC}, which
 * dispatches on the recipe's registered serializer; decoding re-runs the recipe constructor on the
 * client (repopulating any static caches for free). The decoded list is stored in
 * {@link ClientRecipeCache}.
 */
public class RecipeSyncPacket implements IThreadsafePacket {
  /**
   * Length-prefixed list codec over {@link RecipeHolder#STREAM_CODEC}. The runtime buffer is always a
   * {@link RegistryFriendlyByteBuf} (see {@code NetworkWrapper.Registration#codec()}), which recipe
   * decoding requires for registry lookups.
   */
  private static final StreamCodec<RegistryFriendlyByteBuf, List<RecipeHolder<?>>> LIST_CODEC =
    RecipeHolder.STREAM_CODEC.apply(ByteBufCodecs.list());

  private final List<RecipeHolder<?>> recipes;

  public RecipeSyncPacket(List<RecipeHolder<?>> recipes) {
    this.recipes = recipes;
  }

  /** Decode constructor. The buffer passed by the network layer is always a {@link RegistryFriendlyByteBuf}. */
  public RecipeSyncPacket(FriendlyByteBuf buffer) {
    this.recipes = LIST_CODEC.decode((RegistryFriendlyByteBuf) buffer);
  }

  @Override
  public void encode(FriendlyByteBuf buf) {
    LIST_CODEC.encode((RegistryFriendlyByteBuf) buf, this.recipes);
  }

  /** The recipe holders carried by this packet. */
  public List<RecipeHolder<?>> getRecipes() {
    return this.recipes;
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    // IThreadsafePacket already enqueued this onto the client main thread.
    ClientRecipeCache.receive(this.recipes);
  }
}
