package builderb0y.vertigo.networking;

import java.util.List;
import java.util.Optional;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WorldChunk.CreationType;

import builderb0y.vertigo.Vertigo;
import builderb0y.vertigo.api.VertigoClientEvents;

/**
mostly a modified version of {@link ChunkDataS2CPacket} and
{@link ChunkData} which works for single sections instead of whole chunks.
*/
public record ChunkSectionLoadPacket(
	int sectionX,
	int sectionY,
	int sectionZ,
	byte[] sectionData,
	Optional<byte[]> skylightData,
	List<BlockEntityData> blockEntities
)
implements CustomPayload {

	public static final PacketCodec<RegistryByteBuf, ChunkSectionLoadPacket> PACKET_CODEC = (
		PacketCodec.tuple(
			PacketCodecs.INTEGER,                                                      ChunkSectionLoadPacket::sectionX,
			PacketCodecs.INTEGER,                                                      ChunkSectionLoadPacket::sectionY,
			PacketCodecs.INTEGER,                                                      ChunkSectionLoadPacket::sectionZ,
			PacketCodecs.BYTE_ARRAY,                                                   ChunkSectionLoadPacket::sectionData,
			PacketCodecs.optional(VertigoNetworking.fixedSizeByteArray(2048)), ChunkSectionLoadPacket::skylightData,
			BlockEntityData.PACKET_CODEC.collect(PacketCodecs.toList(4096)),           ChunkSectionLoadPacket::blockEntities,
			ChunkSectionLoadPacket::new
		)
	);
	public static final Id<ChunkSectionLoadPacket> ID = new Id<>(Vertigo.modID("section_data"));

	public static void send(ServerPlayerEntity player, WorldChunk chunk, int sectionY) {
		int sectionX = chunk.getPos().x;
		int sectionZ = chunk.getPos().z;
		ChunkSection section = chunk.getSection(chunk.sectionCoordToIndex(sectionY));
		int bytes = section.getPacketSize();
		byte[] sectionData = new byte[bytes];
		ByteBuf buffer = Unpooled.wrappedBuffer(sectionData);
		buffer.writerIndex(0);
		section.toPacket(new PacketByteBuf(buffer));
		List<BlockEntityData> blockEntities = (
			chunk
			.getBlockEntities()
			.values()
			.stream()
			.filter((BlockEntity blockEntity) -> blockEntity.getPos().getY() >> 4 == sectionY)
			.map(BlockEntityData::create)
			.toList()
		);
		ChunkNibbleArray skylight = chunk.getWorld().getLightingProvider().get(LightType.SKY).getLightSection(ChunkSectionPos.from(sectionX, sectionY, sectionZ));
		Optional<byte[]> skylightData = skylight != null ? Optional.of(skylight.asByteArray().clone()) : Optional.empty();
		ServerPlayNetworking.send(player, new ChunkSectionLoadPacket(sectionX, sectionY, sectionZ, sectionData, skylightData, blockEntities));
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}

	@Environment(EnvType.CLIENT)
	public void process(ClientPlayNetworking.Context context) {
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world == null) return;
		WorldChunk chunk = (WorldChunk)(world.getChunk(this.sectionX, this.sectionZ, ChunkStatus.FULL, false));
		if (chunk == null) return;
		ChunkSection section = chunk.getSection(chunk.sectionCoordToIndex(this.sectionY));
		section.readDataPacket(new PacketByteBuf(Unpooled.wrappedBuffer(this.sectionData)));
		for (BlockEntityData blockEntityData : this.blockEntities) {
			int x = chunk.getPos().getStartX() | (blockEntityData.packedXZ & 15);
			int y = blockEntityData.y;
			int z = chunk.getPos().getStartZ() | ((blockEntityData.packedXZ >>> 4) & 15);
			BlockEntity blockEntity = chunk.getBlockEntity(new BlockPos(x, y, z), CreationType.IMMEDIATE);
			if (blockEntity != null && blockEntityData.nbt != null && blockEntity.getType() == blockEntityData.type) {
				blockEntity.read(blockEntityData.nbt, world.getRegistryManager());
			}
		}
		world.getChunkManager().chunks.refreshSections(chunk);
		if (this.skylightData.isPresent()) {
			ChunkSectionPos sectionPos = ChunkSectionPos.from(this.sectionX, this.sectionY, this.sectionZ);
			world.getLightingProvider().enqueueSectionData(
				LightType.SKY,
				sectionPos,
				new ChunkNibbleArray(this.skylightData.get().clone())
			);
			world.getLightingProvider().setSectionStatus(sectionPos, section.isEmpty());
		}
		world.scheduleChunkRenders(this.sectionX - 1, this.sectionY - 1, this.sectionZ - 1, this.sectionX + 1, this.sectionY + 1, this.sectionZ + 1);
		VertigoClientEvents.SECTION_LOADED.invoker().onSectionLoaded(this.sectionX, this.sectionY, this.sectionZ);
	}

	public static record BlockEntityData(
		byte packedXZ,
		int y,
		BlockEntityType<?> type,
		@Nullable NbtCompound nbt
	) {

		public static final PacketCodec<RegistryByteBuf, BlockEntityData> PACKET_CODEC = (
			PacketCodec.tuple(
				PacketCodecs.BYTE,                                           BlockEntityData::packedXZ,
				PacketCodecs.INTEGER,                                        BlockEntityData::y,
				PacketCodecs.registryValue(RegistryKeys.BLOCK_ENTITY_TYPE),  BlockEntityData::type,
				PacketCodecs.nbtCompound(() -> NbtSizeTracker.of(2097152L)), BlockEntityData::nbt,
				BlockEntityData::new
			)
		);

		public static BlockEntityData create(BlockEntity blockEntity) {
			BlockEntityType<?> type = blockEntity.getType();
			NbtCompound nbt = blockEntity.toInitialChunkDataNbt(blockEntity.getWorld().getRegistryManager());
			BlockPos pos = blockEntity.getPos();
			int packedXZ = ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
			int y = pos.getY();
			return new BlockEntityData((byte)(packedXZ), y, type, nbt);
		}
	}
}