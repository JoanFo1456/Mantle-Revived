package slimeknights.mantle.client.model.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Transformation;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import net.neoforged.neoforge.client.model.pipeline.TransformingVertexPipeline;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.mantle.util.LogicHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.PrimitiveIterator;

/**
 * Item layer model that propagates a hardcoded color, reducing rendering time by bypassing item colors for a static color.
 * Also supports luminosity, and when used as a model loader supports telling a layer to not use a tint index.
 * <p>
 * In 26.1.2 the item model system ({@code ItemLayerModel}, {@code CompositeModel}, {@code RenderTypeGroup}) was removed;
 * this now produces a {@link QuadCollection} through {@link #geometry()} using the surviving quad-baking pipeline.
 * per-layer render type selection is no longer applied (transparency now comes from the sprite material).
 */
@SuppressWarnings("WeakerAccess")
public class MantleItemLayerModel extends AbstractUnbakedModel {
  /** Model loader instance */
  public static final net.neoforged.neoforge.client.model.UnbakedModelLoader<MantleItemLayerModel> LOADER = MantleItemLayerModel::deserialize;

  private static final Direction[] HORIZONTALS = {Direction.UP, Direction.DOWN};
  private static final Direction[] VERTICALS = {Direction.WEST, Direction.EAST};

  /** Layers in the model */
  private final List<LayerData> layers;

  public MantleItemLayerModel(StandardModelParameters parameters, List<LayerData> layers) {
    super(parameters);
    this.layers = layers;
  }

  /** Gets the layer at the given index */
  private LayerData getLayer(int index) {
    return LogicHelper.getOrDefault(layers, index, LayerData.DEFAULT);
  }

  /**
   * Applies the root transformation to the model state rotation for an item layer model.
   */
  public static Transformation applyTransform(Transformation state, Transformation rootTransform) {
    if (rootTransform.isIdentity()) {
      return state;
    }
    return rootTransform.compose(state);
  }

  @Override
  public UnbakedGeometry geometry() {
    return this::bakeGeometry;
  }

  private QuadCollection bakeGeometry(TextureSlots textureSlots, ModelBaker baker, net.minecraft.client.renderer.block.dispatch.ModelState state, ModelDebugName name) {
    // gather layer materials
    List<Material.Baked> materials = new ArrayList<>();
    for (int i = 0; textureSlots.getMaterial("layer" + i) != null; i++) {
      materials.add(baker.materials().resolveSlot(textureSlots, "layer" + i, name));
    }
    if (materials.isEmpty()) {
      throw new IllegalStateException("Empty textures list");
    }
    Transformation transform = applyTransform(state.transformation(), parameters.rootTransform() == null ? Transformation.IDENTITY : parameters.rootTransform());
    ItemLayerPixels pixels = materials.size() == 1 ? null : new ItemLayerPixels();
    QuadCollection.Builder builder = new QuadCollection.Builder();
    // build in reverse so pixel tracking works
    List<List<BakedQuad>> layerQuads = new ArrayList<>(Collections.nCopies(materials.size(), null));
    for (int i = materials.size() - 1; i >= 0; i--) {
      LayerData data = getLayer(i);
      layerQuads.set(i, getQuadsForSprite(data.color(), data.noTint() ? -1 : i, materials.get(i), transform, data.luminosity(), pixels));
    }
    for (List<BakedQuad> quads : layerQuads) {
      for (BakedQuad quad : quads) {
        builder.addUnculledFace(quad);
      }
    }
    return builder.build();
  }

  /**
   * Gets all quads for an item layer for the given sprite
   * @param color       Color for the sprite in AARRGGBB format.
   * @param tint        Tint index for block/item colors. Generally unused
   * @param material    Baked material to convert into quads
   * @param transform   Transforms to apply
   * @param luminosity  Extra light to add to the quad from 0-15, makes it appear to glow a bit
   * @return  List of baked quads
   */
  public static List<BakedQuad> getQuadsForSprite(int color, int tint, Material.Baked material, Transformation transform, int luminosity) {
    return getQuadsForSprite(color, tint, material, transform, luminosity, null);
  }

