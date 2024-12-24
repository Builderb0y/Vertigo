package builderb0y.vertigo;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.chunk.WorldChunk;

import builderb0y.vertigo.networking.VertigoNetworking;

@Environment(EnvType.CLIENT)
public class VertigoClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		VertigoNetworking.initClient();
		ClientChunkEvents.CHUNK_LOAD.register((ClientWorld world, WorldChunk chunk) -> TrackingManager.CLIENT.onChunkLoadedClient(chunk));
		ClientChunkEvents.CHUNK_UNLOAD.register((ClientWorld world, WorldChunk chunk) -> TrackingManager.CLIENT.onChunkUnloadedClient(chunk));
	}
}