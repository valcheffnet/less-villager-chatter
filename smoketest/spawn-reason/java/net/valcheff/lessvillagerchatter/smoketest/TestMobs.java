package net.valcheff.lessvillagerchatter.smoketest;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

// 1.21.11 and 26.1: create(Level, EntitySpawnReason), constants still on EntityType.
final class TestMobs {
	private TestMobs() {
	}

	static Mob villager(ServerLevel level) {
		return EntityType.VILLAGER.create(level, EntitySpawnReason.COMMAND);
	}

	static Mob wanderingTrader(ServerLevel level) {
		return EntityType.WANDERING_TRADER.create(level, EntitySpawnReason.COMMAND);
	}

	static Mob zombie(ServerLevel level) {
		return EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
	}
}
