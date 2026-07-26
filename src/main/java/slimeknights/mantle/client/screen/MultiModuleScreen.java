package slimeknights.mantle.client.screen;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2fStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import slimeknights.mantle.inventory.MultiModuleContainerMenu;
import slimeknights.mantle.inventory.WrapperSlot;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class MultiModuleScreen<CONTAINER extends MultiModuleContainerMenu<?>> extends AbstractContainerScreen<CONTAINER> {

  protected List<ModuleScreen<?,?>> modules = Lists.newArrayList();

  // imageWidth/imageHeight are final in AbstractContainerScreen as of 26.1.2; hide them with mutable fields so the
  // multi-module layout can still resize itself. Note: the base container render path still reads the final
  // base dimensions, so sizing during super.* calls falls back to the vanilla default (176x166).
  protected int imageWidth = this.getImageWidth();
  protected int imageHeight = this.getImageHeight();

  public int cornerX;
  public int cornerY;
  public int realWidth;
  public int realHeight;

  public MultiModuleScreen(CONTAINER container, Inventory playerInventory, Component title) {
    super(container, playerInventory, title);

    this.realWidth = -1;
    this.realHeight = -1;
//    this.passEvents = true;  // TODO: needed?
  }

  protected void addModule(ModuleScreen<?,?> module) {
    this.modules.add(module);
  }

  public List<Rect2i> getModuleAreas() {
    List<Rect2i> areas = new ArrayList<>(this.modules.size());
    for (ModuleScreen<?,?> module : this.modules) {
      areas.add(module.getArea());
    }
    return areas;
  }

  @Override
  protected void init() {
    if (this.realWidth > -1) {
      // has to be reset before calling initGui so the position is getting retained
      this.imageWidth = this.realWidth;
      this.imageHeight = this.realHeight;
    }

    super.init();

    this.cornerX = this.leftPos;
    this.cornerY = this.topPos;
    this.realWidth = this.imageWidth;
    this.realHeight = this.imageHeight;

    assert this.minecraft != null;
    for (ModuleScreen<?,?> module : this.modules) {
      this.updateSubmodule(module);
    }
    // TODO: this is a small ordering change, does it need another hook?
    for (ModuleScreen<?,?> module : this.modules) {
      // Note: Screen.init(Minecraft, w, h) was removed; minecraft/font are now always the singleton via the base constructor
      module.init(width, height);
      this.updateSubmodule(module);
    }
  }

//  @Override
//  public void init(Minecraft mc, int width, int height) {
//    super.init(mc, width, height);
//
//    for (ModuleScreen<?,?> module : this.modules) {
//      module.init(mc, width, height);
//      this.updateSubmodule(module);
//    }
//  }

  @Override
  public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    super.extractBackground(graphics, mouseX, mouseY, a);
    for (ModuleScreen<?,?> module : this.modules) {
      module.handleDrawGuiContainerBackgroundLayer(graphics, a, mouseX, mouseY);
    }
  }

  @Override
  protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    this.drawContainerName(graphics);
    this.drawPlayerInventoryName(graphics);

    Matrix3x2fStack poses = graphics.pose();
    for (ModuleScreen<?,?> module : this.modules) {
      // set correct state for the module
      poses.pushMatrix();
      poses.translate(module.getGuiLeft() - this.leftPos, module.getGuiTop() - this.topPos);
      module.handleDrawGuiContainerForegroundLayer(graphics, mouseX, mouseY);
      poses.popMatrix();
    }
  }

  @Override
  protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    super.extractTooltip(graphics, mouseX, mouseY);

    for (ModuleScreen<?,?> module : this.modules) {
      module.handleRenderHoveredTooltip(graphics, mouseX, mouseY);
    }
  }

  protected void drawBackground(GuiGraphicsExtractor graphics, Identifier background) {
    graphics.blit(RenderPipelines.GUI_TEXTURED, background, this.cornerX, this.cornerY, 0, 0, this.realWidth, this.realHeight, 256, 256);
  }

  protected void drawContainerName(GuiGraphicsExtractor graphics) {
    graphics.text(this.font, this.getTitle().getVisualOrderText(), 8, 6, 0x404040, false);
  }

  protected void drawPlayerInventoryName(GuiGraphicsExtractor graphics) {
    assert Minecraft.getInstance().player != null;
    Component localizedName = Minecraft.getInstance().player.getInventory().getDisplayName();
    graphics.text(this.font, localizedName.getVisualOrderText(), 8, this.imageHeight - 96 + 2, 0x404040, false);
  }

  @Override
  public void resize(int width, int height) {
    super.resize(width, height);

    for (ModuleScreen<?,?> module : this.modules) {
      module.resize(width, height);
      this.updateSubmodule(module);
    }
  }

  @Override
  public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
    int oldX = this.leftPos;
    int oldY = this.topPos;
    int oldW = this.imageWidth;
    int oldH = this.imageHeight;

    this.leftPos = this.cornerX;
    this.topPos = this.cornerY;
    this.imageWidth = this.realWidth;
    this.imageHeight = this.realHeight;
    super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    this.leftPos = oldX;
    this.topPos = oldY;
    this.imageWidth = oldW;
    this.imageHeight = oldH;
  }

  // needed to get the correct slot on clicking
  @Override
  protected boolean isHovering(int left, int top, int right, int bottom, double pointX, double pointY) {
    pointX -= this.cornerX;
    pointY -= this.cornerY;
    return pointX >= left - 1 && pointX < left + right + 1 && pointY >= top - 1 && pointY < top + bottom + 1;
  }

  protected void updateSubmodule(ModuleScreen<?,?> module) {
    module.updatePosition(this.cornerX, this.cornerY, this.realWidth, this.realHeight);

    if (module.getGuiLeft() < this.leftPos) {
      this.imageWidth += this.leftPos - module.getGuiLeft();
      this.leftPos = module.getGuiLeft();
    }

    if (module.getGuiTop() < this.topPos) {
      this.imageHeight += this.topPos - module.getGuiTop();
      this.topPos = module.getGuiTop();
    }

    if (module.guiRight() > this.leftPos + this.imageWidth) {
      this.imageWidth = module.guiRight() - this.leftPos;
    }

    if (module.guiBottom() > this.topPos + this.imageHeight) {
      this.imageHeight = module.guiBottom() - this.topPos;
    }
  }

  @Override
  public void extractSlot(GuiGraphicsExtractor graphics, Slot slotIn, int mouseX, int mouseY) {
    ModuleScreen<?,?> module = this.getModuleForSlot(slotIn.index);

    if (slotIn instanceof WrapperSlot wrapper) {
      wrapper.syncPosition();
    }

    if (module != null) {
      Slot slot = slotIn;
      // unwrap for the call to the module
      if (slotIn instanceof WrapperSlot) {
        slot = ((WrapperSlot) slotIn).parent;
      }

      if (!module.shouldDrawSlot(slot)) {
        return;
      }
    }

    super.extractSlot(graphics, slotIn, mouseX, mouseY);
  }

  public boolean isSlotHovered(Slot slotIn, double mouseX, double mouseY) {
    ModuleScreen<?,?> module = this.getModuleForSlot(slotIn.index);

    if (slotIn instanceof WrapperSlot wrapper) {
      wrapper.syncPosition();
    }

    // mouse inside the module of the slot?
    if (module != null) {
      Slot slot = slotIn;
      // unwrap for the call to the module
      if (slotIn instanceof WrapperSlot) {
        slot = ((WrapperSlot) slotIn).parent;
      }

      if (!module.shouldDrawSlot(slot)) {
        return false;
      }
    }

    return this.isHovering(slotIn.x, slotIn.y, 16, 16, mouseX, mouseY);
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    ModuleScreen<?,?> module = this.getModuleForPoint(event.x(), event.y());

    if (module != null) {
      if (module.handleMouseClicked(event.x(), event.y(), event.button())) {
        return false;
      }
    }

    return super.mouseClicked(event, doubleClick);
  }

  @Override
  public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
    ModuleScreen<?,?> module = this.getModuleForPoint(event.x(), event.y());

    if (module != null) {
      // Note: timeSinceLastClick is no longer provided by the drag event; passing 0
      if (module.handleMouseClickMove(event.x(), event.y(), event.button(), 0)) {
        return false;
      }
    }

    return super.mouseDragged(event, dragX, dragY);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    ModuleScreen<?,?> module = this.getModuleForPoint(mouseX, mouseY);

    if (module != null) {
      if (module.handleMouseScrolled(mouseX, mouseY, scrollY)) {
        return false;
      }
    }

    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
  }

  public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
    return this.mouseScrolled(mouseX, mouseY, 0, scrollY);
  }

  public boolean isHovering(Slot slotIn, double mouseX, double mouseY) {
    return this.isHovering(slotIn.x, slotIn.y, 16, 16, mouseX, mouseY);
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent event) {
    ModuleScreen<?,?> module = this.getModuleForPoint(event.x(), event.y());

    if (module != null) {
      if (module.handleMouseReleased(event.x(), event.y(), event.button())) {
        return false;
      }
    }

    return super.mouseReleased(event);
  }

  @Nullable
  protected ModuleScreen<?,?> getModuleForPoint(double x, double y) {
    for (ModuleScreen<?,?> module : this.modules) {
      if (this.isHovering(module.getGuiLeft(), module.getGuiTop(), module.guiRight(), module.guiBottom(), x + this.cornerX, y + this.cornerY)) {
        return module;
      }
    }

    return null;
  }

  @Nullable
  protected ModuleScreen<?,?> getModuleForSlot(int slotNumber) {
    return this.getModuleForContainer(this.getMenu().getSlotContainer(slotNumber));
  }

  @Nullable
  protected ModuleScreen<?,?> getModuleForContainer(AbstractContainerMenu container) {
    for (ModuleScreen<?,?> module : this.modules) {
      if (module.getMenu() == container) {
        return module;
      }
    }

    return null;
  }

  @Override
  public CONTAINER getMenu() {
    return this.menu;
  }
}
