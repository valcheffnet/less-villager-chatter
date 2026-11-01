package net.valcheff.lessvillagerchatter.smoketest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.valcheff.lessvillagerchatter.ChatterConfig;
import net.valcheff.lessvillagerchatter.client.ChatterConfigScreen;
import net.valcheff.lessvillagerchatter.compat.Screens;
import net.valcheff.lessvillagerchatter.smoketest.mixin.SoundEngineAccess;
import net.valcheff.lessvillagerchatter.smoketest.mixin.SoundManagerAccess;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Runs from the client tick once the title screen is up: opens the settings screen the way the loader's mod list
// does, moves its sliders and toggles, checks the config and the client's own volume, takes screenshots, then
// writes a result file and exits. Only widget methods are called; nothing goes through the OS mouse or keyboard.
public final class ClientChecks {
	public static final String RESULT_FILE = "lvc-client-smoketest-result.txt";
	private static final int TITLE_TIMEOUT_TICKS = 6000;

	private static final List<String> failures = new ArrayList<>();
	private static int tick;
	private static int resumeAt;
	private static int step;
	private static boolean placed;
	private static Screen title;

	private ClientChecks() {
	}

	public static void tick(Minecraft mc) {
		tick++;
		if (!placed) {
			placed = true;
			placeWindow(mc);
		}
		if (tick < resumeAt) return;
		try {
			step(mc);
		} catch (Throwable t) {
			failures.add("exception in step " + step + ": " + t);
			t.printStackTrace();
			finish(mc);
		}
	}

	private static void step(Minecraft mc) throws IOException {
		switch (step) {
			case 0 -> {
				Screen screen = ClientCompat.screen(mc);
				if (screen instanceof TitleScreen && !ClientCompat.loading(mc)) {
					title = screen;
					next(40);
				} else if (tick > TITLE_TIMEOUT_TICKS) {
					failures.add("title screen never came up, screen is " + screen);
					finish(mc);
				}
			}
			case 1 -> {
				Screens.show(mc, ConfigScreens.create(mc, title));
				next(10);
			}
			case 2 -> {
				expect(ClientCompat.screen(mc) instanceof ChatterConfigScreen, "mod list factory opened " + ClientCompat.screen(mc));
				ClientCompat.screenshot(mc);
				next(5);
			}
			case 3 -> {
				Screen screen = ClientCompat.screen(mc);
				// value = (x - (left + 4)) / (width - 8) for vanilla sliders.
				slideTo(widget(screen, "lessvillagerchatter.config.villagers"), 0.5);
				expect(ChatterConfig.villagerInterval == ChatterConfig.intervalForSeconds(33),
						"villager slider at half: interval " + ChatterConfig.villagerInterval);
				slideTo(widget(screen, "lessvillagerchatter.config.villager_volume"), 0.3);
				expect(ChatterConfig.villagerVolume == 30, "villager volume slider at 0.3: " + ChatterConfig.villagerVolume);
				AbstractWidget own = widget(screen, "lessvillagerchatter.config.own_volume");
				expect(own instanceof CycleButton<?>, "own volume is not a toggle: " + own);
				click(own, 0.5);
				expect(ChatterConfig.useOwnVolume, "own volume toggle did not switch on");
				checkOwnVolume(mc);
				next(10);
			}
			case 4 -> {
				ClientCompat.screenshot(mc);
				next(5);
			}
			case 5 -> {
				click(widget(ClientCompat.screen(mc), "gui.done"), 0.5);
				next(5);
			}
			case 6 -> {
				expect(ClientCompat.screen(mc) == title, "Done did not return to the mod list parent: " + ClientCompat.screen(mc));
				String saved = Files.readString(ChatterConfig.file());
				expect(saved.contains("villagerInterval=" + ChatterConfig.intervalForSeconds(33) + "\n"), "saved interval:\n" + saved);
				expect(saved.contains("villagerVolume=30\n"), "saved volume:\n" + saved);
				expect(saved.contains("useOwnVolume=true\n"), "saved own volume:\n" + saved);
				Screens.show(mc, ConfigScreens.create(mc, title));
				next(10);
			}
			case 7 -> {
				Screen screen = ClientCompat.screen(mc);
				click(widget(screen, "lessvillagerchatter.config.reset"), 0.5);
				expect(ChatterConfig.villagerInterval == ChatterConfig.DEFAULT_VILLAGER, "reset villager " + ChatterConfig.villagerInterval);
				expect(ChatterConfig.villagerVolume == ChatterConfig.DEFAULT_VOLUME, "reset volume " + ChatterConfig.villagerVolume);
				expect(!ChatterConfig.useOwnVolume, "reset own volume");
				click(widget(screen, "gui.done"), 0.5);
				next(5);
			}
			case 8 -> {
				String saved = Files.readString(ChatterConfig.file());
				expect(saved.contains("villagerInterval=" + ChatterConfig.DEFAULT_VILLAGER + "\n"), "saved after reset:\n" + saved);
				// Screenshots are written on a background thread.
				next(40);
			}
			default -> finish(mc);
		}
	}

