package modernmods.mantle.client.screen.book;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import modernmods.mantle.client.screen.book.element.BookElement;

public interface ILayerRenderFunction {
  void draw(BookElement element, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, Font fontRenderer);
}
