package modernmods.mantle.recipe.helper;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import javax.annotation.Nullable;

/**
 * Recipe serializer that logs network exceptions before throwing them as otherwise the exceptions may be invisible.
 * <p>In MC 26.1, {@link RecipeSerializer} is a final record wrapping a codec and stream codec, so this can no longer
 * implement it directly. Instead call {@link #serializer()} to build the actual serializer record.
 * @param <T>  Recipe class
 */
public interface LoggingRecipeSerializer<T extends Recipe<?>> {
  Identifier UNKNOWN_ID = Identifier.fromNamespaceAndPath("mantle", "unknown");
  LegacySerializer<ShapedRecipe> SHAPED_RECIPE = new LegacySerializer<>(ShapedRecipe.SERIALIZER);
  LegacySerializer<ShapelessRecipe> SHAPELESS_RECIPE = new LegacySerializer<>(ShapelessRecipe.SERIALIZER);

  T fromJson(Identifier recipeId, JsonObject json);

  @Nullable
  default T fromNetworkSafe(Identifier recipeId, FriendlyByteBuf buffer) {
    return streamCodec().decode((RegistryFriendlyByteBuf)buffer);
  }

  default void toNetworkSafe(FriendlyByteBuf buffer, T recipe) {
    streamCodec().encode((RegistryFriendlyByteBuf)buffer, recipe);
  }

  /** Builds the actual recipe serializer record wrapping this instance's codecs. */
  default RecipeSerializer<T> serializer() {
    return new RecipeSerializer<>(codec(), streamCodec());
  }

  @SuppressWarnings("unchecked")
  default MapCodec<T> codec() {
    return MapCodec.assumeMapUnsafe(Codec.PASSTHROUGH.xmap(dynamic -> {
      // capture the registry-aware ops the game decodes recipes with: vanilla ingredient tags ("#c:leathers") decode
      // through a registry-backed HolderSetCodec that needs the reload's HolderGetter, which resolves tags lazily.
      // Re-parsing with a synthetic ops instead either fails structurally (plain JsonOps) or resolves tags eagerly
      // ("Missing tag") before they are bound. fromJson() and other decoders read this via registryJsonOps().
      com.mojang.serialization.DynamicOps<com.google.gson.JsonElement> ops = (com.mojang.serialization.DynamicOps<com.google.gson.JsonElement>) dynamic.getOps();
      JsonObject json = dynamic.convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
      DECODE_OPS.set(ops);
      try {
        return fromJson(UNKNOWN_ID, json);
      } finally {
        DECODE_OPS.remove();
      }
    }, recipe -> new Dynamic<>(JsonOps.INSTANCE, new JsonObject())));
  }

  default StreamCodec<RegistryFriendlyByteBuf,T> streamCodec() {
    return StreamCodec.of((buffer, recipe) -> toNetworkSafe(buffer, recipe), buffer -> fromNetworkSafe(UNKNOWN_ID, buffer));
  }

  /** Holds the registry-aware ops the game is currently decoding a recipe with, so nested manual parses can reuse it. */
  ThreadLocal<com.mojang.serialization.DynamicOps<com.google.gson.JsonElement>> DECODE_OPS = new ThreadLocal<>();
  /** Lazily-built fallback ops for contexts with no active decode (e.g. datagen); resolves tags eagerly, so decode paths must set DECODE_OPS. */
  com.mojang.serialization.DynamicOps<com.google.gson.JsonElement>[] FALLBACK_OPS = new com.mojang.serialization.DynamicOps[1];

  /**
   * Registry-aware JSON ops for manually parsing recipe sub-structures. Vanilla's ingredient codec decodes a
   * {@code "#tag"} reference through a registry-backed {@code HolderSetCodec} that needs a
   * {@link net.minecraft.core.HolderGetter}; the reload ops (captured in {@link #codec()}) resolves those tags lazily.
   * Falls back to a synthetic {@link net.minecraft.resources.RegistryOps} outside an active decode.
   */
  static com.mojang.serialization.DynamicOps<com.google.gson.JsonElement> registryJsonOps() {
    com.mojang.serialization.DynamicOps<com.google.gson.JsonElement> ops = DECODE_OPS.get();
    if (ops != null) {
      return ops;
    }
    if (FALLBACK_OPS[0] == null) {
      FALLBACK_OPS[0] = net.minecraft.resources.RegistryOps.create(JsonOps.INSTANCE, net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY));
    }
    return FALLBACK_OPS[0];
  }

  record LegacySerializer<R extends Recipe<?>>(RecipeSerializer<R> serializer) {
    public R fromJson(Identifier recipeId, JsonObject json) {
      return serializer.codec().codec().parse(registryJsonOps(), upgradeLegacyItemStacks(json)).getOrThrow(IllegalArgumentException::new);
    }

    @Nullable
    public R fromNetwork(Identifier recipeId, FriendlyByteBuf buffer) {
      return serializer.streamCodec().decode((RegistryFriendlyByteBuf)buffer);
    }

    public void toNetwork(FriendlyByteBuf buffer, R recipe) {
      serializer.streamCodec().encode((RegistryFriendlyByteBuf)buffer, recipe);
    }

    /** Minecraft 1.21 item stack JSON uses "id" and components instead of the old "item" and "nbt" keys. */
    private static JsonObject upgradeLegacyItemStacks(JsonObject json) {
      JsonObject copy = json.deepCopy();
      if (copy.has("result") && copy.get("result").isJsonObject()) {
        upgradeLegacyItemStack(copy.getAsJsonObject("result"));
      }
      return copy;
    }

    private static void upgradeLegacyItemStack(JsonObject stack) {
      if (stack.has("item") && !stack.has("id")) {
        stack.add("id", stack.remove("item"));
      }
      if (stack.has("nbt") && stack.get("nbt").isJsonObject()) {
        JsonObject nbt = stack.getAsJsonObject("nbt");
        if (nbt.has("display") && nbt.get("display").isJsonObject()) {
          JsonObject display = nbt.getAsJsonObject("display");
          if (display.has("Name")) {
            JsonObject components = stack.has("components") && stack.get("components").isJsonObject() ? stack.getAsJsonObject("components") : new JsonObject();
            components.add("minecraft:custom_name", display.get("Name"));
            stack.add("components", components);
          }
        }
        stack.remove("nbt");
      }
    }

  }
}
