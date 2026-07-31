package dev.satherov.epitaphs.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class TextureButton extends Button {
    
    public record Skin(Identifier texture, int u, int v, int textureWidth, int textureHeight) { }
    
    @FunctionalInterface
    public interface Skinner { // hehe funny name
        TextureButton.Skin skin(boolean active, boolean hovered);
    }
    
    private final TextureButton.Skinner skinner;
    
    public TextureButton(int x, int y, int width, int height, TextureButton.Skinner skinner, Button.OnPress onPress, Component message) {
        super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
        this.skinner = skinner;
    }
    
    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        final TextureButton.Skin skin = this.skinner.skin(this.isActive(), this.isHoveredOrFocused());
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                skin.texture(),
                this.getX(),
                this.getY(),
                skin.u(),
                skin.v(),
                this.width,
                this.height,
                skin.textureWidth(),
                skin.textureHeight()
        );
    }
}
