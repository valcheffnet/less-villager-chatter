package net.valcheff.lessvillagerchatter.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.valcheff.lessvillagerchatter.ChatterConfig;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.IntSummaryStatistics;

// Runs a real client: checks the mixin on the integrated server, then drives the config screen.
@SuppressWarnings("UnstableApiUsage")
public class ChatterClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			int[] start = intervals(singleplayer);
			// With variation on, getAmbientSoundInterval() reports the longest silence.
			check(start[0] == ChatterConfig.maxInterval(ChatterConfig.DEFAULT_VILLAGER), "villager interval " + start[0]);
			check(start[1] == ChatterConfig.maxInterval(ChatterConfig.DEFAULT_TRADER), "trader interval " + start[1]);
			check(start[2] == ChatterConfig.VANILLA, "zombie interval must stay vanilla, got " + start[2]);

			// New villagers start at random points of their cycle instead of all at 0.
			int[] phases = singleplayer.getServer().computeOnServer((MinecraftServer server) -> {
				int[] t = new int[20];
				for (int i = 0; i < t.length; i++) {
					t[i] = EntityTypes.VILLAGER.create(server.overworld(), EntitySpawnReason.COMMAND).ambientSoundTime;
				}
				return t;
			});
			check(Arrays.stream(phases).distinct().count() > 10, "phases not spread: " + Arrays.toString(phases));
			check(Arrays.stream(phases).allMatch(t -> t <= 0 && t > -ChatterConfig.DEFAULT_VILLAGER),
					"phase out of range: " + Arrays.toString(phases));
			int zombiePhase = singleplayer.getServer().computeOnServer((MinecraftServer server) ->
					EntityTypes.ZOMBIE.create(server.overworld(), EntitySpawnReason.COMMAND).ambientSoundTime);
			check(zombiePhase == 0, "zombie phase must stay vanilla 0, got " + zombiePhase);

			// After each "hmm" the silence lands in interval * (1 +/- variation), averaging the interval.
			int[] silences = singleplayer.getServer().computeOnServer((MinecraftServer server) -> {
				try {
					Method reset = Mob.class.getDeclaredMethod("resetAmbientSoundTime");
					reset.setAccessible(true);
					Mob villager = EntityTypes.VILLAGER.create(server.overworld(), EntitySpawnReason.COMMAND);
					int[] t = new int[400];
					for (int i = 0; i < t.length; i++) {
						reset.invoke(villager);
						t[i] = -villager.ambientSoundTime;
					}
					return t;
				} catch (ReflectiveOperationException e) {
					throw new AssertionError(e);
				}
			});
			int base = ChatterConfig.DEFAULT_VILLAGER;
			int spread = base * ChatterConfig.DEFAULT_VARIATION / 100;
			IntSummaryStatistics st = Arrays.stream(silences).summaryStatistics();
			check(st.getMin() >= base - spread && st.getMax() <= base + spread, "silence out of range: " + st);
			check(st.getMax() - st.getMin() > spread, "silence not spread: " + st);
			check(Math.abs(st.getAverage() - base) < spread / 4.0, "average silence drifted: " + st);

			// Server-side volume: villagers and traders get their own volume, other mobs stay at 1.0.
			ChatterConfig.villagerVolume = 40;
			ChatterConfig.traderVolume = 70;
			float[] volumes = singleplayer.getServer().computeOnServer((MinecraftServer server) -> {
				try {
					Method volume = LivingEntity.class.getDeclaredMethod("getSoundVolume");
					volume.setAccessible(true);
					return new float[] {
							(float) volume.invoke(EntityTypes.VILLAGER.create(server.overworld(), EntitySpawnReason.COMMAND)),
							(float) volume.invoke(EntityTypes.WANDERING_TRADER.create(server.overworld(), EntitySpawnReason.COMMAND)),
							(float) volume.invoke(EntityTypes.ZOMBIE.create(server.overworld(), EntitySpawnReason.COMMAND))
					};
				} catch (ReflectiveOperationException e) {
					throw new AssertionError(e);
				}
			});
			check(Math.abs(volumes[0] - 0.4f) < 1e-4 && Math.abs(volumes[1] - 0.7f) < 1e-4 && volumes[2] == 1f,
					"volumes " + Arrays.toString(volumes));
			ChatterConfig.villagerVolume = ChatterConfig.DEFAULT_VOLUME;
			ChatterConfig.traderVolume = ChatterConfig.DEFAULT_VOLUME;

			// Random start can be switched off.
			ChatterConfig.randomStart = false;
			int fixedPhase = singleplayer.getServer().computeOnServer((MinecraftServer server) ->
					EntityTypes.VILLAGER.create(server.overworld(), EntitySpawnReason.COMMAND).ambientSoundTime);
			check(fixedPhase == 0, "random start off, phase should be 0, got " + fixedPhase);
			ChatterConfig.randomStart = true;

			context.setScreen(() -> new ChatterConfigScreen(null));
			context.waitForScreen(ChatterConfigScreen.class);
			context.takeScreenshot("lvc-config-default");

			// Slider input is checked by hand (simulated input does not reach the screen here);
			// what the slider does on change is set the static value, so do that directly.
			int changed = ChatterConfig.intervalForSeconds(40);
			ChatterConfig.villagerInterval = changed;

			// New value is live on the integrated server without a restart.
			int[] live = intervals(singleplayer);
			check(live[0] == ChatterConfig.maxInterval(changed), "server sees " + live[0] + ", expected " + ChatterConfig.maxInterval(changed));

			context.clickScreenButton("gui.done");
			context.waitFor(mc -> !(mc.gui.screen() instanceof ChatterConfigScreen));
			String saved = readConfig();
			check(saved.contains("villagerInterval=" + changed), "config not saved:\n" + saved);

			// Reset button restores defaults.
			context.setScreen(() -> new ChatterConfigScreen(null));
			context.waitForScreen(ChatterConfigScreen.class);
			context.clickScreenButton("lessvillagerchatter.config.reset");
			context.waitTick();
			check(ChatterConfig.villagerInterval == ChatterConfig.DEFAULT_VILLAGER, "reset villager " + ChatterConfig.villagerInterval);
			check(ChatterConfig.traderInterval == ChatterConfig.DEFAULT_TRADER, "reset trader " + ChatterConfig.traderInterval);
			context.clickScreenButton("gui.done");

			context.setScreen(() -> null);
		}
	}

	private static int[] intervals(TestSingleplayerContext singleplayer) {
		return singleplayer.getServer().computeOnServer((MinecraftServer server) -> new int[] {
				interval(EntityTypes.VILLAGER.create(server.overworld(), EntitySpawnReason.COMMAND)),
				interval(EntityTypes.WANDERING_TRADER.create(server.overworld(), EntitySpawnReason.COMMAND)),
				interval(EntityTypes.ZOMBIE.create(server.overworld(), EntitySpawnReason.COMMAND))
		});
	}

	private static int interval(Mob mob) {
		return mob.getAmbientSoundInterval();
	}

	private static String readConfig() {
		try {
			return Files.readString(ChatterConfig.file());
		} catch (Exception e) {
			throw new AssertionError("cannot read config", e);
		}
	}

	private static void check(boolean ok, String message) {
		if (!ok) throw new AssertionError(message);
	}
}
