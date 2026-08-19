package modernmods.hilt.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import modernmods.hilt.command.argument.TagSourceArgument;
import modernmods.hilt.command.tags.ModifyTagCommand;

import java.util.function.Consumer;

/**
 * Root command for all commands in hilt
 */
public class HiltCommand {
  /** Permission level that allows a user to build in spawn protected areas */
  public static final PermissionCheck PERMISSION_EDIT_SPAWN = Commands.LEVEL_MODERATORS;
  /** Permission level that can run standard game commands, used by command blocks and functions */
  public static final PermissionCheck PERMISSION_GAME_COMMANDS = Commands.LEVEL_GAMEMASTERS;
  /** Standard permission level for server operators */
  public static final PermissionCheck PERMISSION_PLAYER_COMMANDS = Commands.LEVEL_ADMINS;
  /** Permission level for the server owner, server console, or the player in single player */
  public static final PermissionCheck PERMISSION_OWNER = Commands.LEVEL_OWNERS;

  /** Checks if the given source meets the given permission check */
  public static boolean hasPermission(CommandSourceStack source, PermissionCheck check) {
    return check.check(source.permissions());
  }

  /** @deprecated use {@link RegistryArgument#TAG} or {@link TagSourceArgument#TAG} */
  @Deprecated(forRemoval = true)
  public static SuggestionProvider<CommandSourceStack> VALID_TAGS;
  /** @deprecated use {@link RegistryArgument#VALUE} or {@link TagSourceArgument#VALUE} */
  @Deprecated(forRemoval = true)
  public static SuggestionProvider<CommandSourceStack> REGISTRY_VALUES;
  /** @deprecated use {@link RegistryArgument#REGISTRY} or {@link TagSourceArgument#SOURCE} */
  @Deprecated(forRemoval = true)
  public static SuggestionProvider<CommandSourceStack> REGISTRY;

  /** Registers all Hilt command related content */
  public static void init() {
    RegistryArgument.registerSuggestions();
    VALID_TAGS = RegistryArgument.TAG;
    REGISTRY_VALUES = RegistryArgument.VALUE;
    REGISTRY = RegistryArgument.REGISTRY;
    TagSourceArgument.registerSuggestions();

    // register interesting sources
    SourcesCommand.register(Registries.elementsDirPath(Registries.LOOT_TABLE), (context, builder)
      -> SharedSuggestionProvider.suggestResource(context.getSource().getServer().reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE).listElementIds().map(ResourceKey::identifier), builder));
    SourcesCommand.register("recipes", (context, builder)
      -> SharedSuggestionProvider.suggestResource(context.getSource().getServer().getRecipeManager().getRecipes().stream().map(holder -> holder.id().identifier()), builder));

    // add command listener
    NeoForge.EVENT_BUS.addListener(HiltCommand::registerCommand);
  }

  /** Registers a sub command for the root Hilt command */
  private static void register(LiteralArgumentBuilder<CommandSourceStack> root, String name, Consumer<LiteralArgumentBuilder<CommandSourceStack>> consumer) {
    LiteralArgumentBuilder<CommandSourceStack> subCommand = Commands.literal(name);
    consumer.accept(subCommand);
    root.then(subCommand);
  }

  /** Event listener to register the Hilt command */
  private static void registerCommand(RegisterCommandsEvent event) {
    LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("hilt");
    CommandBuildContext context = event.getBuildContext();

    // sub commands
    register(builder, "tags", b -> {
      register(b, "view", ViewTagCommand::register);
      register(b, "entries", DumpTagCommand::register);
      register(b, "dump", DumpAllTagsCommand::register);
      register(b, "for", TagsForCommand::register);
      register(b, "preference", TagPreferenceCommand::register);
      ModifyTagCommand.register(b);
    });
    register(builder, "dump_loot_modifiers", DumpLootModifiers::register);
    register(builder, "harvest_tiers", HarvestTiersCommand::register);
    register(builder, "remove", b -> {
      b = b.requires(sender -> hasPermission(sender, HiltCommand.PERMISSION_GAME_COMMANDS));
      register(b, "recipes", b2 -> RemoveRecipesCommand.register(b2, context));
      RemoveDataCommand.register(b);
    });
    // sources assets is registered as a client command
    register(builder, "sources", b -> {
      register(b, "data", SourcesCommand::register);
    });
    register(builder, "hunger", HungerCommand::register);

    // register final command
    event.getDispatcher().register(builder);
  }

  /* Helpers */

  /**
   * Returns true if the source either does not have reduced debug info or they have the proper level
   * Allows limiting a command that prints debug info to not work in reduced debug info
   * @param source             Command source
   * @param reducedDebugLevel  Level to use when reduced debug info is true
   * @return  True if the command can be run
   */
  public static boolean requiresDebugInfoOrOp(CommandSourceStack source, PermissionCheck reducedDebugLevel) {
    return !source.getLevel().getGameRules().get(GameRules.REDUCED_DEBUG_INFO) || hasPermission(source, reducedDebugLevel);
  }
}
