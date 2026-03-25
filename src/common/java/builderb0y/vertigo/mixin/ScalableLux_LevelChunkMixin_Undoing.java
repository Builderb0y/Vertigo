package builderb0y.vertigo.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LevelChunk.class, priority = 2000)
public class ScalableLux_LevelChunkMixin_Undoing {

	@TargetHandler(
		mixin = "ca.spottedleaf.starlight.mixin.common.chunk.LevelChunkMixin",
		name = "skipLightSources"
	)
	@Inject(
		method = "@MixinSquared:Handler",
		at = @At("HEAD")
	)
	private void vertigo_dontSkipLightSources(ChunkSkyLightSources skyLight, BlockGetter blockView, int x, int y, int z, CallbackInfoReturnable<Boolean> callback) {
		skyLight.update(blockView, x, y, z);
	}
}