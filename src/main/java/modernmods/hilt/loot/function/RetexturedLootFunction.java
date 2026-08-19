package modernmods.hilt.loot.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import modernmods.hilt.Hilt;
import modernmods.hilt.block.entity.IRetexturedBlockEntity;
import modernmods.hilt.loot.HiltLoot;
import modernmods.hilt.util.RetexturedHelper;

import java.util.List;
import java.util.Set;

/**
 * Applies the data for a retextured block to the dropped item. No configuration needed.
 */
@SuppressWarnings("WeakerAccess")
public class RetexturedLootFunction extends LootItemConditionalFunction {
  public static final MapCodec<RetexturedLootFunction> CODEC = RecordCodecBuilder.mapCodec(
    instance -> commonFields(instance).apply(instance, RetexturedLootFunction::new)
  );

  /**
   * Creates a new instance from the given conditions
   * @param conditions Conditions list
   */
  public RetexturedLootFunction(List<LootItemCondition> conditions) {
    super(conditions);
  }

  /** Creates a new instance with no conditions */
  public RetexturedLootFunction() {
    super(List.of());
  }

  @Override
  public Set<ContextKey<?>> getReferencedContextParams() {
    return Set.of(LootContextParams.BLOCK_ENTITY);
  }

  @Override
  protected ItemStack run(ItemStack stack, LootContext context) {
    BlockEntity te = context.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
    if (te instanceof IRetexturedBlockEntity retextured) {
      RetexturedHelper.setTexture(stack, retextured.getTextureName());
    } else {
      String name = te == null ? "null" : te.getClass().getName();
      Hilt.logger.warn("Found wrong tile entity for loot function, expected IRetexturedTileEntity, found {}", name);
    }
    return stack;
  }

  @Override
  public MapCodec<? extends LootItemConditionalFunction> codec() {
    return CODEC;
  }
}
