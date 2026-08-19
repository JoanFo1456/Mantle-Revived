package modernmods.hilt.block.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import modernmods.hilt.registration.deferred.FluidDeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

/** Liquid block setting the entity on fire */
public class MobEffectLiquidBlock extends LiquidBlock {
  private final Supplier<MobEffectInstance> effect;
  public MobEffectLiquidBlock(Supplier<? extends FlowingFluid> supplier, Properties properties, Supplier<MobEffectInstance> effect) {
    super(supplier.get(), properties);
    this.effect = effect;
  }

  @Override
  protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
    // in 26.1.2 there is no per-fluid-type submersion height on the entity; check that the entity is below this block's fluid surface instead
    FluidState fluidState = state.getFluidState();
    if (!fluidState.isEmpty() && entity.getY() < pos.getY() + fluidState.getHeight(level, pos) && entity instanceof LivingEntity living) {
      MobEffectInstance effect = this.effect.get();
      living.addEffect(effect);
    }
  }

  /** Creates a new block supplier */
  public static Function<Supplier<? extends FlowingFluid>, LiquidBlock> createEffect(MapColor color, int lightLevel, Supplier<MobEffectInstance> effect) {
    return fluid -> new MobEffectLiquidBlock(fluid, FluidDeferredRegister.createProperties(color, lightLevel), effect);
  }
}
