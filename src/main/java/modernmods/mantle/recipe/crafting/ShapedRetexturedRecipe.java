package slimeknights.mantle.recipe.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.recipe.MantleRecipes;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.RetexturedHelper;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Recipe which sets the texture for a {@link slimeknights.mantle.block.RetexturedBlock} based on an ingredient input.
 * <p>In 26.1.2 {@link ShapedRecipe} keeps its pattern and result private and can no longer be subclassed for custom
 * serializers, so this composes a delegate {@link ShapedRecipe} rather than extending it.
 */
@SuppressWarnings("WeakerAccess")
public class ShapedRetexturedRecipe implements CraftingRecipe {
  /** Delegate shaped recipe handling the standard crafting behavior */
  private final ShapedRecipe base;
  /** Ingredient used to determine the texture on the output */
  private final Ingredient texture;
  private final boolean matchAll;

  /** Creates a new recipe wrapping the given shaped recipe */
  public ShapedRetexturedRecipe(ShapedRecipe base, Ingredient texture, boolean matchAll) {
    this.base = base;
    this.texture = texture;
    this.matchAll = matchAll;
  }

  /** Gets the texture ingredient */
  public Ingredient getTexture() {
    return texture;
  }

  /** Gets the delegate shaped recipe */
  public ShapedRecipe getBase() {
    return base;
  }

  /** Gets a best-effort recipe ID for legacy JEI hooks. */
  public Identifier getRecipeId() {
    return Mantle.getResource("unknown_retextured_recipe");
  }

  /** Gets the untextured result */
  public ItemStack getResult() {
    return base.assemble(CraftingInput.EMPTY);
  }

  /**
   * Gets the output using the given texture
   * @param texture  Texture to use
   * @return  Output with texture. Will be blank if the input is not a block
   */
  public ItemStack getResultItem(Item texture) {
    return RetexturedHelper.setTexture(getResult().copy(), Block.byItem(texture));
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
  public boolean matches(CraftingInput input, Level level) {
    return base.matches(input, level);
  }

  @Override
  public ItemStack assemble(CraftingInput craftMatrix) {
    ItemStack result = base.assemble(craftMatrix);
    Block currentTexture = null;
    for (int i = 0; i < craftMatrix.size(); i++) {
      ItemStack stack = craftMatrix.getItem(i);
      if (!stack.isEmpty() && texture.test(stack)) {
        // fetch texture from the block if it has one
        Block block = RetexturedHelper.getTexture(stack);
        // assuming it does not, use the block itself as the texture (provided it is not the result that is)
        if (block == Blocks.AIR && stack.getItem() != result.getItem()) {
          block = Block.byItem(stack.getItem());
        }
        // if no texture, skip
        if (block == Blocks.AIR) {
          continue;
        }

        // if we have not found a texture yet, store the found block
        if (currentTexture == null) {
          currentTexture = block;
          // match all means we must check the rest. If not match all, we can be done
          if (!matchAll) {
            break;
          }

          // if we found a texture before, must match or we do no texture
        } else if (currentTexture != block) {
          currentTexture = null;
          break;
        }
      }
    }

    // set the texture if found. No texture will use the fallback
    if (currentTexture != null) {
      return RetexturedHelper.setTexture(result, currentTexture);
    }
    return result;
  }

  @Override
  public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
    return MantleRecipes.CRAFTING_SHAPED_RETEXTURED.get();
  }

  /* Serialization */

