package builderb0y.vertigo.networking;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;

import net.minecraft.client.network.ClientPlayerEntity;

public interface VertigoS2CPacket
                           
	extends net.minecraft.network.packet.CustomPayload
     
                                                           
      
{

	@Environment(EnvType.CLIENT)
	public abstract void process();

	                           

		@Environment(EnvType.CLIENT)
		public default void receive(ClientPlayNetworking.Context context) {
			this.process();
		}

	     

                              
                                                                                       
                  
   

       
}