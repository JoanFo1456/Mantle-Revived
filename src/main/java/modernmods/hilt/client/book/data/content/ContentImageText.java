package modernmods.hilt.client.book.data.content;

import lombok.Getter;
import net.minecraft.resources.Identifier;
import modernmods.hilt.Hilt;
import modernmods.hilt.client.book.data.BookData;
import modernmods.hilt.client.book.data.element.ImageData;
import modernmods.hilt.client.book.data.element.TextData;
import modernmods.hilt.client.screen.book.BookScreen;
import modernmods.hilt.client.screen.book.element.BookElement;
import modernmods.hilt.client.screen.book.element.ImageElement;
import modernmods.hilt.client.screen.book.element.TextElement;
import modernmods.hilt.util.html.HtmlElement;
import modernmods.hilt.util.html.HtmlGroup;
import modernmods.hilt.util.html.HtmlSerializable;

import java.util.ArrayList;

public class ContentImageText extends PageContent {
  public static final Identifier ID = Hilt.getResource("image_text");

  @Getter
  public String title = null;
  public ImageData image;
  public TextData[] text;
  public boolean centerImage = true;

  @Override
  public void build(BookData book, ArrayList<BookElement> list, boolean rightSide) {
    int y = getTitleHeight();

    if (this.title == null || this.title.isEmpty()) {
      y = 0;
    } else {
      this.addTitle(list, this.title);
    }

    if (this.image != null && this.image.location != null) {
      int x = 0;
      int width = BookScreen.PAGE_WIDTH;
      if (centerImage && this.image.width != -1) {
        x = (BookScreen.PAGE_WIDTH - this.image.width) / 2;
        width = this.image.width;
      }
      ImageElement element = new ImageElement(x, y, width, 100, this.image);
      list.add(element);
      y += element.height + 5;
    } else {
      list.add(new ImageElement(0, y, 32, 32, ImageData.MISSING));
      y += 37;
    }

    if (this.text != null && this.text.length > 0) {
      list.add(new TextElement(0, y, BookScreen.PAGE_WIDTH, BookScreen.PAGE_HEIGHT - y, this.text));
    }
  }

  @Override
  public HtmlSerializable toHTML(BookData book) {
    int h = image.height > 0 ? image.height : 100;
    return HtmlGroup.indent().add(
      makeTitleHTML(),
      HtmlElement.div().classes("column").style("padding-top", h * 2 + 14)
        .add(TextData.toHtml(text, book))
    );
  }
}
