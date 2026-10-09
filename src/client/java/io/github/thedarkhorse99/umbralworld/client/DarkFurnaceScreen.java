package io.github.thedarkhorse99.umbralworld.client;

import io.github.thedarkhorse99.umbralworld.menu.DarkFurnaceMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class DarkFurnaceScreen extends AbstractContainerScreen<DarkFurnaceMenu> {
    // PLACEHOLDER ART: vanilla's furnace pictures. Swap these for your own when they're drawn.
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final Identifier DARK_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final Identifier ARROW_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");

    public DarkFurnaceScreen(DarkFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2; // centre the title, like vanilla's furnace
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