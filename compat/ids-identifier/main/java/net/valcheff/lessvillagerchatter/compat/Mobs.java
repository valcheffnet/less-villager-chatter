package net.valcheff.lessvillagerchatter.compat;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;

// From 1.21.11 on, villagers and wandering traders have their own packages.
public final class Mobs {
	private Mobs() {
	}

	public static boolean isVillager(Object mob) {
		return mob instanceof Villager;
	}

	public static boolean isWanderingTrader(Object mob) {
		return mob instanceof WanderingTrader;
	}
}
