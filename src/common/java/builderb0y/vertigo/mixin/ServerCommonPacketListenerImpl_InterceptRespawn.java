package builderb0y.vertigo.mixin;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import builderb0y.vertigo.TrackingManager;

/**
{@link ServerEntityLevelChangeEvents#AFTER_PLAYER_CHANGE_LEVEL}
fires after all the chunks have been sent, but I need to be
notified before this happens so that old chunks can be
cleared from the tracker BEFORE new chunks are added to it.

there are 3 different code paths in vanilla which
are used when a player changes dimensions,
but one thing they have in common is that all
3 of them send a {@link ClientboundRespawnPacket}.
so, that's what I handle here.
*/
@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonPacketListenerImpl_InterceptRespawn {

	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"))
	private void vertigo_interceptRespawn(
		Packet<?> packet,
		/** different class in 1.21.6+ compared to 1.21.5- */
		@Coerce Object callbacks,
		CallbackInfo callback
	) {
		if (((Object)(this)) instanceof ServerGamePacketListenerImpl handler && packet instanceof ClientboundRespawnPacket) {
			TrackingManager trackingManager = TrackingManager.get(handler.player);
			if (trackingManager != null) trackingManager.clear();
		}
	}
}