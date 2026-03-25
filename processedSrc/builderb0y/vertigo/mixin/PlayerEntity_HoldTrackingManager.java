package builderb0y.vertigo.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.entity.player.PlayerEntity;

import builderb0y.vertigo.TrackingManager;
import builderb0y.vertigo.TrackingManager.TrackingManagerHolder;

@Mixin(PlayerEntity.class)
public class PlayerEntity_HoldTrackingManager implements TrackingManagerHolder {

	@Unique
	private TrackingManager vertigo_trackingManager;

	@Override
	public @Nullable TrackingManager vertigo_getTrackingManager() {
		return this.vertigo_trackingManager;
	}

	@Override
	public void vertigo_setTrackingManager(TrackingManager trackingManager) {
		this.vertigo_trackingManager = trackingManager;
	}
}