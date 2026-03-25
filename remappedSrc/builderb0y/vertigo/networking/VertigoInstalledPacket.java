package builderb0y.vertigo.networking;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import builderb0y.vertigo.Vertigo;

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

	public static final StreamCodec<ByteBuf, VertigoInstalledPacket> PACKET_CODEC = (
		StreamCodec.unit(new VertigoInstalledPacket())
	);

	public static final CustomPacketPayload.Type<VertigoInstalledPacket> ID = new CustomPacketPayload.Type<>(PACKET_ID);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void process() {
	}
}