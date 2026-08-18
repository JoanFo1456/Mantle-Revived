package modernmods.mantle.client.book;

import modernmods.mantle.Mantle;
import modernmods.mantle.client.book.data.BookData;
import modernmods.mantle.util.html.HtmlElement;
import modernmods.mantle.util.html.HtmlSerializable;

import javax.annotation.Nullable;

/**
 * Should be implemented by classes that extend {@link modernmods.mantle.client.book.data.content.PageContent}
 * and any {@link modernmods.mantle.client.screen.book.element}'s contained within said class that contains
 * any text that would normally be render in the book page.
 */
public interface IHTML {
  /**
   * Converts content to HTML
   * returns null for content that contains no text
   * @param book reference to the parent BookData
   */
  @Nullable
  default HtmlSerializable toHTML(BookData book) {
    Mantle.logger.warn("{} does not implement IHTML.", this.getClass());
    return HtmlElement.p().add(this.getClass().toString());
  }
}
