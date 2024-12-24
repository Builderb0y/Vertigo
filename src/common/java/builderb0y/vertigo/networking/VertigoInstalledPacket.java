package builderb0y.vertigo.networking;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import builderb0y.vertigo.Vertigo;

#if MC_VERSION >= MC_1_20_5
	import net.minecraft.network.codec.PacketCodec;
	import net.minecraft.network.packet.CustomPayload;
#else
	import net.fabricmc.fabric.api.networking.v1.PacketType;
#endif

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

	#if MC_VERSION >= MC_1_20_5

		public static final PacketCodec<ByteBuf, VertigoInstalledPacket> PACKET_CODEC = (
			PacketCodec.unit(new VertigoInstalledPacket())
		);

		public static final Id<VertigoInstalledPacket> ID = new Id<>(PACKET_ID);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}

	#else

		public static final PacketType<VertigoInstalledPacket> TYPE = PacketType.create(PACKET_ID, VertigoInstalledPacket::read);

		public static VertigoInstalledPacket read(PacketByteBuf buffer) {
			return new VertigoInstalledPacket();
		}

		@Override
		public void write(PacketByteBuf buffer) {}

		@Override
		public PacketType<?> getType() {
			return TYPE;
		}

	#endif

	@Override
	@Environment(EnvType.CLIENT)
	public void process() {}
}