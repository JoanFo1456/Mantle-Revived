package modernmods.hilt.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import modernmods.hilt.util.TranslationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Item with automatic tooltip support
 */
public class TooltipItem extends Item {

  public TooltipItem(Properties properties) {
    super(properties);
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
    List<Component> optional = new ArrayList<>();
    TranslationHelper.addOptionalTooltip(stack, optional);
    optional.forEach(tooltip);
    super.appendHoverText(stack, context, display, tooltip, flagIn);
  }
}
