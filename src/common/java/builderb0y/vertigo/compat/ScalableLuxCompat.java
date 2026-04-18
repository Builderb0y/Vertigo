package builderb0y.vertigo.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScalableLuxCompat {

	public static final Logger LOGGER = LoggerFactory.getLogger("Vertigo/Compat");
	public static final boolean scalableLuxInstalled = FabricLoader.getInstance().isModLoaded("scalablelux");
	static {
		if (scalableLuxInstalled) {
			LOGGER.info("ScalableLux is also installed. Enabling compatibility code.");
		}
		else {
			LOGGER.info("ScalableLux is not installed. Not enabling compatibility code.");
		}
	}
}