package builderb0y.vertigo.networking;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

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

	public static final PacketCodec<ByteBuf, ChunkSectionUnloadPacket> PACKET_CODEC = (
		PacketCodec.tuple(
			PacketCodecs.INTEGER, ChunkSectionUnloadPacket::sectionX,
			PacketCodecs.INTEGER, ChunkSectionUnloadPacket::sectionY,
			PacketCodecs.INTEGER, ChunkSectionUnloadPacket::sectionZ,
			ChunkSectionUnloadPacket::new
		)
	);

	public static final CustomPayload.Id<ChunkSectionUnloadPacket> ID = new CustomPayload.Id<>(PACKET_ID);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}

	public static void send(ServerPlayerEntity player, int sectionX, int sectionY, int sectionZ) {
		ServerPlayNetworking.send(player, new ChunkSectionUnloadPacket(sectionX, sectionY, sectionZ));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process() {
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world == null) return;
		WorldChunk chunk = (WorldChunk)(world.getChunk(this.sectionX, this.sectionZ, ChunkStatus.FULL, false));
		if (chunk == null) return;
		VertigoClientEvents.SECTION_UNLOADED.invoker().onSectionUnloaded(this.sectionX, this.sectionY, this.sectionZ);
		chunk.getSectionArray()[chunk.sectionCoordToIndex(this.sectionY)] = VersionUtil.newEmptyChunkSection(world.getRegistryManager());
		for (BlockPos pos : chunk.getBlockEntities().keySet().stream().filter((BlockPos pos) -> pos.getY() >> 4 == this.sectionY).toArray(BlockPos[]::new)) {
			chunk.removeBlockEntity(pos);
		}

		world.getChunkManager().chunks.refreshSections(chunk);

		world.scheduleBlockRenders(this.sectionX, this.sectionY, this.sectionZ);
	}
}