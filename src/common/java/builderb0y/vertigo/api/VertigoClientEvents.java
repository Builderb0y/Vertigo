package builderb0y.vertigo.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import builderb0y.vertigo.VerticalTrackingManager;
import builderb0y.vertigo.VerticalTrackingManager.ChunkState;

public class VertigoClientEvents {

	/**
	called immediately AFTER the client loads one or more new sections.

	if the player moved horizontally and triggered a new chunk to load and be sent,
	then this event will be called for every section in the chunk which was synced to the player.
	this event will NOT be called for sections that are too far above or below the player.

	if the player moved vertically and triggered new sections to be sent,
	then this event will be called for every section which got loaded in this way.

	get the world from {@link MinecraftClient#world}.

	this event is called on the render thread.
	*/
	public static final Event<Load> SECTION_LOADED = EventFactory.createArrayBacked(
		Load.class,
		(Load[] events) -> {
			return (int sectionX, int sectionY, int sectionZ) -> {
				for (Load event : events) {
					event.onSectionLoaded(sectionX, sectionY, sectionZ);
				}
			};
		}
	);

	/**
	called immediately BEFORE the client processes a packet to unload a chunk section.

	if the player moved horizontally and triggered an old chunk to be unloaded,
	then this event will be called for every section in the chunk which was previously synced to the player.
	this event will NOT be called for sections that were too far above or below the player to be synced.

	if the player moved vertically and triggered sections to unload,
	then this event will be called for every section which got unloaded in this way.

	get the world from {@link MinecraftClient#world}.

	this event is called on the render thread.
	*/
	public static final Event<Unload> SECTION_UNLOADED = EventFactory.createArrayBacked(
		Unload.class,
		(Unload[] events) -> {
			return (int sectionX, int sectionY, int sectionZ) -> {
				for (Unload event : events) {
					event.onSectionUnloaded(sectionX, sectionY, sectionZ);
				}
			};
		}
	);

	static {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			//can't use lambdas due to class loading issues on dedicated servers.
			ClientChunkEvents.CHUNK_UNLOAD.register(new ClientChunkEvents.Unload() {

				@Override
				public void onChunkUnload(ClientWorld world, WorldChunk chunk) {
					ChunkPos chunkPos = chunk.getPos();
					ChunkState state = VerticalTrackingManager.CLIENT.chunkBounds.remove(chunkPos.toLong());
					if (state != null) {
						for (int sectionY = state.minY; sectionY <= state.maxY; sectionY++) {
							SECTION_UNLOADED.invoker().onSectionUnloaded(chunkPos.x, sectionY, chunkPos.z);
						}
					}
				}
			});
		}
	}

	public static interface Load {

		public abstract void onSectionLoaded(int sectionX, int sectionY, int sectionZ);
	}

	public static interface Unload {

		public abstract void onSectionUnloaded(int sectionX, int sectionY, int sectionZ);
	}
}