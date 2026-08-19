package modernmods.hilt.fluid;

import net.neoforged.neoforge.fluids.FluidType;

/**
 * Fluid type adding an extra flipped texture for the in world block.
 * <p>In 26.1.2 {@link FluidType#initializeClient} was removed; the client extension is registered via
 * {@code RegisterClientExtensionsEvent} in {@link modernmods.hilt.fluid.texture.HiltFluidClientExtensions}.
 */
public class InvertedFluidType extends FluidType {
  public InvertedFluidType(Properties properties) {
    super(properties);
  }
}
