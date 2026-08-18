package modernmods.mantle.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import modernmods.mantle.util.TranslationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Armor item that appends the optional translation tooltip. As of 26.1.2 armor is no longer a
 * dedicated {@code ArmorItem} subclass; the material/slot are baked into the item properties via
 * {@link Item.Properties#humanoidArmor}, so this simply extends {@link Item}.
 */
public class ArmorTooltipItem extends Item {

  public ArmorTooltipItem(ArmorMaterial armorMaterial, ArmorType type, Properties builder) {
    super(builder.humanoidArmor(armorMaterial, type));
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
    List<Component> optional = new ArrayList<>();
    TranslationHelper.addOptionalTooltip(stack, optional);
    optional.forEach(tooltip);
    super.appendHoverText(stack, context, display, tooltip, flagIn);
  }
}
