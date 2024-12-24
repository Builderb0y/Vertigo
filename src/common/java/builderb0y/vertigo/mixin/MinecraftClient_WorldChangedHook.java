package builderb0y.vertigo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

import builderb0y.vertigo.TrackingManager;

@Mixin(MinecraftClient.class)
public class MinecraftClient_WorldChangedHook {

	@Inject(method = "setWorld", at = @At("HEAD"))
	private void vertigo_onWorldChanged(ClientWorld world, CallbackInfo callback) {
		TrackingManager.CLIENT = world != null ? TrackingManager.createClient() : null;
	}
}