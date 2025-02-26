package builderb0y.vertigo.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.BlockView;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.ChunkSkyLight;

@Mixin(value = WorldChunk.class, priority = 2000)
public class ScalableLux_LevelChunkMixin_Undoing {

	@TargetHandler(
		mixin = "ca.spottedleaf.starlight.mixin.common.chunk.LevelChunkMixin",
		name = "skipLightSources"
	)
	@Inject(
		method = "@MixinSquared:Handler",
		at = @At("HEAD")
	)
	private void vertigo_dontSkipLightSources(ChunkSkyLight skyLight, BlockView blockView, int x, int y, int z, CallbackInfoReturnable<Boolean> callback) {
		skyLight.isSkyLightAccessible(blockView, x, y, z);
	}
}