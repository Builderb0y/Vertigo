package builderb0y.vertigo.networking;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import builderb0y.vertigo.SectionTrackingManager;
import builderb0y.vertigo.SectionTrackingManager.ChunkState;
import builderb0y.vertigo.TrackingManager;
import builderb0y.vertigo.Vertigo;
import builderb0y.vertigo.api.VertigoClientEvents;

public record LoadRangePacket(
	int chunkX,
	int chunkZ,
	int minY,
	int maxY
)
	implements VertigoS2CPacket {

	public static final Identifier PACKET_ID = Vertigo.modID("load_range");

	public static final StreamCodec<ByteBuf, LoadRangePacket> PACKET_CODEC = (
		StreamCodec.composite(
			ByteBufCodecs.INT, LoadRangePacket::chunkX,
			ByteBufCodecs.INT, LoadRangePacket::chunkZ,
			ByteBufCodecs.INT, LoadRangePacket::minY,
			ByteBufCodecs.INT, LoadRangePacket::maxY,
			LoadRangePacket::new
		)
	);

	public static final CustomPacketPayload.Type<LoadRangePacket> ID = new CustomPacketPayload.Type<>(PACKET_ID);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static void send(ServerPlayer player, int chunkX, int chunkZ, int minY, int maxY) {
		ServerPlayNetworking.send(player, new LoadRangePacket(chunkX, chunkZ, minY, maxY));
	}

	public static void sendUnload(ServerPlayer player, int chunkX, int chunkZ) {
		ServerPlayNetworking.send(player, new LoadRangePacket(chunkX, chunkZ, 0, -1));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;
		if (!(TrackingManager.get(player) instanceof SectionTrackingManager manager)) return;
		long chunkPos = ChunkPos.pack(this.chunkX, this.chunkZ);
		if (this.maxY >= this.minY) {
			ChunkState bound = manager.chunkBounds.get(chunkPos);
			if (bound != null) {
				bound.minY = this.minY;
				bound.maxY = this.maxY;
				//firing of events happens from SectionLoad/UnloadPacket in this case.
			}
			else {
				bound = new ChunkState();
				bound.minY = this.minY;
				bound.maxY = this.maxY;
				manager.chunkBounds.put(chunkPos, bound);
				for (int sectionY = bound.minY; sectionY <= bound.maxY; sectionY++) {
					VertigoClientEvents.SECTION_LOADED.invoker().onSectionLoaded(this.chunkX, sectionY, this.chunkZ);
				}
			}
		}
		else {
			manager.chunkBounds.remove(chunkPos);
		}
	}
}