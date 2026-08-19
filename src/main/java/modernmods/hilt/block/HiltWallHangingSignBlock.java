package modernmods.hilt.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import modernmods.hilt.block.entity.HiltHangingSignBlockEntity;

public class HiltWallHangingSignBlock extends WallHangingSignBlock {
  public HiltWallHangingSignBlock(Properties props, WoodType type) {
    super(type, props);
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
    return new HiltHangingSignBlockEntity(pPos, pState);
  }
}
