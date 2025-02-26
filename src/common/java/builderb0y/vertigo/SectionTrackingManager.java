package builderb0y.vertigo;

import java.util.BitSet;
import java.util.Iterator;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import builderb0y.vertigo.api.VertigoClientEvents;
import builderb0y.vertigo.api.VertigoServerEvents;
import builderb0y.vertigo.compat.ScalableLuxCompat;
import builderb0y.vertigo.networking.ChunkSectionLoadPacket;
import builderb0y.vertigo.networking.ChunkSectionUnloadPacket;
import builderb0y.vertigo.networking.LoadRangePacket;
import builderb0y.vertigo.networking.SkylightUpdatePacket;

/** used when both sides (client and server) have vertigo installed. */
public class SectionTrackingManager extends TrackingManager {

	/** all chunks currently being tracked. */
	public final Long2ObjectOpenHashMap<ChunkState> chunkBounds = new Long2ObjectOpenHashMap<>(256);
	public final LongOpenHashSet skylightUpdates = new LongOpenHashSet();
	public int previousSectionY, previousViewDistance;

	public SectionTrackingManager() {}

	public SectionTrackingManager(ServerPlayerEntity player) {
		this.previousSectionY = player.getBlockY() >> 4;
		this.previousViewDistance = VersionUtil.getViewDistance(player);
	}

	@Override
	public boolean isLoaded(int sectionX, int sectionY, int sectionZ) {
		ChunkState bound = this.chunkBounds.get(ChunkPos.toLong(sectionX, sectionZ));
		return bound != null && bound.isLoaded(sectionY);
	}

	@Override
	public @Nullable LoadedRange getLoadedRange(int chunkX, int chunkZ) {
		return this.chunkBounds.get(ChunkPos.toLong(chunkX, chunkZ));
	}

	@Override
	public void update(ServerPlayerEntity player) {
		if (this.previousSectionY != player.getBlockY() >> 4 || this.previousViewDistance != VersionUtil.getViewDistance(player)) {
			this.doUpdate(player);
			this.previousSectionY = player.getBlockY() >> 4;
			this.previousViewDistance = VersionUtil.getViewDistance(player);
		}
		if (!this.skylightUpdates.isEmpty()) {
			this.updateLight(player);
			this.skylightUpdates.clear();
		}
	}

