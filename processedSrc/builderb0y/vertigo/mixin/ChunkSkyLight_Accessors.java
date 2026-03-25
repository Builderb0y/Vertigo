package builderb0y.vertigo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.chunk.light.ChunkSkyLight;

@Mixin(ChunkSkyLight.class)
public interface ChunkSkyLight_Accessors {

	@Accessor("minY")
	public abstract int vertigo_getMinY();

	@Accessor("palette")
	public abstract PaletteStorage vertigo_getPalette();
}