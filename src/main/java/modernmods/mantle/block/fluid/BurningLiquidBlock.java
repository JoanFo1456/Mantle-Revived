package slimeknights.mantle.block.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import slimeknights.mantle.registration.deferred.FluidDeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

/** Liquid block setting the entity on fire */
public class BurningLiquidBlock extends LiquidBlock {
  /** Burn time in seconds. Lava uses 15 */
  private final int burnTime;
  /** Damage from being in the fluid, lava uses 4 */
  private final float damage;
  public BurningLiquidBlock(Supplier<? extends FlowingFluid> supplier, Properties properties, int burnTime, float damage) {
    super(supplier.get(), properties);
    this.burnTime = burnTime;
    this.damage = damage;
  }

  @Override
  protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
    // in 26.1.2 there is no per-fluid-type submersion height on the entity; check that the entity is below this block's fluid surface instead
    FluidState fluidState = state.getFluidState();
    if (!entity.fireImmune() && !fluidState.isEmpty() && entity.getY() < pos.getY() + fluidState.getHeight(level, pos)) {
      entity.igniteForSeconds(burnTime);
      // hurt() no longer returns whether damage applied; use the server-side variant to preserve the "only play sound when damaged" behavior
      if (level instanceof ServerLevel server && entity.hurtServer(server, entity.damageSources().lava(), damage)) {
        entity.playSound(SoundEvents.GENERIC_BURN, 0.4F, 2.0F + level.getRandom().nextFloat() * 0.4F);
      }
    }
  }

  /** Creates a new block supplier */
  public static Function<Supplier<? extends FlowingFluid>, LiquidBlock> createBurning(MapColor color, int lightLevel, int burnTime, float damage) {
    return fluid -> new BurningLiquidBlock(fluid, FluidDeferredRegister.createProperties(color, lightLevel), burnTime, damage);
  }
}
