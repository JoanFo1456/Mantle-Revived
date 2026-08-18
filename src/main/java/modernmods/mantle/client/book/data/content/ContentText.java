package modernmods.mantle.client.book.data.content;

import lombok.Getter;
import net.minecraft.resources.Identifier;
import modernmods.mantle.Mantle;
import modernmods.mantle.client.book.data.BookData;
import modernmods.mantle.client.book.data.element.TextData;
import modernmods.mantle.client.screen.book.BookScreen;
import modernmods.mantle.client.screen.book.element.BookElement;
import modernmods.mantle.client.screen.book.element.TextElement;
import modernmods.mantle.util.html.HtmlGroup;
import modernmods.mantle.util.html.HtmlSerializable;

import java.util.ArrayList;

public class ContentText extends PageContent {
  public static final Identifier ID = Mantle.getResource("text");

  @Getter
  public String title = null;
  public TextData[] text;

  @Override
  public void build(BookData book, ArrayList<BookElement> list, boolean rightSide) {
    int y;
    if (this.title == null || this.title.isEmpty()) {
      y = 0;
    } else {
      this.addTitle(list, this.title);
      y = getTitleHeight();
    }
    if (this.text != null && this.text.length > 0) {
      list.add(new TextElement(0, y, BookScreen.PAGE_WIDTH, BookScreen.PAGE_HEIGHT - y, this.text));
    }
  }

  @Override
  public HtmlSerializable toHTML(BookData book) {
    return HtmlGroup.indent().add(
      makeTitleHTML(),
      TextData.toHtml(text, book)
    );
  }
}
