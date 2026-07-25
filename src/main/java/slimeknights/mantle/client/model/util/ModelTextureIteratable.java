package slimeknights.mantle.client.model.util;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.resources.model.sprite.Material;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Iterates over a chain of texture maps, used to resolve texture references through Mantle model textures.
 * <p>
 * In 26.1.2 the vanilla {@code BlockModel} parent chain is no longer directly accessible pre-bake, so this now iterates
 * over Mantle's own texture representation only. TODO(26.1.2): parent-model texture chains are no longer walked.
 */
public class ModelTextureIteratable implements Iterable<Map<String,Either<Material, String>>> {
  /** Ordered list of texture maps to iterate over, innermost first */
  private final List<Map<String,Either<Material, String>>> maps;

  public ModelTextureIteratable(List<Map<String,Either<Material,String>>> maps) {
    this.maps = maps;
  }

  /**
   * Creates an iterable over the given single texture map
   * @param textures  Texture map
   */
  public ModelTextureIteratable(@Nullable Map<String,Either<Material,String>> textures) {
    this(textures == null ? List.of() : List.of(textures));
  }

  /**
   * Creates an iterable over the given model's textures.
   * @param fallback  Model providing textures
   * @return  Iterable over the model texture maps
   */
  public static ModelTextureIteratable of(SimpleBlockModel fallback) {
    return new ModelTextureIteratable(fallback.getTextures());
  }

  @Override
  public Iterator<Map<String,Either<Material,String>>> iterator() {
    return maps.iterator();
  }
}
