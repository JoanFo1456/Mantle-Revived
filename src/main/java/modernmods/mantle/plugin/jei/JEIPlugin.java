package modernmods.mantle.plugin.jei;

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
import modernmods.mantle.Mantle;
import modernmods.mantle.client.screen.MultiModuleScreen;
import modernmods.mantle.inventory.MultiModuleContainerMenu;
import modernmods.mantle.plugin.jei.entity.EntityIngredientHelper;
import modernmods.mantle.plugin.jei.entity.EntityIngredientRenderer;
import modernmods.mantle.recipe.ingredient.EntityIngredient.EntityInput;

import java.util.Collections;
import java.util.List;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
  @Override
  public Identifier getPluginUid() {
    return Mantle.getResource("jei");
  }

  /** Codec for entity type ingredients, required by JEI 26.1.2 ingredient registration */
  private static final Codec<EntityInput> ENTITY_INPUT_CODEC = BuiltInRegistries.ENTITY_TYPE.byNameCodec().xmap(EntityInput::new, EntityInput::type);

  @Override
  public void registerIngredients(IModIngredientRegistration registration) {
    registration.register(MantleJEIConstants.ENTITY_TYPE, Collections.emptyList(), new EntityIngredientHelper(), new EntityIngredientRenderer(16), ENTITY_INPUT_CODEC);
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
