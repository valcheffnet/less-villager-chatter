package net.valcheff.lessvillagerchatter.smoketest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.lwjgl.glfw.GLFW;

// Minecraft 1.20.1 to 1.21.1: widgets take raw mouse coordinates, the window is a GLFW handle.
final class ClientCompat {
	private ClientCompat() {
	}

	static Screen screen(Minecraft mc) {
		return mc.screen;
	}

	static boolean loading(Minecraft mc) {
		return mc.getOverlay() != null;
	}

	static void click(AbstractWidget widget, double x, double y) {
		widget.onClick(x, y);
	}

	static void resolve(SoundInstance sound, Minecraft mc) {
		sound.resolve(mc.getSoundManager());
	}

	static void screenshot(Minecraft mc) {
		Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), message -> {});
	}

	static void placeWindow(Minecraft mc, int x, int y) {
		GLFW.glfwSetWindowPos(mc.getWindow().getWindow(), x, y);
	}
}
