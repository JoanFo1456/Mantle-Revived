package modernmods.hilt.registration;

import net.minecraft.world.level.block.entity.BlockEntityType;
import modernmods.hilt.block.entity.HiltHangingSignBlockEntity;
import modernmods.hilt.block.entity.HiltSignBlockEntity;

/**
 * Various objects registered under Hilt
 */
public class HiltRegistrations {
  private HiltRegistrations() {}

  public static BlockEntityType<HiltSignBlockEntity> SIGN;

  public static BlockEntityType<HiltHangingSignBlockEntity> HANGING_SIGN;
}
