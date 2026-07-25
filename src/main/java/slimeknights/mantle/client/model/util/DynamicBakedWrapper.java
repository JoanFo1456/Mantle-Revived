package slimeknights.mantle.client.model.util;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;

/**
 * Base class for a block state model that delegates to another while dynamically swapping out parts.
 * <p>
 * In 26.1.2 the {@code BakedModelWrapper}/{@code IDynamicBakedModel} system was replaced by {@link BlockStateModel} and
 * {@link DelegateBlockStateModel}. This retains the original wrapper's role (holding an {@code originalModel} and allowing
 * dynamic overrides of {@code collectParts}) on top of the new delegate base.
 * @param <T>  Wrapped block state model type
 */
@SuppressWarnings("WeakerAccess")
public abstract class DynamicBakedWrapper<T extends BlockStateModel> extends DelegateBlockStateModel {
  protected final T originalModel;

  protected DynamicBakedWrapper(T originalModel) {
    super(originalModel);
    this.originalModel = originalModel;
  }
}
