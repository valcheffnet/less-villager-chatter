package net.valcheff.lessvillagerchatter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

// Fabric entrypoint. All behaviour lives in the mixins and ChatterConfig, shared with the NeoForge build.
public class LessVillagerChatter implements ModInitializer {
	@Override
	public void onInitialize() {
		ChatterConfig.load(FabricLoader.getInstance().getConfigDir());
	}
}
