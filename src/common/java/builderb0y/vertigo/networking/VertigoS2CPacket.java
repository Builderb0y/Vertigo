package builderb0y.vertigo.networking;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;

import net.minecraft.client.network.ClientPlayerEntity;

public interface VertigoS2CPacket
#if MC_VERSION >= MC_1_20_5
	extends net.minecraft.network.packet.CustomPayload
#else
	extends net.fabricmc.fabric.api.networking.v1.FabricPacket
#endif
{

	@Environment(EnvType.CLIENT)
	public abstract void process();

	#if MC_VERSION >= MC_1_20_5

		@Environment(EnvType.CLIENT)
		public default void receive(ClientPlayNetworking.Context context) {
			this.process();
		}

	#else

		@Environment(EnvType.CLIENT)
		public default void receive(ClientPlayerEntity player, PacketSender responseSender) {
			this.process();
		}

	#endif
}