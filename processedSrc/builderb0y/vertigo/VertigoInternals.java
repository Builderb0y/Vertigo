package builderb0y.vertigo;

import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.chunk.ChunkSection;

public class VertigoInternals {

	/** used by some mixins to keep track of which player a packet is being synced to. */
	public static final ThreadLocal<ServerPlayerEntity> SYNCING_PLAYER = new ThreadLocal<>();
	/**
	{@link ChunkDataS2CPacket} sends the entire chunk payload in one big byte[].
	this includes every section in the chunk, which is a problem because I can't
	just say "only send the sections in this Y range". so instead, I redirect
	the unnecessary chunk sections to this empty section, to reduce the size
	of the packet.
	*/
	public static ChunkSection EMPTY_SECTION;

	@SuppressWarnings("unchecked")
	public static <X extends Throwable> RuntimeException rethrow(Throwable throwable) throws X {
		throw (X)(throwable);
	}
}