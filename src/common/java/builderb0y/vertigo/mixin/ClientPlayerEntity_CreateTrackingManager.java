package builderb0y.vertigo.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import builderb0y.vertigo.TrackingManager;

@Environment(EnvType.CLIENT)
@Mixin(LocalPlayer.class)
public class ClientPlayerEntity_CreateTrackingManager extends AbstractClientPlayer {

	public ClientPlayerEntity_CreateTrackingManager() {
		super(null, null);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void vertigo_createTrackingManager(
		Minecraft client,
		ClientLevel world,
		ClientPacketListener networkHandler,
		StatsCounter stats,
		ClientRecipeBook recipeBook,

		Input lastPlayerInput,

		boolean lastSprinting,
		CallbackInfo callback
	) {
		TrackingManager.set(this, TrackingManager.createClient());
	}
}