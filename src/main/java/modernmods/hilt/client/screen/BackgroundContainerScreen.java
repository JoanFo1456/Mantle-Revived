package modernmods.hilt.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Generic container screen that simply draws the given background
 * @param <T> Container type
 */
@SuppressWarnings("WeakerAccess")
public class BackgroundContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
	/**
	 * Background drawn for this screen
	 */
	protected final Identifier background;

	// imageHeight is final in AbstractContainerScreen as of 26.1.2; hide it with a mutable field so this screen can size itself
	protected int imageHeight = this.getImageHeight();

	/**
	 * Creates a new screen instance
	 * @param container  Container class
	 * @param inventory  Player inventory
	 * @param name       Container name
	 * @param background Container background
	 */
	public BackgroundContainerScreen(T container, Inventory inventory, Component name, int height, Identifier background) {
		super(container, inventory, name);
		this.background = background;
		this.imageHeight = height;
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	protected void init() {
		super.init();
		this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blit(RenderPipelines.GUI_TEXTURED, this.background, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
	}

	public static class Factory<T extends AbstractContainerMenu> implements ScreenConstructor<T,BackgroundContainerScreen<T>> {
		private final Identifier background;
		private final int height;

		// explicit constructor + static factory: lombok @RequiredArgsConstructor(staticName) was unreliable during the 26.1.2 port
		private Factory(Identifier background, int height) {
			this.background = background;
			this.height = height;
		}

		public static <T extends AbstractContainerMenu> Factory<T> of(Identifier background, int height) {
			return new Factory<>(background, height);
		}

		/**
		 * Creates a factory from the container name
		 * @param height Screen height
		 * @param name   Name of this container
		 */
		public static <T extends AbstractContainerMenu> Factory<T> ofName(int height, Identifier name) {
			return of(Identifier.fromNamespaceAndPath(name.getNamespace(), String.format("textures/gui/%s.png", name.getPath())), height);
		}

    @Override
    public BackgroundContainerScreen<T> create(T menu, Inventory inventory, Component title) {
      return new BackgroundContainerScreen<>(menu, inventory, title, height, background);
    }
	}
}
