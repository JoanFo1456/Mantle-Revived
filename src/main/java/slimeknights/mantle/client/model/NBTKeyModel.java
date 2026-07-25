package slimeknights.mantle.client.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.util.JsonHelper;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Model which uses a key in NBT to select which texture variant to load.
 * <p>
 * In 26.1.2 the item model system was rewritten ({@code ItemOverrides} removed in favor of the item model / select
 * property system). This port keeps the extra-texture registry and deserialization and renders the default layer;
 * TODO(26.1.2): NBT-driven variant switching must be reimplemented on the new item model select-property system.
 */
public class NBTKeyModel extends AbstractUnbakedModel {
  /** Model loader instance */
  public static final UnbakedModelLoader<NBTKeyModel> LOADER = NBTKeyModel::deserialize;

  /** Map of statically registered extra textures, used for addon mods */
  private static final Multimap<Identifier,Pair<String,Identifier>> EXTRA_TEXTURES = HashMultimap.create();

  /**
   * Registers an extra variant texture for the model with the given key. Note that resource packs can override the extra texture
   * @param key          Model key, should be defined in the model JSON if supported
   * @param textureName  Name of the texture defined, corresponds to a possible value of the NBT key
   * @param texture      Texture to use, same format as in resource packs
   */
  @SuppressWarnings("unused")  // API
  public static void registerExtraTexture(Identifier key, String textureName, Identifier texture) {
    EXTRA_TEXTURES.put(key, Pair.of(textureName, texture));
  }

  /** Key to check in item NBT */
  private final String nbtKey;
  /** Key denoting which extra textures to fetch from the map */
  @Nullable
  private final Identifier extraTexturesKey;

  public NBTKeyModel(StandardModelParameters parameters, String nbtKey, @Nullable Identifier extraTexturesKey) {
    super(parameters);
    this.nbtKey = nbtKey;
    this.extraTexturesKey = extraTexturesKey;
  }

  /** Gets the NBT key checked by this model */
  public String getNbtKey() {
    return nbtKey;
  }

  /** Gets the extra textures registered for the given key */
  public static Iterable<Pair<String,Identifier>> getExtraTextures(Identifier key) {
    return EXTRA_TEXTURES.get(key);
  }

  @Override
  public UnbakedGeometry geometry() {
    return this::bakeGeometry;
  }

  private QuadCollection bakeGeometry(TextureSlots textureSlots, ModelBaker baker, ModelState state, ModelDebugName name) {
    Material.Baked defaultTexture = baker.materials().resolveSlot(textureSlots, "default", name);
    Transformation transform = MantleItemLayerModel.applyTransform(state.transformation(), parameters.rootTransform() == null ? Transformation.IDENTITY : parameters.rootTransform());
    QuadCollection.Builder builder = new QuadCollection.Builder();
    for (BakedQuad quad : MantleItemLayerModel.getQuadsForSprite(-1, -1, defaultTexture, transform, 0)) {
      builder.addUnculledFace(quad);
    }
    return builder.build();
  }

  /** Deserializes this model from JSON */
  public static NBTKeyModel deserialize(JsonObject json, JsonDeserializationContext context) {
    StandardModelParameters parameters = StandardModelParameters.parse(json, context);
    String key = GsonHelper.getAsString(json, "nbt_key");
    Identifier extraTexturesKey = null;
    if (json.has("extra_textures_key")) {
      extraTexturesKey = JsonHelper.getResourceLocation(json, "extra_textures_key");
    }
    return new NBTKeyModel(parameters, key, extraTexturesKey);
  }
}
