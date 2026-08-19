package modernmods.hilt.client.book.data.content;

import lombok.Getter;
import net.minecraft.resources.Identifier;
import modernmods.hilt.Hilt;
import modernmods.hilt.client.book.data.BookData;
import modernmods.hilt.client.book.data.element.ImageData;
import modernmods.hilt.client.screen.book.BookScreen;
import modernmods.hilt.client.screen.book.element.BookElement;
import modernmods.hilt.client.screen.book.element.ImageElement;
import modernmods.hilt.util.html.HtmlSerializable;

import java.util.ArrayList;

public class ContentImage extends PageContent {
  public static final Identifier ID = Hilt.getResource("image");

  @Getter
  public String title = null;
  public ImageData image;

  @Override
  public void build(BookData book, ArrayList<BookElement> list, boolean rightSide) {
    int y = getTitleHeight();

    if (this.title == null || this.title.isEmpty()) {
      y = 0;
    } else {
      this.addTitle(list, this.title);
    }

    if (this.image != null && this.image.location != null) {
      list.add(new ImageElement(0, y, BookScreen.PAGE_WIDTH, BookScreen.PAGE_HEIGHT - y, this.image));
    } else {
      list.add(new ImageElement(ImageData.MISSING));
    }
  }

  @Override
  public HtmlSerializable toHTML(BookData book) {
    return makeTitleHTML();
  }
}
