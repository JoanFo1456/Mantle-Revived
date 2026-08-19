package modernmods.hilt.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import modernmods.hilt.util.TranslationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BlockTooltipItem extends BlockItem {
  public BlockTooltipItem(Block blockIn, Item.Properties builder) {
    super(blockIn, builder);
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
    super.appendHoverText(stack, context, display, tooltip, flagIn);
    List<Component> optional = new ArrayList<>();
    TranslationHelper.addOptionalTooltip(stack, optional);
    optional.forEach(tooltip);
  }
}
