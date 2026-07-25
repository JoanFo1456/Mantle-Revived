package slimeknights.mantle.client.model.builder;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.generators.template.CustomLoaderBuilder;
import slimeknights.mantle.Mantle;

/** Loader for {@link slimeknights.mantle.client.model.NBTKeyModel} */
@SuppressWarnings("unused")  // API
public class NBTKeyModelBuilder extends CustomLoaderBuilder {
  private String key = null;
  private Identifier extraTexturesKey = null;

  public NBTKeyModelBuilder() {
    super(Mantle.getResource("nbt_key"), true);
  }

  /** Sets the NBT key to check */
  public NBTKeyModelBuilder key(String key) {
    this.key = key;
    return this;
  }

  /** Sets the extra textures key */
  public NBTKeyModelBuilder extraTexturesKey(Identifier extraTexturesKey) {
    this.extraTexturesKey = extraTexturesKey;
    return this;
  }

  @Override
  protected CustomLoaderBuilder copyInternal() {
    NBTKeyModelBuilder builder = new NBTKeyModelBuilder();
    builder.key = this.key;
    builder.extraTexturesKey = this.extraTexturesKey;
    return builder;
  }

  @Override
  public JsonObject toJson(JsonObject json) {
    if (key == null) {
      throw new IllegalStateException("Must set key to use NBTKeyModel");
    }
    json = super.toJson(json);
    json.addProperty("nbt_key", key);
    if (extraTexturesKey != null) {
      json.addProperty("extra_textures_key", extraTexturesKey.toString());
    }
    return json;
  }
}
