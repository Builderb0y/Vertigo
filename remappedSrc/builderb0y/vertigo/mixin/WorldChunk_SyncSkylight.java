package builderb0y.vertigo.mixin;

import java.util.ConcurrentModificationException;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import builderb0y.vertigo.TrackingManager;
import builderb0y.vertigo.Vertigo;

@Mixin(value = LevelChunk.class, priority = 500) //before scalable lux.
public abstract class WorldChunk_SyncSkylight {

	@Unique
	private static final boolean VERTIGO_TRACE_THREADS = Boolean.getBoolean("vertigo.traceWrongThreadForSetBlockState");

	@Shadow
	public abstract Level getLevel();

	@Inject(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;checkBlock(Lnet/minecraft/core/BlockPos;)V", shift = Shift.AFTER))
	private void vertigo_syncSkylight(
		BlockPos pos,
		BlockState state,

		int flags,

		CallbackInfoReturnable<BlockState> callback
	) {
		if (this.getLevel() instanceof ServerLevel serverWorld) {
			if (serverWorld.getServer().isSameThread()) {
				for (ServerPlayer player : serverWorld.players()) {
					TrackingManager manager = TrackingManager.get(player);
					if (manager != null) manager.onLightingChanged(pos);
				}
			}
			else {
				serverWorld.getServer().execute(() -> {
					for (ServerPlayer player : serverWorld.players()) {
						TrackingManager manager = TrackingManager.get(player);
						if (manager != null) manager.onLightingChanged(pos);
					}
				});
			}
		}
	}

	@Inject(method = "setBlockState", at = @At("HEAD"))
	private void vertigo_checkThread(
		BlockPos pos,
		BlockState state,

		int flags,

		CallbackInfoReturnable<BlockState> callback
	) {
		if (VERTIGO_TRACE_THREADS && this.getLevel() instanceof ServerLevel serverWorld && !serverWorld.getServer().isSameThread()) {
			Vertigo.LOGGER.warn("", new ConcurrentModificationException("Caught another mod being naughty and calling setBlockState() from the wrong thread. See the stack trace below to find out who to blame."));
		}
	}
}