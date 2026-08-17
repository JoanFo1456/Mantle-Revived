package slimeknights.mantle.client.book.data.content;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.data.BookLoadException;
import slimeknights.mantle.client.book.data.element.ImageData;
import slimeknights.mantle.client.book.data.element.IngredientData;
import slimeknights.mantle.client.book.data.element.TextData;
import slimeknights.mantle.client.screen.book.BookScreen;
import slimeknights.mantle.client.screen.book.element.BookElement;
import slimeknights.mantle.client.screen.book.element.ImageElement;
import slimeknights.mantle.client.screen.book.element.ItemElement;
import slimeknights.mantle.client.screen.book.element.TextElement;
import slimeknights.mantle.util.html.HtmlElement;
import slimeknights.mantle.util.html.HtmlGroup;
import slimeknights.mantle.util.html.HtmlSerializable;

import javax.annotation.Nullable;
import java.util.ArrayList;

import static slimeknights.mantle.client.screen.book.Textures.TEX_CRAFTING;

public class ContentCrafting extends PageContent {
  public static final Identifier ID = Mantle.getResource("crafting");

  public static final int TEX_SIZE = 256;
  public static final ImageData IMG_CRAFTING_LARGE = new ImageData(TEX_CRAFTING, 0, 0, 183, 114, TEX_SIZE, TEX_SIZE);
  public static final ImageData IMG_CRAFTING_SMALL = new ImageData(TEX_CRAFTING, 0, 114, 155, 78, TEX_SIZE, TEX_SIZE);

  public static final int X_RESULT_SMALL = 118;
  public static final int Y_RESULT_SMALL = 23;
  public static final int X_RESULT_LARGE = 146;
  public static final int Y_RESULT_LARGE = 41;

  public static final float ITEM_SCALE = 2.0F;
  public static final int SLOT_MARGIN = 5;
  public static final int SLOT_PADDING = 4;

  @Getter
  public String title = "Crafting";
  public String grid_size = "auto";
  public IngredientData[][] grid;
  public IngredientData result;
  @Nullable
  public TextData[] description;
  public String recipe;
  private transient boolean recipeLoaded = false;
  private transient boolean recipeMissingLogged = false;
  private transient boolean recipeWrongTypeLogged = false;
  private transient boolean recipeErrorLogged = false;

  @Override
  public void build(BookData book, ArrayList<BookElement> list, boolean rightSide) {
    try {
      this.loadRecipeFromManager();
    } catch (BookLoadException e) {
      if (!recipeErrorLogged) {
        Mantle.logger.error("Failed to load book crafting recipe {} while building page {}.{}.", this.recipe, this.parent.parent.name, this.parent.name, e);
        recipeErrorLogged = true;
      }
    }

    int x = 0;
    int y;
    int height = 100;
    int resultX = 100;
    int resultY = 50;

    if (this.title == null || this.title.isEmpty()) {
      y = 0;
    } else {
      this.addTitle(list, this.title);
      y = getTitleHeight();
    }

    // Fallback for if grid size is not specified in a manual recipe
    String size = this.grid_size.equalsIgnoreCase("auto") ? "large" : this.grid_size;

    if (size.equalsIgnoreCase("small")) {
      x = BookScreen.PAGE_WIDTH / 2 - IMG_CRAFTING_SMALL.width / 2;
      height = y + IMG_CRAFTING_SMALL.height;
      list.add(new ImageElement(x, y, IMG_CRAFTING_SMALL.width, IMG_CRAFTING_SMALL.height, IMG_CRAFTING_SMALL, book.appearance.slotColor));
      resultX = x + X_RESULT_SMALL;
      resultY = y + Y_RESULT_SMALL;
    } else if (size.equalsIgnoreCase("large")) {
      x = BookScreen.PAGE_WIDTH / 2 - IMG_CRAFTING_LARGE.width / 2;
      height = y + IMG_CRAFTING_LARGE.height;
      list.add(new ImageElement(x, y, IMG_CRAFTING_LARGE.width, IMG_CRAFTING_LARGE.height, IMG_CRAFTING_LARGE, book.appearance.slotColor));
      resultX = x + X_RESULT_LARGE;
      resultY = y + Y_RESULT_LARGE;
    }

    if (this.grid != null) {
      for (int i = 0; i < this.grid.length; i++) {
        for (int j = 0; j < this.grid[i].length; j++) {
          if (this.grid[i][j] == null || this.grid[i][j].getItems().isEmpty()) {
            continue;
          }
          list.add(new ItemElement(x + SLOT_MARGIN + (SLOT_PADDING + Math.round(ItemElement.ITEM_SIZE_HARDCODED * ITEM_SCALE)) * j, y + SLOT_MARGIN + (SLOT_PADDING + Math.round(ItemElement.ITEM_SIZE_HARDCODED * ITEM_SCALE)) * i, ITEM_SCALE, this.grid[i][j].getItems(), this.grid[i][j].action));
        }
      }
    }

    if (this.result != null) {
      list.add(new ItemElement(resultX, resultY, ITEM_SCALE, this.result.getItems(), this.result.action));
    }

    if (this.description != null && this.description.length > 0) {
      list.add(new TextElement(0, height + 5, BookScreen.PAGE_WIDTH, BookScreen.PAGE_HEIGHT - height - 5, this.description));
    }
  }

