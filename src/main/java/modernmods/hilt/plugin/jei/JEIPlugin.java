package modernmods.hilt.plugin.jei;

import com.mojang.serialization.Codec;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import modernmods.hilt.Hilt;
import modernmods.hilt.client.screen.MultiModuleScreen;
import modernmods.hilt.inventory.MultiModuleContainerMenu;
import modernmods.hilt.plugin.jei.entity.EntityIngredientHelper;
import modernmods.hilt.plugin.jei.entity.EntityIngredientRenderer;
import modernmods.hilt.recipe.ingredient.EntityIngredient.EntityInput;

import java.util.Collections;
import java.util.List;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
  @Override
  public Identifier getPluginUid() {
    return Hilt.getResource("jei");
  }

  /** Codec for entity type ingredients, required by JEI 26.1.2 ingredient registration */
  private static final Codec<EntityInput> ENTITY_INPUT_CODEC = BuiltInRegistries.ENTITY_TYPE.byNameCodec().xmap(EntityInput::new, EntityInput::type);

  @Override
  public void registerIngredients(IModIngredientRegistration registration) {
    registration.register(HiltJEIConstants.ENTITY_TYPE, Collections.emptyList(), new EntityIngredientHelper(), new EntityIngredientRenderer(16), ENTITY_INPUT_CODEC);
  }

  @Override
  public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registry) {
    // TODO 1.21: re-enable when JEI exposes the replacement for addCategoryExtension.
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void registerGuiHandlers(IGuiHandlerRegistration registration) {
    registration.addGuiContainerHandler(MultiModuleScreen.class, new MultiModuleContainerHandler());
  }

  private static class MultiModuleContainerHandler<C extends MultiModuleContainerMenu<?>> implements IGuiContainerHandler<MultiModuleScreen<C>> {
    @Override
    public List<Rect2i> getGuiExtraAreas(MultiModuleScreen<C> guiContainer) {
      return guiContainer.getModuleAreas();
    }
  }
}
