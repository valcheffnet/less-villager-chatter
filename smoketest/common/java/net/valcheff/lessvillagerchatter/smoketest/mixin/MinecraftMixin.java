package net.valcheff.lessvillagerchatter.smoketest.mixin;

import net.minecraft.client.Minecraft;
import net.valcheff.lessvillagerchatter.smoketest.ClientChecks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void lessvillagerchattertest$runChecks(CallbackInfo ci) {
		ClientChecks.tick((Minecraft) (Object) this);
	}
}
