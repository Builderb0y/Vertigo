package builderb0y.vertigo.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.stat.StatHandler;

import builderb0y.vertigo.TrackingManager;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntity_CreateTrackingManager extends AbstractClientPlayerEntity {

	public ClientPlayerEntity_CreateTrackingManager() {
		super(null, null);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void vertigo_createTrackingManager(
		MinecraftClient client,
		ClientWorld world,
		ClientPlayNetworkHandler networkHandler,
		StatHandler stats,
		ClientRecipeBook recipeBook,
		                           
			net.minecraft.util.PlayerInput lastPlayerInput,
		     
                        
        
		boolean lastSprinting,
		CallbackInfo callback
	) {
		TrackingManager.set(this, TrackingManager.createClient());
	}
}