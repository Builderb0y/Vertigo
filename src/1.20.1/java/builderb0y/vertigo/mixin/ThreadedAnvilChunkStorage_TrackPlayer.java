package builderb0y.vertigo.mixin;

import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import builderb0y.vertigo.VerticalTrackingManager;
import builderb0y.vertigo.Vertigo;

@Mixin(ThreadedAnvilChunkStorage.class)
public class ThreadedAnvilChunkStorage_TrackPlayer {

	@Inject(method = "sendChunkDataPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ChunkDataS2CPacket;<init>(Lnet/minecraft/world/chunk/WorldChunk;Lnet/minecraft/world/chunk/light/LightingProvider;Ljava/util/BitSet;Ljava/util/BitSet;)V"))
	private void vertigo_capturePlayer(ServerPlayerEntity player, MutableObject<ChunkDataS2CPacket> cachedDataPacket, WorldChunk chunk, CallbackInfo callback) {
		Vertigo.SYNCING_PLAYER.set(player);
	}

	@Inject(method = "sendChunkDataPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ChunkDataS2CPacket;<init>(Lnet/minecraft/world/chunk/WorldChunk;Lnet/minecraft/world/chunk/light/LightingProvider;Ljava/util/BitSet;Ljava/util/BitSet;)V", shift = Shift.AFTER))
	private void vertigo_uncapturePlayer(ServerPlayerEntity player, MutableObject<ChunkDataS2CPacket> cachedDataPacket, WorldChunk chunk, CallbackInfo callback) {
		Vertigo.SYNCING_PLAYER.set(null);
		VerticalTrackingManager manager = VerticalTrackingManager.PLAYERS.computeIfAbsent(player, VerticalTrackingManager::new);
		manager.onChunkLoaded(player, chunk.getPos().x, chunk.getPos().z);
	}

	@Inject(method = "sendWatchPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;sendUnloadChunkPacket(Lnet/minecraft/util/math/ChunkPos;)V", shift = Shift.AFTER))
	private void vertigo_onChunkUnloaded(ServerPlayerEntity player, ChunkPos pos, MutableObject<ChunkDataS2CPacket> packet, boolean oldWithinViewDistance, boolean newWithinViewDistance, CallbackInfo callback) {
		VerticalTrackingManager manager = VerticalTrackingManager.PLAYERS.get(player);
		if (manager != null) manager.onChunkUnloaded(player, pos.x, pos.z);
	}
}