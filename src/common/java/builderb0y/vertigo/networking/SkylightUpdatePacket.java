package builderb0y.vertigo.networking;

import java.util.BitSet;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.ChunkLightingView;

import builderb0y.vertigo.Vertigo;

public record SkylightUpdatePacket(
	int chunkX,
	int chunkZ,
	IntArrayList skyPositions
)
implements CustomPayload {

	public static final PacketCodec<ByteBuf, SkylightUpdatePacket> PACKET_CODEC = PacketCodec.of(SkylightUpdatePacket::write, SkylightUpdatePacket::read);
	public static final Id<SkylightUpdatePacket> ID = new Id<>(Vertigo.modID("skylight_update"));

	public static SkylightUpdatePacket read(ByteBuf buffer) {
		int chunkX = buffer.readInt();
		int chunkZ = buffer.readInt();
		IntArrayList skylightPositions;
		if (buffer.readBoolean()) {
			BitSet bits = BitSet.valueOf(new long[] {
				buffer.readLong(),
				buffer.readLong(),
				buffer.readLong(),
				buffer.readLong()
			});
			skylightPositions = new IntArrayList(bits.cardinality());
			for (int index = -1; (index = bits.nextSetBit(index + 1)) >= 0;) {
				skylightPositions.add(packSkylightPos(index, buffer.readUnsignedShort()));
			}
		}
		else {
			int count = buffer.readUnsignedByte();
			skylightPositions = new IntArrayList(count);
			for (int index = 0; index < count; index++) {
				skylightPositions.add(packSkylightPos(buffer.readUnsignedByte(), buffer.readUnsignedShort()));
			}
		}
		return new SkylightUpdatePacket(chunkX, chunkZ, skylightPositions);
	}

	public void write(ByteBuf buffer) {
		buffer.writeInt(this.chunkX).writeInt(this.chunkZ);
		//using small strategy we use one byte per index.
		//using big strategy we use 256 bits for all indices in total.
		if (this.skyPositions.size() >= 256 / 8) {
			buffer.writeBoolean(true);
			BitSet bits = new BitSet(256);
			for (int index = 0, size = this.skyPositions.size(); index < size; index++) {
				bits.set(unpackIndex(this.skyPositions.getInt(index)));
			}
			int count = 0;
			for (long value : bits.toLongArray()) {
				buffer.writeLong(value);
				count++;
			}
			while (count++ < 4) buffer.writeLong(0L);
			for (int index = 0, size = this.skyPositions.size(); index < size; index++) {
				buffer.writeShort(unpackRelativeY(this.skyPositions.getInt(index)));
			}
		}
		else {
			buffer.writeBoolean(false);
			buffer.writeByte(this.skyPositions.size());
			for (int index = 0, size = this.skyPositions.size(); index < size; index++) {
				int packed = this.skyPositions.getInt(index);
				buffer.writeByte(unpackIndex(packed)).writeShort(unpackRelativeY(packed));
			}
		}
	}

	public static int packSkylightPos(int index, int relativeY) {
		return ((index & 0xFF) << 16) | (relativeY & 0xFFFF);
	}

	public static int unpackIndex(int packed) {
		return (packed >>> 16) & 0xFF;
	}

	public static int unpackRelativeY(int packed) {
		return packed & 0xFFFF;
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}

	public static void send(ServerPlayerEntity player, int chunkX, int chunkZ, BitSet mask) {
		WorldChunk chunk = (WorldChunk)(player.getServerWorld().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false));
		if (chunk == null) return;
		PaletteStorage palette = chunk.getChunkSkyLight().palette;
		IntArrayList queuedPositions = new IntArrayList(mask.cardinality());
		for (int index = -1; (index = mask.nextSetBit(index + 1)) >= 0;) {
			queuedPositions.add(packSkylightPos(index, palette.get(index)));
		}
		ServerPlayNetworking.send(player, new SkylightUpdatePacket(chunk.getPos().x, chunk.getPos().z, queuedPositions));
	}

	@Environment(EnvType.CLIENT)
	public void process(ClientPlayNetworking.Context context) {
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world == null) return;
		WorldChunk chunk = (WorldChunk)(world.getChunk(this.chunkX, this.chunkZ, ChunkStatus.FULL, false));
		if (chunk == null) return;
		world.enqueueChunkUpdate(() -> {
			PaletteStorage palette = chunk.getChunkSkyLight().palette;
			ChunkLightingView lighting = world.getLightingProvider().get(LightType.SKY);
			BlockPos.Mutable mutablePos = new BlockPos.Mutable();
			for (int index = 0, size = this.skyPositions.size(); index < size; index++) {
				int pos = this.skyPositions.getInt(index);
				palette.set(unpackIndex(pos), unpackRelativeY(pos));
				lighting.checkBlock(
					mutablePos.set(
						chunk.getPos().getStartX() | (unpackIndex(pos) & 15),
						unpackRelativeY(pos) + chunk.getBottomY(),
						chunk.getPos().getStartZ() | ((unpackIndex(pos) >>> 4) & 15)
					)
				);
			}
		});
	}
}