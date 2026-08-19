package modernmods.hilt.client.book.data.element;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import modernmods.hilt.client.book.repository.BookRepository;

public class DataLocation implements IDataElement {

  public String file;
  public transient Identifier location;

  @Override
  public void load(BookRepository source) {
    this.location = "$BLOCK_ATLAS".equals(this.file) ? TextureAtlas.LOCATION_BLOCKS : source.getResourceLocation(this.file, true);
  }
}
