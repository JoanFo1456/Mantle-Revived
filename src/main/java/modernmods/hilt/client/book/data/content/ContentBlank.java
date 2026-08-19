package modernmods.hilt.client.book.data.content;

import net.minecraft.resources.Identifier;
import modernmods.hilt.Hilt;
import modernmods.hilt.client.book.data.BookData;
import modernmods.hilt.client.screen.book.element.BookElement;
import modernmods.hilt.util.html.HtmlGroup;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class ContentBlank extends PageContent {
  public static final Identifier ID = Hilt.getResource("blank");

  @Override
  public void build(BookData book, ArrayList<BookElement> list, boolean rightSide) {
  }

  @Nullable
  @Override
  public HtmlGroup toHTML(BookData book) {
    return null;
  }
}
