package modernmods.hilt.client.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix4fc;
import modernmods.hilt.client.model.util.DynamicItemModel;
import modernmods.hilt.util.RetexturedHelper;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The item-form half of the {@code hilt:retextured} system: renders a block's item model with its retextured slots
 * swapped to the texture of the block stored in the stack's {@link RetexturedHelper} NBT (so a crafting station made of
 * oak shows oak legs in the inventory/creative icon).
 * <p>
 * Registered on {@code RegisterItemModelsEvent} under {@link #ID} and selected by {@code "type": "hilt:retextured"} in a
 * {@code items/*.json} model definition, carrying the same {@code model} + {@code retextured} keys as the block-state half.
 * @see RetexturedBlockStateModel  the in-world block-state half (shares the reskin logic)
 */
public final class RetexturedItemModel {
  /** Registered id — the value of the {@code "type"} key in a retextured item's model definition. */
  public static final Identifier ID = modernmods.hilt.Hilt.getResource("retextured");

  private RetexturedItemModel() {}

  /**
   * Unbaked retextured item model.
   * @param model       Base block/item model providing the geometry and texture slots
   * @param retextured  Texture slot names whose quads get swapped to the stored block's texture
   */
  public record Unbaked(Identifier model, List<String> retextured) implements ItemModel.Unbaked {
    public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
      Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model),
      Codec.STRING.listOf().fieldOf("retextured").forGetter(Unbaked::retextured)
    ).apply(inst, Unbaked::new));

    @Override
    public MapCodec<? extends ItemModel.Unbaked> type() {
      return MAP_CODEC;
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
      resolver.markDependency(model);
    }

    @Override
    public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
      // bake the base geometry once (mirrors CuboidItemModelWrapper.Unbaked#bake), recording the retextured slots' sprites
      ModelBaker baker = context.blockModelBaker();
      ResolvedModel resolved = baker.getModel(model);
      TextureSlots slots = resolved.getTopTextureSlots();
      QuadCollection baseQuads = resolved.bakeTopGeometry(slots, baker, BlockModelRotation.IDENTITY);
      List<TextureAtlasSprite> originals = RetexturedBlockStateModel.resolveRetexturedSprites(slots, retextured, baker, resolved);
      ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, resolved, slots);
      return new Baked(context, transformation, baseQuads, originals, properties);
    }
  }

  /** Baked retextured item model, reskinning the base quads per the stack's stored texture block. */
  private static final class Baked extends DynamicItemModel<Block> {
    private final QuadCollection baseQuads;
    private final List<TextureAtlasSprite> originalSprites;
    private final ModelRenderProperties properties;
    private final ItemModel fallback;

    private Baked(ItemModel.BakingContext context, Matrix4fc transform, QuadCollection baseQuads, List<TextureAtlasSprite> originalSprites, ModelRenderProperties properties) {
      super(context, transform);
      this.baseQuads = baseQuads;
      this.originalSprites = originalSprites;
      this.properties = properties;
      this.fallback = new CuboidItemModelWrapper(List.of(), baseQuads, properties, transform);
    }

    @Nullable
    @Override
    protected Block getCacheKey(ItemStack stack) {
      Block block = RetexturedHelper.getTexture(stack);
      return block == Blocks.AIR ? null : block;
    }

    @Override
    protected ItemModel getFallback() {
      return fallback;
    }

    @Override
    protected ItemModel bakeModel(Block block) {
      QuadCollection reskinned = RetexturedBlockStateModel.reskin(baseQuads, originalSprites, RetexturedBlockStateModel.blockSprite(block));
      return new CuboidItemModelWrapper(List.of(), reskinned, properties, transform);
    }
  }
}