  /**
   * Gets all quads including side quads for an item layer for the given sprite.
   * @param color       Color for the sprite in AARRGGBB format.
   * @param tint        Tint index for block/item colors. Generally unused
   * @param material    Baked material to convert into quads
   * @param transform   Transforms to apply
   * @param emissivity  Extra light to add to the quad from 0-15, makes it appear to glow a bit
   * @param pixels      Object to keep track of used pixels across multiple layers to help prevent z-fighting. Use null to skip this logic
   * @return  List of baked quads
   */
  public static List<BakedQuad> getQuadsForSprite(int color, int tint, Material.Baked material, Transformation transform, int emissivity, @Nullable ItemLayerPixels pixels) {
    List<BakedQuad> builder = new ArrayList<>();

    TextureAtlasSprite sprite = material.sprite();
    SpriteContents contents = sprite.contents();
    int uMax = contents.width();
    int vMax = contents.height();
    FaceData faceData = new FaceData(uMax, vMax);
    boolean translucent = false;

    PrimitiveIterator.OfInt iterator = sprite.contents().getUniqueFrames().iterator();
    boolean hasFrames = iterator.hasNext();
    while (iterator.hasNext()) {
      int f = iterator.nextInt();
      boolean ptu;
      boolean[] ptv = new boolean[uMax];
      Arrays.fill(ptv, true);
      for(int v = 0; v < vMax; v++) {
        ptu = true;
        for(int u = 0; u < uMax; u++) {
          int alpha = sprite.getPixelRGBA(f, u, vMax - v - 1) >> 24 & 0xFF;
          boolean t = alpha / 255f <= 0.1f;

          if (!t && alpha < 255) {
            translucent = true;
          }

          if(ptu && !t) { // left - transparent, right - opaque
            faceData.set(Direction.WEST, u, v);
          }
          if(!ptu && t) { // left - opaque, right - transparent
            faceData.set(Direction.EAST, u-1, v);
          }
          if(ptv[u] && !t) { // up - transparent, down - opaque
            faceData.set(Direction.UP, u, v);
          }
          if(!ptv[u] && t) { // up - opaque, down - transparent
            faceData.set(Direction.DOWN, u, v-1);
          }

          ptu = t;
          ptv[u] = t;
        }
        if(!ptu) { // last - opaque
          faceData.set(Direction.EAST, uMax-1, v);
        }
      }
      // last line
      for(int u = 0; u < uMax; u++) {
        if(!ptv[u]) {
          faceData.set(Direction.DOWN, u, vMax-1);
        }
      }
    }

    // setup quad builder
    QuadBakingVertexConsumer quadBuilder = new QuadBakingVertexConsumer();
    // common settings
    quadBuilder.setSprite(material);
    quadBuilder.setTintIndex(tint);
    quadBuilder.setShade(false);
    quadBuilder.setAmbientOcclusion(true);
    // only need to set up transforms once, isn't that nice?
    VertexConsumer quadConsumer = quadBuilder;
    if (!transform.isIdentity()) {
      quadConsumer = new TransformingVertexPipeline(quadBuilder, transform);
    }

    // horizontal quads
    for (Direction facing : HORIZONTALS) {
      for (int v = 0; v < vMax; v++) {
        int uStart = 0, uEnd = uMax;
        boolean building = false;
        for (int u = 0; u < uMax; u++) {
          boolean canDraw = pixels == null || !pixels.get(u, v, uMax, vMax);
          boolean face = canDraw && faceData.get(facing, u, v);
          if (face) {
            uEnd = u + 1;
            if (!building) {
              building = true;
              uStart = u;
            }
          }
          else if (building) {
            if (!canDraw || translucent) {
              int off = facing == Direction.DOWN ? 1 : 0;
              builder.add(buildSideQuad(quadBuilder, quadConsumer, facing, color, material, tint, uStart, v + off, uEnd - uStart, emissivity));
              building = false;
            }
          }
        }
        if (building) {
          int off = facing == Direction.DOWN ? 1 : 0;
          builder.add(buildSideQuad(quadBuilder, quadConsumer, facing, color, material, tint, uStart, v+off, uEnd-uStart, emissivity));
        }
      }
    }

    // vertical quads
    for (Direction facing : VERTICALS) {
      for (int u = 0; u < uMax; u++) {
        int vStart = 0, vEnd = vMax;
        boolean building = false;
        for (int v = 0; v < vMax; v++) {
          boolean canDraw = pixels == null || !pixels.get(u, v, uMax, vMax);
          boolean face = canDraw && faceData.get(facing, u, v);
          if (face) {
            vEnd = v + 1;
            if (!building) {
              building = true;
              vStart = v;
            }
          }
          else if (building) {
            if (!canDraw || translucent) {
              int off = facing == Direction.EAST ? 1 : 0;
              builder.add(buildSideQuad(quadBuilder, quadConsumer, facing, color, material, tint, u + off, vStart, vEnd - vStart, emissivity));
              building = false;
            }
          }
        }
        if (building) {
          int off = facing == Direction.EAST ? 1 : 0;
          builder.add(buildSideQuad(quadBuilder, quadConsumer, facing, color, material, tint, u+off, vStart, vEnd-vStart, emissivity));
        }
      }
    }

    // back
    builder.add(buildQuad(quadBuilder, quadConsumer, material, tint, Direction.NORTH, color, emissivity,
              0, 0, 7.5f / 16f, sprite.getU0(), sprite.getV1(),
              0, 1, 7.5f / 16f, sprite.getU0(), sprite.getV0(),
              1, 1, 7.5f / 16f, sprite.getU1(), sprite.getV0(),
              1, 0, 7.5f / 16f, sprite.getU1(), sprite.getV1()));
    // front
    builder.add(buildQuad(quadBuilder, quadConsumer, material, tint, Direction.SOUTH, color, emissivity,
              0, 0, 8.5f / 16f, sprite.getU0(), sprite.getV1(),
              1, 0, 8.5f / 16f, sprite.getU1(), sprite.getV1(),
              1, 1, 8.5f / 16f, sprite.getU1(), sprite.getV0(),
              0, 1, 8.5f / 16f, sprite.getU0(), sprite.getV0()));

    // fill in the pixel map with new pixels from the sprite
    if (pixels != null) {
      if (hasFrames) {
        for(int v = 0; v < vMax; v++) {
          for(int u = 0; u < uMax; u++) {
            int alpha = sprite.getPixelRGBA(0, u, vMax - v - 1) >> 24 & 0xFF;
            if (alpha / 255f > 0.1f) {
              pixels.set(u, v, uMax, vMax);
            }
          }
        }
      }
    }

    return List.copyOf(builder);
  }

