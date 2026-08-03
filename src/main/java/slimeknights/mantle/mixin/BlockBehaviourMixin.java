package slimeknights.mantle.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.mantle.registration.RegistrationIdContext;

/**
 * As of Minecraft 26.1, {@link BlockBehaviour.Properties} carries a mandatory registry id that the block constructor
 * dereferences eagerly (through {@code effectiveDrops()} and {@code effectiveDescriptionId()}), throwing
 * {@code "Block id not set"} when it is missing. Mantle's deferred registers build blocks from opaque suppliers, so
 * they push the registration name into {@link RegistrationIdContext} for the duration of the supplier. Here we read it
 * back and assign the id to the properties before that eager access runs.
 */
@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {
  @Inject(method = "<init>", at = @At("HEAD"))
  private void mantle$assignRegistrationId(BlockBehaviour.Properties properties, CallbackInfo ci) {
    Identifier id = RegistrationIdContext.current();
    if (id != null) {
      properties.setId(ResourceKey.create(Registries.BLOCK, id));
    }
  }
}
