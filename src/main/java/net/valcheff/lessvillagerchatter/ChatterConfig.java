package net.valcheff.lessvillagerchatter;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

// config/lessvillagerchatter.properties. Read once at startup; the config screen changes the values live and saves.
// Average gap between idle sounds is roughly (interval + 40) / 20 seconds; vanilla interval is 80 (~6 s).
public final class ChatterConfig {
	public static final int VANILLA = 80;
	public static final int DEFAULT_VILLAGER = 400;
	public static final int DEFAULT_TRADER = 160;
	public static final int MAX_INTERVAL = 72000;
	public static final boolean DEFAULT_RANDOM_START = true;
	public static final int DEFAULT_VARIATION = 25;
	public static final int MAX_VARIATION = 90;
	public static final int DEFAULT_VOLUME = 100;

	public static volatile int villagerInterval = DEFAULT_VILLAGER;
	public static volatile int traderInterval = DEFAULT_TRADER;
	/** Start each villager at a random point of its cycle so they don't "hmm" in step. */
	public static volatile boolean randomStart = DEFAULT_RANDOM_START;
	/** Each silence after a "hmm" is randomly up to this many percent shorter or longer (average unchanged). */
	public static volatile int variation = DEFAULT_VARIATION;
	/** Volume in percent of all villager / wandering trader sounds. Set by the server for everyone. */
	public static volatile int villagerVolume = DEFAULT_VOLUME;
	public static volatile int traderVolume = DEFAULT_VOLUME;
	/** Client only: hear villagers / traders at the volumes above on any server, instead of what the server sends. */
	public static volatile boolean useOwnVolume = false;

	private ChatterConfig() {
	}

	public static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve(LessVillagerChatter.MOD_ID + ".properties");
	}

	/** Average seconds between idle sounds for a given interval in ticks. */
	public static int averageSeconds(int interval) {
		return Math.round((interval + 40) / 20f);
	}

	/** Interval in ticks that gives the requested average seconds between idle sounds. */
	public static int intervalForSeconds(int seconds) {
		return Math.max(VANILLA, seconds * 20 - 40);
	}

	/** Longest silence for a base interval; this is what getAmbientSoundInterval() reports. */
	public static int maxInterval(int interval) {
		return interval + interval * variation / 100;
	}

	public static void load() {
		Path file = file();
		Properties props = new Properties();
		if (Files.exists(file)) {
			try (Reader r = Files.newBufferedReader(file)) {
				props.load(r);
			} catch (IOException e) {
				LessVillagerChatter.LOG.warn("Could not read {}, using defaults", file, e);
			}
		}

		villagerInterval = readInt(props, "villagerInterval", DEFAULT_VILLAGER, 1, MAX_INTERVAL);
		traderInterval = readInt(props, "traderInterval", DEFAULT_TRADER, 1, MAX_INTERVAL);
		randomStart = Boolean.parseBoolean(props.getProperty("randomStart", String.valueOf(DEFAULT_RANDOM_START)).trim());
		variation = readInt(props, "variation", DEFAULT_VARIATION, 0, MAX_VARIATION);
		villagerVolume = readInt(props, "villagerVolume", DEFAULT_VOLUME, 0, 100);
		traderVolume = readInt(props, "traderVolume", DEFAULT_VOLUME, 0, 100);
		useOwnVolume = Boolean.parseBoolean(props.getProperty("useOwnVolume", "false").trim());

		// Rewrite so files from older versions gain the new keys.
		save();
	}

	public static void save() {
		Path file = file();
		try {
			Files.createDirectories(file.getParent());
			try (Writer w = Files.newBufferedWriter(file)) {
				w.write("# Less Villager Chatter\n");
				w.write("# Ticks of forced silence after each idle 'hmm' sound (20 ticks = 1 second).\n");
				w.write("# Vanilla is 80, which gives about one sound every 6 s on average.\n");
				w.write("# Average gap ~ (interval + 40) / 20 seconds: 160 ~ 10 s, 400 ~ 22 s, 600 ~ 32 s.\n");
				w.write("# On a dedicated server, restart to apply changes.\n");
				w.write("villagerInterval=" + villagerInterval + "\n");
				w.write("traderInterval=" + traderInterval + "\n");
				w.write("\n# Start each villager / trader at a random point of its cycle, so they don't all 'hmm' together.\n");
				w.write("randomStart=" + randomStart + "\n");
				w.write("\n# Each silence is randomly up to this many percent shorter or longer (0-" + MAX_VARIATION + ").\n");
				w.write("# The average stays the same. 0 = every silence is exactly the interval above.\n");
				w.write("variation=" + variation + "\n");
				w.write("\n# Volume of all villager / wandering trader sounds in percent (0-100).\n");
				w.write("# On a server this applies to every player, including players with a vanilla client.\n");
				w.write("villagerVolume=" + villagerVolume + "\n");
				w.write("traderVolume=" + traderVolume + "\n");
				w.write("# Client only: hear the volumes above on any server, instead of what the server sends.\n");
				w.write("useOwnVolume=" + useOwnVolume + "\n");
			}
		} catch (IOException e) {
			LessVillagerChatter.LOG.warn("Could not write {}", file, e);
		}
	}

	private static int readInt(Properties props, String key, int fallback, int min, int max) {
		String raw = props.getProperty(key);
		if (raw == null) return fallback;
		try {
			int v = Integer.parseInt(raw.trim());
			if (v >= min && v <= max) return v;
		} catch (NumberFormatException ignored) {
		}
		LessVillagerChatter.LOG.warn("Invalid {}={} (allowed {}..{}), using {}", key, raw, min, max, fallback);
		return fallback;
	}
}
