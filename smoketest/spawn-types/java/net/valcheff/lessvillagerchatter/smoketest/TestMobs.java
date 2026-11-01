package net.valcheff.lessvillagerchatter.smoketest;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;

// 26.2 and newer: the entity type constants moved to EntityTypes.
final class TestMobs {
	private TestMobs() {
	}

	static Mob villager(ServerLevel level) {
		return EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
	}

	static Mob wanderingTrader(ServerLevel level) {
		return EntityTypes.WANDERING_TRADER.create(level, EntitySpawnReason.COMMAND);
	}

	static Mob zombie(ServerLevel level) {
		return EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
	}
}
