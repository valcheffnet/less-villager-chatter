package net.valcheff.lessvillagerchatter.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.valcheff.lessvillagerchatter.ChatterConfig;

// Forge entrypoint. All behaviour lives in the mixins and ChatterConfig, shared with the other loaders.
@Mod(ChatterConfig.MOD_ID)
public class LessVillagerChatterForge {
	public LessVillagerChatterForge() {
		ChatterConfig.load(FMLPaths.CONFIGDIR.get());
		if (FMLEnvironment.dist == Dist.CLIENT) {
			LessVillagerChatterForgeClient.registerConfigScreen();
		}
	}
}
