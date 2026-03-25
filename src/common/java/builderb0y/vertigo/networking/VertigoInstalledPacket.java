package builderb0y.vertigo.networking;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import builderb0y.vertigo.Vertigo;

                           
	import net.minecraft.network.codec.PacketCodec;
	import net.minecraft.network.packet.CustomPayload;
     
                                                         
      

/**
this packet never actually gets sent by the client,
but the server will indicate that it *can* receive
this packet when the client connects.
as such, this makes it possible to query on the client
whether or not the server has vertigo installed.
if anyone knows a better way to do this, let me know!
*/
public record VertigoInstalledPacket() implements VertigoC2SPacket {

	public static final Identifier PACKET_ID = Vertigo.modID("vertigo_installed");

	                           

		public static final PacketCodec<ByteBuf, VertigoInstalledPacket> PACKET_CODEC = (
			PacketCodec.unit(new VertigoInstalledPacket())
		);

		public static final CustomPayload.Id<VertigoInstalledPacket> ID = new CustomPayload.Id<>(PACKET_ID);

		@Override
		public CustomPayload.Id<? extends CustomPayload> getId() {
			return ID;
		}

	     

                                                                                                                           

                                                                   
                                       
   

           
                                            

           
                                  
               
   

       

	@Override
	@Environment(EnvType.CLIENT)
	public void process() {}
}