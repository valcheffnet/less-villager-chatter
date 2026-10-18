package net.valcheff.lessvillagerchatter.smoketest;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.valcheff.lessvillagerchatter.ChatterConfig;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.List;

// Loaded only by ./gradlew runSmokeTest. Once the dedicated server is up, checks that the shared mixins work on
// NeoForge (interval, random start, variation, server volume), prints the result and exits with 0 or 1.
@Mod("lessvillagerchattertest")
public class SmokeTest {
	private final List<String> failures = new ArrayList<>();

	public SmokeTest() {
		NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> run(event.getServer()));
	}

	private void run(MinecraftServer server) {
		try {
			check(server.overworld());
		} catch (Throwable t) {
			failures.add("exception: " + t);
			t.printStackTrace();
		}
		if (failures.isEmpty()) {
			System.out.println("LVC-SMOKETEST PASSED");
		} else {
			failures.forEach(f -> System.out.println("LVC-SMOKETEST FAILED: " + f));
		}
		System.out.flush();
		Runtime.getRuntime().halt(failures.isEmpty() ? 0 : 1);
	}

	private void check(ServerLevel level) throws ReflectiveOperationException {
		Mob villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
		Mob trader = EntityTypes.WANDERING_TRADER.create(level, EntitySpawnReason.COMMAND);
		Mob zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);

		expect(villager.getAmbientSoundInterval() == ChatterConfig.maxInterval(ChatterConfig.villagerInterval),
				"villager interval " + villager.getAmbientSoundInterval());
		expect(trader.getAmbientSoundInterval() == ChatterConfig.maxInterval(ChatterConfig.traderInterval),
				"trader interval " + trader.getAmbientSoundInterval());
		expect(zombie.getAmbientSoundInterval() == ChatterConfig.VANILLA, "zombie interval " + zombie.getAmbientSoundInterval());

		int[] phases = new int[20];
		for (int i = 0; i < phases.length; i++) {
			phases[i] = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND).ambientSoundTime;
		}
		expect(Arrays.stream(phases).distinct().count() > 10, "random start not spread: " + Arrays.toString(phases));
		expect(zombie.ambientSoundTime == 0, "zombie phase " + zombie.ambientSoundTime);

		Method reset = Mob.class.getDeclaredMethod("resetAmbientSoundTime");
		reset.setAccessible(true);
		int[] silences = new int[400];
		for (int i = 0; i < silences.length; i++) {
			reset.invoke(villager);
			silences[i] = -villager.ambientSoundTime;
		}
		int base = ChatterConfig.villagerInterval;
		int spread = base * ChatterConfig.variation / 100;
		IntSummaryStatistics st = Arrays.stream(silences).summaryStatistics();
		expect(st.getMin() >= base - spread && st.getMax() <= base + spread, "silence out of range: " + st);
		expect(spread == 0 || st.getMax() - st.getMin() > spread, "silence not spread: " + st);

		Method volume = LivingEntity.class.getDeclaredMethod("getSoundVolume");
		volume.setAccessible(true);
		int saved = ChatterConfig.villagerVolume;
		ChatterConfig.villagerVolume = 40;
		float v = (float) volume.invoke(villager);
		float z = (float) volume.invoke(zombie);
		ChatterConfig.villagerVolume = saved;
		expect(Math.abs(v - 0.4f) < 1e-4f, "villager volume " + v);
		expect(z == 1f, "zombie volume " + z);
	}

	private void expect(boolean ok, String message) {
		if (!ok) failures.add(message);
	}
}
