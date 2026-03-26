package builderb0y.vertigo.mixin;

import net.minecraft.util.BitStorage;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkSkyLightSources.class)
public interface ChunkSkyLightSources_Accessors {

	@Accessor("minY")
	public abstract int vertigo_getMinY();

	@Accessor("heightmap")
	public abstract BitStorage vertigo_getPalette();
}