package net.valcheff.lessvillagerchatter.smoketest;

import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.valcheff.lessvillagerchatter.ChatterConfig;

// The settings screen as Mod Menu's Config button builds it: through the mod's "modmenu" entrypoint.
final class ConfigScreens {
	private ConfigScreens() {
	}

	static Screen create(Minecraft mc, Screen parent) {
		return FabricLoader.getInstance().getEntrypointContainers("modmenu", ModMenuApi.class).stream()
				.filter(c -> c.getProvider().getMetadata().getId().equals(ChatterConfig.MOD_ID))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no modmenu entrypoint"))
				.getEntrypoint().getModConfigScreenFactory().create(parent);
	}
}
