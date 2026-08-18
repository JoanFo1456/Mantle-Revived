package modernmods.mantle.client.model.util;

import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Texture source wrapper to add in an extra set of textures, used by dynamic models to inject connected/retextured variants.
 */
public class ExtraTextureContext extends GeometryContextWrapper {
  private final Map<String,Material> textures;

  /**
   * Creates a new wrapper using the given textures
   * @param base      Base texture source
   * @param textures  Textures map, any textures in this map will take precedence over those in the base source
   */
  public ExtraTextureContext(@Nullable GeometryContextWrapper base, Map<String,Material> textures) {
    super(base);
    this.textures = textures;
  }

  /**
   * Creates a new wrapper for a single texture
   * @param base     Base texture source
   * @param name     Texture name, if it matches texture is returned
   * @param texture  Texture path
   */
  public ExtraTextureContext(@Nullable GeometryContextWrapper base, String name, Identifier texture) {
    super(base);
    this.textures = Map.of(name, new Material(texture));
  }

  @Override
  @Nullable
  public Material getMaterial(String name) {
    Material connected = textures.get(name);
    if (connected != null) {
      return connected;
    }
    return super.getMaterial(name);
  }

  @Override
  public boolean hasMaterial(String name) {
    return textures.containsKey(name) || super.hasMaterial(name);
  }
}
