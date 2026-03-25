package builderb0y.vertigo;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import builderb0y.vertigo.networking.VertigoNetworking;

@Environment(EnvType.CLIENT)
public class VertigoClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		VertigoNetworking.initClient();
		ClientChunkEvents.CHUNK_LOAD.register((ClientLevel world, LevelChunk chunk) -> {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player == null) return;
			TrackingManager manager = TrackingManager.get(player);
			if (manager == null) return;
			manager.onChunkLoadedClient(chunk);
		});
		ClientChunkEvents.CHUNK_UNLOAD.register((ClientLevel world, LevelChunk chunk) -> {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player == null) return;
			TrackingManager manager = TrackingManager.get(player);
			if (manager == null) return;
			manager.onChunkUnloadedClient(chunk);
		});
	}
}