  @Override
  public void load() {
    super.load();

    this.loadRecipeFromManager();
  }

  /** Loads auto-populated recipe data. Retries during build as books can be initialized before the client recipe manager is ready. */
  private void loadRecipeFromManager() {
    if (recipeLoaded || StringUtils.isEmpty(recipe)) {
      return;
    }
    Identifier recipeId = Identifier.tryParse(recipe);
    if (recipeId == null) {
      return;
    }

    Level level = Minecraft.getInstance().level;
    if (level == null) {
      return;
    }

    // As of 26.1.2 the client no longer syncs full recipes by id (RecipeAccess exposes only property sets); the
    // integrated server's recipe manager is the only place a recipe can be resolved from its id, so book recipe
    // auto-population is available in singleplayer only.
    IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
    if (server == null) {
      if (!recipeMissingLogged) {
        Mantle.logger.warn("Book crafting recipe {} cannot be auto-populated: recipes are only available on the integrated server (singleplayer).", recipeId);
        recipeMissingLogged = true;
      }
      return;
    }

    Recipe<?> foundRecipe = server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, recipeId)).map(RecipeHolder::value).orElse(null);
    if (foundRecipe == null) {
      if (!recipeMissingLogged) {
        Mantle.logger.warn("Book crafting recipe {} was not found in the recipe manager; will retry when the page is opened.", recipeId);
        recipeMissingLogged = true;
      }
      return;
    }
    if (!(foundRecipe instanceof CraftingRecipe craftingRecipe)) {
      if (!recipeWrongTypeLogged) {
        Mantle.logger.warn("Book crafting recipe {} resolved to {}, not a crafting recipe.", recipeId, foundRecipe.getClass().getName());
        recipeWrongTypeLogged = true;
      }
      return;
    }

    // recipes now expose their contents through the display system rather than direct ingredient/result getters
    List<RecipeDisplay> displays = craftingRecipe.display();
    if (displays.isEmpty()) {
      return;
    }
    RecipeDisplay display = displays.get(0);
    ContextMap context = SlotDisplayContext.fromLevel(level);

    // resolve the result
    result = IngredientData.getItemStackData(resolveFirst(display.result(), context));

    if (display instanceof ShapedCraftingRecipeDisplay shaped) {
      int rw = shaped.width();
      int rh = shaped.height();
      if (grid_size.equalsIgnoreCase("auto")) {
        grid_size = (rw <= 2 && rh <= 2) ? "small" : "large";
      }
      int w = gridDimension();
      if (rw > w || rh > w) {
        throw new BookLoadException("Recipe " + this.recipe + " cannot fit in a " + w + "x" + w + " crafting grid");
      }
      List<SlotDisplay> ingredients = shaped.ingredients();
      grid = new IngredientData[rh][rw];
      for (int y = 0; y < rh; y++) {
        for (int x = 0; x < rw; x++) {
          grid[y][x] = IngredientData.getItemStackData(resolveStacks(ingredients.get(x + y * rw), context));
        }
      }
      recipeLoaded = true;
      return;
    }

    if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
      if (grid_size.equalsIgnoreCase("auto")) {
        grid_size = "large";
      }
      int w = gridDimension();
      List<SlotDisplay> ingredients = shapeless.ingredients();
      if (ingredients.size() > w * w) {
        throw new BookLoadException("Recipe " + this.recipe + " cannot fit in a " + w + "x" + w + " crafting grid");
      }
      grid = new IngredientData[w][w];
      for (int i = 0; i < ingredients.size(); i++) {
        grid[i / w][i % w] = IngredientData.getItemStackData(resolveStacks(ingredients.get(i), context));
      }
      recipeLoaded = true;
    }
  }

  /** Gets the crafting grid dimension (2 or 3) from the configured size */
  private int gridDimension() {
    return switch (grid_size.toLowerCase()) {
      case "small" -> 2;
      default -> 3;
    };
  }

  /** Resolves a slot display to a rotating list of matching stacks */
  private static NonNullList<ItemStack> resolveStacks(SlotDisplay slot, ContextMap context) {
    NonNullList<ItemStack> stacks = NonNullList.create();
    stacks.addAll(slot.resolveForStacks(context));
    return stacks;
  }

  /** Resolves the first stack of a slot display, or empty if none is available */
  private static ItemStack resolveFirst(SlotDisplay slot, ContextMap context) {
    List<ItemStack> stacks = slot.resolveForStacks(context);
    return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get(0);
  }

  @Override
  public HtmlSerializable toHTML(BookData book) {
    return HtmlGroup.indent().add(
      makeTitleHTML(),
      HtmlElement.div()
        .classes(grid_size.equalsIgnoreCase("small") ? "spacing" : "spacing-lg")
        .add(TextData.toHtml(description, book))
    );
  }
}
