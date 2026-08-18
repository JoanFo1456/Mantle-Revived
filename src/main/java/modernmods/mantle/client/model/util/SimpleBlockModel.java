package modernmods.mantle.client.model.util;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import modernmods.mantle.Mantle;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Simpler version of a vanilla block model for use in a custom {@link UnbakedModelLoader}, wrapping the standard
 * top-level model parameters plus a list of cuboid elements.
 * <p>
 * In 26.1.2 the vanilla model system was reworked: geometry now bakes to a {@link QuadCollection} via
 * {@link UnbakedGeometry#bake(TextureSlots, ModelBaker, ModelState, ModelDebugName)} instead of producing a full baked
 * model, and the "owner" of a model is the {@link UnbakedModel} itself. This class adapts to that by extending
 * {@link AbstractUnbakedModel} and exposing its elements through {@link #geometry()}.
 */
@SuppressWarnings("WeakerAccess")
public class SimpleBlockModel extends AbstractUnbakedModel {
  /** Model loader for the basic Mantle block model, mainly intended for use in fallback registration */
  public static final UnbakedModelLoader<SimpleBlockModel> LOADER = SimpleBlockModel::deserialize;
  /** Location used for baking dynamic models, name does not matter so just using a constant */
  public static final Identifier BAKE_LOCATION = Mantle.getResource("dynamic_model_baking");

  /** Model parts for baked model */
  private final List<CuboidModelElement> parts;
  /** Textures for iteration, in Mantle's own representation (left = material, right = reference name) */
  private final Map<String,Either<Material, String>> textures;

  /** Gets the Mantle texture representation for iteration */
  public Map<String,Either<Material,String>> getTextures() {
    return textures;
  }

  /**
   * Creates a new simple block model
   * @param parameters  Standard top-level model parameters (parent, textures, transforms, ...)
   * @param textures    Mantle texture representation for iteration (references resolvable via {@link ModelTextureIteratable})
   * @param parts       List of cuboid elements in the model
   */
  public SimpleBlockModel(StandardModelParameters parameters, Map<String,Either<Material,String>> textures, List<CuboidModelElement> parts) {
    super(parameters);
    this.parts = parts;
    this.textures = textures;
  }

  public SimpleBlockModel(SimpleBlockModel base) {
    this(base.parameters, base.textures, base.parts);
  }

  /** Parent model location, or null if no parent */
  @Nullable
  public Identifier getParentLocation() {
    return parent();
  }


  /* Properties */

  /**
   * Gets the elements in this simple block model
   * @return  Elements in the model
   */
  public List<CuboidModelElement> getElements() {
    return parts;
  }

  @Nullable
  @Override
  public UnbakedGeometry geometry() {
    // return null when this model defines no elements of its own, so the 26.1 baker inherits geometry from the parent
    // chain (it walks parents until a non-null geometry is found, matching vanilla). Returning an empty geometry instead
    // makes a parent-only model (e.g. the tables, which keep their elements in block/table/table) render nothing.
    return parts.isEmpty() ? null : new UnbakedCuboidGeometry(parts);
  }


  /* Baking */

  /**
   * Bakes the given elements into a quad collection.
   * @param elements     Elements to bake
   * @param textureSlots Resolved texture slots
   * @param baker        Model baker
   * @param transform    Model state
   * @param name         Debug name
   * @return  Baked quad collection
   */
  public static QuadCollection bakeElements(List<CuboidModelElement> elements, TextureSlots textureSlots, ModelBaker baker, ModelState transform, ModelDebugName name) {
    return UnbakedCuboidGeometry.bake(elements, textureSlots, baker, transform, name);
  }

  /**
   * Bakes a single model element and applies the given quad transformer to the result. Useful for dynamic models that
   * need to tint or make emissive individual parts (e.g. Tinkers material blocks), replacing the old {@code bakePart}
   * that mutated a baked model builder.
   * @param element      Element to bake
   * @param textureSlots Resolved texture slots
   * @param baker        Model baker
   * @param transform    Model state
   * @param name         Debug name
   * @param transformer  Transformer applied to the baked quads (e.g. {@link QuadTransformer#applyingColor(int)})
   * @return  Baked, transformed quad collection for the element
   */
  public static QuadCollection bakePart(CuboidModelElement element, TextureSlots textureSlots, ModelBaker baker, ModelState transform, ModelDebugName name, QuadTransformer transformer) {
    return transformer.process(UnbakedCuboidGeometry.bake(List.of(element), textureSlots, baker, transform, name));
  }

  /**
   * Bakes this model's elements into a quad collection.
   * @param textureSlots Resolved texture slots
   * @param baker        Model baker
   * @param transform    Model state
   * @param name         Debug name
   * @return  Baked quad collection
   */
  public QuadCollection bake(TextureSlots textureSlots, ModelBaker baker, ModelState transform, ModelDebugName name) {
    return bakeElements(getElements(), textureSlots, baker, transform, name);
  }


  /* Deserializing */

  /**
   * Deserializes a SimpleBlockModel from JSON
   * @param json     Json element containing the model
   * @param context  Json Context
   * @return  Parsed model
   */
  public static SimpleBlockModel deserialize(JsonObject json, JsonDeserializationContext context) {
    StandardModelParameters parameters = StandardModelParameters.parse(json, context);
    Map<String,Either<Material,String>> textures = parseTextures(json);
    List<CuboidModelElement> parts = deserializeElements(json, context);
    return new SimpleBlockModel(parameters, textures, parts);
  }

  /** Parses the texture map into Mantle's representation, in addition to the vanilla parse done by {@link StandardModelParameters} */
  public static Map<String,Either<Material,String>> parseTextures(JsonObject json) {
    if (!json.has("textures")) {
      return Map.of();
    }
    JsonObject textures = GsonHelper.getAsJsonObject(json, "textures");
    ImmutableMap.Builder<String,Either<Material,String>> builder = ImmutableMap.builder();
    for (Entry<String,JsonElement> entry : textures.entrySet()) {
      builder.put(entry.getKey(), parseTextureLocationOrReference(entry.getValue().getAsString()));
    }
    return builder.build();
  }

  private static Either<Material,String> parseTextureLocationOrReference(String name) {
    if (name.charAt(0) == '#') {
      return Either.right(name.substring(1));
    }
    Identifier location = Identifier.tryParse(name);
    if (location == null) {
      throw new JsonParseException(name + " is not valid resource location");
    }
    return Either.left(new Material(location));
  }

  /** Deserializes the element list from the model JSON, empty if absent */
  public static List<CuboidModelElement> deserializeElements(JsonObject json, JsonDeserializationContext context) {
    if (!json.has("elements")) {
      return List.of();
    }
    return getModelElements(context, GsonHelper.getAsJsonArray(json, "elements"), "elements");
  }

  /**
   * Gets a list of cuboid elements from a JSON array
   * @param context  Json Context
   * @param element  Json array
   * @return  Element list
   */
  public static List<CuboidModelElement> getModelElements(JsonDeserializationContext context, JsonElement element, String name) {
    // if just one element, array is optional
    if (element.isJsonObject()) {
      return List.of((CuboidModelElement)context.deserialize(element.getAsJsonObject(), CuboidModelElement.class));
    }
    // if an array, get array of elements
    if (element.isJsonArray()) {
      JsonArray array = element.getAsJsonArray();
      List<CuboidModelElement> builder = new ArrayList<>(array.size());
      for (JsonElement json : array) {
        builder.add(context.deserialize(json, CuboidModelElement.class));
      }
      return List.copyOf(builder);
    }
    throw new JsonSyntaxException("Missing " + name + ", expected to find a JsonArray or JsonObject");
  }

  /** Builds a {@link TextureSlots.Data} instance from a Mantle texture map, for constructing {@link StandardModelParameters} */
  public static TextureSlots.Data buildTextureData(Map<String,Either<Material,String>> textures) {
    if (textures.isEmpty()) {
      return TextureSlots.Data.EMPTY;
    }
    TextureSlots.Data.Builder builder = new TextureSlots.Data.Builder();
    textures.forEach((name, either) -> either.ifLeft(material -> builder.addTexture(name, material)).ifRight(ref -> builder.addReference(name, ref)));
    return builder.build();
  }

  /** Helper to build a map for merging in extra textures */
  protected static Map<String,Either<Material,String>> mutableTextures(Map<String,Either<Material,String>> base) {
    return new HashMap<>(base);
  }
}
