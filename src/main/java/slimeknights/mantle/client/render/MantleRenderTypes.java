package slimeknights.mantle.client.render;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * Class for render types defined by Mantle.
 *
 * TODO(26.1.2): The 1.21.4+ render engine rewrite removed {@code RenderStateShard}, {@code RenderType.CompositeState},
 * and the {@code ShaderInstance} pipeline this class was built on. Both custom render types below relied on Mantle's
 * own core shaders (fluid fog-fix + block full-bright, see {@link MantleShaders}) which no longer have an equivalent
 * registration path; custom render types now require building a {@link com.mojang.blaze3d.pipeline.RenderPipeline}
 * (see {@code net.minecraft.client.renderer.RenderPipelines}) and creating the type via
 * {@code RenderType.create(String, RenderSetup)}. Until those pipelines are ported, both fields fall back to the
 * vanilla translucent block render type so dependent code keeps compiling/rendering (without the custom shaders).
 */
public class MantleRenderTypes {
  /**
   * Render type used for the fluid renderer.
   * TODO(26.1.2): reimplement the fluid fog-fix shader as a RenderPipeline; falls back to vanilla translucent block.
   */
  public static final RenderType FLUID = RenderTypes.translucentMovingBlock();

  /**
   * Render type used for the structure renderer.
   * TODO(26.1.2): reimplement the block full-bright (emissive) shader as a RenderPipeline; falls back to vanilla translucent block.
   */
  public static final RenderType TRANSLUCENT_FULLBRIGHT = RenderTypes.translucentMovingBlock();
}
