package modernmods.mantle.fluid.texture;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.minecraft.core.Registry;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import modernmods.mantle.Mantle;
import modernmods.mantle.data.listener.IEarlySafeManagerReloadListener;
import modernmods.mantle.util.JsonHelper;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Manager for handling fluid textures */
public class FluidTextureManager implements IEarlySafeManagerReloadListener {
  /** Folder containing the logic */
  public static final String FOLDER = "mantle/fluid_texture";

  /* Instance data */
  private static final FluidTextureManager INSTANCE = new FluidTextureManager();
  /** Map of fluid type to texture */
  private Map<FluidType,FluidTexture> textures = Collections.emptyMap();
  /** True once the data has been loaded at least once (via the reload listener or the on-demand fallback) */
  private volatile boolean loaded = false;
  /** Fallback texture instance */
  private static final FluidTexture FALLBACK = new FluidTexture(Identifier.withDefaultNamespace("block/water_still"), Identifier.withDefaultNamespace("block/water_flow"), null, null, 0, -1, -1, false, false, 0, 0);

  private FluidTextureManager() {}


  /**
   * Initializes this manager, registering it with the resource manager
   */
  public static void init(AddClientReloadListenersEvent event) {
    Identifier key = Mantle.getResource("fluid_texture");
    event.addListener(key, INSTANCE);
    // must load before the model manager: the 26.1 RegisterFluidModelsEvent fires during the MODELS reload and reads this
    // manager's data (see MantleFluidClientExtensions). Without the ordering the data is empty at that point, so every
    // fluid registers the water FALLBACK sprites and renders wrong/blank.
    event.addDependency(key, net.neoforged.neoforge.client.resources.VanillaClientListeners.MODELS);
  }

  @Override
  public void onReloadSafe(ResourceManager resourceManager) {
    long time = System.nanoTime();
    // fetch JSONs
    Map<Identifier,JsonElement> jsons = new HashMap<>();
    SimpleJsonResourceReloadListener.scanDirectory(resourceManager, FileToIdConverter.json(FOLDER), JsonOps.INSTANCE, ExtraCodecs.JSON, jsons);

    // start building fluid type map
    Map<FluidType, FluidTexture> map = new HashMap<>();
    Registry<FluidType> fluidTypeRegistry = NeoForgeRegistries.FLUID_TYPES;


    for (Map.Entry<Identifier,JsonElement> entry : jsons.entrySet()) {
      Identifier id = entry.getKey();
      // first step is to find the matching fluid type, if there is none ignore the file
      FluidType type = fluidTypeRegistry.getValue(id);
      if (type == null || !id.equals(fluidTypeRegistry.getKey(type))) {
        Mantle.logger.debug("Ignoring fluid texture {} as no fluid type exists with that name", id);
      } else {
        // parse it if valid
        map.put(type, FluidTexture.deserialize(GsonHelper.convertToJsonObject(entry.getValue(), "fluid_texture")));
      }
    }
    this.textures = map;
    this.loaded = true;
    Mantle.logger.info("Loaded {} fluid textures in {} ms", map.size(), (System.nanoTime() - time) / 1000000f);
  }

  /**
   * Loads the data on demand if the reload listener has not populated it yet. In 26.1 the fluid models bake (and fire
   * RegisterFluidModelsEvent, which reads this data) inside the model manager's ASYNC prepare phase, which can run before
   * this reload listener finishes — reload-listener ordering does not help there. Reading the (static) fluid_texture
   * resources here yields the correct sprites regardless of order.
   */
  private synchronized void ensureLoaded() {
    if (!loaded) {
      net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
      if (mc != null && mc.getResourceManager() != null) {
        try {
          onReloadSafe(mc.getResourceManager());
        } catch (Exception e) {
          Mantle.logger.error("Failed to load fluid textures on demand", e);
        }
      }
    }
  }

  /** Gets the texture for the given fluid */
  public static FluidTexture getData(FluidType fluid) {
    INSTANCE.ensureLoaded();
    return INSTANCE.textures.getOrDefault(fluid, FALLBACK);
  }

  /** Gets the still texture for the given fluid */
  public static Identifier getStillTexture(FluidType fluid) {
    return getData(fluid).still();
  }

  /** Gets the flowing texture for the given fluid */
  public static Identifier getFlowingTexture(FluidType fluid) {
    return getData(fluid).flowing();
  }

  /** Gets the overlay texture for the given fluid */
  @Nullable
  public static Identifier getOverlayTexture(FluidType fluid) {
    return getData(fluid).overlay();
  }

  /** Gets the camera texture for the given fluid */
  @Nullable
  public static Identifier getCameraTexture(FluidType fluid) {
    return getData(fluid).camera();
  }

  /** Gets the still texture for the given fluid */
  public static int getColor(FluidType fluid) {
    return getData(fluid).color();
  }
}
