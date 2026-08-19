package modernmods.hilt.client.book.action.protocol;

import modernmods.hilt.client.screen.book.BookScreen;

public abstract class ActionProtocol {
  public abstract void processCommand(BookScreen book, String param);
}
