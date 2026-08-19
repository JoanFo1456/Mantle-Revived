package modernmods.hilt.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import java.util.function.Consumer;

/**
 * Class for render types defined by Hilt.
 * <p>
 * The 1.21.4+ render engine rewrite replaced the {@code RenderStateShard}/{@code CompositeState}/{@code ShaderInstance}
 * system with render pipelines and {@code RenderType.create}. Hilt no longer defines a bespoke fluid pipeline: shader
 * mods (Iris/Oculus, Euphoria Patches) only render types whose pipeline they recognise, so custom pipelines are dropped
 * under shaders. Both render types below use vanilla block pipelines that shaders map to their gbuffers programs.
 */
public class HiltRenderTypes {
  private HiltRenderTypes() {}

  /**
   * Render type used for the fluid renderer (tanks, smeltery, faucet/casting streams).
   * <p>
   * Uses the vanilla {@link RenderTypes#translucentMovingBlock()} type rather than a bespoke {@code hilt:pipeline/fluid}
   * pipeline. A custom pipeline isn't mapped by shader mods, so under shaders the fluid quads were dropped entirely and
   * molten metal / lava / poured fluids became invisible (they render fine without shaders); this vanilla block pipeline IS
   * recognised. The trade-off is that it culls back faces (the old pipeline disabled culling), so
   * {@link FluidRenderer#putTexturedQuad} emits a mirrored copy of each face to keep the no-cull appearance.
   */
  public static final RenderType FLUID = RenderTypes.translucentMovingBlock();

  /**
   * Render type used for the structure renderer. Historically this used Hilt's block full-bright core shader to force
   * maximum brightness; the engine rewrite removed per-render-type shader overrides, so callers now achieve the emissive
   * look by supplying full packed light (0xF000F0) when building vertices. Uses the standard translucent block type.
   */
  public static final RenderType TRANSLUCENT_FULLBRIGHT = RenderTypes.translucentMovingBlock();

  /**
   * Registers Hilt's custom render pipelines so their shaders are compiled. Call from
   * {@code RegisterRenderPipelinesEvent}. Hilt no longer defines any custom pipelines, so this is a no-op kept for the
   * existing event wiring.
   */
  public static void registerPipelines(Consumer<RenderPipeline> registrar) {
  }
}
