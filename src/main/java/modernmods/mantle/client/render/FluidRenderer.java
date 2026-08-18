package modernmods.mantle.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import modernmods.mantle.client.render.FluidCuboid.FluidFace;

import java.util.List;

@SuppressWarnings({"WeakerAccess", "unused"})
public class FluidRenderer {
  /**
   * Gets a block sprite from the given location
   * @param sprite  Sprite name
   * @return  Sprite location
   */
  public static TextureAtlasSprite getBlockSprite(Identifier sprite) {
    // 26.1: AtlasManager keys atlases by their atlas id (AtlasIds.BLOCKS), not the texture location (LOCATION_BLOCKS);
    // passing the texture location throws "Invalid atlas id" and crashes any screen rendering a fluid/block sprite.
    return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS).getSprite(sprite);
  }

  /** Client-side visual attributes of a fluid: still/flowing sprites and tint color */
  public record FluidTextures(TextureAtlasSprite still, TextureAtlasSprite flowing, int color) {}

  /**
   * Looks up the still/flowing sprites and tint color for the given fluid using the 26.1 fluid model system
   * ({@link net.minecraft.client.renderer.block.FluidStateModelSet}), replacing the removed client fluid extension
   * texture accessors. Tint is resolved position-independently via {@link FluidTintSource#colorAsStack(FluidStack)}.
   */
  public static FluidTextures getFluidTextures(FluidStack fluid) {
    FluidState state = fluid.getFluid().defaultFluidState();
    FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(state);
    int color = -1;
    if (model.tintSource() instanceof FluidTintSource tint) {
      color = tint.colorAsStack(fluid);
    }
    return new FluidTextures(model.stillMaterial().sprite(), model.flowingMaterial().sprite(), color);
  }

  /**
   * Takes the larger light value between combinedLight and the passed block light
   * @param combinedLight  Sky light/block light lightmap value
   * @param blockLight     New 0-15 block light value
   * @return  Updated packed light including the new light value
   */
  public static int withBlockLight(int combinedLight, int blockLight) {
    // skylight from the combined plus larger block light between combined and parameter
    // not using methods from LightTexture to reduce number of operations
    return (combinedLight & 0xFFFF0000) | Math.max(blockLight << 4, combinedLight & 0xFFFF);
  }

  private static void putVertex(VertexConsumer renderer, Matrix4f matrix, Direction face, float x, float y, float z, int r, int g, int b, int a, float u, float v, int light1, int light2) {
    // the block render pipeline (MantleRenderTypes.FLUID uses the BLOCK vertex format) needs a normal for lighting; without
    // it the quads light as fully dark. The face direction is the quad's normal.
    renderer.addVertex(matrix, x, y, z).setColor(r, g, b, a).setUv(u, v).setUv2(light1, light2).setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
  }

  /* Fluid cuboids */

  /**
   * Forces the UV to be between 0 and 1
   * @param value  Original value
   * @param upper  If true, this is the larger UV. Needed to enforce integer values end up at 1
   * @return  UV mapped between 0 and 1
   */
  private static float boundUV(float value, boolean upper) {
    value = value % 1;
    if (value == 0) {
      // if it lands exactly on the 0 bound, map that to 1 instead for the larger UV
      return upper ? 1 : 0;
    }
    // modulo returns a negative result if the input is negative, so add 1 to account for that
    return value < 0 ? (value + 1) : value;
  }

  /**
   * Adds a quad to the renderer
   * @param renderer    Renderer instnace
   * @param matrix      Render matrix
   * @param sprite      Sprite to render
   * @param from        Quad start
   * @param to          Quad end
   * @param face        Face to render
   * @param color       Color to use in rendering
   * @param brightness  Face brightness
   * @param flowing     If true, half texture coordinates
   */
  public static void putTexturedQuad(VertexConsumer renderer, Matrix4f matrix, TextureAtlasSprite sprite, Vector3f from, Vector3f to, Direction face, int color, int brightness, int rotation, boolean flowing) {
    // start with texture coordinates
    float x1 = from.x(), y1 = from.y(), z1 = from.z();
    float x2 = to.x(), y2 = to.y(), z2 = to.z();
    // choose UV based on the directions, some need to negate UV due to the direction
    // note that we use -UV instead of 1-UV as its slightly simpler and the later logic deals with negatives
    float u1, u2, v1, v2;
    switch (face) {
      default -> { // DOWN
        u1 = x1; u2 = x2;
        v1 = z2; v2 = z1;
      }
      case UP -> {
        u1 = x1; u2 = x2;
        v1 = -z1; v2 = -z2;
      }
      case NORTH -> {
        u1 = -x1; u2 = -x2;
        v1 = y1; v2 = y2;
      }
      case SOUTH -> {
        u1 = x2; u2 = x1;
        v1 = y1; v2 = y2;
      }
      case WEST -> {
        u1 = z2; u2 = z1;
        v1 = y1; v2 = y2;
      }
      case EAST -> {
        u1 = -z1; u2 = -z2;
        v1 = y1; v2 = y2;
      }
    }

    // flip V when relevant
    if (rotation == 0 || rotation == 270) {
      float temp = v1;
      v1 = -v2;
      v2 = -temp;
    }
    // flip U when relevant
    if (rotation >= 180) {
      float temp = u1;
      u1 = -u2;
      u2 = -temp;
    }

    // bound UV to be between 0 and 1
    boolean reverse = u1 > u2;
    u1 = boundUV(u1, reverse);
    u2 = boundUV(u2, !reverse);
    reverse = v1 > v2;
    v1 = boundUV(v1, reverse);
    v2 = boundUV(v2, !reverse);

    // if rotating by 90 or 270, swap U and V
    float minU, maxU, minV, maxV;
    float size = flowing ? 0.5f : 1;
    if ((rotation % 180) == 90) {
      minU = sprite.getU(v1 * size);
      maxU = sprite.getU(v2 * size);
      minV = sprite.getV(u1 * size);
      maxV = sprite.getV(u2 * size);
    } else {
      minU = sprite.getU(u1 * size);
      maxU = sprite.getU(u2 * size);
      minV = sprite.getV(v1 * size);
      maxV = sprite.getV(v2 * size);
    }
    // based on rotation, put coords into place
    float u3, u4, v3, v4;
    switch(rotation) {
      default -> { // 0
        u1 = minU; v1 = maxV;
        u2 = minU; v2 = minV;
        u3 = maxU; v3 = minV;
        u4 = maxU; v4 = maxV;
      }
      case 90 -> {
        u1 = minU; v1 = minV;
        u2 = maxU; v2 = minV;
        u3 = maxU; v3 = maxV;
        u4 = minU; v4 = maxV;
      }
      case 180 -> {
        u1 = maxU; v1 = minV;
        u2 = maxU; v2 = maxV;
        u3 = minU; v3 = maxV;
        u4 = minU; v4 = minV;
      }
      case 270 -> {
        u1 = maxU; v1 = maxV;
        u2 = minU; v2 = maxV;
        u3 = minU; v3 = minV;
        u4 = maxU; v4 = minV;
      }
    }
    // add quads
    int light1 = brightness & 0xFFFF;
    int light2 = brightness >> 0x10 & 0xFFFF;
    int a = color >> 24 & 0xFF;
    int r = color >> 16 & 0xFF;
    int g = color >> 8 & 0xFF;
    int b = color & 0xFF;
    // the four corner positions of this face (paired with the u1..u4/v1..v4 computed above)
    float[][] pos = switch (face) {
      case DOWN  -> new float[][]{{x1, y1, z2}, {x1, y1, z1}, {x2, y1, z1}, {x2, y1, z2}};
      case UP    -> new float[][]{{x1, y2, z1}, {x1, y2, z2}, {x2, y2, z2}, {x2, y2, z1}};
      case NORTH -> new float[][]{{x1, y1, z1}, {x1, y2, z1}, {x2, y2, z1}, {x2, y1, z1}};
      case SOUTH -> new float[][]{{x2, y1, z2}, {x2, y2, z2}, {x1, y2, z2}, {x1, y1, z2}};
      case WEST  -> new float[][]{{x1, y1, z2}, {x1, y2, z2}, {x1, y2, z1}, {x1, y1, z1}};
      case EAST  -> new float[][]{{x2, y1, z1}, {x2, y2, z1}, {x2, y2, z2}, {x2, y1, z2}};
    };
    float[] us = {u1, u2, u3, u4};
    float[] vs = {v1, v2, v3, v4};
    // front face, wound for the face's own normal
    for (int i = 0; i < 4; i++) {
      putVertex(renderer, matrix, face, pos[i][0], pos[i][1], pos[i][2], r, g, b, a, us[i], vs[i], light1, light2);
    }
    // 26.1 fluid rendering now uses a vanilla (shader-visible) render type that culls back faces; the old bespoke pipeline
    // disabled culling. Emit the mirrored quad (reversed winding + opposite normal) so each fluid face still shows from both
    // sides (inside the tank, gasses), preserving the no-cull appearance.
    Direction back = face.getOpposite();
    for (int i = 3; i >= 0; i--) {
      putVertex(renderer, matrix, back, pos[i][0], pos[i][1], pos[i][2], r, g, b, a, us[i], vs[i], light1, light2);
    }
  }

  /**
   * Renders a full fluid cuboid for the given data
   * @param matrices  Matrix stack instance
   * @param buffer    Buffer type
   * @param still     Still sprite
   * @param flowing   Flowing sprite
   * @param cube      Fluid cuboid
   * @param from      Fluid start
   * @param to        Fluid end
   * @param color     Fluid color
   * @param light     Quad lighting
   * @param isGas     If true, fluid is a gas
   */
  public static void renderCuboid(PoseStack matrices, VertexConsumer buffer, FluidCuboid cube, TextureAtlasSprite still, TextureAtlasSprite flowing, Vector3f from, Vector3f to, int color, int light, boolean isGas) {
    Matrix4f matrix = matrices.last().pose();
    int rotation = isGas ? 180 : 0;
    for (Direction dir : Direction.values()) {
      FluidFace face = cube.getFace(dir);
      if (face != null) {
        boolean isFlowing = face.isFlowing();
        int faceRot = (rotation + face.rotation()) % 360;
        putTexturedQuad(buffer, matrix, isFlowing ? flowing : still, from, to, dir, color, light, faceRot, isFlowing);
      }
    }
  }

  /**
   * Renders a list of fluid cuboids
   * @param matrices  Matrix stack instance
   * @param buffer    Buffer instance
   * @param cubes     List of cubes to render
   * @param fluid     Fluid to use in rendering
   * @param light     Light level from TER
   */
  public static void renderCuboids(PoseStack matrices, VertexConsumer buffer, List<FluidCuboid> cubes, FluidStack fluid, int light) {
    if (fluid.isEmpty()) {
      return;
    }

    // fluid attributes, fetch once for all fluids to save effort
    FluidTextures textures = getFluidTextures(fluid);
    TextureAtlasSprite still = textures.still();
    TextureAtlasSprite flowing = textures.flowing();
    int color = textures.color();
    FluidType type = fluid.getFluid().getFluidType();
    light = withBlockLight(light, type.getLightLevel(fluid));
    boolean isGas = type.isLighterThanAir();

    // render all given cuboids
    for (FluidCuboid cube : cubes) {
      renderCuboid(matrices, buffer, cube, still, flowing, cube.getFromScaled(), cube.getToScaled(), color, light, isGas);
    }
  }

  /**
   * Renders a fluid cuboid with the given offset, used to manually place cuboids from a list for rendering {@link #renderCuboids(PoseStack, VertexConsumer, List, FluidStack, int)}
   * @param matrices  Matrix stack instance
   * @param buffer    Buffer type
   * @param cube      Fluid cuboid
   * @param yOffset   Amount to offset the cube in the Y direction, used in faucets for rendering fluid in lower block
   * @param still     Still sprite
   * @param flowing   Flowing sprite
   * @param color     Fluid color
   * @param light     Quad lighting from TER
   * @param isGas     If true, fluid is a gas
   */
  public static void renderCuboid(PoseStack matrices, VertexConsumer buffer, FluidCuboid cube, float yOffset, TextureAtlasSprite still, TextureAtlasSprite flowing, int color, int light, boolean isGas) {
    if (yOffset != 0) {
      matrices.pushPose();
      matrices.translate(0, yOffset, 0);
    }
    renderCuboid(matrices, buffer, cube, still, flowing, cube.getFromScaled(), cube.getToScaled(), color, light, isGas);
    if (yOffset != 0) {
      matrices.popPose();
    }
  }

  /**
   * Renders a fluid cuboid with partial height based on the capacity
   * @param matrices  Matrix stack instance
   * @param buffer    Render type buffer instance
   * @param fluid     Fluid to render
   * @param offset    Fluid amount offset, used to animate transitions
   * @param capacity  Fluid tank capacity, must be above 0
   * @param light     Quad lighting from TER
   * @param cube      Fluid cuboid instance
   * @param flipGas   If true, flips gas cubes
   */
  public static void renderScaledCuboid(PoseStack matrices, MultiBufferSource buffer, FluidCuboid cube, FluidStack fluid, float offset, int capacity, int light, boolean flipGas) {
    renderScaledCuboid(matrices, buffer.getBuffer(MantleRenderTypes.FLUID), cube, fluid, offset, capacity, light, flipGas);
  }

  /**
   * Renders a fluid cuboid with partial height based on capacity, drawing directly to a {@link VertexConsumer}. Used by
   * 26.1 block entity renderers that submit fluid geometry via {@code SubmitNodeCollector.submitCustomGeometry}, which
   * hands back a single buffer for the {@link MantleRenderTypes#FLUID} render type rather than a {@link MultiBufferSource}.
   */
  public static void renderScaledCuboid(PoseStack matrices, VertexConsumer buffer, FluidCuboid cube, FluidStack fluid, float offset, int capacity, int light, boolean flipGas) {
    // nothing to render
    if (fluid.isEmpty() || capacity <= 0) {
      return;
    }

    // fluid attributes
    FluidTextures textures = getFluidTextures(fluid);
    TextureAtlasSprite still = textures.still();
    TextureAtlasSprite flowing = textures.flowing();
    int color = textures.color();
    FluidType type = fluid.getFluid().getFluidType();
    boolean isGas = type.isLighterThanAir();
    light = withBlockLight(light, type.getLightLevel(fluid));

    // determine height based on fluid amount
    Vector3f from = cube.getFromScaled();
    Vector3f to = cube.getToScaled();
    // gas renders upside down
    float minY = from.y();
    float maxY = to.y();
    float height = (fluid.getAmount() - offset) / capacity;
    if (isGas && flipGas) {
      from = new Vector3f(from);
      from.y = maxY + (height * (minY - maxY));
    } else {
      to = new Vector3f(to);
      to.y = minY + (height * (maxY - minY));
    }

    // draw cuboid
    renderCuboid(matrices, buffer, cube, still, flowing, from, to, color, light, isGas);
  }

  /**
   * Same as {@code net.minecraft.client.renderer.ScreenEffectRenderer#renderFluid} but with opacity and color control.
   *
   * This used the removed immediate-mode render API - RenderSystem.setShader/setShaderTexture/setShaderColor/
   * enableBlend, GameRenderer.getPositionTexShader, Tesselator+BufferBuilder+BufferUploader.drawWithShader, and
   * LightTexture.getBrightness - none of which survive the 1.21.4+ RenderPipeline rewrite. The whole in-camera fluid
   * overlay draw needs re-expressing via a RenderPipeline (see RenderPipelines) and the GuiGraphics/screen-effect path.
   * Stubbed to a no-op; original body preserved below.
   */
  public static void renderCamera(Minecraft minecraft, PoseStack poseStack, Identifier texture, float opacity, int color) {
    // assert minecraft.player != null;
    // RenderSystem.setShader(GameRenderer::getPositionTexShader);
    // RenderSystem.setShaderTexture(0, texture);
    // BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
    // BlockPos pos = BlockPos.containing(minecraft.player.getX(), minecraft.player.getEyeY(), minecraft.player.getZ());
    // Level level = minecraft.player.level();
    // float brightness = LightTexture.getBrightness(level.dimensionType(), level.getMaxLocalRawBrightness(pos));
    // RenderSystem.enableBlend();
    // if (color != -1) {
    //   RenderSystem.setShaderColor(
    //     brightness * (color >> 16 & 255) / 255f,
    //     brightness * (color >> 8 & 255) / 255f,
    //     brightness * (color & 255) / 255f,
    //     opacity * (color >>> 24) / 255f);
    // } else {
    //   RenderSystem.setShaderColor(brightness, brightness, brightness, opacity);
    // }
    // float yRot = -minecraft.player.getYRot() / 64;
    // float xRot = minecraft.player.getXRot() / 64;
    // Matrix4f matrix = poseStack.last().pose();
    // buffer.addVertex(matrix, -1, -1, -0.5f).setUv(4 + yRot, 4 + xRot);
    // buffer.addVertex(matrix,  1, -1, -0.5f).setUv(0 + yRot, 4 + xRot);
    // buffer.addVertex(matrix,  1,  1, -0.5f).setUv(0 + yRot, 0 + xRot);
    // buffer.addVertex(matrix, -1,  1, -0.5f).setUv(4 + yRot, 0 + xRot);
    // BufferUploader.drawWithShader(buffer.buildOrThrow());
    // RenderSystem.setShaderColor(1, 1, 1, 1);
    // RenderSystem.disableBlend();
  }
}
