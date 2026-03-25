package builderb0y.vertigo.networking;

import java.util.BitSet;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import builderb0y.vertigo.TrackingManager;
import builderb0y.vertigo.TrackingManager.LoadedRange;
import builderb0y.vertigo.VersionUtil;
import builderb0y.vertigo.Vertigo;
import builderb0y.vertigo.compat.ScalableLuxCompat;
import builderb0y.vertigo.mixin.ChunkSkyLight_Accessors;

public record SkylightUpdatePacket(
	int chunkX,
	int chunkZ,
	IntArrayList skyPositions
)
	implements VertigoS2CPacket {

	public static final Identifier PACKET_ID = Vertigo.modID("skylight_update");

	public static final StreamCodec<ByteBuf, SkylightUpdatePacket> PACKET_CODEC = StreamCodec.ofMember(SkylightUpdatePacket::write, SkylightUpdatePacket::read);
	public static final CustomPacketPayload.Type<SkylightUpdatePacket> ID = new CustomPacketPayload.Type<>(PACKET_ID);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

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
			for (int index = -1; (index = bits.nextSetBit(index + 1)) >= 0; ) {
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

	public static void send(ServerPlayer player, int chunkX, int chunkZ, BitSet mask) {
		LevelChunk chunk = (LevelChunk)(VersionUtil.getWorld(player).getChunk(chunkX, chunkZ, ChunkStatus.FULL, false));
		if (chunk == null) return;
		BitStorage palette = ((ChunkSkyLight_Accessors)(chunk.getSkyLightSources())).vertigo_getPalette();
		IntArrayList queuedPositions = new IntArrayList(mask.cardinality());
		for (int index = -1; (index = mask.nextSetBit(index + 1)) >= 0; ) {
			queuedPositions.add(packSkylightPos(index, palette.get(index)));
		}
		ServerPlayNetworking.send(player, new SkylightUpdatePacket(chunk.getPos().x, chunk.getPos().z, queuedPositions));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;
		TrackingManager manager = TrackingManager.get(player);
		if (manager == null) return;
		LoadedRange range = manager.getLoadedRange(this.chunkX, this.chunkZ);
		if (range == null) return;
		ClientLevel world = Minecraft.getInstance().level;
		if (world == null) return;
		LevelChunk chunk = (LevelChunk)(world.getChunk(this.chunkX, this.chunkZ, ChunkStatus.FULL, false));
		if (chunk == null) return;
		ChunkSkyLightSources skylight = chunk.getSkyLightSources();
		ChunkSkyLight_Accessors accessors = (ChunkSkyLight_Accessors)(skylight);
		LayerLightEventListener lighting = world.getLightEngine().getLayerListener(LightLayer.SKY);
		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
		int chunkMinY = accessors.vertigo_getMinY();
		for (int index = 0, size = this.skyPositions.size(); index < size; index++) {
			int pos = this.skyPositions.getInt(index);
			int localIndex = unpackIndex(pos);
			int newRelativeY = unpackRelativeY(pos);
			int oldRelativeY = accessors.vertigo_getPalette().getAndSet(localIndex, newRelativeY);
			int x = chunk.getPos().getMinBlockX() | (localIndex & 15);
			int z = chunk.getPos().getMinBlockZ() | (localIndex >>> 4);
			if (ScalableLuxCompat.scalableLuxInstalled) {
				if (oldRelativeY != newRelativeY) {
					oldRelativeY--;
					newRelativeY--;
					if (!range.isLoaded((oldRelativeY + chunkMinY) >> 4)) {
						world.setBlockAndUpdate(
							mutablePos.set(x, oldRelativeY + chunkMinY, z),
							Blocks.AIR.defaultBlockState()
						);
					}
					if (!range.isLoaded((newRelativeY + chunkMinY) >> 4)) {
						world.setBlockAndUpdate(
							mutablePos.set(x, newRelativeY + chunkMinY, z),
							Blocks.STONE.defaultBlockState()
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