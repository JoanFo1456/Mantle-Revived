package modernmods.mantle.client.model.builder;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.generators.template.CustomLoaderBuilder;
import modernmods.mantle.Mantle;
import modernmods.mantle.client.model.util.ColoredBlockModel.ColorData;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for {@link modernmods.mantle.client.model.util.ColoredBlockModel}, used as a base for other model builders.
 * <p>
 * Ported to the 26.1.2 datagen {@link CustomLoaderBuilder}, which is no longer generic over a model builder and no longer
 * takes an {@code ExistingFileHelper}.
 */
public class ColoredModelBuilder extends CustomLoaderBuilder {
  private final List<ColorData> colors = new ArrayList<>();

  public ColoredModelBuilder() {
    this(Mantle.getResource("colored_block"));
  }

  protected ColoredModelBuilder(Identifier loaderId) {
    super(loaderId, true);
  }

  /** Adds a full color data for the next element */
  public ColoredModelBuilder colorData(ColorData data) {
    colors.add(data);
    return this;
  }

  /** Sets the color for the next element */
  public ColoredModelBuilder color(int color) {
    return colorData(new ColorData(color, -1, null));
  }

  /** Sets the luminosity for the next element */
  public ColoredModelBuilder luminosity(int luminosity) {
    return colorData(new ColorData(-1, luminosity, null));
  }

  /** Copies the color data into the given builder, for {@link #copyInternal()} */
  protected void copyColors(ColoredModelBuilder builder) {
    builder.colors.addAll(this.colors);
  }

  @Override
  protected CustomLoaderBuilder copyInternal() {
    ColoredModelBuilder builder = new ColoredModelBuilder();
    copyColors(builder);
    return builder;
  }

  @Override
  public JsonObject toJson(JsonObject json) {
    json = super.toJson(json);
    if (!colors.isEmpty()) {
      json.add("colors", ColorData.LIST_LOADABLE.serialize(colors));
    }
    return json;
  }
}
