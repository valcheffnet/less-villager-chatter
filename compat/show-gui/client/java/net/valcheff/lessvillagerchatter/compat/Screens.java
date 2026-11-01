package net.valcheff.lessvillagerchatter.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

// 26.2 and newer: screens are switched through Minecraft.gui.
public final class Screens {
	private Screens() {
	}

	public static void show(Minecraft minecraft, Screen screen) {
		minecraft.gui.setScreen(screen);
	}
}