  /**
   * Gets the quad to display in GUIs for the given sprite.
   */
  @SuppressWarnings("unused")  // API
  public static BakedQuad getQuadForGui(int color, int tint, Material.Baked material, Transformation transform, int emissivity) {
    TextureAtlasSprite sprite = material.sprite();
    QuadBakingVertexConsumer quadBuilder = new QuadBakingVertexConsumer();
    quadBuilder.setSprite(material);
    quadBuilder.setTintIndex(tint);
    quadBuilder.setShade(false);
    quadBuilder.setAmbientOcclusion(true);
    VertexConsumer quadConsumer = quadBuilder;
    if (!transform.isIdentity()) {
      quadConsumer = new TransformingVertexPipeline(quadBuilder, transform);
    }
    return buildQuad(quadBuilder, quadConsumer, material, tint, Direction.SOUTH, color, emissivity,
              0, 0, 8.5f / 16f, sprite.getU0(), sprite.getV1(),
              1, 0, 8.5f / 16f, sprite.getU1(), sprite.getV1(),
              1, 1, 8.5f / 16f, sprite.getU1(), sprite.getV0(),
              0, 1, 8.5f / 16f, sprite.getU0(), sprite.getV0());
  }

  /** Builds a single quad on the side of the sprite */
  private static BakedQuad buildSideQuad(QuadBakingVertexConsumer builder, VertexConsumer consumer, Direction side, int color, Material.Baked material, int tint, int u, int v, int size, int luminosity) {
    final float eps = 1e-2f;
    TextureAtlasSprite sprite = material.sprite();
    SpriteContents contents = sprite.contents();
    int width = contents.width();
    int height = contents.height();
    float x0 = (float) u / width;
    float y0 = (float) v / height;
    float x1 = x0, y1 = y0;
    float z0 = 7.5f / 16f, z1 = 8.5f / 16f;
    switch(side) {
      case WEST:
        z0 = 8.5f / 16f;
        z1 = 7.5f / 16f;
        // continue into EAST
      case EAST:
        y1 = (float) (v + size) / height;
        break;
      case DOWN:
        z0 = 8.5f / 16f;
        z1 = 7.5f / 16f;
        // continue into UP
      case UP:
        x1 = (float) (u + size) / width;
        break;
      default:
        throw new IllegalArgumentException("can't handle z-oriented side");
    }

    float dx = side.getUnitVec3i().getX() * eps / width;
    float dy = side.getUnitVec3i().getY() * eps / height;
    float u0 = 16f * (x0 - dx);
    float u1 = 16f * (x1 - dx);
    float v0 = 16f * (1f - y0 - dy);
    float v1 = 16f * (1f - y1 - dy);
    return buildQuad(builder, consumer, material, tint, (side.getAxis() == Axis.Y ? side.getOpposite() : side),
      color, luminosity,
      x0, y0, z0, sprite.getU(u0), sprite.getV(v0),
      x1, y1, z0, sprite.getU(u1), sprite.getV(v1),
      x1, y1, z1, sprite.getU(u1), sprite.getV(v1),
      x0, y0, z1, sprite.getU(u0), sprite.getV(v0));
  }

