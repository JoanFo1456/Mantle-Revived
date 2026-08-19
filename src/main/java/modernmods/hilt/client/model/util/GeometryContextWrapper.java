package modernmods.hilt.client.model.util;

import net.minecraft.client.resources.model.sprite.Material;

import javax.annotation.Nullable;

/**
 * Lightweight texture source used to override textures for dynamic models.
 * <p>
 * In 26.1.2 {@code IGeometryBakingContext} was removed; the "owner" is now the unbaked model itself. This class is
 * retained as a minimal texture-override chain used by the connected and retextured models. this is no
 * longer wired into the vanilla {@link net.minecraft.client.resources.model.sprite.TextureSlots} baking pipeline.
 */
@SuppressWarnings("WeakerAccess")
public class GeometryContextWrapper {
  @Nullable
  private final GeometryContextWrapper base;

  public GeometryContextWrapper() {
    this.base = null;
  }

  /**
   * Creates a new configuration wrapper
   * @param base  Base texture source
   */
  public GeometryContextWrapper(@Nullable GeometryContextWrapper base) {
    this.base = base;
  }

  /** Checks if the given texture name resolves to a material */
  public boolean hasMaterial(String name) {
    return base != null && base.hasMaterial(name);
  }

  /** Gets the material for the given texture name, or null if unknown */
  @Nullable
  public Material getMaterial(String name) {
    return base != null ? base.getMaterial(name) : null;
  }
}
