package slimeknights.mantle.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import slimeknights.mantle.util.TranslationHelper;

import java.util.List;

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
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
    TranslationHelper.addOptionalTooltip(stack, tooltip);
    super.appendHoverText(stack, context, tooltip, flagIn);
  }
}
