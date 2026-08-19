package modernmods.hilt.client.model.builder;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.generators.template.CustomLoaderBuilder;
import modernmods.hilt.Hilt;
import modernmods.hilt.client.model.util.HiltItemLayerModel.LayerData;

import java.util.ArrayList;
import java.util.List;

/** Builder for {@link modernmods.hilt.client.model.util.HiltItemLayerModel} */
@SuppressWarnings("unused")  // API
public class HiltItemLayerBuilder extends CustomLoaderBuilder {
  private final List<LayerData> layers = new ArrayList<>();

  protected HiltItemLayerBuilder(Identifier loaderId) {
    super(loaderId, true);
  }

  public HiltItemLayerBuilder() {
    this(Hilt.getResource("item_layer"));
  }

  /** Adds data for the next element */
  public HiltItemLayerBuilder addLayer(LayerData data) {
    this.layers.add(data);
    return this;
  }

  /** Sets the color for the next element */
  public HiltItemLayerBuilder color(int color) {
    return addLayer(new LayerData(color, 0, false, null));
  }

  /** Sets the luminosity for the next element */
  public HiltItemLayerBuilder luminosity(int luminosity) {
    return addLayer(new LayerData(-1, luminosity, false, null));
  }

  @Override
  protected CustomLoaderBuilder copyInternal() {
    HiltItemLayerBuilder builder = new HiltItemLayerBuilder();
    builder.layers.addAll(this.layers);
    return builder;
  }

  @Override
  public JsonObject toJson(JsonObject json) {
    json = super.toJson(json);
    if (!layers.isEmpty()) {
      json.add("layers", LayerData.LIST_LOADABLE.serialize(layers));
    }
    return json;
  }
}
