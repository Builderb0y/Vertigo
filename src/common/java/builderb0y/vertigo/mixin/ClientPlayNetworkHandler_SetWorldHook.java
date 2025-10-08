package builderb0y.vertigo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;

import builderb0y.vertigo.TrackingManager;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandler_SetWorldHook {

	@Shadow private ClientWorld world;

	#if MC_VERSION >= MC_1_21_9
		@Inject(method = "onGameJoin", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientDebugSubscriptionManager;clearAllSubscriptions()V"))
	#else
		@Inject(method = "onGameJoin", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/debug/DebugRenderer;reset()V"))
	#endif
	private void vertigo_onJoinWorld(GameJoinS2CPacket packet, CallbackInfo callback) {
		TrackingManager.CLIENT = this.world != null ? TrackingManager.createClient() : null;
	}

	#if MC_VERSION >= MC_1_21_9
		@Inject(method = "onPlayerRespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;joinWorld(Lnet/minecraft/client/world/ClientWorld;)V", shift = Shift.AFTER))
	#elif MC_VERSION >= MC_1_20_5
		@Inject(method = "onPlayerRespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;joinWorld(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/gui/screen/DownloadingTerrainScreen$WorldEntryReason;)V", shift = Shift.AFTER))
	#else
		@Inject(method = "onPlayerRespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;joinWorld(Lnet/minecraft/client/world/ClientWorld;)V", shift = Shift.AFTER))
	#endif
	private void vertigo_onRespawn(PlayerRespawnS2CPacket packet, CallbackInfo callback) {
		TrackingManager.CLIENT = this.world != null ? TrackingManager.createClient() : null;
	}
}