	// "Use my volume" replaces the volume the server sent for villager and trader sounds; other sounds keep it.
	private static void checkOwnVolume(Minecraft mc) {
		SoundEngineAccess engine = (SoundEngineAccess) ((SoundManagerAccess) mc.getSoundManager()).lessvillagerchattertest$soundEngine();
		// From 1.21.11 the master volume is part of each sound's volume. The test runs muted, so turn master up for
		// the calculation; music stays at 0 and nothing else is playing on the title screen.
		mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(1.0);
		try {
			compareOwnVolume(mc, engine);
		} finally {
			mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
		}
	}

	private static void compareOwnVolume(Minecraft mc, SoundEngineAccess engine) {
		float serverVolume = 0.4f;
		float villagerOn = volume(mc, engine, SoundEvents.VILLAGER_AMBIENT, serverVolume);
		float traderOn = volume(mc, engine, SoundEvents.WANDERING_TRADER_AMBIENT, serverVolume);
		float zombieOn = volume(mc, engine, SoundEvents.ZOMBIE_AMBIENT, serverVolume);
		ChatterConfig.useOwnVolume = false;
		float villagerOff = volume(mc, engine, SoundEvents.VILLAGER_AMBIENT, serverVolume);
		float traderOff = volume(mc, engine, SoundEvents.WANDERING_TRADER_AMBIENT, serverVolume);
		float zombieOff = volume(mc, engine, SoundEvents.ZOMBIE_AMBIENT, serverVolume);
		ChatterConfig.useOwnVolume = true;
		expect(villagerOff > 0, "villager volume is 0 with own volume off");
		expect(Math.abs(villagerOn / villagerOff - 0.30f / serverVolume) < 1e-3f, "villager own volume " + villagerOn + " vs " + villagerOff);
		expect(Math.abs(traderOn / traderOff - 1.00f / serverVolume) < 1e-3f, "trader own volume " + traderOn + " vs " + traderOff);
		expect(zombieOn == zombieOff, "zombie volume changed: " + zombieOn + " vs " + zombieOff);
	}

	private static float volume(Minecraft mc, SoundEngineAccess engine, SoundEvent event, float serverVolume) {
		SimpleSoundInstance sound = new SimpleSoundInstance(event, SoundSource.NEUTRAL, serverVolume, 1f, RandomSource.create(), 0, 0, 0);
		ClientCompat.resolve(sound, mc);
		return engine.lessvillagerchattertest$calculateVolume(sound);
	}

	private static void slideTo(AbstractWidget slider, double value) {
		expect(slider instanceof AbstractSliderButton, "not a slider: " + slider);
		ClientCompat.click(slider, slider.getX() + 4 + (slider.getWidth() - 8) * value, slider.getY() + slider.getHeight() / 2.0);
	}

	private static void click(AbstractWidget widget, double across) {
		ClientCompat.click(widget, widget.getX() + widget.getWidth() * across, widget.getY() + widget.getHeight() / 2.0);
	}

	// Finds a widget by its translation key; toggles show "name: value", so their name is the first argument.
	private static AbstractWidget widget(Screen screen, String key) {
		return screen.children().stream()
				.filter(c -> c instanceof AbstractWidget)
				.map(c -> (AbstractWidget) c)
				.filter(w -> key(w.getMessage()).filter(key::equals).isPresent())
				.findFirst()
				.orElseThrow(() -> new AssertionError("no widget " + key + " on " + screen));
	}

	private static Optional<String> key(Component message) {
		if (!(message.getContents() instanceof TranslatableContents translatable)) return Optional.empty();
		if (translatable.getKey().equals("options.generic_value") && translatable.getArgs().length > 0
				&& translatable.getArgs()[0] instanceof Component name) {
			return key(name);
		}
		return Optional.of(translatable.getKey());
	}

	private static void next(int waitTicks) {
		step++;
		resumeAt = tick + waitTicks;
	}

	private static void expect(boolean ok, String message) {
		if (!ok) failures.add(message);
	}

	// ERRANDS_TEST_WINDOW="x,y" is the top left corner of the monitor the test window should open on.
	private static void placeWindow(Minecraft mc) {
		String at = System.getenv("ERRANDS_TEST_WINDOW");
		if (at == null || !at.matches("-?\\d+,-?\\d+")) return;
		String[] xy = at.split(",");
		ClientCompat.placeWindow(mc, Integer.parseInt(xy[0]) + 80, Integer.parseInt(xy[1]) + 80);
	}

	private static void finish(Minecraft mc) {
		List<String> lines = new ArrayList<>();
		if (failures.isEmpty()) {
			lines.add("LVC-CLIENT-SMOKETEST PASSED");
		} else {
			failures.forEach(f -> lines.add("LVC-CLIENT-SMOKETEST FAILED: " + f));
		}
		lines.forEach(System.out::println);
		System.out.flush();
		try {
			Files.write(mc.gameDirectory.toPath().resolve(RESULT_FILE), lines);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// A normal shutdown: halting here crashes the native sound and window threads. The build reads the result file.
		mc.stop();
	}
}
