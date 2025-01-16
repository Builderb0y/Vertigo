package builderb0y.vertigo.networking;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.network.ServerPlayerEntity;

public interface VertigoC2SPacket
#if MC_VERSION >= MC_1_20_5
	extends net.minecraft.network.packet.CustomPayload
#else
	extends net.fabricmc.fabric.api.networking.v1.FabricPacket
#endif
{

	public abstract void process();

	#if MC_VERSION >= MC_1_20_5

		public default void receive(ServerPlayNetworking.Context context) {
			this.process();
		}

	#else

		public default void receive(ServerPlayerEntity player, PacketSender responseSender) {
			this.process();
		}

	#endif
}