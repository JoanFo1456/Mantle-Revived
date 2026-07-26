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
 * In 26.1.2 the vanilla baked quad became immutable and stores emissivity ("light emission") directly on the element, and
 * per-vertex color has no direct equivalent (coloring is handled by tint indices / block colors). This port preserves the
 * emissivity behavior by overriding the element light emission; the static per-vertex color is left as a TODO.
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
   * Applies the color data to the model elements, overriding emissivity where requested.
   * static per-vertex color and per-element uv lock are not yet reimplemented on the new immutable BakedQuad pipeline.
   */
  private static List<CuboidModelElement> applyColorData(List<CuboidModelElement> elements, List<ColorData> colorData) {
    if (colorData.isEmpty()) {
      return elements;
    }
    int size = elements.size();
    List<CuboidModelElement> result = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      CuboidModelElement part = elements.get(i);
      ColorData colors = LogicHelper.getOrDefault(colorData, i, ColorData.DEFAULT);
      int emissivity = colors.luminosity;
      if (emissivity >= 0 && emissivity != part.lightEmission()) {
        part = new CuboidModelElement(part.from(), part.to(), part.faces(), part.rotation(), part.shade(), emissivity);
      }
      result.add(part);
    }
    return result;
  }

  @Override
  public UnbakedGeometry geometry() {
    List<CuboidModelElement> elements = applyColorData(getElements(), colorData);
    return (textureSlots, baker, state, name) -> UnbakedCuboidGeometry.bake(elements, textureSlots, baker, state, name);
  }

  @Override
  public QuadCollection bake(TextureSlots textureSlots, ModelBaker baker, ModelState transform, ModelDebugName name) {
    return UnbakedCuboidGeometry.bake(applyColorData(getElements(), colorData), textureSlots, baker, transform, name);
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
