package builderb0y.vertigo;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import builderb0y.vertigo.api.VertigoClientEvents;
import builderb0y.vertigo.api.VertigoServerEvents;

/**
used when only the current side (client or server)
has vertigo installed, and the other side doesn't.
*/
public class ChunkTrackingManager extends TrackingManager {

	public final LongOpenHashSet loadedChunks = new LongOpenHashSet(256);

	public ChunkTrackingManager() {
	}

	public ChunkTrackingManager(ServerPlayer player) {
	}

	@Override
	public void clear() {
		this.loadedChunks.clear();
	}

	@Override
	public boolean isLoaded(int sectionX, int sectionY, int sectionZ) {
		return this.loadedChunks.contains(ChunkPos.pack(sectionX, sectionZ));
	}

	@Override
	public @Nullable LoadedRange getLoadedRange(int chunkX, int chunkZ) {
		boolean loaded = this.loadedChunks.contains(ChunkPos.pack(chunkX, chunkZ));
		return loaded ? (int sectionY) -> true : null;
	}

	@Override
	public void update(ServerPlayer player) {
		//no-op.
	}

	@Override
	public void onChunkLoaded(ServerPlayer player, int chunkX, int chunkZ) {
		this.loadedChunks.add(ChunkPos.pack(chunkX, chunkZ));
		int minSection = VersionUtil.sectionMinYInclusive(VersionUtil.getWorld(player));
		int maxSection = VersionUtil.sectionMaxYExclusive(VersionUtil.getWorld(player));
		for (int sectionY = minSection; sectionY < maxSection; sectionY++) {
			VertigoServerEvents.SECTION_LOADED.invoker().onSectionLoaded(player, chunkX, sectionY, chunkZ);
		}
	}

	@Override
	public void onChunkUnloaded(ServerPlayer player, int chunkX, int chunkZ) {
		this.loadedChunks.remove(ChunkPos.pack(chunkX, chunkZ));
		int minSection = VersionUtil.sectionMinYInclusive(VersionUtil.getWorld(player));
		int maxSection = VersionUtil.sectionMaxYExclusive(VersionUtil.getWorld(player));
		for (int sectionY = minSection; sectionY < maxSection; sectionY++) {
			VertigoServerEvents.SECTION_UNLOADED.invoker().onSectionUnloaded(player, chunkX, sectionY, chunkZ);
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void onChunkLoadedClient(LevelChunk chunk) {
		this.loadedChunks.add(chunk.getPos().pack());
		int minSection = VersionUtil.sectionMinYInclusive(chunk);
		int maxSection = VersionUtil.sectionMaxYExclusive(chunk);
		for (int sectionY = minSection; sectionY < maxSection; sectionY++) {
			VertigoClientEvents.SECTION_LOADED.invoker().onSectionLoaded(chunk.getPos().x(), sectionY, chunk.getPos().z());
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void onChunkUnloadedClient(LevelChunk chunk) {
		this.loadedChunks.remove(chunk.getPos().pack());
		int minSection = VersionUtil.sectionMinYInclusive(chunk);
		int maxSection = VersionUtil.sectionMaxYExclusive(chunk);
		for (int sectionY = minSection; sectionY < maxSection; sectionY++) {
			VertigoClientEvents.SECTION_UNLOADED.invoker().onSectionUnloaded(chunk.getPos().x(), sectionY, chunk.getPos().z());
		}
	}

	@Override
	public void onLightingChanged(BlockPos pos) {
		//no-op.
	}
}