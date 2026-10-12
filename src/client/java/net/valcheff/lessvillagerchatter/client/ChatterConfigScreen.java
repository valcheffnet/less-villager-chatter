package net.valcheff.lessvillagerchatter.client;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.valcheff.lessvillagerchatter.ChatterConfig;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

// All settings in two columns. Values apply immediately to worlds hosted by this client and are saved on close.
public class ChatterConfigScreen extends Screen {
	private static final int MIN_SECONDS = 6;   // vanilla
	private static final int MAX_SECONDS = 60;
	private static final int VARIATION_STEP = 5;
	private static final int VOLUME_STEP = 5;
	private static final int HALF_WIDTH = 150;
	private static final int SPACING = 8;
	private static final int FULL_WIDTH = HALF_WIDTH * 2 + SPACING;

	private final Screen parent;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
	private IntSlider villagerSlider;
	private IntSlider traderSlider;
	private IntSlider villagerVolumeSlider;
	private IntSlider traderVolumeSlider;
	private IntSlider variationSlider;
	private CycleButton<Boolean> randomStartButton;
	private CycleButton<Boolean> ownVolumeButton;

	public ChatterConfigScreen(Screen parent) {
		super(Component.translatable("lessvillagerchatter.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		layout.addTitleHeader(title, font);

		LinearLayout contents = layout.addToContents(LinearLayout.vertical().spacing(6));
		contents.defaultCellSetting().alignHorizontallyCenter();

		villagerSlider = secondsSlider("lessvillagerchatter.config.villagers",
				ChatterConfig.villagerInterval, v -> ChatterConfig.villagerInterval = v);
		traderSlider = secondsSlider("lessvillagerchatter.config.traders",
				ChatterConfig.traderInterval, v -> ChatterConfig.traderInterval = v);
		row(contents, villagerSlider, traderSlider, "lessvillagerchatter.config.interval.tooltip");

		villagerVolumeSlider = volumeSlider("lessvillagerchatter.config.villager_volume",
				ChatterConfig.villagerVolume, v -> ChatterConfig.villagerVolume = v);
		traderVolumeSlider = volumeSlider("lessvillagerchatter.config.trader_volume",
				ChatterConfig.traderVolume, v -> ChatterConfig.traderVolume = v);
		row(contents, villagerVolumeSlider, traderVolumeSlider, "lessvillagerchatter.config.volume.tooltip");

		variationSlider = new IntSlider(HALF_WIDTH, 0, ChatterConfig.MAX_VARIATION, VARIATION_STEP, ChatterConfig.variation,
				p -> Component.translatable("lessvillagerchatter.config.variation", p),
				p -> ChatterConfig.variation = p);
		randomStartButton = toggle(HALF_WIDTH, "lessvillagerchatter.config.random_start",
				ChatterConfig.randomStart, v -> ChatterConfig.randomStart = v);
		row(contents, variationSlider, randomStartButton, null);
		variationSlider.setTooltip(tooltip("lessvillagerchatter.config.variation.tooltip"));
		randomStartButton.setTooltip(tooltip("lessvillagerchatter.config.random_start.tooltip"));

		ownVolumeButton = contents.addChild(toggle(FULL_WIDTH, "lessvillagerchatter.config.own_volume",
				ChatterConfig.useOwnVolume, v -> ChatterConfig.useOwnVolume = v));
		ownVolumeButton.setTooltip(tooltip("lessvillagerchatter.config.own_volume.tooltip"));

		contents.addChild(new MultiLineTextWidget(Component.translatable("lessvillagerchatter.config.note"), font)
				.setMaxWidth(FULL_WIDTH).setCentered(true));

		LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(SPACING));
		footer.addChild(Button.builder(Component.translatable("lessvillagerchatter.config.reset"), b -> resetToDefaults())
				.width(HALF_WIDTH).build());
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).width(HALF_WIDTH).build());

		layout.visitWidgets(this::addRenderableWidget);
		repositionElements();
	}

	private void resetToDefaults() {
		villagerSlider.set(ChatterConfig.averageSeconds(ChatterConfig.DEFAULT_VILLAGER));
		traderSlider.set(ChatterConfig.averageSeconds(ChatterConfig.DEFAULT_TRADER));
		villagerVolumeSlider.set(ChatterConfig.DEFAULT_VOLUME);
		traderVolumeSlider.set(ChatterConfig.DEFAULT_VOLUME);
		variationSlider.set(ChatterConfig.DEFAULT_VARIATION);
		randomStartButton.setValue(ChatterConfig.DEFAULT_RANDOM_START);
		ChatterConfig.randomStart = ChatterConfig.DEFAULT_RANDOM_START;
		ownVolumeButton.setValue(false);
		ChatterConfig.useOwnVolume = false;
	}

	@Override
	protected void repositionElements() {
		layout.arrangeElements();
	}

	@Override
	public void onClose() {
		ChatterConfig.save();
		minecraft.gui.setScreen(parent);
	}

	private static void row(LinearLayout contents, AbstractWidget left, AbstractWidget right, String tooltipKey) {
		LinearLayout row = contents.addChild(LinearLayout.horizontal().spacing(SPACING));
		row.addChild(left);
		row.addChild(right);
		if (tooltipKey != null) {
			left.setTooltip(tooltip(tooltipKey));
			right.setTooltip(tooltip(tooltipKey));
		}
	}

	private static Tooltip tooltip(String key) {
		return Tooltip.create(Component.translatable(key));
	}

	private static CycleButton<Boolean> toggle(int width, String key, boolean value, Consumer<Boolean> apply) {
		return CycleButton.onOffBuilder(value)
				.create(0, 0, width, 20, Component.translatable(key), (button, v) -> apply.accept(v));
	}

	/** Slider in average seconds between sounds, stored as an interval in ticks. */
	private static IntSlider secondsSlider(String key, int interval, IntConsumer apply) {
		return new IntSlider(HALF_WIDTH, MIN_SECONDS, MAX_SECONDS, 1, ChatterConfig.averageSeconds(interval),
				s -> Component.translatable(key, s == MIN_SECONDS
						? Component.translatable("lessvillagerchatter.config.vanilla", s)
						: Component.translatable("lessvillagerchatter.config.seconds", s)),
				s -> apply.accept(ChatterConfig.intervalForSeconds(s)));
	}

	private static IntSlider volumeSlider(String key, int percent, IntConsumer apply) {
		return new IntSlider(HALF_WIDTH, 0, 100, VOLUME_STEP, percent,
				p -> Component.translatable(key, p == 0 ? CommonComponents.OPTION_OFF : Component.literal(p + "%")),
				apply);
	}

	private static class IntSlider extends AbstractSliderButton {
		private final int min;
		private final int max;
		private final int step;
		private final IntFunction<Component> label;
		private final IntConsumer apply;

		IntSlider(int width, int min, int max, int step, int current, IntFunction<Component> label, IntConsumer apply) {
			super(0, 0, width, 20, Component.empty(), 0);
			this.min = min;
			this.max = max;
			this.step = step;
			this.label = label;
			this.apply = apply;
			this.value = toValue(current);
			updateMessage();
		}

		private double toValue(int v) {
			return (Math.clamp(v, min, max) - min) / (double) (max - min);
		}

		private int current() {
			int raw = min + (int) Math.round(value * (max - min));
			return Math.clamp(Math.round(raw / (float) step) * step, min, max);
		}

		void set(int v) {
			setValue(toValue(v));
		}

		@Override
		protected void updateMessage() {
			setMessage(label.apply(current()));
		}

		@Override
		protected void applyValue() {
			apply.accept(current());
		}
	}
}
