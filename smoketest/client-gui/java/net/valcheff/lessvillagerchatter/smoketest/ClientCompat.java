package net.valcheff.lessvillagerchatter.smoketest;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;

// Minecraft 26.2: the screen and overlay live in Minecraft.gui, the window is still a GLFW handle.
final class ClientCompat {
	private ClientCompat() {
	}

	static Screen screen(Minecraft mc) {
		return mc.gui.screen();
	}

	static boolean loading(Minecraft mc) {
		return mc.gui.overlay() != null;
	}

	static void click(AbstractWidget widget, double x, double y) {
		widget.onClick(new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
	}

	static void resolve(SoundInstance sound, Minecraft mc) {
		sound.resolve(mc.getSoundManager());
	}

	static void screenshot(Minecraft mc) {
		Screenshot.grab(mc, false);
	}

	static void placeWindow(Minecraft mc, int x, int y) {
		GLFW.glfwSetWindowPos(mc.getWindow().handle(), x, y);
	}
}
