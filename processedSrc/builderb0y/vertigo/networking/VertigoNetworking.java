package builderb0y.vertigo.networking;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

                           
	import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
	import net.minecraft.network.codec.PacketCodec;
     
                                                                                            
      

public class VertigoNetworking {

	public static void init() {
		                           
			PayloadTypeRegistry.playC2S().register(  VertigoInstalledPacket.ID,   VertigoInstalledPacket.PACKET_CODEC);
			PayloadTypeRegistry.playS2C().register(  ChunkSectionLoadPacket.ID,   ChunkSectionLoadPacket.PACKET_CODEC);
			PayloadTypeRegistry.playS2C().register(ChunkSectionUnloadPacket.ID, ChunkSectionUnloadPacket.PACKET_CODEC);
			PayloadTypeRegistry.playS2C().register(         LoadRangePacket.ID,          LoadRangePacket.PACKET_CODEC);
			PayloadTypeRegistry.playS2C().register(    SkylightUpdatePacket.ID,     SkylightUpdatePacket.PACKET_CODEC);

			ServerPlayNetworking.registerGlobalReceiver(VertigoInstalledPacket.ID,   VertigoC2SPacket::receive);
		     
                                                                                                       
        
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		                           
			ClientPlayNetworking.registerGlobalReceiver(  ChunkSectionLoadPacket.ID,   VertigoS2CPacket::receive);
			ClientPlayNetworking.registerGlobalReceiver(ChunkSectionUnloadPacket.ID,   VertigoS2CPacket::receive);
			ClientPlayNetworking.registerGlobalReceiver(         LoadRangePacket.ID,   VertigoS2CPacket::receive);
			ClientPlayNetworking.registerGlobalReceiver(    SkylightUpdatePacket.ID,   VertigoS2CPacket::receive);
		     
                                                                                                         
                                                                                                         
                                                                                                         
                                                                                                         
        
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