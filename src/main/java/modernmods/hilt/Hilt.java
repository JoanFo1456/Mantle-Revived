package modernmods.hilt;

import net.minecraft.util.Util;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import modernmods.hilt.block.entity.HiltHangingSignBlockEntity;
import modernmods.hilt.block.entity.HiltSignBlockEntity;
import modernmods.hilt.client.ClientEvents;
import modernmods.hilt.command.HiltCommand;
import modernmods.hilt.command.argument.ResourceOrTagKeyArgument;
import modernmods.hilt.config.Config;
import modernmods.hilt.data.predicate.block.BlockPredicate;
import modernmods.hilt.data.predicate.block.BlockPropertiesPredicate;
import modernmods.hilt.data.predicate.damage.DamageSourcePredicate;
import modernmods.hilt.data.predicate.damage.DamageTypePredicate;
import modernmods.hilt.data.predicate.damage.SourceAttackerPredicate;
import modernmods.hilt.data.predicate.damage.SourceMessagePredicate;
import modernmods.hilt.data.predicate.entity.BlockAtEntityPredicate;
import modernmods.hilt.data.predicate.entity.HasEnchantmentEntityPredicate;
import modernmods.hilt.data.predicate.entity.HasMobEffectPredicate;
import modernmods.hilt.data.predicate.entity.LivingEntityPredicate;
import modernmods.hilt.data.predicate.entity.MobTypePredicate;
import modernmods.hilt.data.predicate.fluid.FluidPredicate;
import modernmods.hilt.data.predicate.fluid.FluidTypePredicate;
import modernmods.hilt.data.predicate.item.ItemPredicate;
import modernmods.hilt.datagen.HiltBlockTagProvider;
import modernmods.hilt.datagen.HiltFluidTagProvider;
import modernmods.hilt.datagen.HiltFluidTooltipProvider;
import modernmods.hilt.datagen.HiltFluidTransferProvider;
import modernmods.hilt.datagen.HiltMenuTagProvider;
import modernmods.hilt.datagen.HiltTags;
import modernmods.hilt.fluid.transfer.EmptyFluidContainerTransfer;
import modernmods.hilt.fluid.transfer.EmptyFluidWithNBTTransfer;
import modernmods.hilt.fluid.transfer.EmptyPotionTransfer;
import modernmods.hilt.fluid.transfer.FillFluidContainerTransfer;
import modernmods.hilt.fluid.transfer.FillFluidWithNBTTransfer;
import modernmods.hilt.fluid.transfer.FluidContainerTransferManager;
import modernmods.hilt.item.LecternBookItem;
import modernmods.hilt.loot.LootTableInjector;
import modernmods.hilt.loot.HiltLoot;
import modernmods.hilt.network.HiltNetwork;
import modernmods.hilt.recipe.HiltRecipes;
import modernmods.hilt.recipe.sync.RecipeSyncHandler;
import modernmods.hilt.recipe.helper.TagPreference;
import modernmods.hilt.registration.RegistrationHelper;
import modernmods.hilt.registration.HiltRegistrations;
import modernmods.hilt.registration.adapter.BlockEntityTypeRegistryAdapter;
import modernmods.hilt.util.OffhandCooldownTracker;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Hilt
 *
 * Central mod object for Hilt
 *
 * @author Sunstrike <sun@sunstrike.io>
 */
@Mod(Hilt.modId)
public class Hilt {
  public static final String modId = "hilt";
  public static final Logger logger = LogManager.getLogger("Hilt");
  /** Namespace for common tags, used for easier migration to the future "c" standard */
  public static final String COMMON = "c";

  /* Instance of this mod, used for grabbing prototype fields */
  public static Hilt instance;

