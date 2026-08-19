package modernmods.hilt.datagen;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.ApiStatus.Internal;
import modernmods.hilt.Hilt;

import java.util.concurrent.CompletableFuture;

import static modernmods.hilt.datagen.HiltTags.Fluids.BEETROOT_SOUP;
import static modernmods.hilt.datagen.HiltTags.Fluids.LAVA;
import static modernmods.hilt.datagen.HiltTags.Fluids.MUSHROOM_STEW;
import static modernmods.hilt.datagen.HiltTags.Fluids.RABBIT_STEW;
import static modernmods.hilt.datagen.HiltTags.Fluids.SOUP;
import static modernmods.hilt.datagen.HiltTags.Fluids.WATER;

/** Provider for tags added by hilt, generally not useful for other mods */
@Internal
public class HiltFluidTagProvider extends FluidTagsProvider {
  public HiltFluidTagProvider(PackOutput output, CompletableFuture<Provider> holders) {
    super(output, holders, Hilt.modId);
  }

  @Override
  protected void addTags(Provider pProvider) {
    this.tag(WATER).add(Fluids.WATER, Fluids.FLOWING_WATER);
    this.tag(LAVA).add(Fluids.LAVA, Fluids.FLOWING_LAVA);
    this.tag(SOUP)
      .addOptionalTag(BEETROOT_SOUP)
      .addOptionalTag(MUSHROOM_STEW)
      .addOptionalTag(RABBIT_STEW);
  }

  @Override
  public String getName() {
    return "Hilt Fluid Tag Provider";
  }
}
