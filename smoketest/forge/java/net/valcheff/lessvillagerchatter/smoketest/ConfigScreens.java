package net.valcheff.lessvillagerchatter.smoketest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.valcheff.lessvillagerchatter.ChatterConfig;

// The settings screen as Forge's mod list builds it: through the registered ConfigScreenFactory.
final class ConfigScreens {
	private ConfigScreens() {
	}

	static Screen create(Minecraft mc, Screen parent) {
		return ConfigScreenHandler.getScreenFactoryFor(ModList.get().getModContainerById(ChatterConfig.MOD_ID).orElseThrow().getModInfo())
				.orElseThrow(() -> new AssertionError("no ConfigScreenFactory registered"))
				.apply(mc, parent);
	}
}
