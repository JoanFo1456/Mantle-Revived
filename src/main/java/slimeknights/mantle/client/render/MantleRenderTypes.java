package slimeknights.mantle.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import slimeknights.mantle.Mantle;

import java.util.function.Consumer;

/**
 * Class for render types defined by Mantle.
 * <p>
 * The 1.21.4+ render engine rewrite replaced the {@code RenderStateShard}/{@code CompositeState}/{@code ShaderInstance}
 * system with {@link RenderPipeline}s (see {@link RenderPipelines}) and {@link RenderType#create(String, RenderSetup)}.
 * Custom render types are now built from a pipeline plus a {@link RenderSetup} describing the bound textures/targets.
 */
public class MantleRenderTypes {
  private MantleRenderTypes() {}

  /**
   * Pipeline for the fluid renderer: reuses the vanilla translucent block shader ({@link RenderPipelines#BLOCK_SNIPPET})
   * with backface culling disabled, so both faces of a fluid cuboid render (needed for gasses and inner tank faces).
   */
  public static final RenderPipeline FLUID_PIPELINE = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
    .withLocation(Mantle.getResource("pipeline/fluid"))
    .withShaderDefine("ALPHA_CUTOUT", 0.01F)
    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
    .withDepthStencilState(DepthStencilState.DEFAULT)
    .withCull(false)
    .build();

  /** Render type used for the fluid renderer; block atlas, lightmap, translucent, no culling. */
  public static final RenderType FLUID = RenderType.create("mantle:fluid", RenderSetup.builder(FLUID_PIPELINE)
    .useLightmap()
    .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
    .affectsCrumbling()
    .sortOnUpload()
    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
    .createRenderSetup());

  /**
   * Render type used for the structure renderer. Historically this used Mantle's block full-bright core shader to force
   * maximum brightness; the engine rewrite removed per-render-type shader overrides, so callers now achieve the emissive
   * look by supplying full packed light (0xF000F0) when building vertices. Uses the standard translucent block type.
   */
  public static final RenderType TRANSLUCENT_FULLBRIGHT = RenderTypes.translucentMovingBlock();

  /**
   * Registers Mantle's custom render pipelines so their shaders are compiled. Call from
   * {@code RegisterRenderPipelinesEvent}.
   */
  public static void registerPipelines(Consumer<RenderPipeline> registrar) {
    registrar.accept(FLUID_PIPELINE);
  }
}
