package modernmods.mantle.client.book.data.element;

import modernmods.mantle.client.book.repository.BookRepository;

public interface IDataElement {

  void load(BookRepository source);
}
