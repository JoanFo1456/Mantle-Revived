package slimeknights.mantle.client.model;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.DelegateUnbakedModel;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.ModelTextureIteratable;
import slimeknights.mantle.client.model.util.SimpleBlockModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Model that dynamically retextures a list of textures based on data from {@link slimeknights.mantle.util.RetexturedHelper}.
 * <p>
 * In 26.1.2 dynamic baked-model wrapping ({@code BakedModelWrapper}/{@code ItemOverrides}/{@code ModelData}) was removed;
 * dynamic per-state/per-item retexturing must be reimplemented on the new {@code BlockStateModel}/item model systems.
 * This port preserves the texture-name resolution and deserialization and delegates static geometry to the wrapped model.
 * TODO(26.1.2): reimplement dynamic retexturing on the new block/item model pipeline.
 */
@SuppressWarnings("WeakerAccess")
public class RetexturedModel extends DelegateUnbakedModel {
  /** Loader instance */
  public static final UnbakedModelLoader<RetexturedModel> LOADER = RetexturedModel::deserialize;

  private final SimpleBlockModel model;
  private final Set<String> retextured;

  public RetexturedModel(SimpleBlockModel model, Set<String> retextured) {
    super(model);
    this.model = model;
    this.retextured = retextured;
  }

  /** Gets the set of texture names that are retextured by this model */
  public Set<String> getRetextured() {
    return retextured;
  }

  /**
   * Gets a list of all names to retexture based on the block model texture references
   * @param model        Model fallback
   * @param originalSet  Original list of names to retexture
   * @return  Set of textures including parent textures
   */
  public static Set<String> getAllRetextured(SimpleBlockModel model, Set<String> originalSet) {
    Set<String> retextured = Sets.newHashSet(originalSet);
    for (Map<String,Either<Material, String>> textures : ModelTextureIteratable.of(model)) {
      textures.forEach((name, either) ->
        either.ifRight(parent -> {
          if (retextured.contains(parent)) {
            retextured.add(name);
          }
        })
      );
    }
    return Set.copyOf(retextured);
  }

  /** Deserializes a retextured model from JSON */
  public static RetexturedModel deserialize(JsonObject json, JsonDeserializationContext context) {
    // get base model
    ColoredBlockModel model = ColoredBlockModel.deserialize(json, context);
    // get list of textures to retexture
    Set<String> retextured = getRetexturedNames(json);
    // return retextured model
    return new RetexturedModel(model, getAllRetextured(model, retextured));
  }

  /**
   * Gets the list of retextured textures from the model
   * @param json  Model json
   * @return  Set of textures
   */
  public static Set<String> getRetexturedNames(JsonObject json) {
    if (json.has("retextured")) {
      // if an array, set from each texture in array
      JsonElement retextured = json.get("retextured");
      if (retextured.isJsonArray()) {
        JsonArray array = retextured.getAsJsonArray();
        if (array.isEmpty()) {
          throw new JsonSyntaxException("Must have at least one texture in retextured");
        }
        List<String> builder = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
          builder.add(GsonHelper.convertToString(array.get(i), "retextured[" + i + "]"));
        }
        return Set.copyOf(builder);
      }
      // if string, single texture
      if (retextured.isJsonPrimitive()) {
        return Set.of(retextured.getAsString());
      }
    }
    // if neither or missing, error
    throw new JsonSyntaxException("Missing retextured, expected to find a String or a JsonArray");
  }
}
