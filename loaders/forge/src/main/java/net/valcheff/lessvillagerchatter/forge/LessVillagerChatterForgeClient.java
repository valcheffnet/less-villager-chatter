package net.valcheff.lessvillagerchatter.forge;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.valcheff.lessvillagerchatter.client.ChatterConfigScreen;

// Client side: the Config button in Forge's mod list opens the settings screen.
final class LessVillagerChatterForgeClient {
	private LessVillagerChatterForgeClient() {
	}

	static void registerConfigScreen() {
		ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
				() -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ChatterConfigScreen(parent)));
	}
}
