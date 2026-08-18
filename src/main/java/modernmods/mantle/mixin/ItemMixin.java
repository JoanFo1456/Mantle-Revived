package modernmods.mantle.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import modernmods.mantle.registration.RegistrationIdContext;

/**
 * As of Minecraft 26.1, {@link Item.Properties} carries a mandatory registry id that the item constructor dereferences
 * eagerly (through {@code effectiveDescriptionId()} and {@code itemIdOrThrow()}), throwing {@code "Item id not set"}
 * when it is missing. Mantle's deferred registers build items from opaque suppliers, so they push the registration name
 * into {@link RegistrationIdContext} for the duration of the supplier. Here we read it back and assign the id to the
 * properties before that eager access runs.
 */
@Mixin(Item.class)
public class ItemMixin {
  @Inject(method = "<init>", at = @At("HEAD"))
  private static void mantle$assignRegistrationId(Item.Properties properties, CallbackInfo ci) {
    Identifier id = RegistrationIdContext.current();
    if (id != null) {
      properties.setId(ResourceKey.create(Registries.ITEM, id));
    }
  }
}
