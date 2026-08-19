package modernmods.hilt.datagen;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.NotCondition;
import org.jetbrains.annotations.ApiStatus.Internal;
import modernmods.hilt.Hilt;
import modernmods.hilt.fluid.transfer.AbstractFluidContainerTransferProvider;
import modernmods.hilt.fluid.transfer.EmptyPotionTransfer;
import modernmods.hilt.fluid.transfer.FillFluidContainerTransfer;
import modernmods.hilt.fluid.transfer.FillFluidWithNBTTransfer;
import modernmods.hilt.recipe.condition.TagFilledCondition;
import modernmods.hilt.recipe.helper.ItemOutput;
import modernmods.hilt.recipe.ingredient.FluidIngredient;

import javax.annotation.Nullable;

/** Adds fluid transfer for base fluid. Use {@link AbstractFluidContainerTransferProvider} for mods. */
@Internal
public class HiltFluidTransferProvider extends AbstractFluidContainerTransferProvider {
  public HiltFluidTransferProvider(PackOutput packOutput) {
    super(packOutput, Hilt.modId);
  }

  @Override
  public String getName() {
    return "Hilt fluid transfer provider";
  }

  @Override
  protected void addTransfers() {
    addTransfer("wet_sponge", new FillFluidContainerTransfer(Ingredient.of(Items.SPONGE), ItemOutput.fromItem(Items.WET_SPONGE), FluidIngredient.of(HiltTags.Fluids.WATER, HiltValues.BOTTLE)));

    // potions
    addPotion("potion/",           Items.POTION,  null);
    addPotion("potion/splash/",    Items.SPLASH_POTION,    HiltTags.Items.SPLASH_BOTTLE);
    addPotion("potion/lingering/", Items.LINGERING_POTION, HiltTags.Items.LINGERING_BOTTLE);
    // foods
    optionalFillEmpty("honey_bottle_",  Items.HONEY_BOTTLE,  Items.GLASS_BOTTLE, HiltTags.Fluids.HONEY,         HiltValues.BOTTLE, false);
    optionalFillEmpty("beetroot_soup_", Items.BEETROOT_SOUP, Items.BOWL,         HiltTags.Fluids.BEETROOT_SOUP, HiltValues.BOWL,   false);
    optionalFillEmpty("mushroom_stew_", Items.MUSHROOM_STEW, Items.BOWL,         HiltTags.Fluids.MUSHROOM_STEW, HiltValues.BOWL,   false);
    optionalFillEmpty("rabbit_stew_",   Items.RABBIT_STEW,   Items.BOWL,         HiltTags.Fluids.RABBIT_STEW,   HiltValues.BOWL,   false);
  }

  /** Adds generic fill and empty for a container */
  private void optionalFillEmpty(String prefix, ItemLike item, ItemLike container, TagKey<Fluid> tag, int amount, boolean nbt) {
    addFillEmpty(prefix, item, container, tag, amount, nbt, new TagFilledCondition<>(tag));
  }

  /** Adds generic fill and empty for a container */
  protected void addPotion(String prefix, ItemLike filled, @Nullable TagKey<Item> bottleTag) {
    // Hilt alone does not guarantee we have splash and lingering bottles
    // for emptying, if they are absent just use glass bottles
    // for filling, if they are absent then we can't do the fill recipes
    Ingredient container;
    ICondition potionCondition = new TagFilledCondition<>(HiltTags.Fluids.POTION);
    ICondition[] potionConditions;
    ICondition[] waterConditions;
    if (bottleTag != null) {
      container = Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(bottleTag));
      ICondition containerCondition = new TagFilledCondition<>(bottleTag);
      waterConditions = new ICondition[]{containerCondition};
      potionConditions = new ICondition[]{potionCondition, containerCondition};

      // since the container tag may not be present, add two potion recipes: one when its absent that gives glass bottle, and one when present for unique bottle
      addTransfer(prefix + "empty_glass_bottle", new EmptyPotionTransfer(Ingredient.of(filled), ItemOutput.fromItem(Items.GLASS_BOTTLE), HiltValues.BOTTLE), new NotCondition(containerCondition));
      addTransfer(prefix + "empty_unique_bottle", new EmptyPotionTransfer(Ingredient.of(filled), ItemOutput.fromTag(bottleTag), HiltValues.BOTTLE), containerCondition);
    } else {
      container = Ingredient.of(Items.GLASS_BOTTLE);
      waterConditions = new ICondition[0];
      potionConditions = new ICondition[]{potionCondition};
      addTransfer(prefix + "empty", new EmptyPotionTransfer(Ingredient.of(filled), ItemOutput.fromItem(Items.GLASS_BOTTLE), HiltValues.BOTTLE));
    }

    // filling potions is as simple as a NBT copy, though this requires a potion fluid and possibly a container
    addTransfer(prefix + "fill_potion", new FillFluidWithNBTTransfer(
      container,
      ItemOutput.fromItem(filled),
      FluidIngredient.of(HiltTags.Fluids.POTION, HiltValues.BOTTLE)),
      potionConditions);
    // water bottles are 1/3 of a bucket, to prevent water dupes we round up on fill and down on empty, hence fill being 500mb
    // we can always fill water bottles, not always fill splash and lingering
    addTransfer(prefix + "fill_water", new FillFluidContainerTransfer(
      container,
      ItemOutput.fromStack(PotionContents.createItemStack(filled.asItem(), Potions.WATER)),
      FluidIngredient.of(HiltTags.Fluids.WATER, HiltValues.BOTTLE * 2)),
      waterConditions);
  }
}
