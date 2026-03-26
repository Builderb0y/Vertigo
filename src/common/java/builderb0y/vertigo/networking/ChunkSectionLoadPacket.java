package builderb0y.vertigo.networking;

import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Either;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter.ScopedCollector;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunk.EntityCreationType;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.TagValueInput;
import org.jetbrains.annotations.Nullable;
import builderb0y.vertigo.VersionUtil;
import builderb0y.vertigo.Vertigo;
import builderb0y.vertigo.api.VertigoClientEvents;

/**
mostly a modified version of {@link ClientboundLevelChunkWithLightPacket} and
{@link ClientboundLevelChunkPacketData} which works for single sections instead of whole chunks.
*/
public record ChunkSectionLoadPacket(
	int sectionX,
	int sectionY,
	int sectionZ,
	//chunk sections are serialized on the server thread,
	//and deserialized on the client network thread.
	//as such, this either will contain a byte[] when the server creates the packet,
	//and a chunk section when the client network thread creates it.
	Either<byte[], LevelChunkSection> sectionData,
	Optional<byte[]> skylightData,
	List<BlockEntityData> blockEntities
)
	implements VertigoS2CPacket {

	public static final Identifier PACKET_ID = Vertigo.modID("section_load");

	public static final StreamCodec<RegistryFriendlyByteBuf, ChunkSectionLoadPacket> PACKET_CODEC = (
		StreamCodec.composite(
			ByteBufCodecs.INT,
			ChunkSectionLoadPacket::sectionX,

			ByteBufCodecs.INT,
			ChunkSectionLoadPacket::sectionY,

			ByteBufCodecs.INT,
			ChunkSectionLoadPacket::sectionZ,

			StreamCodec.ofMember(
				(Either<byte[], LevelChunkSection> either, RegistryFriendlyByteBuf buffer) -> {
					buffer.writeBytes(either.left().orElseThrow());
				},
				(RegistryFriendlyByteBuf buffer) -> {
					LevelChunkSection section = VersionUtil.newEmptyChunkSection(buffer.registryAccess());
					section.read(buffer);
					return Either.right(section);
				}
			),
			ChunkSectionLoadPacket::sectionData,

			ByteBufCodecs.optional(VertigoNetworking.fixedSizeByteArray(2048)),
			ChunkSectionLoadPacket::skylightData,

			BlockEntityData.PACKET_CODEC.apply(ByteBufCodecs.list(4096)),
			ChunkSectionLoadPacket::blockEntities,

			ChunkSectionLoadPacket::new
		)
	);
	public static final CustomPacketPayload.Type<ChunkSectionLoadPacket> ID = new CustomPacketPayload.Type<>(PACKET_ID);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static void send(ServerPlayer player, LevelChunk chunk, int sectionY) {
		int sectionX = chunk.getPos().x();
		int sectionZ = chunk.getPos().z();
		LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
		//section.getPacketSize() returns the wrong value. do not trust it.
		/*
		int bytes = section.getPacketSize();
		byte[] sectionData = new byte[bytes];
		*/
		ByteBuf buffer = Unpooled.buffer();
		section.write(new FriendlyByteBuf(buffer));
		byte[] sectionData = new byte[buffer.writerIndex()];
		buffer.readBytes(sectionData);
		if (buffer.isReadable()) throw new IllegalStateException("readable: " + buffer.readableBytes());

		List<BlockEntityData> blockEntities = (
			chunk
			.getBlockEntities()
			.values()
			.stream()
			.filter((BlockEntity blockEntity) -> blockEntity.getBlockPos().getY() >> 4 == sectionY)
			.map(BlockEntityData::create)
			.toList()
		);
		DataLayer skylight = chunk.getLevel().getLightEngine().getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(sectionX, sectionY, sectionZ));
		Optional<byte[]> skylightData = skylight != null ? Optional.of(skylight.getData().clone()) : Optional.empty();
		ServerPlayNetworking.send(player, new ChunkSectionLoadPacket(sectionX, sectionY, sectionZ, Either.left(sectionData), skylightData, blockEntities));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process() {
		ClientLevel world = Minecraft.getInstance().level;
		if (world == null) return;
		LevelChunk chunk = (LevelChunk)(world.getChunk(this.sectionX, this.sectionZ, ChunkStatus.FULL, false));
		if (chunk == null) return;
		chunk.getSections()[chunk.getSectionIndexFromSectionY(this.sectionY)] = this.sectionData.right().orElseThrow();
		for (BlockEntityData blockEntityData : this.blockEntities) {
			int x = chunk.getPos().getMinBlockX() | (blockEntityData.packedXZ & 15);
			int y = blockEntityData.y;
			int z = chunk.getPos().getMinBlockZ() | ((blockEntityData.packedXZ >>> 4) & 15);
			BlockEntity blockEntity = chunk.getBlockEntity(new BlockPos(x, y, z), EntityCreationType.IMMEDIATE);
			if (blockEntity != null && blockEntityData.nbt != null && blockEntity.getType() == blockEntityData.type) {

				try (ScopedCollector logging = new ScopedCollector(blockEntity.problemPath(), Vertigo.LOGGER)) {
					blockEntity.loadWithComponents(TagValueInput.create(logging, world.registryAccess(), blockEntityData.nbt));
				}
			}
		}

		world.getChunkSource().storage.refreshEmptySections(chunk);

		if (this.skylightData.isPresent()) {
			SectionPos sectionPos = SectionPos.of(this.sectionX, this.sectionY, this.sectionZ);
			world.getLightEngine().queueSectionData(
				LightLayer.SKY,
				sectionPos,
				new DataLayer(this.skylightData.get().clone())
			);
			world.getLightEngine().updateSectionStatus(sectionPos, this.sectionData.right().orElseThrow().hasOnlyAir());
		}
		world.setSectionDirtyWithNeighbors(this.sectionX, this.sectionY, this.sectionZ);
		VertigoClientEvents.SECTION_LOADED.invoker().onSectionLoaded(this.sectionX, this.sectionY, this.sectionZ);
	}

	public static record BlockEntityData(
		byte packedXZ,
		int y,
		BlockEntityType<?> type,
		@Nullable CompoundTag nbt
	) {

		public static final StreamCodec<RegistryFriendlyByteBuf, BlockEntityData> PACKET_CODEC = (
			StreamCodec.composite(
				ByteBufCodecs.BYTE, BlockEntityData::packedXZ,
				ByteBufCodecs.INT, BlockEntityData::y,
				ByteBufCodecs.registry(Registries.BLOCK_ENTITY_TYPE), BlockEntityData::type,
				ByteBufCodecs.compoundTagCodec(() -> NbtAccounter.create(2097152L)), BlockEntityData::nbt,
				BlockEntityData::new
			)
		);

		public static BlockEntityData create(BlockEntity blockEntity) {
			BlockEntityType<?> type = blockEntity.getType();
			CompoundTag nbt;

			nbt = blockEntity.getUpdateTag(blockEntity.getLevel().registryAccess());

			BlockPos pos = blockEntity.getBlockPos();
			int packedXZ = ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
			int y = pos.getY();
			return new BlockEntityData((byte)(packedXZ), y, type, nbt);
		}
	}
}