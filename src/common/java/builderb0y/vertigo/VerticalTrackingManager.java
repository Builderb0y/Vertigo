package builderb0y.vertigo;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents.AfterClientWorldChange;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import builderb0y.vertigo.api.VertigoServerEvents;
import builderb0y.vertigo.networking.ChunkSectionLoadPacket;
import builderb0y.vertigo.networking.ChunkSectionUnloadPacket;
import builderb0y.vertigo.networking.LoadRangePacket;
import builderb0y.vertigo.networking.SkylightUpdatePacket;

public class VerticalTrackingManager {

	/** used on the logical server to refer to every player on the server. */
	public static final WeakHashMap<ServerPlayerEntity, VerticalTrackingManager> PLAYERS = new WeakHashMap<>();
	/** used on the logical client to refer to the current player. */
	public static VerticalTrackingManager CLIENT;

	static {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register(new AfterClientWorldChange() {

				@Override
				public void afterWorldChange(MinecraftClient client, ClientWorld world) {
					CLIENT = world != null ? new VerticalTrackingManager() : null;
				}
			});
		}
	}

	/** all chunks currently being tracked. */
	public final Long2ObjectOpenHashMap<ChunkState> chunkBounds = new Long2ObjectOpenHashMap<>();
	public final LongOpenHashSet skylightUpdates = new LongOpenHashSet();
	public int previousSectionY, previousViewDistance;

	public VerticalTrackingManager(ServerPlayerEntity player) {
		this.previousSectionY = player.getBlockY() >> 4;
		this.previousViewDistance = player.getViewDistance();
	}

	public VerticalTrackingManager() {}

	public boolean isLoaded(int sectionX, int sectionY, int sectionZ) {
		ChunkState bound = this.chunkBounds.get(ChunkPos.toLong(sectionX, sectionZ));
		return bound != null && bound.contains(sectionY);
	}

	public static void tickAll(MinecraftServer server) {
		if (!PLAYERS.isEmpty()) {
			for (
				Iterator<Map.Entry<ServerPlayerEntity, VerticalTrackingManager>> iterator = PLAYERS.entrySet().iterator();
				iterator.hasNext();
			) {
				Map.Entry<ServerPlayerEntity, VerticalTrackingManager> entry = iterator.next();
				ServerPlayerEntity player = entry.getKey();
				VerticalTrackingManager manager = entry.getValue();
				if (player.isDisconnected()) {
					manager.chunkBounds.clear();
					iterator.remove();
				}
				else {
					if (manager.previousSectionY != player.getBlockY() >> 4 || manager.previousViewDistance != player.getViewDistance()) {
						manager.update(player);
						manager.previousSectionY = player.getBlockY() >> 4;
						manager.previousViewDistance = player.getViewDistance();
					}
					if (!manager.skylightUpdates.isEmpty()) {
						manager.updateLight(player);
						manager.skylightUpdates.clear();
					}
				}
			}
		}
	}

	public void update(ServerPlayerEntity player) {
		int playerCenterY = player.getBlockY() >> 4;
		int range = player.getViewDistance();
		for (Iterator<Long2ObjectMap.Entry<ChunkState>> iterator = this.chunkBounds.long2ObjectEntrySet().iterator(); iterator.hasNext(); ) {
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
				bound.minY = playerCenterY - range;
				bound.maxY = playerCenterY + range;
				for (int sectionY = bound.minY; sectionY <= bound.maxY; sectionY++) {
					ChunkSectionLoadPacket.send(player, chunk, bound.maxY);
					VertigoServerEvents.SECTION_LOADED.invoker().onSectionLoaded(player, chunkX, bound.maxY, chunkZ);
				}
				changed = true;
			}
			else {
				while (bound.maxY < Math.min(playerCenterY + range, chunk.getTopSectionCoord())) {
					bound.maxY++;
					ChunkSectionLoadPacket.send(player, chunk, bound.maxY);
					VertigoServerEvents.SECTION_LOADED.invoker().onSectionLoaded(player, chunkX, bound.maxY, chunkZ);
					changed = true;
				}
				while (bound.minY > Math.max(playerCenterY - range, chunk.getBottomSectionCoord())) {
					bound.minY--;
					ChunkSectionLoadPacket.send(player, chunk, bound.minY);
					changed = true;
					VertigoServerEvents.SECTION_LOADED.invoker().onSectionLoaded(player, chunkX, bound.maxY, chunkZ);
				}
				while (bound.maxY > playerCenterY + range + 1) {
					VertigoServerEvents.SECTION_UNLOADED.invoker().onSectionUnloaded(player, chunkX, bound.maxY, chunkZ);
					ChunkSectionUnloadPacket.send(player, chunkX, bound.maxY, chunkZ);
					bound.maxY--;
					changed = true;
				}
				while (bound.minY < playerCenterY - range - 1) {
					VertigoServerEvents.SECTION_UNLOADED.invoker().onSectionUnloaded(player, chunkX, bound.maxY, chunkZ);
					ChunkSectionUnloadPacket.send(player, chunkX, bound.minY, chunkZ);
					bound.minY++;
					changed = true;
				}
			}
			if (changed) {
				LoadRangePacket.send(player, chunk.getPos().x, chunk.getPos().z, bound.minY, bound.maxY);
				player.networkHandler.sendPacket(
					new LightUpdateS2CPacket(
						chunk.getPos(),
						player.getServerWorld().getLightingProvider(),
						null,
						null
					)
				);
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

	public void onChunkLoaded(ServerPlayerEntity player, int chunkX, int chunkZ) {
		ChunkState bound = this.chunkBounds.computeIfAbsent(ChunkPos.toLong(chunkX, chunkZ), (long packedPos) -> new ChunkState());
		int playerCenterY = player.getBlockY() >> 4;
		bound.minY = Math.max(playerCenterY - player.getViewDistance(), player.getWorld().getBottomSectionCoord());
		bound.maxY = Math.min(playerCenterY + player.getViewDistance(), player.getWorld().getTopSectionCoord());
		LoadRangePacket.send(player, chunkX, chunkZ, bound.minY, bound.maxY);
		for (int sectionY = bound.minY; sectionY <= bound.maxY; sectionY++) {
			VertigoServerEvents.SECTION_LOADED.invoker().onSectionLoaded(player, chunkX, sectionY, chunkZ);
		}
	}

	public void onChunkUnloaded(ServerPlayerEntity player, int chunkX, int chunkZ) {
		ChunkState bounds = this.chunkBounds.remove(ChunkPos.toLong(chunkX, chunkZ));
		if (bounds != null) {
			LoadRangePacket.sendUnload(player, chunkX, chunkZ);
			for (int sectionY = bounds.minY; sectionY <= bounds.maxY; sectionY++) {
				VertigoServerEvents.SECTION_UNLOADED.invoker().onSectionUnloaded(player, chunkX, sectionY, chunkZ);
			}
		}
	}

	public void onLightingChanged(BlockPos pos) {
		long chunkPos = ChunkPos.toLong(pos);
		ChunkState info = this.chunkBounds.get(chunkPos);
		if (info != null) {
			this.skylightUpdates.add(chunkPos);
			info.skylightMask.set(((pos.getZ() & 15) << 4) | (pos.getX() & 15));
		}
	}

	/** information about the chunk that a player sees. */
	public static class ChunkState {

		/** both inclusive; measured in sections, not blocks. */
		public int minY, maxY;
		/** indices where the skylight heightmap has changed since the previous tick. */
		public final BitSet skylightMask = new BitSet(256);

		public boolean contains(int sectionY) {
			return sectionY >= this.minY && sectionY <= this.maxY;
		}
	}
}