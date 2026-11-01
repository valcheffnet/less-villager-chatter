package net.valcheff.lessvillagerchatter.smoketest;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

// Up to 1.21.1: EntityType.create(Level).
final class TestMobs {
	private TestMobs() {
	}

	static Mob villager(ServerLevel level) {
		return EntityType.VILLAGER.create(level);
	}

	static Mob wanderingTrader(ServerLevel level) {
		return EntityType.WANDERING_TRADER.create(level);
	}

	static Mob zombie(ServerLevel level) {
		return EntityType.ZOMBIE.create(level);
	}
}
