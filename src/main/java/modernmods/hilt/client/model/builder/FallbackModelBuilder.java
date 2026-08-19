package modernmods.hilt.client.model.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.neoforged.neoforge.client.model.generators.template.CustomLoaderBuilder;
import modernmods.hilt.Hilt;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for {@link modernmods.hilt.client.model.FallbackModelLoader}.
 * <p>
 * In 26.1.2 datagen no longer exposes a generic {@code ModelBuilder}; child models are now supplied as pre-serialized
 * {@link JsonObject} instances. update callers to pass serialized child models.
 */
public class FallbackModelBuilder extends CustomLoaderBuilder {
  private final List<DomainModel> models = new ArrayList<>();

  public FallbackModelBuilder() {
    super(Hilt.getResource("fallback"), true);
  }

  /** Adds a fallback model with a domain restriction */
  public FallbackModelBuilder fallback(JsonObject model, @Nullable String modId) {
    this.models.add(new DomainModel(model, modId));
    return this;
  }

  /** Adds a fallback model using the loader ID as the domain restriction */
  public FallbackModelBuilder fallback(JsonObject model) {
    return fallback(model, null);
  }

  @Override
  protected CustomLoaderBuilder copyInternal() {
    FallbackModelBuilder builder = new FallbackModelBuilder();
    builder.models.addAll(this.models);
    return builder;
  }

  @Override
  public JsonObject toJson(JsonObject json) {
    json = super.toJson(json);
    if (this.models.size() < 2) {
      throw new IllegalStateException("Must have at least two models to use the fallback loader");
    }
    JsonArray fallbacks = new JsonArray();
    for (DomainModel builder : models) {
      fallbacks.add(builder.toJson());
    }
    json.add("models", fallbacks);
    return json;
  }

  /** Builder with an optional domain restriction */
  private record DomainModel(JsonObject model, @Nullable String domain) {
    /** Converts this to JSON */
    public JsonObject toJson() {
      JsonObject json = model.deepCopy();
      if (domain != null) {
        json.addProperty("fallback_mod_id", domain);
      }
      return json;
    }
  }
}
