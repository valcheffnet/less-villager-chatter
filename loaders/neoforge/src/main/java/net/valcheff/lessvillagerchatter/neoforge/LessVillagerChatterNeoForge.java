package net.valcheff.lessvillagerchatter.neoforge;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.valcheff.lessvillagerchatter.ChatterConfig;

// NeoForge entrypoint. All behaviour lives in the mixins and ChatterConfig, shared with the Fabric build.
@Mod(ChatterConfig.MOD_ID)
public class LessVillagerChatterNeoForge {
	public LessVillagerChatterNeoForge() {
		ChatterConfig.load(FMLPaths.CONFIGDIR.get());
	}
}
