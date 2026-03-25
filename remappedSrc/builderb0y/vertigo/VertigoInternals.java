package builderb0y.vertigo;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class VertigoInternals {

	/**
	used by some mixins to keep track of which player a packet is being synced to.
	*/
	public static final ThreadLocal<ServerPlayer> SYNCING_PLAYER = new ThreadLocal<>();
	/**
	{@link ClientboundLevelChunkWithLightPacket} sends the entire chunk payload in one big byte[].
	this includes every section in the chunk, which is a problem because I can't
	just say "only send the sections in this Y range". so instead, I redirect
	the unnecessary chunk sections to this empty section, to reduce the size
	of the packet.
	*/
	public static LevelChunkSection EMPTY_SECTION;

	@SuppressWarnings("unchecked")
	public static <X extends Throwable> RuntimeException rethrow(Throwable throwable) throws X {
		throw (X)(throwable);
	}
}