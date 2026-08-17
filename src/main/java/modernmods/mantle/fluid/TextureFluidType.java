package slimeknights.mantle.fluid;

import net.neoforged.neoforge.fluids.FluidType;

/**
 * Fluid type whose color and textures are determined by the model.
 * <p>In 26.1.2 {@link FluidType#initializeClient} was removed; the {@link slimeknights.mantle.fluid.texture.ClientTextureFluidType}
 * client extension is registered via {@code RegisterClientExtensionsEvent} in
 * {@link slimeknights.mantle.fluid.texture.MantleFluidClientExtensions}.
 */
public class TextureFluidType extends FluidType {
  public TextureFluidType(Properties properties) {
    super(properties);
  }
}
