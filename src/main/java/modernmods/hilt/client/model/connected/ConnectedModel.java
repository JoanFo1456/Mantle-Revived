package modernmods.hilt.client.model.connected;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateUnbakedModel;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import modernmods.hilt.client.model.util.ColoredBlockModel;
import modernmods.hilt.client.model.util.ModelTextureIteratable;
import modernmods.hilt.client.model.util.SimpleBlockModel;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Model that handles generating variants for connected textures.
 * <p>
 * In 26.1.2 the dynamic model-data pipeline ({@code ModelData}/{@code ModelProperty}/{@code BakedModelWrapper}) and the
 * mutable {@code BlockElement} representation were removed. This class preserves the deserialization, the connection
 * registry hookup, and the pure connection bit math, delegating static geometry to the wrapped model. To connect textures
 * dynamically, register a {@code CustomUnbakedBlockStateModel} whose baked model extends
 * {@link modernmods.hilt.client.model.util.DynamicBakedWrapper} and, from the connection bits in the block's
 * {@code ModelData}, selects the pre-baked connected variant. The per-connection re-bake plus the blockstate JSON wiring
 * must be validated visually in-game.
 */
public class ConnectedModel extends DelegateUnbakedModel {
  /** Loader instance */
  public static final UnbakedModelLoader<ConnectedModel> LOADER = ConnectedModel::deserialize;

  /** Parent model */
  private final SimpleBlockModel model;
  /** Map of texture name to index of suffixes (indexed as 0bENWS) */
  private final Map<String,String[]> connectedTextures;
  /** Function to run to check if this block connects to another */
  private final BiPredicate<BlockState,BlockState> connectionPredicate;
  /** List of sides to check when getting block directions */
  private final Set<Direction> sides;
  /** Cache of resolved connected texture names */
  private final Map<String,String> nameMappingCache = new ConcurrentHashMap<>();
  private final ModelTextureIteratable modelTextures;

  public ConnectedModel(SimpleBlockModel model, Map<String,String[]> connectedTextures, BiPredicate<BlockState,BlockState> connectionPredicate, Set<Direction> sides) {
    super(model);
    this.model = model;
    this.connectedTextures = connectedTextures;
    this.connectionPredicate = connectionPredicate;
    this.sides = sides;
    this.modelTextures = ModelTextureIteratable.of(model);
  }

  /** Gets the sides checked for connections */
  public Set<Direction> getSides() {
    return sides;
  }

  /** Gets the connection predicate */
  public BiPredicate<BlockState,BlockState> getConnectionPredicate() {
    return connectionPredicate;
  }

  /**
   * Gets the direction rotated
   * @param direction  Original direction to rotate
   * @param rotation   Rotation origin, aka the face of the block we are looking at. As a result, UP is identity
   * @return  Rotated direction
   */
  public static Direction rotateDirection(Direction direction, Direction rotation) {
    if (rotation == Direction.UP) {
      return direction;
    }
    if (rotation == Direction.DOWN) {
      // Z is backwards on the bottom
      if (direction.getAxis() == Axis.Z) {
        return direction.getOpposite();
      }
      // X is normal
      return direction;
    }
    // sides all just have the next side for left and right, and consistent up and down
    return switch (direction) {
      case NORTH -> Direction.UP;
      case SOUTH -> Direction.DOWN;
      case EAST -> rotation.getCounterClockWise();
      case WEST -> rotation.getClockWise();
      default -> throw new IllegalArgumentException("Direction must be horizontal axis");
    };
  }

  /** Uncached variant of {@link #getConnectedName(String)}, used internally */
  private String getConnectedNameUncached(String key) {
    // iterate into the parent models, trying to find a match
    String check = key;
    String found = "";
    for(Map<String, Either<Material, String>> textures : modelTextures) {
      Either<Material, String> either = textures.get(check);
      if (either != null) {
        // if no name, its not connected
        Optional<String> newName = either.right();
        if (newName.isEmpty()) {
          break;
        }
        // if the name is connected, we are done
        check = newName.get();
        if (connectedTextures.containsKey(check)) {
          found = check;
          break;
        }
      }
    }
    return found;
  }

