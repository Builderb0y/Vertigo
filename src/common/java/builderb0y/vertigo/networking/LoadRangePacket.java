package builderb0y.vertigo.networking;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;

import builderb0y.vertigo.Vertigo;
import builderb0y.vertigo.VerticalTrackingManager;
import builderb0y.vertigo.VerticalTrackingManager.ChunkState;
import builderb0y.vertigo.VertigoClient;
import builderb0y.vertigo.api.VertigoClientEvents;

#if MC_VERSION >= MC_1_20_5
	import net.minecraft.network.codec.PacketCodec;
	import net.minecraft.network.codec.PacketCodecs;
#else
	import net.fabricmc.fabric.api.networking.v1.PacketType;
#endif

public record LoadRangePacket(
	int chunkX,
	int chunkZ,
	int minY,
	int maxY
)
implements VertigoPacket {

	#if MC_VERSION >= MC_1_20_5

		public static final PacketCodec<ByteBuf, LoadRangePacket> PACKET_CODEC = (
			PacketCodec.tuple(
				PacketCodecs.INTEGER, LoadRangePacket::chunkX,
				PacketCodecs.INTEGER, LoadRangePacket::chunkZ,
				PacketCodecs.INTEGER, LoadRangePacket::minY,
				PacketCodecs.INTEGER, LoadRangePacket::maxY,
				LoadRangePacket::new
			)
		);

		public static final CustomPayload.Id<LoadRangePacket> ID = new CustomPayload.Id<>(Vertigo.modID("load_range"));

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

	#else

		public static final PacketType<LoadRangePacket> TYPE = PacketType.create(Vertigo.modID("load_range"), LoadRangePacket::read);

		public static LoadRangePacket read(PacketByteBuf buffer) {
			return new LoadRangePacket(
				buffer.readInt(),
				buffer.readInt(),
				buffer.readInt(),
				buffer.readInt()
			);
		}

		@Override
		public void write(PacketByteBuf buffer) {
			buffer
			.writeInt(this.chunkX)
			.writeInt(this.chunkZ)
			.writeInt(this.minY)
			.writeInt(this.maxY);
		}

		@Override
		public PacketType<?> getType() {
			return TYPE;
		}

	#endif

	public static void send(ServerPlayerEntity player, int chunkX, int chunkZ, int minY, int maxY) {
		ServerPlayNetworking.send(player, new LoadRangePacket(chunkX, chunkZ, minY, maxY));
	}

	public static void sendUnload(ServerPlayerEntity player, int chunkX, int chunkZ) {
		ServerPlayNetworking.send(player, new LoadRangePacket(chunkX, chunkZ, 0, -1));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process() {
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