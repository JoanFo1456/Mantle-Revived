package modernmods.mantle.client.book.data.content;

import lombok.Getter;
import net.minecraft.resources.Identifier;
import modernmods.mantle.Mantle;
import modernmods.mantle.client.book.data.BookData;
import modernmods.mantle.client.book.data.element.IngredientData;
import modernmods.mantle.client.book.data.element.TextData;
import modernmods.mantle.client.screen.book.BookScreen;
import modernmods.mantle.client.screen.book.element.BookElement;
import modernmods.mantle.client.screen.book.element.ItemElement;
import modernmods.mantle.client.screen.book.element.TextElement;
import modernmods.mantle.util.html.HtmlElement;
import modernmods.mantle.util.html.HtmlGroup;
import modernmods.mantle.util.html.HtmlSerializable;

import java.util.ArrayList;

/** Page that showcases an item with text below */
public class ContentShowcase extends PageContent {
  public static final transient Identifier ID = Mantle.getResource("showcase");

  /** Title of the page */
  @Getter
  public String title = null;
  /** Text to display below the item */
  public TextData[] text;
  /** Item to display */
  public IngredientData item;

  @Override
  public void build(BookData book, ArrayList<BookElement> list, boolean rightSide) {
    int y = getTitleHeight();

    if (this.title == null || this.title.isEmpty()) {
      y = 0;
    } else {
      this.addTitle(list, this.title);
    }

    if (this.item != null && !this.item.getItems().isEmpty()) {
      ItemElement element = new ItemElement(BookScreen.PAGE_WIDTH / 2 - 15, y, 2.5f, this.item.getItems(), this.item.action);
      list.add(element);
      y += element.height;
    }

    if (this.text != null && this.text.length > 0) {
      list.add(new TextElement(0, y, BookScreen.PAGE_WIDTH, BookScreen.PAGE_HEIGHT - y, this.text));
    }
  }

  @Override
  public HtmlSerializable toHTML(BookData book) {
    return HtmlGroup.indent().add(
      makeTitleHTML(),
      HtmlElement.div().classes("column").style("padding-top", 80)
        .add(TextData.toHtml(text, book))
    );
  }
}