	public void doUpdate(ServerPlayerEntity player) {
		int playerCenterY = player.getBlockY() >> 4;
		int range = VersionUtil.getViewDistance(player);
		for (Iterator<Long2ObjectMap.Entry<ChunkState>> iterator = this.chunkBounds.long2ObjectEntrySet().iterator(); iterator.hasNext();) {
			Long2ObjectMap.Entry<ChunkState> entry = iterator.next();
			int chunkX = ChunkPos.getPackedX(entry.getLongKey());
			int chunkZ = ChunkPos.getPackedZ(entry.getLongKey());
			WorldChunk chunk = (WorldChunk)(player.getServerWorld().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false));
			if (chunk == null) { //???
				iterator.remove();
				continue;
			}
			boolean changed = false;
			ChunkState bound = entry.getValue();
			if (bound.maxY < playerCenterY - range || bound.minY > playerCenterY + range) {
				//player teleported a long distance vertically. use special logic for them.
				for (int sectionY = bound.minY; sectionY <= bound.maxY; sectionY++) {
					VertigoServerEvents.SECTION_UNLOADED.invoker().onSectionUnloaded(player, chunkX, bound.maxY, chunkZ);
					ChunkSectionUnloadPacket.send(player, chunkX, bound.maxY, chunkZ);
				}
				bound.minY = Math.max(playerCenterY - range, VersionUtil.sectionMinYInclusive(chunk));
				bound.maxY = Math.min(playerCenterY + range, VersionUtil.sectionMaxYInclusive(chunk));
				for (int sectionY = bound.minY; sectionY <= bound.maxY; sectionY++) {
					ChunkSectionLoadPacket.send(player, chunk, sectionY);
					VertigoServerEvents.SECTION_LOADED.invoker().onSectionLoaded(player, chunkX, bound.maxY, chunkZ);
				}
				changed = true;
			}
			else {
				while (bound.maxY < Math.min(playerCenterY + range, VersionUtil.sectionMaxYInclusive(chunk))) {
					bound.maxY++;
					ChunkSectionLoadPacket.send(player, chunk, bound.maxY);
					VertigoServerEvents.SECTION_LOADED.invoker().onSectionLoaded(player, chunkX, bound.maxY, chunkZ);
					changed = true;
				}
				while (bound.minY > Math.max(playerCenterY - range, VersionUtil.sectionMinYInclusive(chunk))) {
					bound.minY--;
					ChunkSectionLoadPacket.send(player, chunk, bound.minY);
					VertigoServerEvents.SECTION_LOADED.invoker().onSectionLoaded(player, chunkX, bound.minY, chunkZ);
					changed = true;
				}
				while (bound.maxY > playerCenterY + range + 1) {
					VertigoServerEvents.SECTION_UNLOADED.invoker().onSectionUnloaded(player, chunkX, bound.maxY, chunkZ);
					ChunkSectionUnloadPacket.send(player, chunkX, bound.maxY, chunkZ);
					bound.maxY--;
					changed = true;
				}
				while (bound.minY < playerCenterY - range - 1) {
					VertigoServerEvents.SECTION_UNLOADED.invoker().onSectionUnloaded(player, chunkX, bound.minY, chunkZ);
					ChunkSectionUnloadPacket.send(player, chunkX, bound.minY, chunkZ);
					bound.minY++;
					changed = true;
				}
			}
			if (changed) {
				LoadRangePacket.send(player, chunk.getPos().x, chunk.getPos().z, bound.minY, bound.maxY);
			}
		}
	}

	public void updateLight(ServerPlayerEntity player) {
		for (
			LongIterator iterator = this.skylightUpdates.longIterator();
			iterator.hasNext();
		) {
			long chunkPos = iterator.nextLong();
			ChunkState info = this.chunkBounds.get(chunkPos);
			if (info != null) {
				int chunkX = ChunkPos.getPackedX(chunkPos);
				int chunkZ = ChunkPos.getPackedZ(chunkPos);
				SkylightUpdatePacket.send(player, chunkX, chunkZ, info.skylightMask);
				info.skylightMask.clear();
			}
		}
	}

	@Override
	public void onDisconnect() {
		this.chunkBounds.clear();
	}

	@Override
	public void onChunkLoaded(ServerPlayerEntity player, int chunkX, int chunkZ) {
		ChunkState bound = this.chunkBounds.computeIfAbsent(ChunkPos.toLong(chunkX, chunkZ), (long packedPos) -> new ChunkState());
		int playerCenterY = player.getBlockY() >> 4;
		bound.minY = Math.max(playerCenterY - VersionUtil.getViewDistance(player), VersionUtil.sectionMinYInclusive(player.getWorld()));
		bound.maxY = Math.min(playerCenterY + VersionUtil.getViewDistance(player), VersionUtil.sectionMaxYInclusive(player.getWorld()));
		LoadRangePacket.send(player, chunkX, chunkZ, bound.minY, bound.maxY);
		BitSet everywhere = new BitSet(256);
		everywhere.set(0, 256);
		SkylightUpdatePacket.send(player, chunkX, chunkZ, everywhere);
		for (int sectionY = bound.minY; sectionY <= bound.maxY; sectionY++) {
			VertigoServerEvents.SECTION_LOADED.invoker().onSectionLoaded(player, chunkX, sectionY, chunkZ);
		}
	}

	@Override
	public void onChunkUnloaded(ServerPlayerEntity player, int chunkX, int chunkZ) {
		ChunkState bounds = this.chunkBounds.remove(ChunkPos.toLong(chunkX, chunkZ));
		if (bounds != null) {
			LoadRangePacket.sendUnload(player, chunkX, chunkZ);
			for (int sectionY = bounds.minY; sectionY <= bounds.maxY; sectionY++) {
				VertigoServerEvents.SECTION_UNLOADED.invoker().onSectionUnloaded(player, chunkX, sectionY, chunkZ);
			}
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void onChunkLoadedClient(WorldChunk chunk) {
		//no-op; wait for LoadRangePacket to arrive.
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void onChunkUnloadedClient(WorldChunk chunk) {
		ChunkPos chunkPos = chunk.getPos();
		ChunkState state = this.chunkBounds.remove(chunkPos.toLong());
		if (state != null) {
			for (int sectionY = state.minY; sectionY <= state.maxY; sectionY++) {
				VertigoClientEvents.SECTION_UNLOADED.invoker().onSectionUnloaded(chunkPos.x, sectionY, chunkPos.z);
			}
		}
	}

	@Override
	public void onLightingChanged(BlockPos pos) {
		long chunkPos = ChunkPos.toLong(pos);
		ChunkState info = this.chunkBounds.get(chunkPos);
		if (info != null) {
			this.skylightUpdates.add(chunkPos);
			info.skylightMask.set(((pos.getZ() & 15) << 4) | (pos.getX() & 15));
		}
	}

	/** information about the chunk that a player sees. */
	public static class ChunkState implements LoadedRange {

		/** both inclusive; measured in sections, not blocks. */
		public int minY, maxY;
		/** indices where the skylight heightmap has changed since the previous tick. */
		public final BitSet skylightMask = new BitSet(256);

		@Override
		public boolean isLoaded(int sectionY) {
			return sectionY >= this.minY && sectionY <= this.maxY;
		}
	}
}