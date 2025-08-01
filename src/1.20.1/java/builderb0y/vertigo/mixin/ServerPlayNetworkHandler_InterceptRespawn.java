package builderb0y.vertigo.mixin;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import builderb0y.vertigo.TrackingManager;

/**
{@link ServerEntityWorldChangeEvents#AFTER_PLAYER_CHANGE_WORLD}
fires after all the chunks have been sent, but I need to be
notified before this happens so that old chunks can be
cleared from the tracker BEFORE new chunks are added to it.

there are 3 different code paths in vanilla which
are used when a player changes dimensions,
but one thing they have in common is that all
3 of them send a {@link PlayerRespawnS2CPacket}.
so, that's what I handle here.
*/
@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandler_InterceptRespawn {

	@Shadow public ServerPlayerEntity player;

	@Inject(method = "sendPacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V", at = @At("HEAD"))
	private void vertigo_interceptDimensionChange(Packet<?> packet, @Nullable PacketCallbacks callbacks, CallbackInfo ci) {
		if (packet instanceof PlayerRespawnS2CPacket) {
			TrackingManager trackingManager = TrackingManager.PLAYERS.get(this.player);
			if (trackingManager != null) trackingManager.clear();
		}
	}
}