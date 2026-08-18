package modernmods.mantle.client.book.action.protocol;

import modernmods.mantle.client.screen.book.BookScreen;

public abstract class ActionProtocol {
  public abstract void processCommand(BookScreen book, String param);
}
