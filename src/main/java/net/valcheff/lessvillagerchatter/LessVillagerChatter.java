package net.valcheff.lessvillagerchatter;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LessVillagerChatter implements ModInitializer {
	public static final String MOD_ID = "lessvillagerchatter";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ChatterConfig.load();
		LOG.info("Idle sound interval: villagers {} ticks, wandering traders {} ticks (vanilla {}), random start {}, variation {}%",
				ChatterConfig.villagerInterval, ChatterConfig.traderInterval, ChatterConfig.VANILLA,
				ChatterConfig.randomStart, ChatterConfig.variation);
	}
}
