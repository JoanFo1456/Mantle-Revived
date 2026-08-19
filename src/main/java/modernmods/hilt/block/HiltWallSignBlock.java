package modernmods.hilt.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import modernmods.hilt.block.entity.HiltSignBlockEntity;

public class HiltWallSignBlock extends WallSignBlock {
  public HiltWallSignBlock(Properties props, WoodType type) {
    super(type, props);
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
    return new HiltSignBlockEntity(pPos, pState);
  }
}
