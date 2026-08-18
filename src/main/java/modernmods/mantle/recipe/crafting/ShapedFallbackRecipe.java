package modernmods.mantle.recipe.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import modernmods.mantle.data.loadable.Loadables;
import modernmods.mantle.recipe.MantleRecipes;
import modernmods.mantle.util.JsonHelper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Shaped recipe that only matches when a set of alternative recipes do not match.
 * <p>In 26.1.2 {@link ShapedRecipe} keeps its pattern and result private and can no longer be subclassed for custom
 * serializers, so this composes a delegate {@link ShapedRecipe} rather than extending it.
 */
@SuppressWarnings("WeakerAccess")
public class ShapedFallbackRecipe implements CraftingRecipe {
  /** Delegate shaped recipe handling the standard crafting behavior */
  private final ShapedRecipe base;
  /** Recipes to skip if they match */
  private final List<Identifier> alternatives;
  private List<CraftingRecipe> alternativeCache;

  /**
   * Creates a recipe wrapping a shaped recipe base
   * @param base          Shaped recipe to delegate to
   * @param alternatives  List of recipe names to fail this match if they match
   */
  public ShapedFallbackRecipe(ShapedRecipe base, List<Identifier> alternatives) {
    this.base = base;
    this.alternatives = alternatives;
  }

  /** Gets the delegate shaped recipe */
  public ShapedRecipe getBase() {
    return base;
  }

  /* Delegate crafting behavior to the base shaped recipe */

  @Override
  public CraftingBookCategory category() {
    return base.category();
  }

  @Override
  public boolean showNotification() {
    return base.showNotification();
  }

  @Override
  public String group() {
    return base.group();
  }

  @Override
  public PlacementInfo placementInfo() {
    return base.placementInfo();
  }

  @Override
  public List<RecipeDisplay> display() {
    return base.display();
  }

  @Override
  public ItemStack assemble(CraftingInput input) {
    return base.assemble(input);
  }

  @Override
  public boolean matches(CraftingInput inv, Level world) {
    // if this recipe does not match, fail it
    if (!base.matches(inv, world)) {
      return false;
    }

    // fetch all alternatives, fail if any match
    // cache to save effort down the line
    if (alternativeCache == null) {
      MinecraftServer server = world.getServer();
      if (server == null) {
        // cannot resolve alternatives without the server-side recipe manager; allow the match
        return true;
      }
      RecipeManager manager = server.getRecipeManager();
      alternativeCache = alternatives.stream()
                                     .map(id -> manager.byKey(ResourceKey.create(Registries.RECIPE, id)))
                                     .flatMap(Optional::stream)
                                     .map(RecipeHolder::value)
                                     .filter(recipe -> {
                                       // only allow exact shaped or shapeless match, prevent infinite recursion due to complex recipes
                                       Class<?> clazz = recipe.getClass();
                                       return clazz == ShapedRecipe.class || clazz == ShapelessRecipe.class;
                                     })
                                     .map(recipe -> (CraftingRecipe) recipe).collect(Collectors.toList());
    }
    // fail if any alternative matches
    return this.alternativeCache.stream().noneMatch(recipe -> recipe.matches(inv, world));
  }

  @Override
  public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
    return MantleRecipes.CRAFTING_SHAPED_FALLBACK.get();
  }

  /* Serialization */

  @SuppressWarnings("unchecked")
  private static final Codec<ShapedFallbackRecipe> JSON_CODEC = Codec.PASSTHROUGH.xmap(
    dynamic -> {
      // capture the reload's registry-aware ops so tag ingredients in the key map resolve lazily (see
      // LoggingRecipeSerializer#registryJsonOps); re-parsing with plain JsonOps makes tag ingredients fail structurally
      com.mojang.serialization.DynamicOps<JsonElement> ops = (com.mojang.serialization.DynamicOps<JsonElement>) dynamic.getOps();
      JsonObject json = dynamic.convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
      modernmods.mantle.recipe.helper.LoggingRecipeSerializer.DECODE_OPS.set(ops);
      try {
        return fromJson(json);
      } finally {
        modernmods.mantle.recipe.helper.LoggingRecipeSerializer.DECODE_OPS.remove();
      }
    },
    recipe -> new Dynamic<>(JsonOps.INSTANCE, toJson(recipe)));
  public static final MapCodec<ShapedFallbackRecipe> CODEC = MapCodec.assumeMapUnsafe(JSON_CODEC);
  public static final StreamCodec<RegistryFriendlyByteBuf,ShapedFallbackRecipe> STREAM_CODEC = StreamCodec.of(ShapedFallbackRecipe::toNetwork, ShapedFallbackRecipe::fromNetwork);
  /** Recipe serializer instance, RecipeSerializer is now a record wrapping the codecs. */
  public static final RecipeSerializer<ShapedFallbackRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

  private static ShapedFallbackRecipe fromJson(JsonObject json) {
    ShapedRecipe base = ShapedRecipe.MAP_CODEC.codec().parse(modernmods.mantle.recipe.helper.LoggingRecipeSerializer.registryJsonOps(), json).getOrThrow(JsonSyntaxException::new);
    List<Identifier> alternatives = JsonHelper.parseList(json, "alternatives", Loadables.RESOURCE_LOCATION);
    return new ShapedFallbackRecipe(base, alternatives);
  }

  private static JsonObject toJson(ShapedFallbackRecipe recipe) {
    JsonObject json = ShapedRecipe.MAP_CODEC.codec().encodeStart(JsonOps.INSTANCE, recipe.base).getOrThrow(JsonSyntaxException::new).getAsJsonObject();
    json.add("alternatives", Loadables.RESOURCE_LOCATION.list(0).serialize(recipe.alternatives));
    return json;
  }

  private static ShapedFallbackRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
    ShapedRecipe base = ShapedRecipe.STREAM_CODEC.decode(buffer);
    int size = buffer.readVarInt();
    List<Identifier> builder = new java.util.ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      builder.add(buffer.readIdentifier());
    }
    return new ShapedFallbackRecipe(base, List.copyOf(builder));
  }

  private static void toNetwork(RegistryFriendlyByteBuf buffer, ShapedFallbackRecipe recipe) {
    ShapedRecipe.STREAM_CODEC.encode(buffer, recipe.base);
    buffer.writeVarInt(recipe.alternatives.size());
    for (Identifier alternative : recipe.alternatives) {
      buffer.writeIdentifier(alternative);
    }
  }
}
