package modernmods.hilt.recipe.data;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import modernmods.hilt.registration.object.BuildingBlockObject;
import modernmods.hilt.registration.object.MetalItemObject;
import modernmods.hilt.registration.object.WallBuildingBlockObject;
import modernmods.hilt.registration.object.WoodBlockObject;

import java.util.function.Consumer;

/**
 * Crafting helper for common recipe types, like stairs, slabs, and packing.
 */
@SuppressWarnings("unused") // API
public interface ICommonRecipeHelper extends IRecipeHelper {
  /* 26.1.2 helpers */

  /**
   * Item lookup used by vanilla recipe builders, which now require a {@link HolderGetter}.
   * The built-in registry is fully populated during datagen so it is a valid source.
   */
  static HolderGetter<Item> items() {
    return BuiltInRegistries.ITEM;
  }

  /** Wraps a recipe {@link Identifier} into the {@link ResourceKey} now required by {@link net.minecraft.data.recipes.RecipeBuilder#save}. */
  static ResourceKey<Recipe<?>> key(Identifier id) {
    return ResourceKey.create(Registries.RECIPE, id);
  }

  /* Metals */

  /**
   * Registers a recipe packing a small item into a large one
   * @param consumer   Recipe consumer
   * @param category   Recipe category
   * @param large      Large item
   * @param small      Small item
   * @param largeName  Large name
   * @param smallName  Small name
   * @param folder     Recipe folder
   */
  default void packingRecipe(Consumer<FinishedRecipe> consumer, RecipeCategory category, String largeName, ItemLike large, String smallName, ItemLike small, String folder) {
    // ingot to block
    Identifier largeId = id(large);
    ShapedRecipeBuilder.shaped(items(), category, large)
                       .define('#', small)
                       .pattern("###")
                       .pattern("###")
                       .pattern("###")
                       .unlockedBy("has_item", has(small))
                       .group(largeId.toString())
                       .save(VanillaFinishedRecipe.output(consumer), key(wrap(largeId, folder, String.format("_from_%ss", smallName))));
    // block to ingot
    Identifier smallId = id(small);
    ShapelessRecipeBuilder.shapeless(items(), category, small, 9)
                          .requires(large)
                          .unlockedBy("has_item", has(large))
                          .group(smallId.toString())
                          .save(VanillaFinishedRecipe.output(consumer), key(wrap(smallId, folder, String.format("_from_%s", largeName))));
  }

  /**
   * Registers a recipe packing a small item into a large one
   * @param consumer   Recipe consumer
   * @param largeItem  Large item
   * @param smallItem  Small item
   * @param smallTag   Tag for small item
   * @param largeName  Large name
   * @param smallName  Small name
   * @param folder     Recipe folder
   */
  default void packingRecipe(Consumer<FinishedRecipe> consumer, RecipeCategory category, String largeName, ItemLike largeItem, String smallName, ItemLike smallItem, TagKey<Item> smallTag, String folder) {
    // ingot to block
    // note our item is in the center, any mod allowed around the edges
    Identifier largeId = id(largeItem);
    ShapedRecipeBuilder.shaped(items(), category, largeItem)
                       .define('#', modernmods.hilt.data.loadable.common.LazyTagIngredient.of(smallTag))
                       .define('*', smallItem)
                       .pattern("###")
                       .pattern("#*#")
                       .pattern("###")
                       .unlockedBy("has_item", has(smallItem))
                       .group(largeId.toString())
                       .save(VanillaFinishedRecipe.output(consumer), key(wrap(largeId, folder, String.format("_from_%ss", smallName))));
    // block to ingot
    Identifier smallId = id(smallItem);
    ShapelessRecipeBuilder.shapeless(items(), category, smallItem, 9)
                          .requires(largeItem)
                          .unlockedBy("has_item", has(largeItem))
                          .group(smallId.toString())
                          .save(VanillaFinishedRecipe.output(consumer), key(wrap(smallId, folder, String.format("_from_%s", largeName))));
  }

