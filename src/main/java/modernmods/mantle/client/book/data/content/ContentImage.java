package modernmods.mantle.client.book.data.content;

import lombok.Getter;
import net.minecraft.resources.Identifier;
import modernmods.mantle.Mantle;
import modernmods.mantle.client.book.data.BookData;
import modernmods.mantle.client.book.data.element.ImageData;
import modernmods.mantle.client.screen.book.BookScreen;
import modernmods.mantle.client.screen.book.element.BookElement;
import modernmods.mantle.client.screen.book.element.ImageElement;
import modernmods.mantle.util.html.HtmlSerializable;

import java.util.ArrayList;

public class ContentImage extends PageContent {
  public static final Identifier ID = Mantle.getResource("image");

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
