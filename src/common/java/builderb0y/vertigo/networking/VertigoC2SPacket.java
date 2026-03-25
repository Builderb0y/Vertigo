package builderb0y.vertigo.networking;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface VertigoC2SPacket

	extends CustomPacketPayload {

	public abstract void process();

	public default void receive(ServerPlayNetworking.Context context) {
		this.process();
	}
}