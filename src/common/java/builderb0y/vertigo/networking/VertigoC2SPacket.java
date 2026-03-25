package builderb0y.vertigo.networking;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.packet.CustomPayload;

public interface VertigoC2SPacket

	extends CustomPayload {

	public abstract void process();

	public default void receive(ServerPlayNetworking.Context context) {
		this.process();
	}
}