  /**
   * Gets the name of this texture that supports connected textures, or empty string if never connected
   * @param key  Name of the part texture
   * @return  Name of the connected texture
   */
  public String getConnectedName(String key) {
    if (key.isEmpty()) {
      return "";
    }
    if (key.charAt(0) == '#') {
      key = key.substring(1);
    }
    // if the name is connected, we are done
    if (connectedTextures.containsKey(key)) {
      return key;
    }
    return nameMappingCache.computeIfAbsent(key, this::getConnectedNameUncached);
  }

  /**
   * Gets the texture suffix
   * @param texture      Texture name, must be a connected texture
   * @param connections  Connections byte
   * @param transform    Rotations to apply to faces
   * @return  Key used to cache it
   */
  public String getTextureSuffix(String texture, byte connections, Function<Direction,Direction> transform) {
    int key = 0;
    for (Direction dir : Plane.HORIZONTAL) {
      int flag = 1 << transform.apply(dir).get3DDataValue();
      if ((connections & flag) == flag) {
        key |= 1 << dir.get2DDataValue();
      }
    }
    // if empty, do not prefix
    String[] suffixes = connectedTextures.get(texture);
    assert suffixes != null;
    String suffix = suffixes[key];
    if (suffix.isEmpty()) {
      return suffix;
    }
    return "_" + suffix;
  }

  /**
   * Gets an array of directions to whether a block exists on the side, indexed using direction indexes
   * @param predicate  Function that returns true if the block is connected on the given side
   * @return  Byte with 6 bits for the 6 different sides
   */
  public static byte getConnections(java.util.function.Predicate<Direction> predicate) {
    byte connections = 0;
    for (Direction dir : Direction.values()) {
      if (predicate.test(dir)) {
        connections |= 1 << dir.get3DDataValue();
      }
    }
    return connections;
  }

  /** Loader class containing singleton instance */
  public static ConnectedModel deserialize(JsonObject json, JsonDeserializationContext context) {
    ColoredBlockModel model = ColoredBlockModel.deserialize(json, context);

    // root object for all model data
    JsonObject data = GsonHelper.getAsJsonObject(json, "connection");

    // need at least one connected texture
    JsonObject connected = GsonHelper.getAsJsonObject(data, "textures");
    if (connected.size() == 0) {
      throw new JsonSyntaxException("Must have at least one texture in connected");
    }

    // build texture list
    Map<String,String[]> connectedTextures = new HashMap<>(connected.size());
    for (Entry<String,JsonElement> entry : connected.entrySet()) {
      // don't validate texture as it may be contained in a child model that is not yet loaded
      // get type, put in map
      String name = entry.getKey();
      connectedTextures.put(name, ConnectedModelRegistry.deserializeType(entry.getValue(), "textures[" + name + "]"));
    }

    // get a list of sides to pay attention to
    Set<Direction> sides;
    if (data.has("sides")) {
      JsonArray array = GsonHelper.getAsJsonArray(data, "sides");
      sides = EnumSet.noneOf(Direction.class);
      for (int i = 0; i < array.size(); i++) {
        String side = GsonHelper.convertToString(array.get(i), "sides[" + i + "]");
        Direction dir = Direction.byName(side);
        if (dir == null) {
          throw new JsonParseException("Invalid side " + side);
        }
        sides.add(dir);
      }
    } else {
      sides = EnumSet.allOf(Direction.class);
    }

    // other data
    BiPredicate<BlockState,BlockState> predicate = ConnectedModelRegistry.deserializePredicate(data, "predicate");

    // final model instance
    return new ConnectedModel(model, Map.copyOf(connectedTextures), predicate, sides);
  }
}
