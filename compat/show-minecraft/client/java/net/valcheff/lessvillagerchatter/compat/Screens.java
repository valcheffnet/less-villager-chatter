package net.valcheff.lessvillagerchatter.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

// Up to 26.1: Minecraft switches screens itself.
public final class Screens {
	private Screens() {
	}

	public static void show(Minecraft minecraft, Screen screen) {
		minecraft.setScreen(screen);
	}
}
