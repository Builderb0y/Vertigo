package builderb0y.vertigo.mixin;

import java.util.BitSet;

import com.llamalad7.mixinextras.sugar.Local;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;

import builderb0y.vertigo.VerticalTrackingManager;
import builderb0y.vertigo.Vertigo;

@Mixin(ThreadedAnvilChunkStorage.class)
public class ThreadedAnvilChunkStorage_TrackPlayer {

	@Shadow @Final private ServerLightingProvider lightingProvider;

	/**
	by default, minecraft will cache the chunk packet
	so that it can be sent to multiple players.
	this is problematic for vertigo,
	since the players could be at different Y levels,
	and should therefore receive different packets.

	the method below, {@link #vertigo_createNewPacket(MutableObject, ServerPlayerEntity, WorldChunk)},
	is used to create a new packet for each player.
	this method here is used to not create a packet
	which will never get sent anywhere.
	*/
	@Redirect(method = "sendChunkDataPackets", at = @At(value = "NEW", target = "(Lnet/minecraft/world/chunk/WorldChunk;Lnet/minecraft/world/chunk/light/LightingProvider;Ljava/util/BitSet;Ljava/util/BitSet;)Lnet/minecraft/network/packet/s2c/play/ChunkDataS2CPacket;"))
	private ChunkDataS2CPacket vertigo_dontCachePacket(WorldChunk chunk, LightingProvider lightProvider, BitSet skyBits, BitSet blockBits) {
		return null;
	}

	@Redirect(method = "sendChunkDataPackets", at = @At(value = "INVOKE", target = "Lorg/apache/commons/lang3/mutable/MutableObject;getValue()Ljava/lang/Object;", ordinal = 1))
	private Object vertigo_createNewPacket(MutableObject<?> instance, @Local(argsOnly = true) ServerPlayerEntity player, @Local(argsOnly = true) WorldChunk chunk) {
		Vertigo.SYNCING_PLAYER.set(player);
		try {
			return new ChunkDataS2CPacket(chunk, this.lightingProvider, null, null);
		}
		finally {
			Vertigo.SYNCING_PLAYER.set(null);
		}
	}

	@Inject(method = "sendChunkDataPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;sendChunkPacket(Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/network/packet/Packet;)V", shift = Shift.AFTER))
	private void vertigo_uncapturePlayer(ServerPlayerEntity player, MutableObject<ChunkDataS2CPacket> cachedDataPacket, WorldChunk chunk, CallbackInfo callback) {
		VerticalTrackingManager manager = VerticalTrackingManager.PLAYERS.computeIfAbsent(player, VerticalTrackingManager::new);
		manager.onChunkLoaded(player, chunk.getPos().x, chunk.getPos().z);
	}

	@Inject(method = "sendWatchPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;sendUnloadChunkPacket(Lnet/minecraft/util/math/ChunkPos;)V", shift = Shift.AFTER))
	private void vertigo_onChunkUnloaded(ServerPlayerEntity player, ChunkPos pos, MutableObject<ChunkDataS2CPacket> packet, boolean oldWithinViewDistance, boolean newWithinViewDistance, CallbackInfo callback) {
		VerticalTrackingManager manager = VerticalTrackingManager.PLAYERS.get(player);
		if (manager != null) manager.onChunkUnloaded(player, pos.x, pos.z);
	}
}