package modernmods.hilt.client.render;

/**
 * Handles any custom shaders registered by Hilt.
 *
 * The 1.21.4+ render engine rewrite removed {@code ShaderInstance}, {@code RegisterShadersEvent},
 * {@code GameRenderer.getPositionColorTexLightmapShader()} and the entire core-shader loading system this class
 * depended on. Core/custom shaders are now expressed as {@link com.mojang.blaze3d.pipeline.RenderPipeline}s
 * registered through the pipeline system (see {@code net.minecraft.client.renderer.RenderPipelines}). The two
 * former Hilt shaders (block full-bright + fluid fog-fix, consumed by {@link HiltRenderTypes}) need to be
 * ported to that system. Gutted to a no-op placeholder so the client keeps compiling in the meantime.
 */
public class HiltShaders {
  private HiltShaders() {}
}
