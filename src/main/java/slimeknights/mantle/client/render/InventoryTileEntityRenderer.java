package slimeknights.mantle.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * @deprecated use {@link InventoryBlockEntityRenderer} for the new render item registry.
 *
 * TODO(26.1.2): ported to a correct-shaped stub for the 1.21.5+ render-state pipeline; see
 * {@link InventoryBlockEntityRenderer} for details. Original logic preserved in the comment below.
 */
@Deprecated(forRemoval = true)
public class InventoryTileEntityRenderer<T extends BlockEntity & Container> implements BlockEntityRenderer<T, BlockEntityRenderState> {
  public InventoryTileEntityRenderer(BlockEntityRendererProvider.Context context) {}

  @Override
  public BlockEntityRenderState createRenderState() {
    return new BlockEntityRenderState();
  }

  @Override
  public void submit(BlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
    // TODO(26.1.2): reimplement against the new SubmitNodeCollector pipeline.
    // Original logic (immediate mode, pre-26.1.2):
    //   if (inventory.isEmpty()) return;
    //   BlockState state = inventory.getBlockState();
    //   List<RenderItem> renderItems = RenderItem.REGISTRY.get(state.getBlock(), List.of());
    //   if (!renderItems.isEmpty()) {
    //     boolean isRotated = RenderingHelper.applyRotation(matrices, state);
    //     for (int i = 0; i < renderItems.size(); i++) {
    //       RenderingHelper.renderItem(matrices, buffer, inventory.getItem(i), renderItems.get(i), light);
    //     }
    //     if (isRotated) { matrices.popPose(); }
    //   }
  }
}
