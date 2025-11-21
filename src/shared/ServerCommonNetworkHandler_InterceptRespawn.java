package builderb0y.vertigo.mixin;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;

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
@Mixin(ServerCommonNetworkHandler.class)
public class ServerCommonNetworkHandler_InterceptRespawn {

	@Inject(method = "send", at = @At("HEAD"))
	private void vertigo_interceptRespawn(
		Packet<?> packet,
		/** different class in 1.21.6+ compared to 1.21.5- */
		@Coerce Object callbacks,
		CallbackInfo callback
	) {
		if (((Object)(this)) instanceof ServerPlayNetworkHandler handler && packet instanceof PlayerRespawnS2CPacket) {
			TrackingManager trackingManager = TrackingManager.get(handler.player);
			if (trackingManager != null) trackingManager.clear();
		}
	}
}