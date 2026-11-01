package net.valcheff.lessvillagerchatter.smoketest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.valcheff.lessvillagerchatter.ChatterConfig;

// The settings screen as NeoForge's mod list builds it: through the registered IConfigScreenFactory.
final class ConfigScreens {
	private ConfigScreens() {
	}

	static Screen create(Minecraft mc, Screen parent) {
		ModContainer mod = ModList.get().getModContainerById(ChatterConfig.MOD_ID).orElseThrow();
		IConfigScreenFactory factory = mod.getCustomExtension(IConfigScreenFactory.class)
				.orElseThrow(() -> new AssertionError("no IConfigScreenFactory registered"));
		return factory.createScreen(mod, parent);
	}
}
