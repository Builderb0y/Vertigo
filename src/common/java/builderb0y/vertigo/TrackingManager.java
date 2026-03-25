package builderb0y.vertigo;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import builderb0y.vertigo.networking.LoadRangePacket;
import builderb0y.vertigo.networking.VertigoInstalledPacket;

public abstract class TrackingManager {

	static {
		ServerPlayerEvents.COPY_FROM.register((ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) -> {
			TrackingManager manager = TrackingManager.get(oldPlayer);
			if (manager != null) TrackingManager.set(newPlayer, manager);
		});
	}

	public static TrackingManager get(Player player) {
		return TrackingManagerHolder.of(player).vertigo_getTrackingManager();
	}

	public static TrackingManager getOrCreate(ServerPlayer player) {
		TrackingManager manager = get(player);
		if (manager == null) set(player, manager = create(player));
		return manager;
	}

	public static void set(Player player, TrackingManager manager) {
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

	public static TrackingManager create(ServerPlayer player) {
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

	public static void tickAll(ServerLevel world) {
		for (ServerPlayer player : world.players()) {
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

	public abstract void update(ServerPlayer player);

	public abstract void onChunkLoaded(ServerPlayer player, int chunkX, int chunkZ);

	public abstract void onChunkUnloaded(ServerPlayer player, int chunkX, int chunkZ);

	@Environment(EnvType.CLIENT)
	public abstract void onChunkLoadedClient(LevelChunk chunk);

	@Environment(EnvType.CLIENT)
	public abstract void onChunkUnloadedClient(LevelChunk chunk);

	public abstract void onLightingChanged(BlockPos pos);

	public abstract void clear();

	public static interface TrackingManagerHolder {

		public abstract @Nullable TrackingManager vertigo_getTrackingManager();

		public abstract void vertigo_setTrackingManager(TrackingManager trackingManager);

		public static TrackingManagerHolder of(Player player) {
			return (TrackingManagerHolder)(player);
		}
	}
}