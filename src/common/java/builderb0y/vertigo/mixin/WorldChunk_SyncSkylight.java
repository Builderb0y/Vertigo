package builderb0y.vertigo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import builderb0y.vertigo.TrackingManager;

@Mixin(value = WorldChunk.class, priority = 500) //before scalable lux.
public abstract class WorldChunk_SyncSkylight {

	@Shadow public abstract World getWorld();

	@Inject(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/light/LightingProvider;checkBlock(Lnet/minecraft/util/math/BlockPos;)V", shift = Shift.AFTER))
	private void vertigo_syncSkylight(
		BlockPos pos,
		BlockState state,
		#if MC_VERSION >= MC_1_21_5
			int flags,
		#else
			boolean moved,
		#endif
		CallbackInfoReturnable<BlockState> callback
	) {
		if (this.getWorld() instanceof ServerWorld serverWorld) {
			for (ServerPlayerEntity player : serverWorld.getPlayers()) {
				TrackingManager manager = TrackingManager.PLAYERS.get(player);
				if (manager != null) manager.onLightingChanged(pos);
			}
		}
	}
}