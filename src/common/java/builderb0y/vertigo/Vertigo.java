package builderb0y.vertigo;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import builderb0y.vertigo.api.VertigoClientEvents;
import builderb0y.vertigo.api.VertigoServerEvents;
import builderb0y.vertigo.networking.VertigoNetworking;

public class Vertigo implements ModInitializer {

	public static final String
		MODID   = "vertigo",
		MODNAME = "Vertigo";

	public static final boolean
		AUDIT        = false,
		PRINT_EVENTS = false;

	public static final Logger LOGGER = LoggerFactory.getLogger(MODNAME);

	/** the current running server. */
	public static MinecraftServer SERVER;

	@Override
	public void onInitialize() {
		VertigoNetworking.init();
		ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> {
			SERVER = server;
			VertigoInternals.EMPTY_SECTION = VersionUtil.newEmptyChunkSection(server.getRegistryManager());
		});
		ServerLifecycleEvents.SERVER_STOPPED.register((MinecraftServer server) -> {
			SERVER = null;
			VertigoInternals.EMPTY_SECTION = null;
		});
		ServerTickEvents.END_SERVER_TICK.register(SectionTrackingManager::tickAll);

		if (AUDIT) {
			MixinEnvironment.getCurrentEnvironment().audit();
		}
		if (PRINT_EVENTS) {
			VertigoClientEvents.SECTION_LOADED.register((sectionX, sectionY, sectionZ) -> System.out.println("CLIENT LOAD " + sectionX + ", " + sectionY + ", " + sectionZ));
			VertigoClientEvents.SECTION_UNLOADED.register((sectionX, sectionY, sectionZ) -> System.out.println("CLIENT UNLOAD " + sectionX + ", " + sectionY + ", " + sectionZ));
			VertigoServerEvents.SECTION_LOADED.register((player, sectionX, sectionY, sectionZ) -> System.out.println("SERVER LOAD " + sectionX + ", " + sectionY + ", " + sectionZ));
			VertigoServerEvents.SECTION_UNLOADED.register((player, sectionX, sectionY, sectionZ) -> System.out.println("SERVER UNLOAD " + sectionX + ", " + sectionY + ", " + sectionZ));
		}
	}

	public static Identifier modID(String path) {
		return Identifier.of(MODID, path);
	}
}