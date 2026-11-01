package net.valcheff.lessvillagerchatter.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.valcheff.lessvillagerchatter.ChatterConfig;
import net.valcheff.lessvillagerchatter.compat.Screens;

import java.util.List;

import static net.valcheff.lessvillagerchatter.client.SettingsWidgets.FULL_WIDTH;
import static net.valcheff.lessvillagerchatter.client.SettingsWidgets.HALF_WIDTH;
import static net.valcheff.lessvillagerchatter.client.SettingsWidgets.SPACING;

// Minecraft 1.20.6 and newer: the settings in two columns, laid out with HeaderAndFooterLayout.
// Values apply immediately to worlds hosted by this client and are saved on close.
public class ChatterConfigScreen extends Screen {
	private final Screen parent;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

	public ChatterConfigScreen(Screen parent) {
		super(Component.translatable("lessvillagerchatter.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		SettingsWidgets widgets = new SettingsWidgets();
		layout.addTitleHeader(title, font);

		LinearLayout contents = layout.addToContents(LinearLayout.vertical().spacing(6));
		contents.defaultCellSetting().alignHorizontallyCenter();
		for (List<AbstractWidget> cells : widgets.rows()) {
			LinearLayout row = contents.addChild(LinearLayout.horizontal().spacing(SPACING));
			cells.forEach(row::addChild);
		}
		contents.addChild(widgets.ownVolume());
		contents.addChild(new MultiLineTextWidget(Component.translatable("lessvillagerchatter.config.note"), font)
				.setMaxWidth(FULL_WIDTH).setCentered(true));

		LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(SPACING));
		footer.addChild(Button.builder(Component.translatable("lessvillagerchatter.config.reset"), b -> widgets.resetToDefaults())
				.width(HALF_WIDTH).build());
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).width(HALF_WIDTH).build());

		layout.visitWidgets(this::addRenderableWidget);
		repositionElements();
	}

	@Override
	protected void repositionElements() {
		layout.arrangeElements();
	}

	@Override
	public void onClose() {
		ChatterConfig.save();
		Screens.show(minecraft, parent);
	}
}
