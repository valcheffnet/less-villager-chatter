package net.valcheff.lessvillagerchatter.compat;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;

// Minecraft up to 1.21.10 keeps both classes in the npc package.
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
