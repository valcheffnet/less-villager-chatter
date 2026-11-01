package net.valcheff.lessvillagerchatter.client;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.valcheff.lessvillagerchatter.ChatterConfig;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

// The settings widgets, shared by every Minecraft version; ChatterConfigScreen only lays them out.
// Each widget writes its value to ChatterConfig as soon as it changes.
public final class SettingsWidgets {
	public static final int HALF_WIDTH = 150;
	public static final int SPACING = 8;
	public static final int FULL_WIDTH = HALF_WIDTH * 2 + SPACING;

	private static final int MIN_SECONDS = 6;   // vanilla
	private static final int MAX_SECONDS = 60;
	private static final int VARIATION_STEP = 5;
	private static final int VOLUME_STEP = 5;

	private final IntSlider villagerInterval;
	private final IntSlider traderInterval;
	private final IntSlider villagerVolume;
	private final IntSlider traderVolume;
	private final IntSlider variation;
	private final CycleButton<Boolean> randomStart;
	private final CycleButton<Boolean> ownVolume;

	public SettingsWidgets() {
		villagerInterval = secondsSlider("lessvillagerchatter.config.villagers",
				ChatterConfig.villagerInterval, v -> ChatterConfig.villagerInterval = v);
		traderInterval = secondsSlider("lessvillagerchatter.config.traders",
				ChatterConfig.traderInterval, v -> ChatterConfig.traderInterval = v);
		villagerVolume = volumeSlider("lessvillagerchatter.config.villager_volume",
				ChatterConfig.villagerVolume, v -> ChatterConfig.villagerVolume = v);
		traderVolume = volumeSlider("lessvillagerchatter.config.trader_volume",
				ChatterConfig.traderVolume, v -> ChatterConfig.traderVolume = v);
		variation = new IntSlider(HALF_WIDTH, 0, ChatterConfig.MAX_VARIATION, VARIATION_STEP, ChatterConfig.variation,
				p -> Component.translatable("lessvillagerchatter.config.variation", p),
				p -> ChatterConfig.variation = p);
		randomStart = toggle(HALF_WIDTH, "lessvillagerchatter.config.random_start",
				ChatterConfig.randomStart, v -> ChatterConfig.randomStart = v);
		ownVolume = toggle(FULL_WIDTH, "lessvillagerchatter.config.own_volume",
				ChatterConfig.useOwnVolume, v -> ChatterConfig.useOwnVolume = v);

		villagerInterval.setTooltip(tooltip("lessvillagerchatter.config.interval.tooltip"));
		traderInterval.setTooltip(tooltip("lessvillagerchatter.config.interval.tooltip"));
		villagerVolume.setTooltip(tooltip("lessvillagerchatter.config.volume.tooltip"));
		traderVolume.setTooltip(tooltip("lessvillagerchatter.config.volume.tooltip"));
		variation.setTooltip(tooltip("lessvillagerchatter.config.variation.tooltip"));
		randomStart.setTooltip(tooltip("lessvillagerchatter.config.random_start.tooltip"));
		ownVolume.setTooltip(tooltip("lessvillagerchatter.config.own_volume.tooltip"));
	}

	/** Two-column rows, top to bottom. */
	public List<List<AbstractWidget>> rows() {
		return List.of(
				List.of(villagerInterval, traderInterval),
				List.of(villagerVolume, traderVolume),
				List.of(variation, randomStart));
	}

	/** The full-width toggle under the rows. */
	public AbstractWidget ownVolume() {
		return ownVolume;
	}

	public void resetToDefaults() {
		villagerInterval.set(ChatterConfig.averageSeconds(ChatterConfig.DEFAULT_VILLAGER));
		traderInterval.set(ChatterConfig.averageSeconds(ChatterConfig.DEFAULT_TRADER));
		villagerVolume.set(ChatterConfig.DEFAULT_VOLUME);
		traderVolume.set(ChatterConfig.DEFAULT_VOLUME);
		variation.set(ChatterConfig.DEFAULT_VARIATION);
		randomStart.setValue(ChatterConfig.DEFAULT_RANDOM_START);
		ChatterConfig.randomStart = ChatterConfig.DEFAULT_RANDOM_START;
		ownVolume.setValue(false);
		ChatterConfig.useOwnVolume = false;
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

	private static final class IntSlider extends AbstractSliderButton {
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
			return (Mth.clamp(v, min, max) - min) / (double) (max - min);
		}

		private int current() {
			int raw = min + (int) Math.round(value * (max - min));
			return Mth.clamp(Math.round(raw / (float) step) * step, min, max);
		}

		// setValue(double) is private before 1.21.2, so set the field and apply by hand.
		void set(int v) {
			value = toValue(v);
			updateMessage();
			applyValue();
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
