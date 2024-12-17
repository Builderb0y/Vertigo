package builderb0y.vertigo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.network.ChunkDataSender;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import builderb0y.vertigo.Vertigo;
import builderb0y.vertigo.VerticalTrackingManager;

@Mixin(ChunkDataSender.class)
public class ChunkDataSender_TrackPlayer {

	@Inject(method = "sendChunkData", at = @At("HEAD"))
	private static void vertigo_markPlayer(ServerPlayNetworkHandler handler, ServerWorld world, WorldChunk chunk, CallbackInfo callback) {
		Vertigo.SYNCING_PLAYER.set(handler.player);
	}

	@Inject(method = "sendChunkData", at = @At("RETURN"))
	private static void vertigo_unmarkPlayer(ServerPlayNetworkHandler handler, ServerWorld world, WorldChunk chunk, CallbackInfo callback) {
		Vertigo.SYNCING_PLAYER.set(null);
		VerticalTrackingManager manager = VerticalTrackingManager.PLAYERS.computeIfAbsent(handler.player, VerticalTrackingManager::new);
		manager.onChunkLoaded(handler.player, chunk.getPos().x, chunk.getPos().z);
	}

	@Inject(method = "unload", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V", shift = Shift.AFTER))
	private void vertigo_onUnload(ServerPlayerEntity player, ChunkPos pos, CallbackInfo callback) {
		VerticalTrackingManager manager = VerticalTrackingManager.PLAYERS.get(player);
		if (manager != null) manager.onChunkUnloaded(player, pos.x, pos.z);
	}
}