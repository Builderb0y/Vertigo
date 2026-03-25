package builderb0y.vertigo;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;

public class VersionUtil {

	public static int getViewDistance(ServerPlayer player) {

		return player.requestedViewDistance();
	}

	public static LevelChunkSection newEmptyChunkSection(RegistryAccess registries) {
		//using an anonymous subclass fixes compatibility with the AntiXray mod.

		return new LevelChunkSection(PalettedContainerFactory.create(registries)) {

		};
	}

	public static int blockMinYInclusive(LevelHeightAccessor view) {
		return view.getMinY();
	}

	public static int sectionMinYInclusive(LevelHeightAccessor view) {
		return view.getMinSectionY();
	}

	public static int blockMaxYExclusive(LevelHeightAccessor view) {
		return view.getMinY() + view.getHeight();
	}

	public static int sectionMaxYExclusive(LevelHeightAccessor view) {
		return blockMaxYExclusive(view) >> 4;
	}

	public static int blockMaxYInclusive(LevelHeightAccessor view) {
		return blockMaxYExclusive(view) - 1;
	}

	public static int sectionMaxYInclusive(LevelHeightAccessor view) {
		return sectionMaxYExclusive(view) - 1;
	}

	public static Level getWorld(Entity entity) {

		return entity.level();
	}
}