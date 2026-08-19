package modernmods.hilt.fluid.texture;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Vector3f;
import org.joml.Vector4f;
import modernmods.hilt.client.render.FluidRenderer;

import javax.annotation.Nullable;

/** Implementation of {@link IClientFluidTypeExtensions} using {@link FluidTexture} */
public class ClientTextureFluidType implements IClientFluidTypeExtensions {
  protected final FluidType type;
  private Vector3f fogColor;

  public ClientTextureFluidType(FluidType type) {
    this.type = type;
  }

  /* Note: the still/flowing/overlay/tint texture hooks were removed from IClientFluidTypeExtensions.
   * Fluid textures are now supplied through the FluidStateModelSet/model system. These accessors are kept for
   * internal use (and for other Hilt code) but no longer participate in fluid rendering. */
  public int getTintColor() {
    return FluidTextureManager.getColor(type);
  }

  public Identifier getStillTexture() {
    return FluidTextureManager.getStillTexture(type);
  }

  public Identifier getFlowingTexture() {
    return FluidTextureManager.getFlowingTexture(type);
  }

  @Nullable
  public Identifier getOverlayTexture() {
    return FluidTextureManager.getOverlayTexture(type);
  }

  @Nullable
  @Override
  public Identifier getRenderOverlayTexture(Minecraft mc) {
    return FluidTextureManager.getCameraTexture(type);
  }

  @Override
  public void renderOverlay(Minecraft mc, PoseStack poseStack, MultiBufferSource buffer) {
    FluidTexture data = FluidTextureManager.getData(type);
    Identifier camera = data.camera();
    if (camera != null) {
      FluidRenderer.renderCamera(mc, poseStack, camera, data.cameraOpacity(), data.color());
    }
  }

  @Override
  public void modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector4f fluidFogColor) {
    // nothing to do if fog color is white
    int fluidColor = FluidTextureManager.getData(type).fogColor();
    if (fluidColor != -1) {
      // cache the vector for fog color to reduce computation time
      if (fogColor == null) {
        fogColor = new Vector3f(ARGB.red(fluidColor) / 255f, ARGB.green(fluidColor) / 255f, ARGB.blue(fluidColor) / 255f);
      }
      fluidFogColor.x *= fogColor.x;
      fluidFogColor.y *= fogColor.y;
      fluidFogColor.z *= fogColor.z;
    }
  }

  @Override
  public void modifyFogRender(Camera camera, FogEnvironment environment, float renderDistance, float partialTick, FogData fogData) {
    FluidTexture data = FluidTextureManager.getData(type);
    // Note: fog shape was removed; we now only clamp the environmental fog start/end when a custom range is set
    if (data.customFog()) {
      float start = data.fogStart();
      if (start < fogData.environmentalStart) {
        fogData.environmentalStart = start;
      }
      float end = data.fogEnd();
      if (end < fogData.environmentalEnd) {
        fogData.environmentalEnd = end;
      }
    }
  }
}
