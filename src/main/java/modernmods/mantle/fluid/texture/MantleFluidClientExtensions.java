package modernmods.mantle.fluid.texture;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import modernmods.mantle.Mantle;
import modernmods.mantle.fluid.InvertedFluidType;
import modernmods.mantle.fluid.TextureFluidType;

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

  /**
   * Registers a {@link FluidModel} for every texture-driven fluid. 26.1 renders fluids via the fluid model
   * ({@link net.minecraft.client.renderer.block.FluidStateModelSet}) rather than {@code IClientFluidTypeExtensions}
   * textures, so a fluid without a registered model renders as the missing (black/magenta) sprite. Still/flowing/overlay
   * sprites and the tint come from the fluid's {@code mantle/fluid_texture} data ({@link FluidTextureManager}).
   */
  @SubscribeEvent
  static void registerFluidModels(RegisterFluidModelsEvent event) {
    for (Fluid fluid : BuiltInRegistries.FLUID) {
      FluidType type = fluid.getFluidType();
      if (type instanceof TextureFluidType || type instanceof InvertedFluidType) {
        FluidTexture data = FluidTextureManager.getData(type);
        Material still = new Material(data.still());
        Material flowing = new Material(data.flowing());
        Material overlay = data.overlay() == null ? null : new Material(data.overlay());
        event.register(new FluidModel.Unbaked(still, flowing, overlay, new ConstantFluidTintSource(data.color())), fluid);
      }
    }
  }
}
