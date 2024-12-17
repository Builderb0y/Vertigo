package builderb0y.vertigo;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.chunk.ChunkSection;

public class VersionUtil {

	public static int getViewDistance(ServerPlayerEntity player) {
		#if MC_VERSION >= MC_1_20_2
			return player.getViewDistance();
		#else
			return player.getServer().getPlayerManager().getViewDistance();
		#endif
	}

	public static ChunkSection newEmptyChunkSection(DynamicRegistryManager registries) {
		#if MC_VERSION >= MC_1_21_2
			return new ChunkSection(registries.getOrThrow(RegistryKeys.BIOME));
		#else
			return new ChunkSection(registries.get(RegistryKeys.BIOME));
		#endif
	}
}