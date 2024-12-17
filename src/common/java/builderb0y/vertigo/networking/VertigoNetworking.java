package builderb0y.vertigo.networking;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;

import net.minecraft.client.network.ClientPlayerEntity;

#if MC_VERSION >= MC_1_20_5
	import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
	import net.minecraft.network.codec.PacketCodec;
#else
	import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.PlayPacketHandler;
#endif

public class VertigoNetworking {

	public static void init() {
		#if MC_VERSION >= MC_1_20_5
			PayloadTypeRegistry.playS2C().register(  ChunkSectionLoadPacket.ID,   ChunkSectionLoadPacket.PACKET_CODEC);
			PayloadTypeRegistry.playS2C().register(ChunkSectionUnloadPacket.ID, ChunkSectionUnloadPacket.PACKET_CODEC);
			PayloadTypeRegistry.playS2C().register(         LoadRangePacket.ID,          LoadRangePacket.PACKET_CODEC);
			PayloadTypeRegistry.playS2C().register(    SkylightUpdatePacket.ID,     SkylightUpdatePacket.PACKET_CODEC);
		#endif
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		#if MC_VERSION >= MC_1_20_5
			ClientPlayNetworking.registerGlobalReceiver(  ChunkSectionLoadPacket.ID,   VertigoPacket::receive);
			ClientPlayNetworking.registerGlobalReceiver(ChunkSectionUnloadPacket.ID,   VertigoPacket::receive);
			ClientPlayNetworking.registerGlobalReceiver(         LoadRangePacket.ID,   VertigoPacket::receive);
			ClientPlayNetworking.registerGlobalReceiver(    SkylightUpdatePacket.ID,   VertigoPacket::receive);
		#else
			ClientPlayNetworking.registerGlobalReceiver(  ChunkSectionLoadPacket.TYPE, VertigoPacket::receive);
			ClientPlayNetworking.registerGlobalReceiver(ChunkSectionUnloadPacket.TYPE, VertigoPacket::receive);
			ClientPlayNetworking.registerGlobalReceiver(         LoadRangePacket.TYPE, VertigoPacket::receive);
			ClientPlayNetworking.registerGlobalReceiver(    SkylightUpdatePacket.TYPE, VertigoPacket::receive);
		#endif
	}

	#if MC_VERSION >= MC_1_20_5

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

	#endif
}