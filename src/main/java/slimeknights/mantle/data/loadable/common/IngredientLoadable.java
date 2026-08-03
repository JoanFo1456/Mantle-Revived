package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.recipe.data.ItemNameIngredient;
import slimeknights.mantle.util.typed.TypedMap;

/** Loadable for ingredients, handling Forge ingredients */
public enum IngredientLoadable implements Loadable<Ingredient> {
  ALLOW_EMPTY,
  DISALLOW_EMPTY;

  @Override
  public Ingredient convert(JsonElement element, String key, TypedMap context) {
    element = normalizeLegacyIngredient(element);
    element = normalizeNestedIngredients(element, true);
    if (element.isJsonObject()) {
      JsonObject object = element.getAsJsonObject();
      if (object.has("type") && "forge:nbt".equals(object.get("type").getAsString())) {
        ItemStack stack = ItemStackLoadable.REQUIRED_STACK_NBT.deserialize(object, context);
        return Ingredient.of(stack.getItem());
      }
    }
    return Ingredient.CODEC.parse(JsonOps.INSTANCE, element).getOrThrow(JsonParseException::new);
  }

  /**
   * Converts the pre-1.21.2 object ingredient forms to the 1.21.2+ representation the vanilla codec accepts:
   * {@code {"item": "id"}} becomes the string {@code "id"} and {@code {"tag": "id"}} becomes {@code "#id"}. Recurses
   * through arrays and custom-ingredient children so nested legacy entries are converted too. Objects carrying a
   * {@code type}/{@code neoforge:ingredient_type} key are custom ingredients and are left as-is (only their values recurse).
   */
  private static JsonElement normalizeLegacyIngredient(JsonElement element) {
    if (element.isJsonArray()) {
      JsonArray normalized = new JsonArray();
      for (JsonElement child : element.getAsJsonArray()) {
        normalized.add(normalizeLegacyIngredient(child));
      }
      return normalized;
    }
    if (element.isJsonObject()) {
      JsonObject object = element.getAsJsonObject();
      if (!object.has("type") && !object.has("neoforge:ingredient_type")) {
        if (object.has("item")) {
          return object.get("item");
        }
        if (object.has("tag")) {
          return new JsonPrimitive("#" + object.get("tag").getAsString());
        }
      }
      JsonObject normalized = new JsonObject();
      for (var entry : object.entrySet()) {
        normalized.add(entry.getKey(), normalizeLegacyIngredient(entry.getValue()));
      }
      return normalized;
    }
    return element;
  }

  /** NeoForge's ingredient map codec no longer accepts array ingredients nested inside custom ingredient children. */
  private static JsonElement normalizeNestedIngredients(JsonElement element, boolean topLevel) {
    if (element.isJsonArray()) {
      JsonArray array = element.getAsJsonArray();
      JsonArray normalized = new JsonArray();
      for (JsonElement child : array) {
        normalized.add(normalizeNestedIngredients(child, false));
      }
      if (topLevel) {
        return normalized;
      }
      JsonObject compound = new JsonObject();
      compound.addProperty("type", "neoforge:compound");
      compound.add("children", normalized);
      return compound;
    }
    if (element.isJsonObject()) {
      JsonObject normalized = new JsonObject();
      for (var entry : element.getAsJsonObject().entrySet()) {
        JsonElement value = entry.getValue();
        if (value.isJsonArray()) {
          JsonArray array = new JsonArray();
          for (JsonElement child : value.getAsJsonArray()) {
            array.add(normalizeNestedIngredients(child, false));
          }
          normalized.add(entry.getKey(), array);
        } else {
          normalized.add(entry.getKey(), normalizeNestedIngredients(value, false));
        }
      }
      return normalized;
    }
    return element;
  }

  @Override
  public JsonElement serialize(Ingredient object) {
    if (object.isEmpty() && this == DISALLOW_EMPTY) {
      throw new IllegalArgumentException("Ingredient cannot be empty");
    }
    JsonElement namedItem = ItemNameIngredient.serialize(object);
    if (namedItem != null) {
      return namedItem;
    }
    return Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, object).getOrThrow(JsonParseException::new);
  }

  @Override
  public Ingredient decode(FriendlyByteBuf buffer, TypedMap context) {
    return Ingredient.CONTENTS_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
  }

  @Override
  public void encode(FriendlyByteBuf buffer, Ingredient object) {
    Ingredient.CONTENTS_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, object);
  }
}
