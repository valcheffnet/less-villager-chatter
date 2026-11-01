package net.valcheff.lessvillagerchatter.smoketest;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.valcheff.lessvillagerchatter.ChatterConfig;
import net.valcheff.lessvillagerchatter.smoketest.mixin.LivingEntityAccess;
import net.valcheff.lessvillagerchatter.smoketest.mixin.MobAccess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.List;

// Runs once the dedicated server has loaded its worlds: checks that the mod's mixins work on this loader and
// Minecraft version (interval, random start, variation, server volume), prints the result and exits with 0 or 1.
public final class SmokeChecks {
	public static final String RESULT_FILE = "lvc-smoketest-result.txt";

	private final List<String> failures = new ArrayList<>();

	private SmokeChecks() {
	}

	public static void run(MinecraftServer server) {
		SmokeChecks checks = new SmokeChecks();
		try {
			checks.check(server.overworld());
		} catch (Throwable t) {
			checks.failures.add("exception: " + t);
			t.printStackTrace();
		}
		List<String> lines = new ArrayList<>();
		if (checks.failures.isEmpty()) {
			lines.add("LVC-SMOKETEST PASSED");
		} else {
			checks.failures.forEach(f -> lines.add("LVC-SMOKETEST FAILED: " + f));
		}
		lines.forEach(System.out::println);
		System.out.flush();
		// The build reads this file: a server that never got this far leaves none, and that counts as a failure.
		try {
			Files.write(Path.of(RESULT_FILE), lines);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Runtime.getRuntime().halt(checks.failures.isEmpty() ? 0 : 1);
	}

	private void check(ServerLevel level) {
		Mob villager = TestMobs.villager(level);
		Mob trader = TestMobs.wanderingTrader(level);
		Mob zombie = TestMobs.zombie(level);

		expect(villager.getAmbientSoundInterval() == ChatterConfig.maxInterval(ChatterConfig.villagerInterval),
				"villager interval " + villager.getAmbientSoundInterval());
		expect(trader.getAmbientSoundInterval() == ChatterConfig.maxInterval(ChatterConfig.traderInterval),
				"trader interval " + trader.getAmbientSoundInterval());
		expect(zombie.getAmbientSoundInterval() == ChatterConfig.VANILLA, "zombie interval " + zombie.getAmbientSoundInterval());

		int[] phases = new int[20];
		for (int i = 0; i < phases.length; i++) {
			phases[i] = TestMobs.villager(level).ambientSoundTime;
		}
		expect(Arrays.stream(phases).distinct().count() > 10, "random start not spread: " + Arrays.toString(phases));
		expect(zombie.ambientSoundTime == 0, "zombie phase " + zombie.ambientSoundTime);

		int[] silences = new int[400];
		for (int i = 0; i < silences.length; i++) {
			((MobAccess) villager).lessvillagerchattertest$resetAmbientSoundTime();
			silences[i] = -villager.ambientSoundTime;
		}
		int base = ChatterConfig.villagerInterval;
		int spread = base * ChatterConfig.variation / 100;
		IntSummaryStatistics st = Arrays.stream(silences).summaryStatistics();
		expect(st.getMin() >= base - spread && st.getMax() <= base + spread, "silence out of range: " + st);
		expect(spread == 0 || st.getMax() - st.getMin() > spread, "silence not spread: " + st);

		int saved = ChatterConfig.villagerVolume;
		ChatterConfig.villagerVolume = 40;
		float v = ((LivingEntityAccess) villager).lessvillagerchattertest$getSoundVolume();
		float z = ((LivingEntityAccess) zombie).lessvillagerchattertest$getSoundVolume();
		ChatterConfig.villagerVolume = saved;
		expect(Math.abs(v - 0.4f) < 1e-4f, "villager volume " + v);
		expect(z == 1f, "zombie volume " + z);
	}

	private void expect(boolean ok, String message) {
		if (!ok) failures.add(message);
	}
}
