package modernmods.hilt.client.book.data.content;

import lombok.Getter;
import net.minecraft.resources.Identifier;
import modernmods.hilt.Hilt;
import modernmods.hilt.client.book.data.BookData;
import modernmods.hilt.client.book.data.element.TextData;
import modernmods.hilt.client.screen.book.BookScreen;
import modernmods.hilt.client.screen.book.element.BookElement;
import modernmods.hilt.client.screen.book.element.TextElement;
import modernmods.hilt.util.html.HtmlGroup;
import modernmods.hilt.util.html.HtmlSerializable;

import java.util.ArrayList;

public class ContentText extends PageContent {
  public static final Identifier ID = Hilt.getResource("text");

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
