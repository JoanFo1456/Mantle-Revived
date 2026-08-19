// Credit to Immersive Engineering and blusunrize for this class
// See: https://github.com/BluSunrize/ImmersiveEngineering/blob/1.18/src/main/java/blusunrize/immersiveengineering/common/util/fakeworld/TemplateWorld.java
package modernmods.hilt.client.book.structure.level;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * World implementation for the book structures
 */
public class TemplateLevel extends Level {

  private final Map<MapId, MapItemSavedData> maps = new HashMap<>();
  private final Scoreboard scoreboard = new Scoreboard();
  private final TickRateManager tickRateManager = new TickRateManager();
  private final TemplateChunkSource chunkSource;
  private LevelData.RespawnData respawnData = LevelData.RespawnData.DEFAULT;
  private float dayTimeFraction;
  private float dayTimePerTick = 1;

  public TemplateLevel(List<StructureBlockInfo> blocks, Predicate<BlockPos> shouldShow) {
    super(
      new FakeLevelData(), Level.OVERWORLD, Objects.requireNonNull(Minecraft.getInstance().level).registryAccess(),
      Objects.requireNonNull(Minecraft.getInstance().level).registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD),
      true, false, 0, 0
    );

    this.chunkSource = new TemplateChunkSource(blocks, this, shouldShow);
  }

  @Override
  public void sendBlockUpdated(@Nonnull BlockPos pos, @Nonnull BlockState oldState, @Nonnull BlockState newState, int flags) {}

  @Override
  public void playSeededSound(@Nullable Entity pPlayer, double pX, double pY, double pZ, Holder<SoundEvent> pSound, SoundSource pSource, float pVolume, float pPitch, long pSeed) {}

  @Override
  public void playSeededSound(@Nullable Entity pPlayer, Entity pEntity, Holder<SoundEvent> pSound, SoundSource pCategory, float pVolume, float pPitch, long pSeed) {}

  @Override
  public String gatherChunkSourceStats() {
    return chunkSource.gatherStats();
  }

  @Nullable
  @Override
  public Entity getEntity(int id) {
    return null;
  }

  @Nullable
  @Override
  public MapItemSavedData getMapData(@Nonnull MapId mapId) {
    return this.maps.get(mapId);
  }

  @Override
  public void destroyBlockProgress(int breakerId, @Nonnull BlockPos pos, int progress) {}

  @Nonnull
  @Override
  public Scoreboard getScoreboard() {
    return this.scoreboard;
  }

  @Override
  public net.minecraft.world.item.crafting.RecipeAccess recipeAccess() {
    // the structure-preview level never resolves recipes, so an empty access is correct
    return new net.minecraft.world.item.crafting.RecipeAccess() {
      @Override
      public net.minecraft.world.item.crafting.RecipePropertySet propertySet(net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.RecipePropertySet> key) {
        return net.minecraft.world.item.crafting.RecipePropertySet.EMPTY;
      }

      @Override
      public net.minecraft.world.item.crafting.SelectableRecipe.SingleInputSet<net.minecraft.world.item.crafting.StonecutterRecipe> stonecutterRecipes() {
        return net.minecraft.world.item.crafting.SelectableRecipe.SingleInputSet.empty();
      }
    };
  }

  @Override
  public net.minecraft.world.clock.ClockManager clockManager() {
    // the preview level does not advance time, so every clock reads zero total ticks
    return clock -> 0L;
  }

  @Override
  public net.minecraft.world.attribute.EnvironmentAttributeSystem environmentAttributes() {
    // an attribute system with no layers yields vanilla defaults, which is what a static preview needs
    return net.minecraft.world.attribute.EnvironmentAttributeSystem.builder().build();
  }

  @Override
  public net.minecraft.world.level.block.entity.FuelValues fuelValues() {
    return net.minecraft.world.level.block.entity.FuelValues.vanillaBurnTimes(registryAccess(), enabledFeatures());
  }

  @Override
  public void explode(@Nullable Entity source, net.minecraft.world.damagesource.DamageSource damageSource, @Nullable net.minecraft.world.level.ExplosionDamageCalculator calculator, double x, double y, double z, float radius, boolean fire, Level.ExplosionInteraction interaction, net.minecraft.core.particles.ParticleOptions smallParticle, net.minecraft.core.particles.ParticleOptions largeParticle, net.minecraft.util.random.WeightedList<net.minecraft.core.particles.ExplosionParticleInfo> particles, Holder<SoundEvent> sound) {}

  @Override
  public void setRespawnData(LevelData.RespawnData respawnData) {
    this.respawnData = respawnData;
  }

  @Override
  public LevelData.RespawnData getRespawnData() {
    return this.respawnData;
  }

  @Override
  public java.util.Collection<? extends net.neoforged.neoforge.entity.PartEntity<?>> dragonParts() {
    return List.of();
  }

  @Override
  public TickRateManager tickRateManager() {
    return this.tickRateManager;
  }

  @Override
  public PotionBrewing potionBrewing() {
    return PotionBrewing.EMPTY;
  }

  @Override
  protected LevelEntityGetter<Entity> getEntities() {
    return FakeEntityGetter.INSTANCE;
  }

  @Nonnull
  @Override
  public LevelTickAccess<Block> getBlockTicks() {
    return BlackholeTickAccess.emptyLevelList();
  }

  @Nonnull
  @Override
  public LevelTickAccess<Fluid> getFluidTicks() {
    return BlackholeTickAccess.emptyLevelList();
  }

  @Nonnull
  @Override
  public ChunkSource getChunkSource() {
    return this.chunkSource;
  }

  @Override
  public void levelEvent(@Nullable Entity player, int type, @Nonnull BlockPos pos, int data) {}

  @Override
  public void gameEvent(Holder<GameEvent> pEvent, Vec3 pPosition, Context pContext) {}

  @Override
  public FeatureFlagSet enabledFeatures() {
    return FeatureFlagSet.of();
  }

  @Override
  public int getSeaLevel() {
    return 63;
  }

  @Override
  public net.minecraft.world.level.border.WorldBorder getWorldBorder() {
    return new net.minecraft.world.level.border.WorldBorder();
  }

  @Nonnull
  @Override
  public List<? extends Player> players() {
    return List.of();
  }

  @Nonnull
  @Override
  public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
    return registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
  }
}
