package modernmods.mantle.loot.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import modernmods.mantle.loot.MantleLoot;

import java.util.List;

/**
 * Loot function to set the fluid on a dropped item
 */
public class SetFluidLootFunction extends LootItemConditionalFunction {
  /**
   * Fluid + amount pair. Stored instead of a live {@link FluidStack} because as of 26.1 constructing a FluidStack reads
   * the fluid's data components, which are not bound yet while loot tables are decoded during the early resource reload.
   * The stack is built lazily in {@link #run} once components are bound. Component-carrying fluids in loot are not
   * supported (the legacy {@code {fluid, amount}} form is all the data uses); add a components field here if needed.
   */
  private record FluidData(Fluid fluid, int amount) {
    static final Codec<FluidData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(FluidData::fluid),
      Codec.INT.fieldOf("amount").forGetter(FluidData::amount)
    ).apply(instance, FluidData::new));
  }

  public static final MapCodec<SetFluidLootFunction> CODEC = RecordCodecBuilder.mapCodec(
    instance -> commonFields(instance).and(FluidData.CODEC.fieldOf("fluid").forGetter(loot -> new FluidData(loot.fluid, loot.amount)))
                            .apply(instance, (conditions, data) -> new SetFluidLootFunction(conditions, data.fluid(), data.amount()))
  );

  /** Fluid to add to the item */
  private final Fluid fluid;
  /** Amount of fluid to add */
  private final int amount;
  protected SetFluidLootFunction(List<LootItemCondition> conditionsIn, Fluid fluid, int amount) {
    super(conditionsIn);
    this.fluid = fluid;
    this.amount = amount;
  }

  @Override
  protected ItemStack run(ItemStack stack, LootContext context) {
    if (amount <= 0) {
      return stack;
    }
    int count = stack.getCount();
    FluidStack fluidStack = new FluidStack(fluid, amount);
    ItemAccess access = ItemAccess.forStack(stack.copyWithCount(1));
    ResourceHandler<FluidResource> handler = access.getCapability(Capabilities.Fluid.ITEM);
    if (handler != null) {
      try (Transaction tx = Transaction.openRoot()) {
        handler.insert(FluidResource.of(fluidStack), fluidStack.getAmount(), tx);
        tx.commit();
      }
      return access.getResource().toStack(count);
    }
    return stack;
  }

  @Override
  public MapCodec<? extends LootItemConditionalFunction> codec() {
    return CODEC;
  }

  /**
   * Creates a new builder with the given fluid
   * @param fluid  Fluid to set
   * @return  Builder instance
   */
  public static Builder<?> builder(FluidStack fluid) {
    return simpleBuilder(conditions -> new SetFluidLootFunction(conditions, fluid.getFluid(), fluid.getAmount()));
  }
}
