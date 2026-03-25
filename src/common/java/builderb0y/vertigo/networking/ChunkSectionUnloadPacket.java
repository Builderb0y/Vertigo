package builderb0y.vertigo.networking;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import builderb0y.vertigo.VersionUtil;
import builderb0y.vertigo.Vertigo;
import builderb0y.vertigo.api.VertigoClientEvents;

public record ChunkSectionUnloadPacket(
	int sectionX,
	int sectionY,
	int sectionZ
)
	implements VertigoS2CPacket {

	public static final Identifier PACKET_ID = Vertigo.modID("section_unload");

	public static final StreamCodec<ByteBuf, ChunkSectionUnloadPacket> PACKET_CODEC = (
		StreamCodec.composite(
			ByteBufCodecs.INT, ChunkSectionUnloadPacket::sectionX,
			ByteBufCodecs.INT, ChunkSectionUnloadPacket::sectionY,
			ByteBufCodecs.INT, ChunkSectionUnloadPacket::sectionZ,
			ChunkSectionUnloadPacket::new
		)
	);

	public static final CustomPacketPayload.Type<ChunkSectionUnloadPacket> ID = new CustomPacketPayload.Type<>(PACKET_ID);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static void send(ServerPlayer player, int sectionX, int sectionY, int sectionZ) {
		ServerPlayNetworking.send(player, new ChunkSectionUnloadPacket(sectionX, sectionY, sectionZ));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process() {
		ClientLevel world = Minecraft.getInstance().level;
		if (world == null) return;
		LevelChunk chunk = (LevelChunk)(world.getChunk(this.sectionX, this.sectionZ, ChunkStatus.FULL, false));
		if (chunk == null) return;
		VertigoClientEvents.SECTION_UNLOADED.invoker().onSectionUnloaded(this.sectionX, this.sectionY, this.sectionZ);
		chunk.getSections()[chunk.getSectionIndexFromSectionY(this.sectionY)] = VersionUtil.newEmptyChunkSection(world.registryAccess());
		for (BlockPos pos : chunk.getBlockEntities().keySet().stream().filter((BlockPos pos) -> pos.getY() >> 4 == this.sectionY).toArray(BlockPos[]::new)) {
			chunk.removeBlockEntity(pos);
		}

		world.getChunkSource().storage.refreshEmptySections(chunk);

		world.setSectionDirtyWithNeighbors(this.sectionX, this.sectionY, this.sectionZ);
	}
}