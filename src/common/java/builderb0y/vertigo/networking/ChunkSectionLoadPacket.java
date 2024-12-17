package builderb0y.vertigo.networking;

import java.util.ArrayList;
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
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.network.packet.s2c.play.ChunkData.BlockEntityData;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.registry.Registries;
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

#if MC_VERSION >= MC_1_20_5
	import net.minecraft.nbt.NbtSizeTracker;
	import net.minecraft.network.RegistryByteBuf;
	import net.minecraft.network.codec.PacketCodec;
	import net.minecraft.network.codec.PacketCodecs;
	import net.minecraft.network.packet.CustomPayload;
	import net.minecraft.network.packet.CustomPayload.Id;
#else
	import net.fabricmc.fabric.api.networking.v1.PacketType;
#endif

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
implements VertigoPacket {

	#if MC_VERSION >= MC_1_20_5

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
		public static final Id<ChunkSectionLoadPacket> ID = new Id<>(Vertigo.modID("section_load"));

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

	#else

		public static final PacketType<ChunkSectionLoadPacket> TYPE = PacketType.create(Vertigo.modID("section_load"), ChunkSectionLoadPacket::read);

		public static ChunkSectionLoadPacket read(PacketByteBuf buffer) {
			int sectionX = buffer.readInt();
			int sectionY = buffer.readInt();
			int sectionZ = buffer.readInt();
			byte[] sectionData = buffer.readByteArray();
			Optional<byte[]> skylightData;
			if (buffer.readBoolean()) {
				skylightData = Optional.of(new byte[2048]);
				buffer.readBytes(skylightData.get());
			}
			else {
				skylightData = Optional.empty();
			}
			int blockEntityCount = buffer.readVarInt();
			ArrayList<BlockEntityData> blockEntities = new ArrayList<>(blockEntityCount);
			for (int index = 0; index < blockEntityCount; index++) {
				blockEntities.add(BlockEntityData.read(buffer));
			}
			return new ChunkSectionLoadPacket(sectionX, sectionY, sectionZ, sectionData, skylightData, blockEntities);
		}

		@Override
		public void write(PacketByteBuf buffer) {
			buffer
			.writeInt(this.sectionX)
			.writeInt(this.sectionY)
			.writeInt(this.sectionZ);
			buffer
			.writeByteArray(this.sectionData)
			.writeBoolean(this.skylightData.isPresent());
			if (this.skylightData.isPresent()) buffer.writeBytes(this.skylightData.get());
			buffer.writeVarInt(this.blockEntities.size());
			for (BlockEntityData data : this.blockEntities) {
				data.write(buffer);
			}
		}

		@Override
		public PacketType<?> getType() {
			return TYPE;
		}

	#endif

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
	@Environment(EnvType.CLIENT)
	public void process() {
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
				#if MC_VERSION >= MC_1_20_5
					blockEntity.read(blockEntityData.nbt, world.getRegistryManager());
				#else
					blockEntity.readNbt(blockEntityData.nbt);
				#endif
			}
		}
		#if MC_VERSION >= MC_1_21_4
			world.getChunkManager().chunks.refreshSections(chunk);
		#elif MC_VERSION >= MC_1_21_2
			//if I'm reading minecraft's code correctly, I should provide true here,
			//but that breaks things, and false works flawlessly.
			//I do not understand why.
			world.getChunkManager().chunks.onSectionStatusChanged(this.sectionX, this.sectionY, this.sectionZ, false);
		#endif
		if (this.skylightData.isPresent()) {
			ChunkSectionPos sectionPos = ChunkSectionPos.from(this.sectionX, this.sectionY, this.sectionZ);
			world.getLightingProvider().enqueueSectionData(
				LightType.SKY,
				sectionPos,
				new ChunkNibbleArray(this.skylightData.get().clone())
			);
			world.getLightingProvider().setSectionStatus(sectionPos, section.isEmpty());
		}
		world.scheduleBlockRenders(this.sectionX, this.sectionY, this.sectionZ);
		VertigoClientEvents.SECTION_LOADED.invoker().onSectionLoaded(this.sectionX, this.sectionY, this.sectionZ);
	}

	public static record BlockEntityData(
		byte packedXZ,
		int y,
		BlockEntityType<?> type,
		@Nullable NbtCompound nbt
	) {

		#if MC_VERSION >= MC_1_20_5

			public static final PacketCodec<RegistryByteBuf, BlockEntityData> PACKET_CODEC = (
				PacketCodec.tuple(
					PacketCodecs.BYTE,                                           BlockEntityData::packedXZ,
					PacketCodecs.INTEGER,                                        BlockEntityData::y,
					PacketCodecs.registryValue(RegistryKeys.BLOCK_ENTITY_TYPE),  BlockEntityData::type,
					PacketCodecs.nbtCompound(() -> NbtSizeTracker.of(2097152L)), BlockEntityData::nbt,
					BlockEntityData::new
				)
			);

		#else

			public static BlockEntityData read(PacketByteBuf buffer) {
				return new BlockEntityData(
					buffer.readByte(),
					buffer.readInt(),
					buffer.readRegistryValue(Registries.BLOCK_ENTITY_TYPE),
					buffer.readNbt()
				);
			}

			public void write(PacketByteBuf buffer) {
				buffer.writeByte(this.packedXZ).writeInt(this.y);
				buffer.writeRegistryValue(Registries.BLOCK_ENTITY_TYPE, this.type);
				buffer.writeNbt(this.nbt);
			}

		#endif

		public static BlockEntityData create(BlockEntity blockEntity) {
			BlockEntityType<?> type = blockEntity.getType();
			NbtCompound nbt;
			#if MC_VERSION >= MC_1_20_5
				nbt = blockEntity.toInitialChunkDataNbt(blockEntity.getWorld().getRegistryManager());
			#else
				nbt = blockEntity.toInitialChunkDataNbt();
			#endif
			BlockPos pos = blockEntity.getPos();
			int packedXZ = ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
			int y = pos.getY();
			return new BlockEntityData((byte)(packedXZ), y, type, nbt);
		}
	}
}