  @SuppressWarnings("unchecked")
  private static final Codec<ShapedRetexturedRecipe> JSON_CODEC = Codec.PASSTHROUGH.xmap(
    dynamic -> {
      // capture the reload's registry-aware ops so the tag ingredients in the key map resolve lazily (see
      // LoggingRecipeSerializer#registryJsonOps); re-parsing with plain JsonOps makes tag ingredients fail structurally
      com.mojang.serialization.DynamicOps<JsonElement> ops = (com.mojang.serialization.DynamicOps<JsonElement>) dynamic.getOps();
      JsonObject json = dynamic.convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
      slimeknights.mantle.recipe.helper.LoggingRecipeSerializer.DECODE_OPS.set(ops);
      try {
        return fromJson(json);
      } finally {
        slimeknights.mantle.recipe.helper.LoggingRecipeSerializer.DECODE_OPS.remove();
      }
    },
    recipe -> new Dynamic<>(JsonOps.INSTANCE, toJson(recipe)));
  public static final MapCodec<ShapedRetexturedRecipe> CODEC = MapCodec.assumeMapUnsafe(JSON_CODEC);
  public static final StreamCodec<RegistryFriendlyByteBuf,ShapedRetexturedRecipe> STREAM_CODEC = StreamCodec.of(ShapedRetexturedRecipe::toNetwork, ShapedRetexturedRecipe::fromNetwork);
  /** Recipe serializer instance, RecipeSerializer is now a record wrapping the codecs. */
  public static final RecipeSerializer<ShapedRetexturedRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

  private static ShapedRetexturedRecipe fromJson(JsonObject json) {
    ShapedRecipe base = ShapedRecipe.MAP_CODEC.codec().parse(slimeknights.mantle.recipe.helper.LoggingRecipeSerializer.registryJsonOps(), json).getOrThrow(JsonSyntaxException::new);
    // fetch the texture from the pattern key if it is a primitive
    JsonElement textureElement = JsonHelper.getElement(json, "texture");
    Ingredient texture;
    if (textureElement.isJsonPrimitive()) {
      String textureKey = textureElement.getAsString();
      if (textureKey.length() != 1) {
        throw new JsonSyntaxException("Invalid texture key: '" + textureKey + "' is an invalid symbol (must be 1 character only).");
      }
      // parse the key map from the JSON to resolve the symbol
      ShapedRecipePattern.Data data = ShapedRecipePattern.Data.MAP_CODEC.codec().parse(slimeknights.mantle.recipe.helper.LoggingRecipeSerializer.registryJsonOps(), json).getOrThrow(JsonSyntaxException::new);
      texture = data.key().get(textureKey.charAt(0));
      if (texture == null) {
        throw new JsonSyntaxException("Texture ingredient references symbol '" + textureKey + "' but it's not defined in the key");
      }
    } else {
      // if it's an object or array, treat as an ingredient object
      texture = IngredientLoadable.DISALLOW_EMPTY.convert(textureElement, "texture");
      Mantle.logger.warn("Using deprecated ingredient format on 'texture' for `mantle:crafting_shaped_retextured`. Use key instead.");
    }
    boolean matchAll = GsonHelper.getAsBoolean(json, "match_all", false);
    return new ShapedRetexturedRecipe(base, texture, matchAll);
  }

  private static JsonObject toJson(ShapedRetexturedRecipe recipe) {
    JsonObject json = ShapedRecipe.MAP_CODEC.codec().encodeStart(JsonOps.INSTANCE, recipe.base).getOrThrow(JsonSyntaxException::new).getAsJsonObject();
    json.add("texture", IngredientLoadable.DISALLOW_EMPTY.serialize(recipe.texture));
    if (recipe.matchAll) {
      json.addProperty("match_all", true);
    }
    return json;
  }

  private static ShapedRetexturedRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
    ShapedRecipe base = ShapedRecipe.STREAM_CODEC.decode(buffer);
    Ingredient texture = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
    boolean matchAll = buffer.readBoolean();
    return new ShapedRetexturedRecipe(base, texture, matchAll);
  }

  private static void toNetwork(RegistryFriendlyByteBuf buffer, ShapedRetexturedRecipe recipe) {
    ShapedRecipe.STREAM_CODEC.encode(buffer, recipe.base);
    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.texture);
    buffer.writeBoolean(recipe.matchAll);
  }
}
