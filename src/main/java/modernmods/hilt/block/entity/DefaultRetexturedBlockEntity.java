package modernmods.hilt.block.entity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.model.data.ModelData;
import modernmods.hilt.block.RetexturedBlock;
import modernmods.hilt.util.RetexturedHelper;

import javax.annotation.Nonnull;

import static modernmods.hilt.util.RetexturedHelper.TAG_TEXTURE;

/**
 * Standard implementation for {@link IRetexturedBlockEntity}, use alongside {@link RetexturedBlock} and {@link modernmods.hilt.item.RetexturedBlockItem}
 */
public class DefaultRetexturedBlockEntity extends HiltBlockEntity implements IRetexturedBlockEntity {
  @Nonnull
  @Getter
  private Block texture = Blocks.AIR;
  public DefaultRetexturedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  @Nonnull
  @Override
  public ModelData getModelData() {
    return RetexturedHelper.getModelData(texture);
  }

  @Override
  public String getTextureName() {
    return RetexturedHelper.getTextureName(texture);
  }

  @Override
  public void updateTexture(String name) {
    Block oldTexture = texture;
    texture = RetexturedHelper.getBlock(name);
    if (oldTexture != texture) {
      setChangedFast();
      RetexturedHelper.onTextureUpdated(this);
    }
  }

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }

  @Override
  protected void saveSynced(ValueOutput output) {
    super.saveSynced(output);
    if (texture != Blocks.AIR) {
      output.putString(TAG_TEXTURE, getTextureName());
    }
  }

  @Override
  public void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    String textureName = input.getStringOr(TAG_TEXTURE, "");
    if (!textureName.isEmpty()) {
      texture = RetexturedHelper.getBlock(textureName);
      RetexturedHelper.onTextureUpdated(this);
    }
  }
}
