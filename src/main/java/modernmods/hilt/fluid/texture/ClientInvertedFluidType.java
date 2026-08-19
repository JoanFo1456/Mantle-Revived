package modernmods.hilt.fluid.texture;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;

/** Client logic for {@link modernmods.hilt.fluid.InvertedFluidType} */
public class ClientInvertedFluidType extends ClientTextureFluidType {
  private Identifier lastFlowing;
  private Identifier invertedFlowing;
  public ClientInvertedFluidType(FluidType type) {
    super(type);
  }

  // Note: per-position flowing texture hook was removed from IClientFluidTypeExtensions (textures now come from FluidStateModelSet)
  public Identifier getFlowingTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
    Identifier flowing = getFlowingTexture();
    if (flowing == lastFlowing) {
      return invertedFlowing;
    }
    invertedFlowing = flowing.withSuffix("_inverted");
    lastFlowing = flowing;
    return invertedFlowing;
  }
}
