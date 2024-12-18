package builderb0y.vertigo;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.spongepowered.asm.mixin.MixinEnvironment;

import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.chunk.ChunkSection;

import builderb0y.vertigo.api.VertigoClientEvents;
import builderb0y.vertigo.api.VertigoServerEvents;
import builderb0y.vertigo.networking.VertigoNetworking;

public class Vertigo implements ModInitializer {

	public static final String
		MODID   = "vertigo",
		MODNAME = "Vertigo";

	/** used by some mixins to keep track of which player a packet is being synced to. */
	public static final ThreadLocal<ServerPlayerEntity> SYNCING_PLAYER = new ThreadLocal<>();
	/** the current running server. */
	public static MinecraftServer SERVER;
	/**
	{@link ChunkDataS2CPacket} sends the entire chunk payload in one big byte[].
	this includes every section in the chunk, which is a problem because I can't
	just say "only send the sections in this Y range". so instead, I redirect
	the unnecessary chunk sections to this empty section, to reduce the size
	of the packet.
	*/
	public static ChunkSection EMPTY_SECTION;

	@Override
	public void onInitialize() {
		VertigoNetworking.init();
		ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> {
			SERVER = server;
			EMPTY_SECTION = VersionUtil.newEmptyChunkSection(server.getRegistryManager());
		});
		ServerLifecycleEvents.SERVER_STOPPED.register((MinecraftServer server) -> {
			SERVER = null;
			EMPTY_SECTION = null;
		});
		ServerTickEvents.END_SERVER_TICK.register(VerticalTrackingManager::tickAll);

		//MixinEnvironment.getCurrentEnvironment().audit();
		/*
		VertigoClientEvents.SECTION_LOADED.register((sectionX, sectionY, sectionZ) -> System.out.println("CLIENT LOAD " + sectionX + ", " + sectionY + ", " + sectionZ));
		VertigoClientEvents.SECTION_UNLOADED.register((sectionX, sectionY, sectionZ) -> System.out.println("CLIENT UNLOAD " + sectionX + ", " + sectionY + ", " + sectionZ));
		VertigoServerEvents.SECTION_LOADED.register((player, sectionX, sectionY, sectionZ) -> System.out.println("SERVER LOAD " + sectionX + ", " + sectionY + ", " + sectionZ));
		VertigoServerEvents.SECTION_UNLOADED.register((player, sectionX, sectionY, sectionZ) -> System.out.println("SERVER UNLOAD " + sectionX + ", " + sectionY + ", " + sectionZ));
		//*/
	}

	public static Identifier modID(String path) {
		return Identifier.of(MODID, path);
	}
}