package link.sakkit.sakchat.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class DisplayerGui extends Screen {
    private final Component displayerTitle;
    private final Component dynamicContent;
    private MultiLineTextWidget textWidget;

    private int boxX;
    private int boxY;
    private int boxWidth;
    private int boxHeight;
    private int scrollAmount;

    public DisplayerGui(Component title, Component content) {
        super(title);
        this.displayerTitle = title;
        this.dynamicContent = content;
    }

    @Override
    protected void init() {
        this.boxWidth = (int) (this.width * 0.75);
        this.boxWidth = Math.clamp(this.boxWidth, 280, 800); //420

        this.boxHeight = this.height - 64;
        this.boxX = (this.width - this.boxWidth) / 2;
        this.boxY = 24;

        StringWidget titleWidget = new StringWidget(this.boxX, this.boxY + 8, this.boxWidth, 20, this.displayerTitle, this.font);
        titleWidget.setPosition(10, 5);
        this.addRenderableWidget(titleWidget);

        this.textWidget = new MultiLineTextWidget(this.boxX + 12, this.boxY + 32, this.dynamicContent, this.font); //12; 32
        this.textWidget.setMaxWidth(this.boxWidth - 28);

        this.scrollAmount = Math.max(0, this.textWidget.getHeight() - (this.boxHeight - 44));

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(this.width / 2 - 60, this.height - 30, 120, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x90000000);
        guiGraphics.fill(this.boxX, this.boxY, this.boxX + this.boxWidth, this.boxY + this.boxHeight, 0xE50B0B0D);

        guiGraphics.fill(this.boxX, this.boxY, this.boxX + this.boxWidth, this.boxY + 1, 0xFF4A4A4A);
        guiGraphics.fill(this.boxX, this.boxY + this.boxHeight - 1, this.boxX + this.boxWidth, this.boxY + this.boxHeight, 0xFF4A4A4A);
        guiGraphics.fill(this.boxX, this.boxY, this.boxX + 1, this.boxY + this.boxHeight, 0xFF4A4A4A);
        guiGraphics.fill(this.boxX + this.boxWidth - 1, this.boxY, this.boxX + this.boxWidth, this.boxY + this.boxHeight, 0xFF4A4A4A);

        int minY = this.boxY + 30;
        int maxY = this.boxY + this.boxHeight - 12;
        int maxScroll = Math.max(0, this.textWidget.getHeight() - (this.boxHeight - 44));

        if (maxScroll > 0) {
            int trackHeight = maxY - minY;
            int thumbHeight = Math.max(8, (int) ((double) trackHeight * trackHeight / this.textWidget.getHeight()));
            thumbHeight = Math.min(thumbHeight, trackHeight - 4);

            int thumbY = minY + (int) ((double) (trackHeight - thumbHeight) * this.scrollAmount / maxScroll);
            int thumbX = this.boxX + this.boxWidth - 6;

            guiGraphics.fill(thumbX, minY, thumbX + 3, maxY, 0x33FFFFFF);
            guiGraphics.fill(thumbX, thumbY, thumbX + 3, thumbY + thumbHeight, 0x80FFFFFF);
        }

        if (this.textWidget != null) {
            guiGraphics.enableScissor(this.boxX + 4, minY, this.boxX + this.boxWidth - 8, maxY);
            this.textWidget.setY(minY - this.scrollAmount);
            this.textWidget.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.disableScissor();
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.textWidget != null) {
            int maxScroll = Math.max(0, this.textWidget.getHeight() - (this.boxHeight - 44));
            this.scrollAmount = Math.clamp(this.scrollAmount - (int) (scrollY * 16), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        if (this.textWidget != null) {
            int keyCode = InputConstants.getKey(keyEvent).getValue();
            int maxScroll = Math.max(0, this.textWidget.getHeight() - (this.boxHeight - 44));

            if (keyCode == 265) {
                this.scrollAmount = Math.max(0, this.scrollAmount - 16);
                return true;
            } else if (keyCode == 264) {
                this.scrollAmount = Math.min(maxScroll, this.scrollAmount + 16);
                return true;
            }
        }
        return super.keyPressed(keyEvent);
    }

}
