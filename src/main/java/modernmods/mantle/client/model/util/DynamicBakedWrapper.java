package modernmods.mantle.client.model.util;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;

import java.util.List;

/**
 * Base class for a block state model that delegates to another while dynamically swapping the rendered model based on the
 * block's {@link ModelData}.
 * <p>
 * In 26.1.2 the {@code BakedModelWrapper}/{@code IDynamicBakedModel} system was replaced by {@link BlockStateModel} and
 * {@link DelegateBlockStateModel}. Where the old wrapper overrode {@code getQuads(BlockState, side, rand, ModelData, ...)},
 * this overrides the context-aware {@link BlockStateModel#collectParts(BlockAndTintGetter, BlockPos, BlockState, RandomSource, List)}
 * and reads {@link BlockAndTintGetter#getModelData(BlockPos)} to pick the model to render. Subclasses implement
 * {@link #getModel(ModelData)} to return the (usually cached) delegate for a given data snapshot.
 * <p>
 * The deprecated context-free {@code collectParts(RandomSource, List)} still delegates to {@link #originalModel} for callers
 * (e.g. inventory-style baking) that render without block context.
 * @param <T>  Wrapped block state model type
 */
@SuppressWarnings("WeakerAccess")
public abstract class DynamicBakedWrapper<T extends BlockStateModel> extends DelegateBlockStateModel {
  protected final T originalModel;

  protected DynamicBakedWrapper(T originalModel) {
    super(originalModel);
    this.originalModel = originalModel;
  }

  /**
   * Gets the block state model to render for the given data snapshot, allowing dynamic swapping (e.g. per material or
   * per retexture). Defaults to the original model; override to return a cached dynamic model.
   * @param data  Model data for the block being rendered, or {@link ModelData#EMPTY} if none
   * @return  Model to render
   */
  protected BlockStateModel getModel(ModelData data) {
    return originalModel;
  }

  @Override
  public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
    getModel(level.getModelData(pos)).collectParts(level, pos, state, random, parts);
  }

  @Override
  public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
    return getModel(level.getModelData(pos)).particleMaterial(level, pos, state);
  }

  @Override
  public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
    return getModel(level.getModelData(pos)).materialFlags(level, pos, state);
  }

  @Override
  public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
    return getModel(level.getModelData(pos)).createGeometryKey(level, pos, state, random);
  }
}
