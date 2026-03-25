package builderb0y.vertigo.networking;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;

import builderb0y.vertigo.TrackingManager;
import builderb0y.vertigo.Vertigo;
import builderb0y.vertigo.SectionTrackingManager;
import builderb0y.vertigo.SectionTrackingManager.ChunkState;
import builderb0y.vertigo.api.VertigoClientEvents;

                           
	import net.minecraft.network.codec.PacketCodec;
	import net.minecraft.network.codec.PacketCodecs;
	import net.minecraft.network.packet.CustomPayload;
     
                                                         
      

public record LoadRangePacket(
	int chunkX,
	int chunkZ,
	int minY,
	int maxY
)
implements VertigoS2CPacket {

	public static final Identifier PACKET_ID = Vertigo.modID("load_range");

	                           

		public static final PacketCodec<ByteBuf, LoadRangePacket> PACKET_CODEC = (
			PacketCodec.tuple(
				PacketCodecs.INTEGER, LoadRangePacket::chunkX,
				PacketCodecs.INTEGER, LoadRangePacket::chunkZ,
				PacketCodecs.INTEGER, LoadRangePacket::minY,
				PacketCodecs.INTEGER, LoadRangePacket::maxY,
				LoadRangePacket::new
			)
		);

		public static final CustomPayload.Id<LoadRangePacket> ID = new CustomPayload.Id<>(PACKET_ID);

		@Override
		public CustomPayload.Id<? extends CustomPayload> getId() {
			return ID;
		}

	     

                                                                                                             

                                                            
                              
                     
                     
                     
                    
     
   

           
                                           
         
                         
                         
                       
                        
   

           
                                  
               
   

       

	public static void send(ServerPlayerEntity player, int chunkX, int chunkZ, int minY, int maxY) {
		ServerPlayNetworking.send(player, new LoadRangePacket(chunkX, chunkZ, minY, maxY));
	}

	public static void sendUnload(ServerPlayerEntity player, int chunkX, int chunkZ) {
		ServerPlayNetworking.send(player, new LoadRangePacket(chunkX, chunkZ, 0, -1));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return;
		if (!(TrackingManager.get(player) instanceof SectionTrackingManager manager)) return;
		long chunkPos = ChunkPos.toLong(this.chunkX, this.chunkZ);
		if (this.maxY >= this.minY) {
			ChunkState bound = manager.chunkBounds.get(chunkPos);
			if (bound != null) {
				bound.minY = this.minY;
				bound.maxY = this.maxY;
				//firing of events happens from SectionLoad/UnloadPacket in this case.
			}
			else {
				bound = new ChunkState();
				bound.minY = this.minY;
				bound.maxY = this.maxY;
				manager.chunkBounds.put(chunkPos, bound);
				for (int sectionY = bound.minY; sectionY <= bound.maxY; sectionY++) {
					VertigoClientEvents.SECTION_LOADED.invoker().onSectionLoaded(this.chunkX, sectionY, this.chunkZ);
				}
			}
		}
		else {
			manager.chunkBounds.remove(chunkPos);
		}
	}
}