package slimeknights.mantle.client.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.util.RetexturedHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The block-state half of the {@code mantle:retextured} system, replacing the removed 1.21.1 dynamic {@code BakedModel}
 * that re-textured itself in {@code getQuads(state, side, rand, ModelData, ...)}.
 * <p>
 * In 26.1 the block model bake produces a static {@link QuadCollection} with no access to {@link net.neoforged.neoforge.model.data.ModelData};
 * the only place that still sees the world (and therefore the block entity's model data) is a {@link BlockStateModel}.
 * So the retexture is implemented here as a {@link CustomUnbakedBlockStateModel} registered on
 * {@code RegisterBlockStateModels} under {@link #ID} and selected by a {@code "type": "mantle:retextured"} key inside the
 * variant object of the block's blockstate JSON. The variant is a vanilla {@link Variant} (so {@code model}/{@code x}/
 * {@code y}/{@code uvlock} still apply) plus a {@code "retextured"} array naming the texture slots to swap.
 * <p>
 * At bake time it records the original sprite of each named slot. At collect time it reads {@link RetexturedHelper#BLOCK_PROPERTY}
 * from the block's model data and rewrites every quad using one of those sprites to the particle sprite of the stored block
 * (matching the 1.21.1 {@code ModelHelper.getParticleTexture(block)} behaviour), caching the result per texture block.
 * @see RetexturedModel  the model-loader half ({@code "loader": "mantle:retextured"})
 */
public final class RetexturedBlockStateModel {
  /** Registered id — the value of the {@code "type"} key in a retextured block's blockstate variant. */
  public static final Identifier ID = Mantle.getResource("retextured");

  private RetexturedBlockStateModel() {}

  /**
   * Unbaked retextured block state model: a vanilla {@link Variant} plus the list of texture slot names to swap.
   * @param variant     Wrapped vanilla variant (model reference + rotation)
   * @param retextured  Texture slot names whose quads get swapped to the stored block's texture
   */
  public record Unbaked(Variant variant, List<String> retextured) implements CustomUnbakedBlockStateModel {
    public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
      Variant.MAP_CODEC.forGetter(Unbaked::variant),
      Codec.STRING.listOf().fieldOf("retextured").forGetter(Unbaked::retextured)
    ).apply(inst, Unbaked::new));

    @Override
    public BlockStateModel bake(ModelBaker baker) {
      ResolvedModel model = baker.getModel(variant.modelLocation());
      ModelState state = variant.modelState().asModelState();
      TextureSlots slots = model.getTopTextureSlots();
      boolean ambientOcclusion = model.getTopAmbientOcclusion();
      Material.Baked particle = model.resolveParticleMaterial(slots, baker);
      QuadCollection baseQuads = model.bakeTopGeometry(slots, baker, state);
      return new Baked(baseQuads, resolveRetexturedSprites(slots, retextured, baker, model), particle, ambientOcclusion);
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

  /** A single named part of a baked retextured model (mirrors the vanilla single-variant part). */
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

  /** Cache key for a fully re-textured model: the baked model plus the texture block's id. */
  private record GeometryKey(Baked model, String textureBlock) {}

  /** The baked retextured block state model; swaps the named-slot quads per {@link RetexturedHelper#BLOCK_PROPERTY}. */
  public static final class Baked implements DynamicBlockStateModel {
    private final QuadCollection baseQuads;
    private final List<TextureAtlasSprite> originalSprites;
    private final Material.Baked particle;
    private final boolean ambientOcclusion;
    private final List<BlockStateModelPart> plainParts;
    /** Re-textured parts by texture-block id. New in 26.1: 1.21.1 re-textured on every getQuads call. */
    private final Map<Block,List<BlockStateModelPart>> cache = new ConcurrentHashMap<>();

    public Baked(QuadCollection baseQuads, List<TextureAtlasSprite> originalSprites, Material.Baked particle, boolean ambientOcclusion) {
      this.baseQuads = baseQuads;
      this.originalSprites = originalSprites;
      this.particle = particle;
      this.ambientOcclusion = ambientOcclusion;
      this.plainParts = List.of(new Part(baseQuads, ambientOcclusion, particle));
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> output) {
      output.addAll(resolve(level.getModelData(pos).get(RetexturedHelper.BLOCK_PROPERTY)));
    }

    @Nullable
    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
      Block block = level.getModelData(pos).get(RetexturedHelper.BLOCK_PROPERTY);
      return new GeometryKey(this, block == null ? "" : BuiltInRegistries.BLOCK.getKey(block).toString());
    }

    /** Resolves the parts for the given texture block, or the plain parts if none/unretextured. */
    public List<BlockStateModelPart> resolve(@Nullable Block block) {
      if (block == null || block == Blocks.AIR || originalSprites.isEmpty()) {
        return plainParts;
      }
      return cache.computeIfAbsent(block, this::build);
    }

    private List<BlockStateModelPart> build(Block block) {
      return List.of(new Part(reskin(baseQuads, originalSprites, blockSprite(block)), ambientOcclusion, particle));
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


  /* Re-texturing (shared with the item model half) */

  /** Resolves the baked sprite of each named retextured slot, for later matching against the baked quads. */
  public static List<TextureAtlasSprite> resolveRetexturedSprites(TextureSlots slots, List<String> retextured, ModelBaker baker, ResolvedModel model) {
    List<TextureAtlasSprite> originals = new ArrayList<>(retextured.size());
    for (String name : retextured) {
      Material material = slots.getMaterial(name);
      if (material != null) {
        originals.add(baker.materials().get(material, model).sprite());
      }
    }
    return List.copyOf(originals);
  }

  /** Gets the particle sprite of the given block, the texture retextured slots are swapped to. */
  public static TextureAtlasSprite blockSprite(Block block) {
    return Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(block.defaultBlockState()).sprite();
  }

  /** Rewrites every quad in {@code base} whose sprite is one of {@code originals} to use {@code target}. */
  public static QuadCollection reskin(QuadCollection base, List<TextureAtlasSprite> originals, TextureAtlasSprite target) {
    QuadCollection.Builder builder = new QuadCollection.Builder();
    appendReskinned(builder, base, null, originals, target);
    for (Direction direction : Direction.values()) {
      appendReskinned(builder, base, direction, originals, target);
    }
    return builder.build();
  }

  private static void appendReskinned(QuadCollection.Builder builder, QuadCollection base, @Nullable Direction side, List<TextureAtlasSprite> originals, TextureAtlasSprite target) {
    for (BakedQuad quad : base.getQuads(side)) {
      BakedQuad result = matchesRetextured(quad, originals) ? reskinQuad(quad, target) : quad;
      if (side == null) {
        builder.addUnculledFace(result);
      } else {
        builder.addCulledFace(side, result);
      }
    }
  }

  /** True if the quad's sprite is one of the retextured slots' original sprites (atlas sprites are singletons). */
  private static boolean matchesRetextured(BakedQuad quad, List<TextureAtlasSprite> originals) {
    TextureAtlasSprite sprite = quad.materialInfo().sprite();
    for (TextureAtlasSprite original : originals) {
      if (original == sprite) {
        return true;
      }
    }
    return false;
  }

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
