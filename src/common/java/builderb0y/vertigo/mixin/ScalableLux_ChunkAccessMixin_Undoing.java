package builderb0y.vertigo.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.light.ChunkSkyLight;

@Mixin(value = Chunk.class, priority = 2000)
public class ScalableLux_ChunkAccessMixin_Undoing {

	@TargetHandler(
		mixin = "ca.spottedleaf.starlight.mixin.common.chunk.ChunkAccessMixin",
		name = "nullSources"
	)
	@Redirect(
		method = "@MixinSquared:Handler",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/world/chunk/Chunk;chunkSkyLight:Lnet/minecraft/world/chunk/light/ChunkSkyLight;",
			opcode = Opcodes.PUTFIELD
		)
	)
	private void vertigo_dontNull(Chunk chunk, ChunkSkyLight alwaysNull) {}

	@TargetHandler(
		mixin = "ca.spottedleaf.starlight.mixin.common.chunk.ChunkAccessMixin",
		name = "skipInit"
	)
	@Inject(
		method = "@MixinSquared:Handler",
		at = @At("HEAD")
	)
	private void vertigo_dontSkipInit(ChunkSkyLight skyLight, Chunk chunk, CallbackInfo callback) {
		skyLight.refreshSurfaceY(chunk);
	}
}