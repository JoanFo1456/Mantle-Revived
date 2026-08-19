package modernmods.hilt.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Renders the items stored in an inventory block entity at positions defined by the {@link RenderItem} data map.
 *
 * The 1.21.5+ block entity render rewrite replaced the immediate-mode
 * {@code render(T, float, PoseStack, MultiBufferSource, int, int)} method with a two-phase
 * render-state pipeline: {@link #createRenderState()} + {@link #extractRenderState} build a
 * {@link BlockEntityRenderState}, and {@link #submit} queues geometry into a {@link SubmitNodeCollector}.
 * Item rendering inside also needs to move to the new item render pipeline (see {@link RenderingHelper}).
 * The original per-item placement logic is preserved in the comment below and needs to be re-expressed
 * against the new pipeline.
 */
public class InventoryBlockEntityRenderer<T extends BlockEntity & Container> implements BlockEntityRenderer<T, BlockEntityRenderState> {
  public InventoryBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

  @Override
  public BlockEntityRenderState createRenderState() {
    return new BlockEntityRenderState();
  }

  @Override
  public void submit(BlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
    // reimplement inventory item rendering against the new SubmitNodeCollector pipeline.
    // Original logic (immediate mode, pre-26.1.2):
    //   if (inventory.isEmpty()) return;
    //   BlockState state = inventory.getBlockState();
    //   List<RenderItem> renderItems = RenderItem.STATE_REGISTRY.get(state, List.of());
    //   if (!renderItems.isEmpty()) {
    //     boolean isRotated = RenderingHelper.applyRotation(matrices, state);
    //     for (int i = 0; i < renderItems.size(); i++) {
    //       RenderingHelper.renderItem(matrices, buffer, inventory.getItem(i), renderItems.get(i), light);
    //     }
    //     if (isRotated) { matrices.popPose(); }
    //   }
  }
}
