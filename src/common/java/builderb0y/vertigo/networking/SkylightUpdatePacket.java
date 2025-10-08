package builderb0y.vertigo.networking;

import java.util.BitSet;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload.Id;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.ChunkLightingView;
import net.minecraft.world.chunk.light.ChunkSkyLight;

import builderb0y.vertigo.TrackingManager;
import builderb0y.vertigo.TrackingManager.LoadedRange;
import builderb0y.vertigo.VersionUtil;
import builderb0y.vertigo.Vertigo;
import builderb0y.vertigo.mixin.ChunkSkyLight_Accessors;
import builderb0y.vertigo.compat.ScalableLuxCompat;

#if MC_VERSION >= MC_1_20_5
	import net.minecraft.network.codec.PacketCodec;
	import net.minecraft.network.packet.CustomPayload;
#else
	import net.fabricmc.fabric.api.networking.v1.PacketType;
#endif

public record SkylightUpdatePacket(
	int chunkX,
	int chunkZ,
	IntArrayList skyPositions
)
implements VertigoS2CPacket {

	public static final Identifier PACKET_ID = Vertigo.modID("skylight_update");

	#if MC_VERSION >= MC_1_20_5

		public static final PacketCodec<ByteBuf, SkylightUpdatePacket> PACKET_CODEC = PacketCodec.of(SkylightUpdatePacket::write, SkylightUpdatePacket::read);
		public static final Id<SkylightUpdatePacket> ID = new Id<>(PACKET_ID);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

	#else

		public static final PacketType<SkylightUpdatePacket> TYPE = PacketType.create(PACKET_ID, SkylightUpdatePacket::read);

		@Override
		public void write(PacketByteBuf buffer) {
			this.write((ByteBuf)(buffer));
		}

		@Override
		public PacketType<?> getType() {
			return TYPE;
		}

	#endif

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

	public static void send(ServerPlayerEntity player, int chunkX, int chunkZ, BitSet mask) {
		WorldChunk chunk = (WorldChunk)(VersionUtil.getWorld(player).getChunk(chunkX, chunkZ, ChunkStatus.FULL, false));
		if (chunk == null) return;
		PaletteStorage palette = ((ChunkSkyLight_Accessors)(chunk.getChunkSkyLight())).vertigo_getPalette();
		IntArrayList queuedPositions = new IntArrayList(mask.cardinality());
		for (int index = -1; (index = mask.nextSetBit(index + 1)) >= 0;) {
			queuedPositions.add(packSkylightPos(index, palette.get(index)));
		}
		ServerPlayNetworking.send(player, new SkylightUpdatePacket(chunk.getPos().x, chunk.getPos().z, queuedPositions));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process() {
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world == null) return;
		WorldChunk chunk = (WorldChunk)(world.getChunk(this.chunkX, this.chunkZ, ChunkStatus.FULL, false));
		if (chunk == null) return;
		LoadedRange range = TrackingManager.CLIENT.getLoadedRange(this.chunkX, this.chunkZ);
		if (range == null) return;
		ChunkSkyLight skylight = chunk.getChunkSkyLight();
		ChunkSkyLight_Accessors accessors = (ChunkSkyLight_Accessors)(skylight);
		ChunkLightingView lighting = world.getLightingProvider().get(LightType.SKY);
		BlockPos.Mutable mutablePos = new BlockPos.Mutable();
		int chunkMinY = accessors.vertigo_getMinY();
		for (int index = 0, size = this.skyPositions.size(); index < size; index++) {
			int pos = this.skyPositions.getInt(index);
			int localIndex = unpackIndex(pos);
			int newRelativeY = unpackRelativeY(pos);
			int oldRelativeY = accessors.vertigo_getPalette().swap(localIndex, newRelativeY);
			int x = chunk.getPos().getStartX() | (localIndex & 15);
			int z = chunk.getPos().getStartZ() | (localIndex >>> 4);
			if (ScalableLuxCompat.scalableLuxInstalled) {
				if (oldRelativeY != newRelativeY) {
					oldRelativeY--;
					newRelativeY--;
					if (!range.isLoaded((oldRelativeY + chunkMinY) >> 4)) {
						world.setBlockState(
							mutablePos.set(x, oldRelativeY + chunkMinY, z),
							Blocks.AIR.getDefaultState()
						);
					}
					if (!range.isLoaded((newRelativeY + chunkMinY) >> 4)) {
						world.setBlockState(
							mutablePos.set(x, newRelativeY + chunkMinY, z),
							Blocks.STONE.getDefaultState()
						);
					}
				}
			}
			else {
				lighting.checkBlock(mutablePos.set(x, newRelativeY + chunkMinY, z));
			}
		}
	}
}