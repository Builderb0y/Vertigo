package builderb0y.vertigo.networking;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WorldChunk.WrappedBlockEntityTickInvoker;

import builderb0y.vertigo.Vertigo;
import builderb0y.vertigo.api.VertigoClientEvents;

public record ChunkSectionUnloadPacket(
	int sectionX,
	int sectionY,
	int sectionZ
)
implements CustomPayload {

	public static final PacketCodec<ByteBuf, ChunkSectionUnloadPacket> PACKET_CODEC = (
		PacketCodec.tuple(
			PacketCodecs.INTEGER, ChunkSectionUnloadPacket::sectionX,
			PacketCodecs.INTEGER, ChunkSectionUnloadPacket::sectionY,
			PacketCodecs.INTEGER, ChunkSectionUnloadPacket::sectionZ,
			ChunkSectionUnloadPacket::new
		)
	);

	public static final Id<ChunkSectionUnloadPacket> ID = new Id<>(Vertigo.modID("section_unload"));

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}

	public static void send(ServerPlayerEntity player, int sectionX, int sectionY, int sectionZ) {
		ServerPlayNetworking.send(player, new ChunkSectionUnloadPacket(sectionX, sectionY, sectionZ));
	}

	@Environment(EnvType.CLIENT)
	public void process(ClientPlayNetworking.Context context) {
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world == null) return;
		WorldChunk chunk = (WorldChunk)(world.getChunk(this.sectionX, this.sectionZ, ChunkStatus.FULL, false));
		if (chunk == null) return;
		VertigoClientEvents.SECTION_UNLOADED.invoker().onSectionUnloaded(this.sectionX, this.sectionY, this.sectionZ);
		Registry<Biome> biomeRegistry;
		#if MC_VERSION >= MC_1_21_3
			biomeRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.BIOME);
		#else
			biomeRegistry = world.getRegistryManager().get(RegistryKeys.BIOME);
		#endif
		chunk.getSectionArray()[chunk.sectionCoordToIndex(this.sectionY)] = new ChunkSection(biomeRegistry);
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