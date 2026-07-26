package slimeknights.mantle.fluid.texture;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.fluid.InvertedFluidType;
import slimeknights.mantle.fluid.TextureFluidType;

/**
 * Registers the client fluid type extensions for Mantle's model-driven fluid types.
 * <p>Replaces the removed {@link FluidType#initializeClient} hook (26.1.2) with a
 * {@link RegisterClientExtensionsEvent} handler that registers the matching client extension for every
 * {@link TextureFluidType} / {@link InvertedFluidType} instance in the fluid type registry.
 */
@EventBusSubscriber(modid = Mantle.modId, value = Dist.CLIENT)
public class MantleFluidClientExtensions {
  private MantleFluidClientExtensions() {}

  @SubscribeEvent
  static void registerClientExtensions(RegisterClientExtensionsEvent event) {
    for (FluidType type : NeoForgeRegistries.FLUID_TYPES) {
      // InvertedFluidType is checked first as its client extension differs from the plain texture one
      if (type instanceof InvertedFluidType) {
        event.registerFluidType(new ClientInvertedFluidType(type), type);
      } else if (type instanceof TextureFluidType) {
        event.registerFluidType(new ClientTextureFluidType(type), type);
      }
    }
  }
}
