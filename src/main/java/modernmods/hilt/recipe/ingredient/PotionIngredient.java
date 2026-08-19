package modernmods.hilt.recipe.ingredient;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.IngredientType;
import org.jetbrains.annotations.Nullable;
import modernmods.hilt.data.loadable.Loadables;
import modernmods.hilt.data.loadable.record.RecordLoadable;
import modernmods.hilt.recipe.HiltRecipes;
import modernmods.hilt.recipe.helper.LoadableIngredientSerializer;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Simple ingredient checking for an item with a specific potion */
public class PotionIngredient extends ItemIngredient {
  /** Ingredient serializer instance */
  public static final LoadableIngredientSerializer<PotionIngredient> SERIALIZER = new LoadableIngredientSerializer<>(RecordLoadable.create(
    ItemsField.INSTANCE, TAG_FIELD,
    Loadables.POTION.defaultField("potion", Potions.WATER.value(), false, i -> i.potion),
    PotionIngredient::new
  ));

  private final Potion potion;
  protected PotionIngredient(List<Item> items, @Nullable TagKey<Item> itemTag, Potion potion) {
    super(items, itemTag);
    this.potion = potion;
  }

  /** Creates a potion ingredient matching a list of items */
  public static Ingredient of(Potion potion, List<ItemLike> items) {
    return new PotionIngredient(toItem(items), null, potion).toVanilla();
  }

  /** Creates a potion ingredient matching a list of items */
  public static Ingredient of(Potion potion, ItemLike... items) {
    return of(potion, Arrays.asList(items));
  }

  /** Creates a potion ingredient matching a tag */
  public static Ingredient of(Potion potion, TagKey<Item> tag) {
    return new PotionIngredient(List.of(), tag, potion).toVanilla();
  }

  @Override
  public boolean test(ItemStack stack) {
    // stack must match, any item must match, and potion must match
    return super.test(stack) && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion().map(holder -> holder.value() == potion).orElse(false);
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  /** Builds a display stack for the given item carrying this ingredient's potion */
  private ItemStack withPotion(Holder<Item> item) {
    ItemStack stack = new ItemStack(item);
    stack.set(DataComponents.POTION_CONTENTS, new PotionContents(BuiltInRegistries.POTION.wrapAsHolder(potion)));
    return stack;
  }

  @Override
  public SlotDisplay display() {
    // items() only yields the base items/tag; attach the potion component so recipe viewers show the correct potion
    return new SlotDisplay.Composite(items()
      .<SlotDisplay>map(item -> new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(withPotion(item))))
      .toList());
  }

  @Override
  public IngredientType<?> getType() {
    return HiltRecipes.POTION_INGREDIENT.get();
  }

  @Override
  public boolean equals(Object object) {
    return this == object || object instanceof PotionIngredient that && super.equals(object) && potion == that.potion;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), potion);
  }
}
