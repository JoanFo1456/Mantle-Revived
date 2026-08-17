package modernmods.mantle.client.model.connected;

import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * The block-state half of the {@code mantle:connected} system, replacing the removed 1.21.1 dynamic {@code BakedModel}
 * that re-textured each face in {@code getQuads(state, side, rand, ModelData, ...)} based on which neighbours the block
 * connected to.
 * <p>
 * In 26.1 the block model bake produces a static {@link QuadCollection}; the only place that still sees the world is a
 * {@link BlockStateModel}. So the connection is implemented here as a {@link CustomUnbakedBlockStateModel} registered on
 * {@code RegisterBlockStateModels} under {@link #ID} and selected by a {@code "type": "mantle:connected"} key inside the
 * variant object of the block's blockstate JSON. The variant is a vanilla {@link Variant} (so {@code model}/{@code x}/
 * {@code y}/{@code uvlock} still apply) plus a {@code "connection"} object mirroring the old model {@code connection}
 * block ({@code textures} = name→connection-type, optional {@code predicate}, optional {@code sides}).
 * <p>
 * Unlike the 1.21.1 model — which needed the block's {@code ModelData} to carry the connection byte — the connection is
 * computed directly from the world in {@link Baked#collectParts}, so blocks without a block entity (glass) work. At bake
 * time it records, per connected base sprite, the sixteen suffix sprites ({@code <texture>/<suffix>}); at collect time it
 * reads the neighbours, computes the six-bit connection mask, and rebuilds the quad collection swapping each face's sprite
 * to the suffix matching that face's local connections, caching the result per mask.
 * <p>
 * Limitation: the per-face direction transform uses the identity UV orientation (correct for {@code cube_all}-style
 * models such as the glass blocks); models that rotate their face UVs on the connected texture are not remapped.
 * @see ConnectedModel  the model-loader half ({@code "loader": "mantle:connected"}), which still parses the same model JSON
 */
public final class ConnectedBlockStateModel {
  /** Registered id — the value of the {@code "type"} key in a connected block's blockstate variant. */
  public static final Identifier ID = modernmods.mantle.Mantle.getResource("connected");

  private ConnectedBlockStateModel() {}

  /** Deserialized {@code connection} object from the blockstate variant. */
  public record Connection(Map<String,String[]> textures, String predicate, Set<Direction> sides) {
    /** Codec for a texture map value: a connection-type name resolved to its sixteen suffixes via the registry. */
    private static final Codec<String[]> TYPE_CODEC = Codec.STRING.xmap(
      name -> ConnectedModelRegistry.deserializeType(new com.google.gson.JsonPrimitive(name), "textures"),
      suffixes -> { throw new UnsupportedOperationException("Connection types are not re-serializable"); });
    private static final Codec<Set<Direction>> SIDES_CODEC = Direction.CODEC.listOf().xmap(
      list -> list.isEmpty() ? EnumSet.allOf(Direction.class) : EnumSet.copyOf(list),
      List::copyOf);
    public static final MapCodec<Connection> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
      Codec.unboundedMap(Codec.STRING, TYPE_CODEC).fieldOf("textures").forGetter(Connection::textures),
      Codec.STRING.optionalFieldOf("predicate", "block").forGetter(Connection::predicate),
      SIDES_CODEC.optionalFieldOf("sides", EnumSet.allOf(Direction.class)).forGetter(Connection::sides)
    ).apply(inst, Connection::new));
  }

  /**
   * Unbaked connected block state model: a vanilla {@link Variant} plus the parsed {@link Connection} config and an
   * optional static ARGB color modulator (replacing the model's {@code colors} block — in 26.1 a {@code ColoredBlockModel}
   * that inherits its geometry from a vanilla parent such as {@code cube_all} can no longer apply that color during bake,
   * so the connected block state applies it to the baked quads here, matching the coloured glass variants).
   * @param variant     Wrapped vanilla variant (model reference + rotation)
   * @param connection  Connection config (which textures connect, the connection predicate, the sides to check)
   * @param color       ARGB color applied to every quad ({@code -1} = untinted)
   */
  public record Unbaked(Variant variant, Connection connection, int color) implements CustomUnbakedBlockStateModel {
    public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
      Variant.MAP_CODEC.forGetter(Unbaked::variant),
      Connection.MAP_CODEC.fieldOf("connection").forGetter(Unbaked::connection),
      Codec.INT.optionalFieldOf("color", -1).forGetter(Unbaked::color)
    ).apply(inst, Unbaked::new));

    @Override
    public BlockStateModel bake(ModelBaker baker) {
      ResolvedModel model = baker.getModel(variant.modelLocation());
      ModelState modelState = variant.modelState().asModelState();
      TextureSlots slots = model.getTopTextureSlots();
      boolean ambientOcclusion = model.getTopAmbientOcclusion();
      Material.Baked particle = model.resolveParticleMaterial(slots, baker);
      QuadCollection baseQuads = model.bakeTopGeometry(slots, baker, modelState);
      if (color != -1) {
        baseQuads = modernmods.mantle.client.model.util.ColoredBlockModel.applyColorQuadTransformer(color).process(baseQuads);
      }

      // for each connected texture, resolve its base sprite and the sixteen suffix sprites (indexed by the 2D key)
      Map<TextureAtlasSprite,TextureAtlasSprite[]> connectedSprites = new HashMap<>();
      for (Map.Entry<String,String[]> entry : connection.textures().entrySet()) {
        Material base = slots.getMaterial(entry.getKey());
        if (base == null) {
          continue;
        }
        TextureAtlasSprite baseSprite = baker.materials().get(base, model).sprite();
        String[] suffixes = entry.getValue();
        TextureAtlasSprite[] sprites = new TextureAtlasSprite[suffixes.length];
        for (int i = 0; i < suffixes.length; i++) {
          String suffix = suffixes[i];
          if (suffix.isEmpty()) {
            sprites[i] = baseSprite;
          } else {
            Identifier suffixed = base.sprite().withPath(p -> p + "/" + suffix);
            sprites[i] = baker.materials().get(new Material(suffixed, base.forceTranslucent()), model).sprite();
          }
        }
        connectedSprites.put(baseSprite, sprites);
      }

      BiPredicate<BlockState,BlockState> predicate = ConnectedModelRegistry.getPredicate(connection.predicate());
      return new Baked(baseQuads, connectedSprites, particle, ambientOcclusion, predicate, connection.sides(), modelState.transformation());
    }

    @Override
    public void resolveDependencies(Resolver resolver) {
      variant.resolveDependencies(resolver);
    }

    @Override
    public MapCodec<Unbaked> codec() {
      return MAP_CODEC;
    }
  }

  /** A single named part of a baked connected model. */
  public record Part(QuadCollection quads, boolean useAmbientOcclusion, Material.Baked particleMaterial) implements BlockStateModelPart {
    @Override
    public List<BakedQuad> getQuads(@Nullable Direction direction) {
      return quads.getQuads(direction);
    }

    @Override
    public int materialFlags() {
      return quads.materialFlags();
    }
  }

  /** The baked connected block state model; rebuilds and caches the quads per six-bit connection mask. */
  public static final class Baked implements DynamicBlockStateModel {
    private final QuadCollection baseQuads;
    private final Map<TextureAtlasSprite,TextureAtlasSprite[]> connectedSprites;
    private final Material.Baked particle;
    private final boolean ambientOcclusion;
    private final BiPredicate<BlockState,BlockState> predicate;
    private final Set<Direction> sides;
    private final Transformation rotation;
    /** Parts by connection mask (0-63). 1.21.1 rebuilt on every getQuads call; here we cache the 64 possibilities. */
    private final List<BlockStateModelPart>[] cache;

    @SuppressWarnings("unchecked")
    public Baked(QuadCollection baseQuads, Map<TextureAtlasSprite,TextureAtlasSprite[]> connectedSprites, Material.Baked particle,
                 boolean ambientOcclusion, BiPredicate<BlockState,BlockState> predicate, Set<Direction> sides, Transformation rotation) {
      this.baseQuads = baseQuads;
      this.connectedSprites = connectedSprites;
      this.particle = particle;
      this.ambientOcclusion = ambientOcclusion;
      this.predicate = predicate;
      this.sides = sides;
      this.rotation = rotation;
      this.cache = new List[64];
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> output) {
      output.addAll(resolve(getConnections(level, pos, state)));
    }

    @Nullable
    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
      return getConnections(level, pos, state);
    }

    /** Computes the six-bit connection mask for the block at the given position. */
    private byte getConnections(BlockAndTintGetter level, BlockPos pos, BlockState state) {
      byte connections = 0;
      for (Direction dir : Direction.values()) {
        if (sides.contains(dir) && predicate.test(state, level.getBlockState(pos.relative(rotation.rotateTransform(dir))))) {
          connections |= 1 << dir.get3DDataValue();
        }
      }
      return connections;
    }

    /** Returns the cached parts for the given mask, building them on first use. */
    private List<BlockStateModelPart> resolve(byte connections) {
      List<BlockStateModelPart> cached = cache[connections];
      if (cached == null) {
        cached = List.of(new Part(build(connections), ambientOcclusion, particle));
        cache[connections] = cached;
      }
      return cached;
    }

    /** Rebuilds the quad collection for the given connection mask, swapping each connected face to its suffix sprite. */
    private QuadCollection build(byte connections) {
      QuadCollection.Builder builder = new QuadCollection.Builder();
      appendFace(builder, null, connections);
      for (Direction direction : Direction.values()) {
        appendFace(builder, direction, connections);
      }
      return builder.build();
    }

    private void appendFace(QuadCollection.Builder builder, @Nullable Direction side, byte connections) {
      for (BakedQuad quad : baseQuads.getQuads(side)) {
        BakedQuad result = connectQuad(quad, connections);
        if (side == null) {
          builder.addUnculledFace(result);
        } else {
          builder.addCulledFace(side, result);
        }
      }
    }

    /** Swaps a quad's sprite to the connected suffix matching its face's local connections, or returns it unchanged. */
    private BakedQuad connectQuad(BakedQuad quad, byte connections) {
      TextureAtlasSprite[] sprites = connectedSprites.get(quad.materialInfo().sprite());
      if (sprites == null) {
        return quad;
      }
      int key = faceKey(quad, connections);
      TextureAtlasSprite target = sprites[key];
      if (target == quad.materialInfo().sprite()) {
        return quad;
      }
      return reskinQuad(quad, target);
    }

    /**
     * Computes the sixteen-value 2D connection key for a quad by deriving the texture's up/right axes in world space from
     * the quad geometry (vertex positions + UVs), then testing the connection mask in those world directions. Unlike a
     * fixed cube-face transform this is correct for ANY face orientation, so rotated pane faces connect the same as cube
     * faces. Suffix 2D bits follow the 1.21.1 convention: NORTH=texture-up, SOUTH=down, WEST=left, EAST=right.
     * <p>
     * v1 and v3 are the two corners adjacent to v0, so they span the face; solving the 2x2 UV system gives the world
     * vectors for +U (texture right) and +V. Texture "up" is -V because sprite V grows downward.
     */
    private static int faceKey(BakedQuad quad, byte connections) {
      float p0x = quad.position0().x(), p0y = quad.position0().y(), p0z = quad.position0().z();
      float dx1 = quad.position1().x() - p0x, dy1 = quad.position1().y() - p0y, dz1 = quad.position1().z() - p0z;
      float dx3 = quad.position3().x() - p0x, dy3 = quad.position3().y() - p0y, dz3 = quad.position3().z() - p0z;
      float u0 = UVPair.unpackU(quad.packedUV0()), v0 = UVPair.unpackV(quad.packedUV0());
      float du1 = UVPair.unpackU(quad.packedUV1()) - u0, dv1 = UVPair.unpackV(quad.packedUV1()) - v0;
      float du3 = UVPair.unpackU(quad.packedUV3()) - u0, dv3 = UVPair.unpackV(quad.packedUV3()) - v0;
      float det = du1 * dv3 - du3 * dv1;
      if (Math.abs(det) < 1.0e-9f) {
        return faceKeyFallback(quad.direction(), connections);
      }
      // dPos/dU (texture right) and dPos/dV, per world component
      float rx = (dv3 * dx1 - dv1 * dx3) / det, ry = (dv3 * dy1 - dv1 * dy3) / det, rz = (dv3 * dz1 - dv1 * dz3) / det;
      float vx = (du1 * dx3 - du3 * dx1) / det, vy = (du1 * dy3 - du3 * dy1) / det, vz = (du1 * dz3 - du3 * dz1) / det;
      Direction right = nearestDirection(rx, ry, rz);
      Direction up = nearestDirection(-vx, -vy, -vz);
      int key = 0;
      if (connectedIn(connections, up))                key |= 1 << Direction.NORTH.get2DDataValue();
      if (connectedIn(connections, up.getOpposite()))  key |= 1 << Direction.SOUTH.get2DDataValue();
      if (connectedIn(connections, right.getOpposite())) key |= 1 << Direction.WEST.get2DDataValue();
      if (connectedIn(connections, right))             key |= 1 << Direction.EAST.get2DDataValue();
      return key;
    }

    /** Fallback for degenerate UVs: the fixed cube-face transform (identity-based), matching the pre-geometry behaviour. */
    private static int faceKeyFallback(Direction face, byte connections) {
      int key = 0;
      for (Direction dir : Plane.HORIZONTAL) {
        if (connectedIn(connections, ConnectedModel.rotateDirection(dir, face))) {
          key |= 1 << dir.get2DDataValue();
        }
      }
      return key;
    }

    /** True if the connection mask has the bit for the given world direction. */
    private static boolean connectedIn(byte connections, Direction dir) {
      int flag = 1 << dir.get3DDataValue();
      return (connections & flag) == flag;
    }

    /** The world Direction whose unit vector best matches the given world-space vector. */
    private static Direction nearestDirection(float x, float y, float z) {
      Direction best = Direction.NORTH;
      float bestDot = -Float.MAX_VALUE;
      for (Direction dir : Direction.values()) {
        float dot = x * dir.getStepX() + y * dir.getStepY() + z * dir.getStepZ();
        if (dot > bestDot) {
          bestDot = dot;
          best = dir;
        }
      }
      return best;
    }

    @Override
    public Material.Baked particleMaterial() {
      return particle;
    }

    @Override
    public int materialFlags() {
      return baseQuads.materialFlags();
    }
  }

  /* Sprite swapping (mirrors RetexturedBlockStateModel) */

  /** Copies a quad with its sprite swapped to {@code to}, translating UVs from the old sprite origin to the new. */
  private static BakedQuad reskinQuad(BakedQuad shape, TextureAtlasSprite to) {
    TextureAtlasSprite from = shape.materialInfo().sprite();
    float du = to.getU0() - from.getU0();
    float dv = to.getV0() - from.getV0();
    BakedQuad.MaterialInfo old = shape.materialInfo();
    BakedQuad.MaterialInfo material = new BakedQuad.MaterialInfo(
      to, old.layer(), old.itemRenderType(), old.tintIndex(), old.shade(), old.lightEmission(), old.ambientOcclusion());
    return new BakedQuad(
      shape.position0(), shape.position1(), shape.position2(), shape.position3(),
      shiftUV(shape.packedUV0(), du, dv),
      shiftUV(shape.packedUV1(), du, dv),
      shiftUV(shape.packedUV2(), du, dv),
      shiftUV(shape.packedUV3(), du, dv),
      shape.direction(),
      material,
      shape.bakedNormals(),
      shape.bakedColors());
  }

  private static long shiftUV(long packedUV, float du, float dv) {
    return UVPair.pack(UVPair.unpackU(packedUV) + du, UVPair.unpackV(packedUV) + dv);
  }
}
