package builderb0y.vertigo;

import java.util.Iterator;
import java.util.Map;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import builderb0y.vertigo.networking.LoadRangePacket;
import builderb0y.vertigo.networking.VertigoInstalledPacket;

public abstract class TrackingManager {

	/** used on the logical server to refer to every player on the server. */
	public static final WeakIdentityHashMap<ServerPlayerEntity, TrackingManager> PLAYERS = new WeakIdentityHashMap<>();
	/** used on the logical client to refer to the current player. */
	public static TrackingManager CLIENT;

	static {
		ServerPlayerEvents.COPY_FROM.register((ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) -> {
			TrackingManager manager = PLAYERS.get(oldPlayer);
			if (manager != null) PLAYERS.put(newPlayer, manager);
		});
	}

	@Environment(EnvType.CLIENT)
	public static TrackingManager createClient() {
		if (ClientPlayNetworking.canSend(VertigoInstalledPacket.PACKET_ID)) {
			return new SectionTrackingManager();
		}
		else {
			return new ChunkTrackingManager();
		}
	}

	public static TrackingManager create(ServerPlayerEntity player) {
		if (ServerPlayNetworking.canSend(player, LoadRangePacket.PACKET_ID)) {
			return new SectionTrackingManager(player);
		}
		else {
			return new ChunkTrackingManager(player);
		}
	}

	public boolean otherSideHasVertigoInstalled() {
		return this instanceof SectionTrackingManager;
	}

	public static void tickAll(MinecraftServer server) {
		if (!PLAYERS.isEmpty()) {
			for (
				Iterator<Map.Entry<ServerPlayerEntity, TrackingManager>> iterator = PLAYERS.entrySet().iterator();
				iterator.hasNext();
			) {
				Map.Entry<ServerPlayerEntity, TrackingManager> entry = iterator.next();
				ServerPlayerEntity player = entry.getKey();
				TrackingManager manager = entry.getValue();
				if (player.isDisconnected()) {
					manager.clear();
					iterator.remove();
				}
				else {
					manager.update(player);
				}
			}
		}
	}

	public abstract boolean isLoaded(int sectionX, int sectionY, int sectionZ);

	public abstract @Nullable LoadedRange getLoadedRange(int chunkX, int chunkZ);

	@FunctionalInterface
	public static interface LoadedRange {

		public abstract boolean isLoaded(int sectionY);
	}

	public abstract void update(ServerPlayerEntity player);

	public abstract void onChunkLoaded(ServerPlayerEntity player, int chunkX, int chunkZ);

	public abstract void onChunkUnloaded(ServerPlayerEntity player, int chunkX, int chunkZ);

	@Environment(EnvType.CLIENT)
	public abstract void onChunkLoadedClient(WorldChunk chunk);

	@Environment(EnvType.CLIENT)
	public abstract void onChunkUnloadedClient(WorldChunk chunk);

	public abstract void onLightingChanged(BlockPos pos);

	public abstract void clear();
}