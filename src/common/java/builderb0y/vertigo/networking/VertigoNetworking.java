package builderb0y.vertigo.networking;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

#if MC_VERSION >= MC_1_20_5
	import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
	import net.minecraft.network.codec.PacketCodec;
#else
	import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.PlayPacketHandler;
#endif

public class VertigoNetworking {

	public static void init() {
		#if MC_VERSION >= MC_1_20_5
			PayloadTypeRegistry.playC2S().register(  VertigoInstalledPacket.ID,   VertigoInstalledPacket.PACKET_CODEC);
			PayloadTypeRegistry.playS2C().register(  ChunkSectionLoadPacket.ID,   ChunkSectionLoadPacket.PACKET_CODEC);
			PayloadTypeRegistry.playS2C().register(ChunkSectionUnloadPacket.ID, ChunkSectionUnloadPacket.PACKET_CODEC);
			PayloadTypeRegistry.playS2C().register(         LoadRangePacket.ID,          LoadRangePacket.PACKET_CODEC);
			PayloadTypeRegistry.playS2C().register(    SkylightUpdatePacket.ID,     SkylightUpdatePacket.PACKET_CODEC);

			ServerPlayNetworking.registerGlobalReceiver(VertigoInstalledPacket.ID,   VertigoC2SPacket::receive);
		#else
			ServerPlayNetworking.registerGlobalReceiver(VertigoInstalledPacket.TYPE, VertigoC2SPacket::receive);
		#endif
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		#if MC_VERSION >= MC_1_20_5
			ClientPlayNetworking.registerGlobalReceiver(  ChunkSectionLoadPacket.ID,   VertigoS2CPacket::receive);
			ClientPlayNetworking.registerGlobalReceiver(ChunkSectionUnloadPacket.ID,   VertigoS2CPacket::receive);
			ClientPlayNetworking.registerGlobalReceiver(         LoadRangePacket.ID,   VertigoS2CPacket::receive);
			ClientPlayNetworking.registerGlobalReceiver(    SkylightUpdatePacket.ID,   VertigoS2CPacket::receive);
		#else
			ClientPlayNetworking.registerGlobalReceiver(  ChunkSectionLoadPacket.TYPE, VertigoS2CPacket::receive);
			ClientPlayNetworking.registerGlobalReceiver(ChunkSectionUnloadPacket.TYPE, VertigoS2CPacket::receive);
			ClientPlayNetworking.registerGlobalReceiver(         LoadRangePacket.TYPE, VertigoS2CPacket::receive);
			ClientPlayNetworking.registerGlobalReceiver(    SkylightUpdatePacket.TYPE, VertigoS2CPacket::receive);
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