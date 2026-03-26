package builderb0y.vertigo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import builderb0y.vertigo.TrackingManager;
import builderb0y.vertigo.VertigoInternals;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(PlayerChunkSender.class)
public class ChunkDataSender_TrackPlayer {

	@Inject(method = "sendChunk", at = @At("HEAD"))
	private static void vertigo_markPlayer(ServerGamePacketListenerImpl handler, ServerLevel world, LevelChunk chunk, CallbackInfo callback) {
		TrackingManager manager = TrackingManager.getOrCreate(handler.player);
		if (manager.otherSideHasVertigoInstalled()) {
			VertigoInternals.SYNCING_PLAYER.set(handler.player);
		}
	}

	@Inject(method = "sendChunk", at = @At("RETURN"))
	private static void vertigo_unmarkPlayer(ServerGamePacketListenerImpl handler, ServerLevel world, LevelChunk chunk, CallbackInfo callback) {
		VertigoInternals.SYNCING_PLAYER.set(null);
		TrackingManager manager = TrackingManager.getOrCreate(handler.player);
		manager.onChunkLoaded(handler.player, chunk.getPos().x(), chunk.getPos().z());
	}

	@Inject(method = "dropChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V", shift = Shift.AFTER))
	private void vertigo_onUnload(ServerPlayer player, ChunkPos pos, CallbackInfo callback) {
		TrackingManager manager = TrackingManager.get(player);
		if (manager != null) manager.onChunkUnloaded(player, pos.x(), pos.z());
	}
}