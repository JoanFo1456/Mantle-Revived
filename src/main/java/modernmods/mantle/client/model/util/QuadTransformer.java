package modernmods.mantle.client.model.util;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.BakedQuad.MaterialInfo;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.model.quad.BakedColors;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable transformation applied to baked quads, replacing NeoForge's removed {@code IQuadTransformer}/{@code QuadTransformers}.
 * <p>
 * In 26.1 a {@link BakedQuad} is immutable, so transformers produce a new quad rather than mutating a vertex array. Static
 * per-vertex color is expressed through NeoForge's {@link BakedColors} field on the quad, and light emission (emissivity)
 * lives on the quad's {@link MaterialInfo}.
 */
@FunctionalInterface
public interface QuadTransformer {
  /** Identity transformer that returns the quad unchanged */
  QuadTransformer IDENTITY = quad -> quad;

  /** Transforms a single quad, returning a new quad */
  BakedQuad apply(BakedQuad quad);

  /** Returns a transformer that applies this transformer followed by {@code after} */
  default QuadTransformer andThen(QuadTransformer after) {
    if (this == IDENTITY) {
      return after;
    }
    if (after == IDENTITY) {
      return this;
    }
    return quad -> after.apply(this.apply(quad));
  }

  /** Applies this transformer to every quad, returning a new list */
  default List<BakedQuad> process(List<BakedQuad> quads) {
    if (this == IDENTITY) {
      return quads;
    }
    List<BakedQuad> result = new ArrayList<>(quads.size());
    for (BakedQuad quad : quads) {
      result.add(apply(quad));
    }
    return result;
  }

  /** Applies this transformer to every quad in the mutable list in place */
  default void processInPlace(List<BakedQuad> quads) {
    if (this != IDENTITY) {
      quads.replaceAll(this::apply);
    }
  }

  /** Applies this transformer to every quad in the collection, rebuilding it face by face */
  default QuadCollection process(QuadCollection collection) {
    if (this == IDENTITY) {
      return collection;
    }
    QuadCollection.Builder builder = new QuadCollection.Builder();
    for (BakedQuad quad : collection.getQuads(null)) {
      builder.addUnculledFace(apply(quad));
    }
    for (Direction direction : Direction.values()) {
      for (BakedQuad quad : collection.getQuads(direction)) {
        builder.addCulledFace(direction, apply(quad));
      }
    }
    return builder.build();
  }

  /** Rebuilds a quad, replacing its baked colors */
  static BakedQuad withColors(BakedQuad quad, BakedColors colors) {
    return new BakedQuad(
      quad.position0(), quad.position1(), quad.position2(), quad.position3(),
      quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
      quad.direction(), quad.materialInfo(), quad.bakedNormals(), colors);
  }

  /** Rebuilds a quad, replacing its material info */
  static BakedQuad withMaterial(BakedQuad quad, MaterialInfo material) {
    return new BakedQuad(
      quad.position0(), quad.position1(), quad.position2(), quad.position3(),
      quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
      quad.direction(), material, quad.bakedNormals(), quad.bakedColors());
  }

  /**
   * Creates a transformer applying the given ARGB color as a static color modulator, multiplied against any color already
   * present on the quad. A fully opaque white color ({@code 0xFFFFFFFF}) produces the identity transformer.
   */
  static QuadTransformer applyingColor(int color) {
    if (color == 0xFFFFFFFF) {
      return IDENTITY;
    }
    return quad -> {
      BakedColors existing = quad.bakedColors();
      int c0 = ARGB.multiply(existing.color(0), color);
      int c1 = ARGB.multiply(existing.color(1), color);
      int c2 = ARGB.multiply(existing.color(2), color);
      int c3 = ARGB.multiply(existing.color(3), color);
      return withColors(quad, BakedColors.of(c0, c1, c2, c3));
    };
  }

  /**
   * Creates a transformer forcing the given light emission (0-15) on each quad, used for full-bright/emissive parts.
   * A value less than 0 produces the identity transformer.
   */
  static QuadTransformer settingEmissivity(int light) {
    if (light < 0) {
      return IDENTITY;
    }
    return quad -> {
      MaterialInfo info = quad.materialInfo();
      if (info.lightEmission() == light) {
        return quad;
      }
      return withMaterial(quad, new MaterialInfo(info.sprite(), info.layer(), info.itemRenderType(), info.tintIndex(), info.shade(), light, info.ambientOcclusion()));
    };
  }
}
