package slimeknights.mantle.command.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import org.apache.commons.lang3.text.WordUtils;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.screen.book.BookScreen;
import slimeknights.mantle.command.GeneratePackHelper;
import slimeknights.mantle.command.MantleCommand;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/** A command for different book operations, currently open and export_images */
public class BookCommand {
  private static final String BOOK_NOT_FOUND = "command.mantle.book_test.not_found";

  private static final String EXPORT_SUCCESS = "command.mantle.book.export.success";
  private static final String EXPORT_SUCCESS_HTML = "command.mantle.book.export.html.success";
  private static final String EXPORT_FAIL = "command.mantle.book.export.error_generic";
  private static final String EXPORT_FAIL_IO = "command.mantle.book.export.error_io";

  private static final String DEFAULT_BOOK_VERSION = "20";
  private static final String VERSION_FULL = "1.20";
  private static final int DEFAULT_SCALE = 2;

  /**
   * Registers this sub command with the root command
   * @param subCommand  Command builder
   */
  public static void register(LiteralArgumentBuilder<CommandSourceStack> subCommand) {
    subCommand.requires(source -> source.hasPermission(MantleCommand.PERMISSION_GAME_COMMANDS) && source.getEntity() instanceof AbstractClientPlayer)
      .then(Commands.literal("open")
        .then(Commands.argument("id", ResourceLocationArgument.id()).suggests(MantleClientCommand.REGISTERED_BOOKS)
          .executes(BookCommand::openBook)))

      .then(Commands.literal("export_images")
        // mantle book export_images <domain> [version]
        .then(Commands.argument("domain", StringArgumentType.word()).suggests(MantleClientCommand.REGISTERED_BOOK_DOMAINS)
          .then(Commands.argument("scale", IntegerArgumentType.integer(1, 16))
            .executes(context -> exportDomainImages(context, IntegerArgumentType.getInteger(context, "scale"))))
          .executes(context -> exportDomainImages(context, DEFAULT_SCALE)))
        // mantle book export_images <domain> [version]
        .then(Commands.argument("id", ResourceLocationArgument.id()).suggests(MantleClientCommand.REGISTERED_BOOKS)
          .then(Commands.argument("scale", IntegerArgumentType.integer(1, 16))
            .executes(context -> exportImages(context, IntegerArgumentType.getInteger(context, "scale"))))
          .executes(context -> exportImages(context, DEFAULT_SCALE))))

      .then(Commands.literal("export_html")
        // mantle book export_html <domain> [version]
        .then(Commands.argument("domain", StringArgumentType.word()).suggests(MantleClientCommand.REGISTERED_BOOK_DOMAINS)
          .then(Commands.argument("version", StringArgumentType.word())
            .executes(context -> exportDomainHtml(context, StringArgumentType.getString(context, "version"))))
          .executes(context -> exportDomainHtml(context, DEFAULT_BOOK_VERSION)))
        // mantle book export_html <id> [version]
        .then(Commands.argument("id", ResourceLocationArgument.id()).suggests(MantleClientCommand.REGISTERED_BOOKS)
          .then(Commands.argument("version", StringArgumentType.word())
            .executes(context -> exportHTML(context, StringArgumentType.getString(context, "version"))))
          .executes(context -> exportHTML(context, DEFAULT_BOOK_VERSION))));
  }

  /**
   * Opens the specified book
   * @param context  Command context
   * @return  Integer return
   */
  private static int openBook(CommandContext<CommandSourceStack> context) {
    Identifier book = ResourceLocationArgument.getId(context, "id");

    BookData bookData = BookLoader.getBook(book);
    if(bookData != null) {
      // Delay execution to ensure chat window is closed
      Minecraft.getInstance().tell(() ->
        bookData.openGui(Component.literal("Book"), "", null, null)
      );
    } else {
      bookNotFound(book);
      return 1;
    }

    return 0;
  }