  /**
   * Adds recipes to convert a block to ingot, ingot to block, and for nuggets
   * @param consumer  Recipe consumer
   * @param metal     Metal object
   * @param folder    Folder for recipes
   */
  default void metalCrafting(Consumer<FinishedRecipe> consumer, MetalItemObject metal, String folder) {
    ItemLike ingot = metal.getIngot();
    packingRecipe(consumer, RecipeCategory.MISC, "block", metal.get(), "ingot", ingot, metal.getIngotTag(), folder);
    packingRecipe(consumer, RecipeCategory.MISC, "ingot", ingot, "nugget", metal.getNugget(), metal.getNuggetTag(), folder);
  }


  /* Building blocks */

  /**
   * Registers generic saveing block recipes for slabs and stairs
   * @param consumer  Recipe consumer
   * @param building  Building object instance
   */
  default void slabStairsCrafting(Consumer<FinishedRecipe> consumer, BuildingBlockObject building, String folder, boolean addStonecutter) {
    Item item = building.asItem();
    Identifier itemId = id(item);
    Criterion<?> hasBlock = has(item);
    // slab
    ItemLike slab = building.getSlab();
    ShapedRecipeBuilder.shaped(items(), RecipeCategory.BUILDING_BLOCKS, slab, 6)
                       .define('B', item)
                       .pattern("BBB")
                       .unlockedBy("has_item", hasBlock)
                       .group(id(slab).toString())
                       .save(VanillaFinishedRecipe.output(consumer), key(wrap(itemId, folder, "_slab")));
    // stairs
    ItemLike stairs = building.getStairs();
    ShapedRecipeBuilder.shaped(items(), RecipeCategory.BUILDING_BLOCKS, stairs, 4)
                       .define('B', item)
                       .pattern("B  ")
                       .pattern("BB ")
                       .pattern("BBB")
                       .unlockedBy("has_item", hasBlock)
                       .group(id(stairs).toString())
                       .save(VanillaFinishedRecipe.output(consumer), key(wrap(itemId, folder, "_stairs")));

    // only add stonecutter if relevant
    if (addStonecutter) {
      Ingredient ingredient = Ingredient.of(item);
      SingleItemRecipeBuilder.stonecutting(ingredient, RecipeCategory.BUILDING_BLOCKS, slab, 2)
                             .unlockedBy("has_item", hasBlock)
                             .save(VanillaFinishedRecipe.output(consumer), key(wrap(itemId, folder, "_slab_stonecutter")));
      SingleItemRecipeBuilder.stonecutting(ingredient, RecipeCategory.BUILDING_BLOCKS, stairs, 1)
                             .unlockedBy("has_item", hasBlock)
                             .save(VanillaFinishedRecipe.output(consumer), key(wrap(itemId, folder, "_stairs_stonecutter")));
    }
  }

  /**
   * Registers generic saveing block recipes for slabs, stairs, and walls
   * @param consumer  Recipe consumer
   * @param building  Building object instance
   */
  default void stairSlabWallCrafting(Consumer<FinishedRecipe> consumer, WallBuildingBlockObject building, String folder, boolean addStonecutter) {
    slabStairsCrafting(consumer, building, folder, addStonecutter);
    // wall
    Item item = building.asItem();
    Identifier itemId = id(item);
    Criterion<?> hasBlock = has(item);
    ItemLike wall = building.getWall();
    ShapedRecipeBuilder.shaped(items(), RecipeCategory.BUILDING_BLOCKS, wall, 6)
                       .define('B', item)
                       .pattern("BBB")
                       .pattern("BBB")
                       .unlockedBy("has_item", hasBlock)
                       .group(id(wall).toString())
                       .save(VanillaFinishedRecipe.output(consumer), key(wrap(itemId, folder, "_wall")));
    // only add stonecutter if relevant
    if (addStonecutter) {
      Ingredient ingredient = Ingredient.of(item);
      SingleItemRecipeBuilder.stonecutting(ingredient, RecipeCategory.BUILDING_BLOCKS, wall, 1)
                             .unlockedBy("has_item", hasBlock)
                             .save(VanillaFinishedRecipe.output(consumer), key(wrap(itemId, folder, "_wall_stonecutter")));
    }
  }

