package builderb0y.vertigo;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import builderb0y.vertigo.networking.VertigoNetworking;

@Environment(EnvType.CLIENT)
public class VertigoClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		VertigoNetworking.initClient();
	}
}