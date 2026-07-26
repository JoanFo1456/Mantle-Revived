package slimeknights.mantle.client.model.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import lombok.Getter;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.LogicHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Block model for setting color, luminosity, and per element uv lock. Similar to {@link MantleItemLayerModel} but for blocks.
 * <p>
 * In 26.1.2 the vanilla baked quad became immutable and stores emissivity ("light emission") directly on the element.
 * Static per-vertex color is expressed through NeoForge's {@link net.neoforged.neoforge.client.model.quad.BakedColors}
 * field on the quad; {@link #applyColorQuadTransformer(int)} builds a reusable {@link QuadTransformer} for that, and
 * emissivity is applied by overriding the element light emission during baking.
 */
@SuppressWarnings("unused")  // API
public class ColoredBlockModel extends SimpleBlockModel {
  /** Model loader to allow doing basic coloring outside of other models */
  public static final UnbakedModelLoader<SimpleBlockModel> LOADER = ColoredBlockModel::deserialize;

  /** Colors to use for each piece */
  @Getter
  private final List<ColorData> colorData;

  /**
   * Creates a new colored block model
   * @param parameters  Standard top-level model parameters
   * @param textures    Mantle texture representation for iteration
   * @param parts       List of parts in the model
   * @param colorData   Additional information about colors in the model
   */
  public ColoredBlockModel(StandardModelParameters parameters, Map<String,Either<Material,String>> textures, List<CuboidModelElement> parts, List<ColorData> colorData) {
    super(parameters, textures, parts);
    this.colorData = colorData;
  }

  public ColoredBlockModel(SimpleBlockModel base, List<ColorData> colorData) {
    super(base);
    this.colorData = colorData;
  }

  /**
   * Applies the requested light emission (emissivity) to a single element, returning a copy if changed.
   * Per-element uv lock now follows the model state's face transformation, so it is not overridden here.
   */
  private static CuboidModelElement withEmissivity(CuboidModelElement part, ColorData colors) {
    int emissivity = colors.luminosity;
    if (emissivity >= 0 && emissivity != part.lightEmission()) {
      return new CuboidModelElement(part.from(), part.to(), part.faces(), part.rotation(), part.shade(), emissivity);
    }
    return part;
  }

  /**
   * Bakes the given elements applying per-part color and emissivity. Each element is baked individually so its own
   * {@link ColorData#color()} can be applied as a static color modulator on the resulting quads.
   */
  public static QuadCollection bakeColored(List<CuboidModelElement> elements, List<ColorData> colorData, TextureSlots textureSlots, ModelBaker baker, ModelState transform, ModelDebugName name) {
    if (colorData.isEmpty()) {
      return UnbakedCuboidGeometry.bake(elements, textureSlots, baker, transform, name);
    }
    QuadCollection.Builder builder = new QuadCollection.Builder();
    int size = elements.size();
    for (int i = 0; i < size; i++) {
      ColorData colors = LogicHelper.getOrDefault(colorData, i, ColorData.DEFAULT);
      CuboidModelElement part = withEmissivity(elements.get(i), colors);
      QuadCollection baked = UnbakedCuboidGeometry.bake(List.of(part), textureSlots, baker, transform, name);
      builder.addAll(applyColorQuadTransformer(colors.color).process(baked));
    }
    return builder.build();
  }

  /**
   * Creates a reusable transformer applying the given ARGB color as a static per-quad color modulator. Fully opaque white
   * ({@code -1}) is a no-op. Consumers (e.g. Tinkers material models) apply this to the quads produced when baking parts.
   */
  public static QuadTransformer applyColorQuadTransformer(int color) {
    return QuadTransformer.applyingColor(color);
  }

  @Override
  public UnbakedGeometry geometry() {
    return (textureSlots, baker, state, name) -> bakeColored(getElements(), colorData, textureSlots, baker, state, name);
  }

  @Override
  public QuadCollection bake(TextureSlots textureSlots, ModelBaker baker, ModelState transform, ModelDebugName name) {
    return bakeColored(getElements(), colorData, textureSlots, baker, transform, name);
  }

  /**
   * Data class for setting properties when baking colored elements
   */
  public record ColorData(int color, @Deprecated int luminosity, @Nullable Boolean uvlock) {
    public static final ColorData DEFAULT = new ColorData(-1, -1, null);
    public static final RecordLoadable<ColorData> LOADABLE = RecordLoadable.create(
      ColorLoadable.ALPHA.defaultField("color", false, ColorData::color),
      IntLoadable.range(-1, 15).defaultField("luminosity", -1, ColorData::luminosity),
      BooleanLoadable.INSTANCE.nullableField("uvlock", ColorData::uvlock),
      ColorData::new);
    public static final Loadable<List<ColorData>> LIST_LOADABLE = LOADABLE.list(0);

    /** Gets the UV lock for the given part */
    public boolean isUvLock(boolean defaultLock) {
      if (uvlock == null) {
        return defaultLock;
      }
      return uvlock;
    }

    /** @deprecated use {@link #LOADABLE} */
    @Deprecated(forRemoval = true)
    public static ColorData fromJson(JsonObject json) {
      return LOADABLE.deserialize(json);
    }

    /** @deprecated use {@link #LOADABLE} */
    @Deprecated(forRemoval = true)
    public JsonObject toJson() {
      JsonObject json = new JsonObject();
      LOADABLE.serialize(this, json);
      return json;
    }
  }


  /* Deserializing */

  /** Deserializes the model from JSON */
  public static ColoredBlockModel deserialize(JsonObject json, JsonDeserializationContext context) {
    SimpleBlockModel model = SimpleBlockModel.deserialize(json, context);
    List<ColorData> colorData = ColorData.LIST_LOADABLE.getOrDefault(json, "colors", List.of());
    return new ColoredBlockModel(model, colorData);
  }


  /* Color helpers */

  /**
   * Converts an ARGB color to an ABGR color, as the commonly used color format is not the format colors end up packed into.
   * This function doubles as its own inverse, not that its needed.
   * @param color  ARGB color
   * @return  ABGR color
   */
  public static int swapColorRedBlue(int color) {
    return (color & 0xFF00FF00) // alpha and green same spot
           | ((color >> 16) & 0x000000FF) // red moves to blue
           | ((color << 16) & 0x00FF0000); // blue moves to red
  }
}
