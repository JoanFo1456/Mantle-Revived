package modernmods.hilt.client.book.data.element;

import modernmods.hilt.client.book.repository.BookRepository;

public interface IDataElement {

  void load(BookRepository source);
}