  /**
   * Renders all images in the book to files
   * @param context  Command context
   * @return  Integer return
   */
  private static int exportImages(CommandContext<CommandSourceStack> context, int scale) throws CommandSyntaxException {
    Identifier book = ResourceLocationArgument.getId(context, "id");
    return doExport(book, scale, false, DEFAULT_BOOK_VERSION);
  }

  /**
   * Renders all images in the books in the given domain to files
   * @param context  Command context
   * @return  Integer return
   */
  private static int exportDomainImages(CommandContext<CommandSourceStack> context, int scale) throws CommandSyntaxException {
    String domain = StringArgumentType.getString(context, "domain");
    for (Identifier book : BookLoader.getAllBooks()) {
      if (domain.equals(book.getNamespace())) {
        int code = doExport(book, scale, false, DEFAULT_BOOK_VERSION);
        if (code != 0) return code;
      }
    }
    return 0;
  }

  /**
   * Exports all pages in the book to HTML and png
   * @param context Command context
   * @return Integer return
   */
  private static int exportHTML(CommandContext<CommandSourceStack> context, String version) throws CommandSyntaxException {
    Identifier book = ResourceLocationArgument.getId(context, "id");
    return doExport(book, 2, true, version);
  }

  /**
   * Exports all pages in all books to HTML and png
   * @param context Command context
   * @return Integer return
   */
  private static int exportDomainHtml(CommandContext<CommandSourceStack> context, String version) throws CommandSyntaxException {
    String domain = StringArgumentType.getString(context, "domain");
    for (Identifier book : BookLoader.getAllBooks()) {
      if (domain.equals(book.getNamespace())) {
        int code = doExport(book, 2, true, version);
        if (code != 0) return code;
      }
    }
    return 0;
  }

  /**
   * Renders all images in the book to files
   * @param book  Book to export
   * @param scale  Scale to export at
   * @param html  Include HTML
   * @param version  version in each files header
   * @return  Integer return
   */
  private static int doExport(Identifier book, int scale, boolean html, String version) throws CommandSyntaxException {
    // TODO 26.1.2 BLOCKER: offscreen book export used the removed immediate-mode GUI pipeline
    // (new GuiGraphics(mc, buffer), gui.flush(), RenderSystem.getModelViewStack/applyModelViewMatrix/
    // setProjectionMatrix(matrix, VertexSorting), TextureTarget/RenderTarget). Needs a rewrite onto the
    // new deferred GuiRenderer/GuiRenderState submission flow. Disabled until ported.
    throw commandException(Component.translatable(EXPORT_FAIL));
  }

  /** Creates a command failure from a localized component. */
  private static CommandSyntaxException commandException(Component message) {
    return new SimpleCommandExceptionType(message).create();
  }


  /** Creates the gallery HTML page */
  private static String galleryHtml(String bookName, String title, String mod) {
    return "---\n" +
      "layout: book-gallery\n" +
      "title: " + title + " (" + VERSION_FULL + ") - Gallery" + '\n' +
      "breadcrumb: Gallery\n" +
      "description: Gallery of all pages for " + title + " from " + mod + " in Minecraft " + VERSION_FULL + ".\n" +
      "book: " + bookName + '\n' +
      "link_prefix: ../\n" +
      "link_suffix: /gallery\n" +
      "---\n\n";
  }

  /** Send a message to the player linking the directory */
  private static void sendFileMessage(Path screenshotDir, @Nullable Path htmlDir) {
    Player player = Minecraft.getInstance().player;
    if (player != null) {
      Component fileComponent = GeneratePackHelper.getOutputComponent(screenshotDir);
      if (htmlDir != null) {
        player.displayClientMessage(Component.translatable(EXPORT_SUCCESS_HTML, fileComponent, GeneratePackHelper.getOutputComponent(htmlDir)), false);
      } else {
        player.displayClientMessage(Component.translatable(EXPORT_SUCCESS, fileComponent), false);
      }
    }
  }


  public static void bookNotFound(Identifier book) {
    Player player = Minecraft.getInstance().player;
    if (player != null) {
      player.displayClientMessage(Component.translatable(BOOK_NOT_FOUND, book).withStyle(ChatFormatting.RED), false);
    }
  }
}
