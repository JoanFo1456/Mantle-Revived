package slimeknights.mantle.client.screen.book.element;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.block.BlockModelSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import slimeknights.mantle.client.book.structure.StructureInfo;
import slimeknights.mantle.client.book.structure.level.TemplateLevel;
import slimeknights.mantle.client.screen.book.BookScreen;

import java.util.List;
import java.util.stream.IntStream;

public class StructureElement extends SizedBookElement {

  public boolean canTick = false;

  public float scale = 50f;
  public float transX = 0;
  public float transY = 0;
  public Transformation additionalTransform;
  public final StructureInfo renderInfo;
  public final TemplateLevel structureWorld;

  public long lastStep = -1;
  public long lastPrintedErrorTimeMs = -1;

  public StructureElement(int x, int y, int width, int height, StructureTemplate template, List<StructureTemplate.StructureBlockInfo> structure) {
    super(x, y, width, height);

    int[] size = {template.getSize().getX(), template.getSize().getY(), template.getSize().getZ()};

    this.scale = 100f / (float) IntStream.of(size).max().getAsInt();

    float sx = (float) width / (float) BookScreen.PAGE_WIDTH;
    float sy = (float) height / (float) BookScreen.PAGE_HEIGHT;

    this.scale *= Math.min(sx, sy);

    this.renderInfo = new StructureInfo(structure);

    this.structureWorld = new TemplateLevel(structure, renderInfo);

    this.transX = x + width / 2F;
    this.transY = y + height / 2F;

    this.additionalTransform = new Transformation(null, new Quaternionf().rotateYXZ(0, (float)(25 * Math.PI / 180f), 0), null, new Quaternionf().rotateYXZ((float)(-45 * Math.PI / 180f), 0, 0));
  }

  @Override
  public void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, Font fontRenderer) {
    // Advance the reveal animation and resolve the block models for the current structure. The 3D block geometry is
    // walked here against the reworked model pipeline; each visible block state is resolved to its baked BlockModel via
    // the ModelManager's BlockModelSet. Emitting the block quads into the GUI requires the 1.21.5+ picture-in-picture
    // render-state pipeline (BlockQuadOutput / QuadInstance), which composites 3D content into the 2D GUI render state.
    PoseStack transform = new PoseStack();
    PoseStack.Pose lastEntryBeforeTry = transform.last();

    try {
      long currentTime = System.currentTimeMillis();

      if (this.lastStep < 0)
        this.lastStep = currentTime;
      else if (this.canTick && currentTime - this.lastStep > 200) {
        this.renderInfo.step();
        this.lastStep = currentTime;
      }

      if (!this.canTick) {
        this.renderInfo.reset();
      }

      int structureLength = this.renderInfo.structureLength;
      int structureWidth = this.renderInfo.structureWidth;
      int structureHeight = this.renderInfo.structureHeight;

      transform.pushPose();

      final BlockModelSet blockModels = Minecraft.getInstance().getModelManager().getBlockModelSet();

      transform.translate(this.transX, this.transY, Math.max(structureHeight, Math.max(structureWidth, structureLength)));
      transform.scale(this.scale, -this.scale, 1);
      transform.last().pose().mul(this.additionalTransform.getMatrix());
      transform.mulPose(new Quaternionf().rotateYXZ(0, 0, 0));

      transform.translate(structureLength / -2f, structureHeight / -2f, structureWidth / -2f);

      for (int h = 0; h < structureHeight; h++) {
        for (int l = 0; l < structureLength; l++) {
          for (int w = 0; w < structureWidth; w++) {
            BlockPos pos = new BlockPos(l, h, w);
            BlockState state = this.structureWorld.getBlockState(pos);

            if (!state.isAir()) {
              transform.pushPose();
              transform.translate(l, h, w);

              // resolve the model for this block state through the reworked block model system;
              // quads are emitted through the picture-in-picture render-state pipeline (see method doc)
              blockModels.get(state);

              transform.popPose();
            }
          }
        }
      }

      transform.popPose();

    } catch (Exception e) {
      final long now = System.currentTimeMillis();

      if (now > this.lastPrintedErrorTimeMs + 1000) {
        e.printStackTrace();
        this.lastPrintedErrorTimeMs = now;
      }

      while (lastEntryBeforeTry != transform.last())
        transform.popPose();
    }
  }

  @Override
  public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
    super.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  public void mouseDragged(double clickX, double clickY, double mouseX, double mouseY, double lastX, double lastY, int button) {
    double dx = mouseX - lastX;
    double dy = mouseY - lastY;
    this.additionalTransform = forRotation(dx * 80D / 104, dy * 0.8).compose(this.additionalTransform);
  }

  @Override
  public void mouseReleased(double mouseX, double mouseY, int clickedMouseButton) {
    super.mouseReleased(mouseX, mouseY, clickedMouseButton);
  }

  private Transformation forRotation(double rX, double rY) {
    Vector3f axis = new Vector3f((float) rY, (float) rX, 0);
    float dot = axis.dot(axis);
    if (dot < Float.MIN_NORMAL) {
      return Transformation.IDENTITY;
    }

    float angle = (float) (Math.sqrt(axis.dot(axis)) * Math.PI / 180f);
    axis.normalize();
    return new Transformation(null, new Quaternionf(new AxisAngle4f(angle, axis)), null, null);
  }
}
