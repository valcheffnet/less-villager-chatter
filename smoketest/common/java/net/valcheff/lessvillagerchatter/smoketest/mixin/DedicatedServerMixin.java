package net.valcheff.lessvillagerchatter.smoketest.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.valcheff.lessvillagerchatter.smoketest.SmokeChecks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// initServer() returns once the worlds are loaded; it exists unchanged from 1.20.1 to 26.x on every loader.
@Mixin(DedicatedServer.class)
public class DedicatedServerMixin {
	@Inject(method = "initServer", at = @At("RETURN"))
	private void lessvillagerchattertest$runChecks(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			SmokeChecks.run((MinecraftServer) (Object) this);
		}
	}
}
