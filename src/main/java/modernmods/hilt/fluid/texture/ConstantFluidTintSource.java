package modernmods.hilt.fluid.texture;

import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.fluid.FluidTintSource;

/**
 * {@link FluidTintSource} returning a constant ARGB color, taken from a fluid's {@code hilt/fluid_texture} data. In
 * 26.1 the per-fluid tint moved from {@code IClientFluidTypeExtensions#getTintColor} into the fluid's registered
 * {@link net.minecraft.client.renderer.block.FluidModel}, so texture-driven fluids need this to reproduce their color.
 */
public record ConstantFluidTintSource(int color) implements FluidTintSource {
  @Override
  public int color(FluidState state) {
    return color;
  }
}