  /** Builds a single quad in the model with color and luminosity */
  public static BakedQuad buildQuad(QuadBakingVertexConsumer builder, VertexConsumer consumer, Material.Baked material, int tint, Direction side, int color, int luminosity,
                                     float x0, float y0, float z0, float u0, float v0,
                                     float x1, float y1, float z1, float u1, float v1,
                                     float x2, float y2, float z2, float u2, float v2,
                                     float x3, float y3, float z3, float u3, float v3) {
    // in 26.1 QuadBakingVertexConsumer.bakeQuad() clears its state (sprite, tint, shade, ambient occlusion) after every
    // quad, so the once-per-model setup no longer sticks. Re-apply the per-quad state before each bake or the second and
    // later quads throw "No sprite set" (and would drop the tint/shade otherwise).
    builder.setSprite(material);
    builder.setTintIndex(tint);
    builder.setShade(false);
    builder.setAmbientOcclusion(true);
    builder.setDirection(side);
    builder.setLightEmission(luminosity);
    putVertex(consumer, side, x0, y0, z0, u0, v0, color);
    putVertex(consumer, side, x1, y1, z1, u1, v1, color);
    putVertex(consumer, side, x2, y2, z2, u2, v2, color);
    putVertex(consumer, side, x3, y3, z3, u3, v3, color);
    return builder.bakeQuad();
  }

  /** Adds a single vertex to the quad */
  private static void putVertex(VertexConsumer consumer, Direction side, float x, float y, float z, float u, float v, int color) {
    consumer.addVertex(x, y, z);
    consumer.setColor(color);
    consumer.setNormal(side.getStepX(), side.getStepY(), side.getStepZ());
    consumer.setUv(u, v);
  }

  /** Cloned FaceData subclass */
  private static class FaceData {
    private final EnumMap<Direction,BitSet> data = new EnumMap<>(Direction.class);
    private final int vMax;

    FaceData(int uMax, int vMax) {
      this.vMax = vMax;
      data.put(Direction.WEST, new BitSet(uMax * vMax));
      data.put(Direction.EAST, new BitSet(uMax * vMax));
      data.put(Direction.UP,   new BitSet(uMax * vMax));
      data.put(Direction.DOWN, new BitSet(uMax * vMax));
    }

    public void set(Direction facing, int u, int v) {
      data.get(facing).set(getIndex(u, v));
    }

    public boolean get(Direction facing, int u, int v) {
      return data.get(facing).get(getIndex(u, v));
    }

    private int getIndex(int u, int v) {
      return v * vMax + u;
    }
  }

  /**
   * Class holding details about a single layer in the model
   */
  public record LayerData(int color, int luminosity, boolean noTint, @Nullable Identifier renderType) {
    public static final LayerData DEFAULT = new LayerData(-1, 0, false, null);
    public static final RecordLoadable<LayerData> LOADABLE = RecordLoadable.create(
      ColorLoadable.ALPHA.defaultField("color", false, LayerData::color),
      IntLoadable.range(0, 15).defaultField("luminosity", 0, LayerData::luminosity),
      BooleanLoadable.INSTANCE.defaultField("no_tint", false, false, LayerData::noTint),
      Loadables.RESOURCE_LOCATION.nullableField("render_type", LayerData::renderType),
      LayerData::new);
    public static final Loadable<List<LayerData>> LIST_LOADABLE = LOADABLE.list(1);

    /** @deprecated use {@link #LOADABLE} */
    @Deprecated(forRemoval = true)
    public static LayerData fromJson(JsonObject json) {
      return LOADABLE.deserialize(json);
    }

    /** @deprecated use {@link #LOADABLE} */
    @Deprecated(forRemoval = true)
    public JsonObject toJson() {
      JsonObject json = new JsonObject();
      LOADABLE.serialize(this, json);
      return json;
    }
  }

  /** Deserializes this model from JSON */
  public static MantleItemLayerModel deserialize(JsonObject json, JsonDeserializationContext context) {
    StandardModelParameters parameters = StandardModelParameters.parse(json, context);
    return new MantleItemLayerModel(parameters, LayerData.LIST_LOADABLE.getOrDefault(json, "layers", List.of()));
  }
}
