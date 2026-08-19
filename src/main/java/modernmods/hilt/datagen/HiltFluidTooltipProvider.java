package modernmods.hilt.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.ApiStatus.Internal;
import modernmods.hilt.Hilt;
import modernmods.hilt.fluid.tooltip.AbstractFluidTooltipProvider;
import modernmods.hilt.fluid.tooltip.FluidTooltipHandler;

/** Hilt datagen for fluid tooltips. For mods, don't use this, use {@link AbstractFluidTooltipProvider} */
@Internal
public class HiltFluidTooltipProvider extends AbstractFluidTooltipProvider {
  public HiltFluidTooltipProvider(PackOutput packOutput) {
    super(packOutput, Hilt.modId);
  }

  @Override
  protected void addFluids() {
    add("buckets").addUnit("bucket", FluidType.BUCKET_VOLUME);
    addRedirect(FluidTooltipHandler.DEFAULT_ID, id("buckets"));
    // water divides into bottles then "drops"
    add("water", HiltTags.Fluids.WATER)
      .addUnit("bucket", FluidType.BUCKET_VOLUME)
      .addUnit("bottle", HiltValues.BOTTLE)
      .addUnit("drop", HiltValues.DROP);
    // potions and soup don't bother with buckets, stick with the directly useful units
    add("potion", HiltTags.Fluids.POTION)
      .addUnit("bottle", HiltValues.BOTTLE)
      .addUnit("sip", HiltValues.SIP);
    add("soup", HiltTags.Fluids.SOUP)
      .addUnit("bowl", HiltValues.BOWL)
      .addUnit("sip", HiltValues.SIP);
    // honey buckets are equal to honey blocks making it a useful number
    add("honey", HiltTags.Fluids.HONEY)
      .addUnit("bucket", HiltValues.BOTTLE * 4)
      .addUnit("bottle", HiltValues.BOTTLE)
      .addUnit("sip", HiltValues.SIP);
  }

  @Override
  public String getName() {
    return "Hilt Fluid Tooltip Provider";
  }
}
