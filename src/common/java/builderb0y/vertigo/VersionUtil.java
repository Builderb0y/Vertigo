package builderb0y.vertigo;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.HeightLimitView;
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
		//using an anonymous subclass fixes compatibility with the AntiXray mod.
		#if MC_VERSION >= MC_1_21_2
			return new ChunkSection(registries.getOrThrow(RegistryKeys.BIOME)) {};
		#else
			return new ChunkSection(registries.get(RegistryKeys.BIOME)) {};
		#endif
	}

	public static int blockMinYInclusive(HeightLimitView view) {
		return view.getBottomY();
	}

	public static int sectionMinYInclusive(HeightLimitView view) {
		return view.getBottomSectionCoord();
	}

	public static int blockMaxYExclusive(HeightLimitView view) {
		return view.getBottomY() + view.getHeight();
	}

	public static int sectionMaxYExclusive(HeightLimitView view) {
		return blockMaxYExclusive(view) >> 4;
	}

	public static int blockMaxYInclusive(HeightLimitView view) {
		return blockMaxYExclusive(view) - 1;
	}

	public static int sectionMaxYInclusive(HeightLimitView view) {
		return sectionMaxYExclusive(view) - 1;
	}
}