package net.valcheff.lessvillagerchatter.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.valcheff.lessvillagerchatter.ChatterConfig;
import net.valcheff.lessvillagerchatter.client.ChatterConfigScreen;

// Client side: the Config button in NeoForge's mod list opens the same settings screen as Mod Menu on Fabric.
@Mod(value = ChatterConfig.MOD_ID, dist = Dist.CLIENT)
public class LessVillagerChatterNeoForgeClient {
	public LessVillagerChatterNeoForgeClient(ModContainer container) {
		container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new ChatterConfigScreen(parent));
	}
}
