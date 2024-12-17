package builderb0y.vertigo.networking;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;

import builderb0y.vertigo.Vertigo;
import builderb0y.vertigo.VerticalTrackingManager;
import builderb0y.vertigo.VerticalTrackingManager.ChunkState;
import builderb0y.vertigo.VertigoClient;
import builderb0y.vertigo.api.VertigoClientEvents;

public record LoadRangePacket(
	int chunkX,
	int chunkZ,
	int minY,
	int maxY
)
implements CustomPayload {

	public static final PacketCodec<ByteBuf, LoadRangePacket> PACKET_CODEC = (
		PacketCodec.tuple(
			PacketCodecs.INTEGER, LoadRangePacket::chunkX,
			PacketCodecs.INTEGER, LoadRangePacket::chunkZ,
			PacketCodecs.INTEGER, LoadRangePacket::minY,
			PacketCodecs.INTEGER, LoadRangePacket::maxY,
			LoadRangePacket::new
		)
	);

	public static final Id<LoadRangePacket> ID = new Id<>(Vertigo.modID("load_range"));

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}

	public static void send(ServerPlayerEntity player, int chunkX, int chunkZ, int minY, int maxY) {
		ServerPlayNetworking.send(player, new LoadRangePacket(chunkX, chunkZ, minY, maxY));
	}

	public static void sendUnload(ServerPlayerEntity player, int chunkX, int chunkZ) {
		ServerPlayNetworking.send(player, new LoadRangePacket(chunkX, chunkZ, 0, -1));
	}

	@Environment(EnvType.CLIENT)
	public void process(ClientPlayNetworking.Context context) {
		long chunkPos = ChunkPos.toLong(this.chunkX, this.chunkZ);
		if (this.maxY >= this.minY) {
			ChunkState bound = VerticalTrackingManager.CLIENT.chunkBounds.get(chunkPos);
			if (bound != null) {
				bound.minY = this.minY;
				bound.maxY = this.maxY;
			}
			else {
				bound = new ChunkState();
				bound.minY = this.minY;
				bound.maxY = this.maxY;
				VerticalTrackingManager.CLIENT.chunkBounds.put(chunkPos, bound);
				for (int sectionY = bound.minY; sectionY <= bound.maxY; sectionY++) {
					VertigoClientEvents.SECTION_LOADED.invoker().onSectionLoaded(this.chunkX, sectionY, this.chunkZ);
				}
			}
		}
		else {
			VerticalTrackingManager.CLIENT.chunkBounds.remove(chunkPos);
		}
	}
}