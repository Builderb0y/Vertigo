package builderb0y.vertigo.networking;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WorldChunk.WrappedBlockEntityTickInvoker;

import builderb0y.vertigo.VersionUtil;
import builderb0y.vertigo.Vertigo;
import builderb0y.vertigo.api.VertigoClientEvents;

#if MC_VERSION >= MC_1_20_5
	import net.minecraft.network.codec.PacketCodec;
	import net.minecraft.network.codec.PacketCodecs;
	import net.minecraft.network.packet.CustomPayload;
#else
	import net.fabricmc.fabric.api.networking.v1.PacketType;
#endif

public record ChunkSectionUnloadPacket(
	int sectionX,
	int sectionY,
	int sectionZ
)
implements VertigoS2CPacket {

	public static final Identifier PACKET_ID = Vertigo.modID("section_unload");

	#if MC_VERSION >= MC_1_20_5

		public static final PacketCodec<ByteBuf, ChunkSectionUnloadPacket> PACKET_CODEC = (
			PacketCodec.tuple(
				PacketCodecs.INTEGER, ChunkSectionUnloadPacket::sectionX,
				PacketCodecs.INTEGER, ChunkSectionUnloadPacket::sectionY,
				PacketCodecs.INTEGER, ChunkSectionUnloadPacket::sectionZ,
				ChunkSectionUnloadPacket::new
			)
		);

		public static final Id<ChunkSectionUnloadPacket> ID = new Id<>(PACKET_ID);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

	#else

		public static final PacketType<ChunkSectionUnloadPacket> TYPE = PacketType.create(PACKET_ID, ChunkSectionUnloadPacket::read);

		public static ChunkSectionUnloadPacket read(PacketByteBuf buffer) {
			return new ChunkSectionUnloadPacket(
				buffer.readInt(),
				buffer.readInt(),
				buffer.readInt()
			);
		}

		@Override
		public void write(PacketByteBuf buffer) {
			buffer.writeInt(this.sectionX).writeInt(this.sectionY).writeInt(this.sectionZ);
		}

		@Override
		public PacketType<?> getType() {
			return TYPE;
		}

	#endif

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
		chunk.getBlockEntities().values().removeIf((BlockEntity blockEntity) -> {
			if (blockEntity.getPos().getY() >> 4 == this.sectionY) {
				blockEntity.markRemoved();
				return true;
			}
			else {
				return false;
			}
		});
		chunk.blockEntityTickers.values().removeIf((WrappedBlockEntityTickInvoker ticker) -> {
			if (ticker.isRemoved()) {
				return true;
			}
			if (ticker.getPos().getY() >> 4 == this.sectionY) {
				ticker.setWrapped(WorldChunk.EMPTY_BLOCK_ENTITY_TICKER);
				return true;
			}
			return false;
		});
		#if MC_VERSION >= MC_1_21_4
			world.getChunkManager().chunks.refreshSections(chunk);
		#elif MC_VERSION >= MC_1_21_2
			//if I'm reading minecraft's code correctly, I should provide false here,
			//but that breaks things, and true works flawlessly.
			//I do not understand why.
			world.getChunkManager().chunks.onSectionStatusChanged(this.sectionX, this.sectionY, this.sectionZ, true);
		#endif

		world.scheduleBlockRenders(this.sectionX, this.sectionY, this.sectionZ);
	}
}