package builderb0y.vertigo.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChunkAccess.class, priority = 2000)
public class ScalableLux_ChunkAccessMixin_Undoing {

	@TargetHandler(
		mixin = "ca.spottedleaf.starlight.mixin.common.chunk.ChunkAccessMixin",
		name = "nullSources"
	)
	@Redirect(
		method = "@MixinSquared:Handler",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/world/level/chunk/ChunkAccess;skyLightSources:Lnet/minecraft/world/level/lighting/ChunkSkyLightSources;",
			opcode = Opcodes.PUTFIELD
		)
	)
	private void vertigo_dontNull(ChunkAccess chunk, ChunkSkyLightSources alwaysNull) {}

	@TargetHandler(
		mixin = "ca.spottedleaf.starlight.mixin.common.chunk.ChunkAccessMixin",
		name = "skipInit"
	)
	@Inject(
		method = "@MixinSquared:Handler",
		at = @At("HEAD")
	)
	private void vertigo_dontSkipInit(ChunkSkyLightSources skyLight, ChunkAccess chunk, CallbackInfo callback) {
		skyLight.fillFrom(chunk);
	}
}