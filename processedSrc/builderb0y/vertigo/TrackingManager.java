package builderb0y.vertigo;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import builderb0y.vertigo.networking.LoadRangePacket;
import builderb0y.vertigo.networking.VertigoInstalledPacket;

public abstract class TrackingManager {

	static {
		ServerPlayerEvents.COPY_FROM.register((ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) -> {
			TrackingManager manager = TrackingManager.get(oldPlayer);
			if (manager != null) TrackingManager.set(newPlayer, manager);
		});
	}

	public static TrackingManager get(PlayerEntity player) {
		return TrackingManagerHolder.of(player).vertigo_getTrackingManager();
	}

	public static TrackingManager getOrCreate(ServerPlayerEntity player) {
		TrackingManager manager = get(player);
		if (manager == null) set(player, manager = create(player));
		return manager;
	}

	public static void set(PlayerEntity player, TrackingManager manager) {
		TrackingManagerHolder.of(player).vertigo_setTrackingManager(manager);
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

	public static void tickAll(ServerWorld world) {
		for (ServerPlayerEntity player : world.getPlayers()) {
			TrackingManager manager = TrackingManager.get(player);
			if (manager != null) manager.update(player);
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

	public static interface TrackingManagerHolder {

		public abstract @Nullable TrackingManager vertigo_getTrackingManager();

		public abstract void vertigo_setTrackingManager(TrackingManager trackingManager);

		public static TrackingManagerHolder of(PlayerEntity player) {
			return (TrackingManagerHolder)(player);
		}
	}
}