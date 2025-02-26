package builderb0y.vertigo;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import builderb0y.vertigo.api.VertigoClientEvents;
import builderb0y.vertigo.api.VertigoServerEvents;

/**
used when only the current side (client or server)
has vertigo installed, and the other side doesn't.
*/
public class ChunkTrackingManager extends TrackingManager {

	public final LongOpenHashSet loadedChunks = new LongOpenHashSet(256);

	public ChunkTrackingManager() {}

	public ChunkTrackingManager(ServerPlayerEntity player) {}

	@Override
	public boolean isLoaded(int sectionX, int sectionY, int sectionZ) {
		return this.loadedChunks.contains(ChunkPos.toLong(sectionX, sectionZ));
	}

	@Override
	public @Nullable LoadedRange getLoadedRange(int chunkX, int chunkZ) {
		boolean loaded = this.loadedChunks.contains(ChunkPos.toLong(chunkX, chunkZ));
		return loaded ? (int sectionY) -> true : null;
	}

	@Override
	public void update(ServerPlayerEntity player) {
		//no-op.
	}

	@Override
	public void onDisconnect() {
		this.loadedChunks.clear();
	}

	@Override
	public void onChunkLoaded(ServerPlayerEntity player, int chunkX, int chunkZ) {
		this.loadedChunks.add(ChunkPos.toLong(chunkX, chunkZ));
		int minSection = VersionUtil.sectionMinYInclusive(player.getWorld());
		int maxSection = VersionUtil.sectionMaxYExclusive(player.getWorld());
		for (int sectionY = minSection; sectionY < maxSection; sectionY++) {
			VertigoServerEvents.SECTION_LOADED.invoker().onSectionLoaded(player, chunkX, sectionY, chunkZ);
		}
	}

	@Override
	public void onChunkUnloaded(ServerPlayerEntity player, int chunkX, int chunkZ) {
		this.loadedChunks.remove(ChunkPos.toLong(chunkX, chunkZ));
		int minSection = VersionUtil.sectionMinYInclusive(player.getWorld());
		int maxSection = VersionUtil.sectionMaxYExclusive(player.getWorld());
		for (int sectionY = minSection; sectionY < maxSection; sectionY++) {
			VertigoServerEvents.SECTION_UNLOADED.invoker().onSectionUnloaded(player, chunkX, sectionY, chunkZ);
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void onChunkLoadedClient(WorldChunk chunk) {
		this.loadedChunks.add(chunk.getPos().toLong());
		int minSection = VersionUtil.sectionMinYInclusive(chunk);
		int maxSection = VersionUtil.sectionMaxYExclusive(chunk);
		for (int sectionY = minSection; sectionY < maxSection; sectionY++) {
			VertigoClientEvents.SECTION_LOADED.invoker().onSectionLoaded(chunk.getPos().x, sectionY, chunk.getPos().z);
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void onChunkUnloadedClient(WorldChunk chunk) {
		this.loadedChunks.remove(chunk.getPos().toLong());
		int minSection = VersionUtil.sectionMinYInclusive(chunk);
		int maxSection = VersionUtil.sectionMaxYExclusive(chunk);
		for (int sectionY = minSection; sectionY < maxSection; sectionY++) {
			VertigoClientEvents.SECTION_UNLOADED.invoker().onSectionUnloaded(chunk.getPos().x, sectionY, chunk.getPos().z);
		}
	}

	@Override
	public void onLightingChanged(BlockPos pos) {
		//no-op.
	}
}