package io.github.thedarkhorse99.umbralworld.client;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import io.github.thedarkhorse99.umbralworld.menu.DarkFurnaceMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class DarkFurnaceScreen extends AbstractContainerScreen<DarkFurnaceMenu> {
    // Mac's art: the background, and two sprites in textures/gui/sprites/container/dark_furnace/
    private static final Identifier TEXTURE = UmbralWorld.id("textures/gui/container/dark_furnace.png");
    private static final Identifier DARK_SPRITE = UmbralWorld.id("container/dark_furnace/darkness_progress");
    private static final Identifier ARROW_SPRITE = UmbralWorld.id("container/dark_furnace/burn_progress");
    /** Light grey labels: vanilla's dark grey would barely show on the dark background. */
    private static final int LABEL_COLOR = 0xFFB0B0B0;

    public DarkFurnaceScreen(DarkFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2; // centre the title, like vanilla's furnace
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, LABEL_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL_COLOR, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0f, 0.0f, imageWidth, imageHeight, 256, 256);

        // Darkness icon (where vanilla's flame goes): shown whole while it's dark enough to cook.
        if (menu.isDark()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DARK_SPRITE, x + 56, y + 36, 14, 14);
        }

        // Progress arrow: grows left to right as the item cooks.
        int arrowWidth = Mth.ceil(menu.getCookProgress() * 24.0f);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW_SPRITE, 24, 16, 0, 0, x + 79, y + 34, arrowWidth, 16);
    }
}