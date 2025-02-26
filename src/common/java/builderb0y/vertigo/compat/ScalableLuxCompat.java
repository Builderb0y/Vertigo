package builderb0y.vertigo.compat;

import net.fabricmc.loader.api.FabricLoader;

import builderb0y.vertigo.Vertigo;

public class ScalableLuxCompat {

	public static final boolean scalableLuxInstalled = FabricLoader.getInstance().isModLoaded("scalablelux");
	static {
		if (scalableLuxInstalled) {
			Vertigo.LOGGER.info("ScalableLux is also installed. Enabling compatibility code.");
		}
		else {
			Vertigo.LOGGER.info("ScalableLux is not installed. Not enabling compatibility code.");
		}
	}
}