  /**
   * Registers recipes relevant to wood
   * @param consumer  Recipe consumer
   * @param wood      Wood types
   * @param folder    Wood folder
   */
  default void woodCrafting(Consumer<FinishedRecipe> consumer, WoodBlockObject wood, String folder) {
    Criterion<?> hasPlanks = has(wood);

    // planks
    ShapelessRecipeBuilder.shapeless(items(), RecipeCategory.BUILDING_BLOCKS, wood, 4).requires(modernmods.hilt.data.loadable.common.LazyTagIngredient.of(wood.getLogItemTag()))
                          .group("planks")
                          .unlockedBy("has_log", has(wood.getLogItemTag()))
                          .save(VanillaFinishedRecipe.output(consumer), key(location(folder + "planks")));
    // slab
    ItemLike slab = wood.getSlab();
    ShapedRecipeBuilder.shaped(items(), RecipeCategory.BUILDING_BLOCKS, slab, 6)
                       .define('#', wood)
                       .pattern("###")
                       .unlockedBy("has_planks", hasPlanks)
                       .group("wooden_slab")
                       .save(VanillaFinishedRecipe.output(consumer), key(location(folder + "slab")));
    // stairs
    ItemLike stairs = wood.getStairs();
    ShapedRecipeBuilder.shaped(items(), RecipeCategory.BUILDING_BLOCKS, stairs, 4)
                       .define('#', wood)
                       .pattern("#  ")
                       .pattern("## ")
                       .pattern("###")
                       .unlockedBy("has_planks", hasPlanks)
                       .group("wooden_stairs")
                       .save(VanillaFinishedRecipe.output(consumer), key(location(folder + "stairs")));

    // log to stripped
    ShapedRecipeBuilder.shaped(items(), RecipeCategory.BUILDING_BLOCKS, wood.getWood(), 3)
                       .define('#', wood.getLog())
                       .pattern("##").pattern("##")
                       .group("bark")
                       .unlockedBy("has_log", has(wood.getLog()))
                       .save(VanillaFinishedRecipe.output(consumer), key(location(folder + "log_to_wood")));
    ShapedRecipeBuilder.shaped(items(), RecipeCategory.BUILDING_BLOCKS, wood.getStrippedWood(), 3)
                       .define('#', wood.getStrippedLog())
                       .pattern("##").pattern("##")
                       .group("bark")
                       .unlockedBy("has_log", has(wood.getStrippedLog()))
                       .save(VanillaFinishedRecipe.output(consumer), key(location(folder + "stripped_log_to_wood")));
    // doors
    ShapedRecipeBuilder.shaped(items(), RecipeCategory.DECORATIONS, wood.getFence(), 3)
                       .define('#', modernmods.hilt.data.loadable.common.LazyTagIngredient.of(Tags.Items.RODS_WOODEN)).define('W', wood)
                       .pattern("W#W").pattern("W#W")
                       .group("wooden_fence")
                       .unlockedBy("has_planks", hasPlanks)
                       .save(VanillaFinishedRecipe.output(consumer), key(location(folder + "fence")));
    ShapedRecipeBuilder.shaped(items(), RecipeCategory.REDSTONE, wood.getFenceGate())
                       .define('#', Items.STICK).define('W', wood)
                       .pattern("#W#").pattern("#W#")
                       .group("wooden_fence_gate")
                       .unlockedBy("has_planks", hasPlanks)
                       .save(VanillaFinishedRecipe.output(consumer), key(location(folder + "fence_gate")));
    ShapedRecipeBuilder.shaped(items(), RecipeCategory.REDSTONE, wood.getDoor(), 3)
                       .define('#', wood)
                       .pattern("##").pattern("##").pattern("##")
                       .group("wooden_door")
                       .unlockedBy("has_planks", hasPlanks)
                       .save(VanillaFinishedRecipe.output(consumer), key(location(folder + "door")));
    ShapedRecipeBuilder.shaped(items(), RecipeCategory.REDSTONE, wood.getTrapdoor(), 2)
                       .define('#', wood)
                       .pattern("###").pattern("###")
                       .group("wooden_trapdoor")
                       .unlockedBy("has_planks", hasPlanks)
                       .save(VanillaFinishedRecipe.output(consumer), key(location(folder + "trapdoor")));
    // buttons
    ShapelessRecipeBuilder.shapeless(items(), RecipeCategory.REDSTONE, wood.getButton())
                          .requires(wood)
                          .group("wooden_button")
                          .unlockedBy("has_planks", hasPlanks)
                          .save(VanillaFinishedRecipe.output(consumer), key(location(folder + "button")));
    ShapedRecipeBuilder.shaped(items(), RecipeCategory.REDSTONE, wood.getPressurePlate())
                       .define('#', wood)
                       .pattern("##")
                       .group("wooden_pressure_plate")
                       .unlockedBy("has_planks", hasPlanks)
                       .save(VanillaFinishedRecipe.output(consumer), key(location(folder + "pressure_plate")));
    // signs
    ShapedRecipeBuilder.shaped(items(), RecipeCategory.DECORATIONS, wood.getSign(), 3)
                       .group("sign")
                       .define('#', wood).define('X', modernmods.hilt.data.loadable.common.LazyTagIngredient.of(Tags.Items.RODS_WOODEN))
                       .pattern("###").pattern("###").pattern(" X ")
                       .unlockedBy("has_planks", has(wood))
                       .save(VanillaFinishedRecipe.output(consumer), key(location(folder + "sign")));
    ShapedRecipeBuilder.shaped(items(), RecipeCategory.DECORATIONS, wood.getHangingSign(), 6)
                       .group("hanging_sign")
                       .define('#', wood.getStrippedLog())
                       .define('X', Items.IRON_CHAIN)
                       .pattern("X X").pattern("###").pattern("###")
                       .unlockedBy("has_stripped_logs", has(wood.getStrippedLog()))
                       .save(VanillaFinishedRecipe.output(consumer), key(location(folder + "hanging_sign")));
  }

  /** Creates an unlock criterion for having an item. */
  static Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike item) {
    return inventoryTrigger(ItemPredicate.Builder.item().of(items(), item).build());
  }

  /** Creates an unlock criterion for having an item tag. */
  static Criterion<InventoryChangeTrigger.TriggerInstance> has(TagKey<Item> tag) {
    // build the predicate with a lazy tag holder set: ItemPredicate.Builder.of(lookup, tag) resolves the tag eagerly
    // via getOrThrow, which throws "Missing tag" at datagen time (tags not yet bound).
    return inventoryTrigger(new ItemPredicate(
      java.util.Optional.of(modernmods.hilt.data.loadable.common.LazyTagIngredient.holderSet(tag)),
      net.minecraft.advancements.criterion.MinMaxBounds.Ints.ANY,
      net.minecraft.advancements.criterion.DataComponentMatchers.ANY));
  }

  /** Creates an inventory criterion. */
  static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryTrigger(ItemPredicate... predicates) {
    return CriteriaTriggers.INVENTORY_CHANGED.createCriterion(new InventoryChangeTrigger.TriggerInstance(java.util.Optional.empty(), InventoryChangeTrigger.TriggerInstance.Slots.ANY, java.util.List.of(predicates)));
  }
}