  /* Proxies for sides, used for graphics processing */
  public Hilt(IEventBus bus, ModContainer container) {
    container.registerConfig(Type.CLIENT, Config.CLIENT_SPEC);
    container.registerConfig(Type.SERVER, Config.SERVER_SPEC);

    FluidContainerTransferManager.INSTANCE.init();
    HiltTags.init();

    instance = this;
    bus.addListener(EventPriority.NORMAL, false, FMLCommonSetupEvent.class, this::commonSetup);
    bus.addListener(EventPriority.NORMAL, false, RegisterCapabilitiesEvent.class, this::registerCapabilities);
    // 26.1.2 split GatherDataEvent into Server/Client subclasses; register for both and detect which via instanceof
    bus.addListener(EventPriority.NORMAL, false, GatherDataEvent.Server.class, this::gatherData);
    bus.addListener(EventPriority.NORMAL, false, GatherDataEvent.Client.class, this::gatherData);
    bus.addListener(EventPriority.NORMAL, false, RegisterPayloadHandlersEvent.class, HiltNetwork::registerPackets);
    bus.addListener(EventPriority.NORMAL, false, RegisterEvent.class, this::register);
    HiltNetwork.init();
    HiltRecipes.init(bus);
    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerInteractEvent.RightClickBlock.class, LecternBookItem::interactWithBlock);
    // server-side recipe sync: OnDatapackSyncEvent is a game-bus event, fired on join and /reload
    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, OnDatapackSyncEvent.class, RecipeSyncHandler::onDatapackSync);

    if (FMLEnvironment.getDist() == Dist.CLIENT) {
      ClientEvents.onConstruct();
    }
  }

  private void registerCapabilities(RegisterCapabilitiesEvent event) {
    OffhandCooldownTracker.register(event);
  }

  private void commonSetup(final FMLCommonSetupEvent event) {
    HiltCommand.init();
    OffhandCooldownTracker.init();
    TagPreference.init();
    LootTableInjector.init();
  }

  private void register(RegisterEvent event) {
    ResourceKey<?> key = event.getRegistryKey();
    if (key == Registries.RECIPE_SERIALIZER) {
      // fluid container transfer
      FluidContainerTransferManager.TRANSFER_LOADERS.registerDeserializer(EmptyFluidContainerTransfer.ID, EmptyFluidContainerTransfer.DESERIALIZER);
      FluidContainerTransferManager.TRANSFER_LOADERS.registerDeserializer(FillFluidContainerTransfer.ID, FillFluidContainerTransfer.DESERIALIZER);
      FluidContainerTransferManager.TRANSFER_LOADERS.registerDeserializer(EmptyFluidWithNBTTransfer.ID, EmptyFluidWithNBTTransfer.DESERIALIZER);
      FluidContainerTransferManager.TRANSFER_LOADERS.registerDeserializer(FillFluidWithNBTTransfer.ID, FillFluidWithNBTTransfer.DESERIALIZER);
      FluidContainerTransferManager.TRANSFER_LOADERS.registerDeserializer(EmptyPotionTransfer.ID, EmptyPotionTransfer.DESERIALIZER);

      // predicates
      {
        // block predicates
        BlockPredicate.LOADER.register(getResource("requires_tool"), BlockPredicate.REQUIRES_TOOL.getLoader());
        BlockPredicate.LOADER.register(getResource("blocks_motion"), BlockPredicate.BLOCKS_MOTION.getLoader());
        BlockPredicate.LOADER.register(getResource("can_be_replaced"), BlockPredicate.CAN_BE_REPLACED.getLoader());
        BlockPredicate.LOADER.register(getResource("block_properties"), BlockPropertiesPredicate.LOADER);

        // item predicates
        ItemPredicate.LOADER.register(getResource("has_container"), ItemPredicate.HAS_CONTAINER.getLoader());
        ItemPredicate.LOADER.register(getResource("may_have_transfer"), ItemPredicate.MAY_HAVE_TRANSFER.getLoader());

        // fluid predicates
        FluidPredicate.LOADER.register(getResource("fluid_type"), FluidTypePredicate.LOADER);
        FluidPredicate.LOADER.register(getResource("is_source"), FluidPredicate.SOURCE.getLoader());
        FluidPredicate.LOADER.register(getResource("has_bucket"), FluidPredicate.HAS_BUCKET.getLoader());
        FluidPredicate.LOADER.register(getResource("lighter_than_air"), FluidPredicate.LIGHTER_THAN_AIR.getLoader());

        // entity predicates
        // simple
        LivingEntityPredicate.LOADER.register(getResource("fire_immune"), LivingEntityPredicate.FIRE_IMMUNE.getLoader());
        LivingEntityPredicate.LOADER.register(getResource("can_freeze"), LivingEntityPredicate.CAN_FREEZE.getLoader());
        LivingEntityPredicate.LOADER.register(getResource("water_sensitive"), LivingEntityPredicate.WATER_SENSITIVE.getLoader());
        LivingEntityPredicate.LOADER.register(getResource("on_fire"), LivingEntityPredicate.ON_FIRE.getLoader());
        LivingEntityPredicate.LOADER.register(getResource("is_freezing"), LivingEntityPredicate.IS_FREEZING.getLoader());
        LivingEntityPredicate.LOADER.register(getResource("is_in_powdered_snow"), LivingEntityPredicate.IS_IN_POWDERED_SNOW.getLoader());
        LivingEntityPredicate.LOADER.register(getResource("on_ground"), LivingEntityPredicate.ON_GROUND.getLoader());
        LivingEntityPredicate.LOADER.register(getResource("crouching"), LivingEntityPredicate.CROUCHING.getLoader());
        LivingEntityPredicate.LOADER.register(getResource("sprinting"), LivingEntityPredicate.SPRINTING.getLoader());
        LivingEntityPredicate.LOADER.register(getResource("blocking"), LivingEntityPredicate.BLOCKING.getLoader());
        LivingEntityPredicate.LOADER.register(getResource("elytra_flying"), LivingEntityPredicate.ELYTRA_FLYING.getLoader());
        LivingEntityPredicate.LOADER.register(getResource("has_effect"), HasMobEffectPredicate.LOADER);
        LivingEntityPredicate.LOADER.register(getResource("block_at_entity"), BlockAtEntityPredicate.LOADER);
        LivingEntityPredicate.LOADER.register(getResource("eyes_in_water"), LivingEntityPredicate.EYES_IN_WATER.getLoader());
        LivingEntityPredicate.LOADER.register(getResource("feet_in_water"), LivingEntityPredicate.FEET_IN_WATER.getLoader());
        LivingEntityPredicate.LOADER.register(getResource("underwater"), LivingEntityPredicate.UNDERWATER.getLoader());
        LivingEntityPredicate.LOADER.register(getResource("raining_at"), LivingEntityPredicate.RAINING.getLoader());
        // property
        LivingEntityPredicate.LOADER.register(getResource("mob_type"), MobTypePredicate.LOADER);
        LivingEntityPredicate.LOADER.register(getResource("has_enchantment"), HasEnchantmentEntityPredicate.LOADER);
        // register mob types
        MobTypePredicate.registerDefaults();

        // damage predicates
        // simple
        DamageSourcePredicate.LOADER.register(getResource("has_entity"), DamageSourcePredicate.HAS_ENTITY.getLoader());
        DamageSourcePredicate.LOADER.register(getResource("is_indirect"), DamageSourcePredicate.IS_INDIRECT.getLoader());
        DamageSourcePredicate.LOADER.register(getResource("can_protect"), DamageSourcePredicate.CAN_PROTECT.getLoader());
        // fields
        DamageSourcePredicate.LOADER.register(getResource("damage_type"), DamageTypePredicate.LOADER);
        DamageSourcePredicate.LOADER.register(getResource("message"), SourceMessagePredicate.LOADER);
        DamageSourcePredicate.LOADER.register(getResource("attacker"), SourceAttackerPredicate.LOADER);
      }
    }
    else if (key == Registries.BLOCK_ENTITY_TYPE) {
      BlockEntityTypeRegistryAdapter adapter = new BlockEntityTypeRegistryAdapter(Objects.requireNonNull(event.getRegistry(Registries.BLOCK_ENTITY_TYPE)), modId);
      Set<Block> signs = HiltSignBlockEntity.buildSignBlocks();
      if (!signs.isEmpty()) {
        HiltRegistrations.SIGN = adapter.register(HiltSignBlockEntity::new, signs, "sign");
      }
      signs = HiltHangingSignBlockEntity.buildSignBlocks();
      if (!signs.isEmpty()) {
        HiltRegistrations.HANGING_SIGN = adapter.register(HiltHangingSignBlockEntity::new, signs, "hanging_sign");
      }
    }
    else if (key == Registries.COMMAND_ARGUMENT_TYPE) {
      ResourceOrTagKeyArgument.Info<?> info = new ResourceOrTagKeyArgument.Info<>();
      Registry.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, getResource("resource_or_tag_key"), info);
      ArgumentTypeInfos.registerByClass(RegistrationHelper.genericArgumentType(ResourceOrTagKeyArgument.class), info);
    }
    else {
      HiltLoot.registerGlobalLootModifiers(event);
    }
  }

  private void gatherData(final GatherDataEvent event) {
    DataGenerator generator = event.getGenerator();
    boolean server = event instanceof GatherDataEvent.Server;
    boolean client = event instanceof GatherDataEvent.Client;
    PackOutput packOutput = generator.getPackOutput();
    CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
    generator.addProvider(server, new HiltBlockTagProvider(packOutput, lookupProvider));
    generator.addProvider(server, new HiltFluidTagProvider(packOutput, lookupProvider));
    generator.addProvider(server, new HiltMenuTagProvider(packOutput, lookupProvider));
    generator.addProvider(server, new HiltFluidTransferProvider(packOutput));
    generator.addProvider(client, new HiltFluidTooltipProvider(packOutput));
  }

  /**
   * Gets a resource location for Hilt
   * @param name  Name
   * @return  Resource location instance
   */
  public static Identifier getResource(String name) {
    return Identifier.fromNamespaceAndPath(modId, name);
  }

  /**
   * Gets a resource location for the common namespace, which is "forge" for 1.20 and "c" for 1.21.
   * @param name  Name
   * @return  Resource location instance
   */
  public static Identifier commonResource(String name) {
    return Identifier.fromNamespaceAndPath(COMMON, name);
  }

  /**
   * Makes a translation key for the given name
   * @param base  Base name, such as "block" or "gui"
   * @param name  Object name
   * @return  Translation key
   */
  public static String makeDescriptionId(String base, String name) {
    return Util.makeDescriptionId(base, getResource(name));
  }

  /**
   * Makes a translation text component for the given name
   * @param base  Base name, such as "block" or "gui"
   * @param name  Object name
   * @return  Translation key
   */
  public static MutableComponent makeComponent(String base, String name) {
    return Component.translatable(makeDescriptionId(base, name));
  }

  /**
   * Makes a translation text component for the given name
   * @param base  Base name, such as "block" or "gui"
   * @param name  Object name
   * @param args  Additional arguments to format strings
   * @return  Translation key
   */
  public static MutableComponent makeComponent(String base, String name, Object... args) {
    return Component.translatable(makeDescriptionId(base, name), args);
  }
}
