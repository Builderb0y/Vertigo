package builderb0y.vertigo.networking;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.network.ServerPlayerEntity;

public interface VertigoC2SPacket
                           
	extends net.minecraft.network.packet.CustomPayload
     
                                                           
      
{

	public abstract void process();

	                           

		public default void receive(ServerPlayNetworking.Context context) {
			this.process();
		}

	     

                                                                                       
                  
   

       
}