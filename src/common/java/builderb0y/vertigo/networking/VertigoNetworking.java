package builderb0y.vertigo.networking;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import net.minecraft.network.codec.PacketCodec;

public class VertigoNetworking {

	public static void init() {
		PayloadTypeRegistry.playS2C().register(  ChunkSectionLoadPacket.ID,   ChunkSectionLoadPacket.PACKET_CODEC);
		PayloadTypeRegistry.playS2C().register(ChunkSectionUnloadPacket.ID, ChunkSectionUnloadPacket.PACKET_CODEC);
		PayloadTypeRegistry.playS2C().register(         LoadRangePacket.ID,          LoadRangePacket.PACKET_CODEC);
		PayloadTypeRegistry.playS2C().register(    SkylightUpdatePacket.ID,     SkylightUpdatePacket.PACKET_CODEC);
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		ClientPlayNetworking.registerGlobalReceiver(  ChunkSectionLoadPacket.ID,   ChunkSectionLoadPacket::process);
		ClientPlayNetworking.registerGlobalReceiver(ChunkSectionUnloadPacket.ID, ChunkSectionUnloadPacket::process);
		ClientPlayNetworking.registerGlobalReceiver(         LoadRangePacket.ID,          LoadRangePacket::process);
		ClientPlayNetworking.registerGlobalReceiver(    SkylightUpdatePacket.ID,     SkylightUpdatePacket::process);
	}

	public static PacketCodec<ByteBuf, byte[]> fixedSizeByteArray(int size) {
		return new PacketCodec<ByteBuf, byte[]>() {

			@Override
			public byte[] decode(ByteBuf buffer) {
				byte[] bytes = new byte[size];
				buffer.readBytes(bytes);
				return bytes;
			}

			@Override
			public void encode(ByteBuf buffer, byte[] value) {
				buffer.writeBytes(value);
			}
		};
	}
}