package net.valcheff.lessvillagerchatter.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.valcheff.lessvillagerchatter.ChatterConfig;
import net.valcheff.lessvillagerchatter.compat.Screens;

import java.util.List;

import static net.valcheff.lessvillagerchatter.client.SettingsWidgets.FULL_WIDTH;
import static net.valcheff.lessvillagerchatter.client.SettingsWidgets.HALF_WIDTH;
import static net.valcheff.lessvillagerchatter.client.SettingsWidgets.SPACING;

// Minecraft 1.20.1: same two columns as the newer screen, placed by hand. This version has no
// LinearLayout.vertical() or addTitleHeader, and screens draw their own background and title.
public class ChatterConfigScreen extends Screen {
	private static final int ROW_HEIGHT = 26;

	private final Screen parent;

	public ChatterConfigScreen(Screen parent) {
		super(Component.translatable("lessvillagerchatter.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		SettingsWidgets widgets = new SettingsWidgets();
		int left = width / 2 - FULL_WIDTH / 2;
		int y = 40;
		for (List<AbstractWidget> cells : widgets.rows()) {
			int x = left;
			for (AbstractWidget cell : cells) {
				cell.setPosition(x, y);
				addRenderableWidget(cell);
				x += HALF_WIDTH + SPACING;
			}
			y += ROW_HEIGHT;
		}
		AbstractWidget ownVolume = widgets.ownVolume();
		ownVolume.setPosition(left, y);
		addRenderableWidget(ownVolume);
		y += ROW_HEIGHT;

		MultiLineTextWidget note = new MultiLineTextWidget(left, y, Component.translatable("lessvillagerchatter.config.note"), font)
				.setMaxWidth(FULL_WIDTH).setCentered(true);
		note.setX(width / 2 - note.getWidth() / 2);
		addRenderableWidget(note);

		int footerY = height - 28;
		addRenderableWidget(Button.builder(Component.translatable("lessvillagerchatter.config.reset"), b -> widgets.resetToDefaults())
				.bounds(left, footerY, HALF_WIDTH, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
				.bounds(left + HALF_WIDTH + SPACING, footerY, HALF_WIDTH, 20).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
	}

	@Override
	public void onClose() {
		ChatterConfig.save();
		Screens.show(minecraft, parent);
	}
}
