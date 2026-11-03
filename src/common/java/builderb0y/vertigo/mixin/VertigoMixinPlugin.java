package builderb0y.vertigo.mixin;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import builderb0y.vertigo.compat.ScalableLuxCompat;

public class VertigoMixinPlugin implements IMixinConfigPlugin {

	public static final Logger LOGGER = LoggerFactory.getLogger("Vertigo/Mixins");

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return switch (mixinClassName) {
			case "builderb0y.vertigo.mixin.ScalableLux_ChunkAccessMixin_Undoing" -> {
				if (ScalableLuxCompat.scalableLuxInstalled) {
					Version version = FabricLoader.getInstance().getModContainer("scalablelux").orElseThrow().getMetadata().getVersion();
					if (version.compareTo(version("0.3.*")) < 0) {
						LOGGER.info("ScalableLux version is " + version + ". Using legacy mixin.");
						yield true;
					}
					else {
						LOGGER.info("ScalableLux version is " + version + ". Not using legacy mixin.");
						yield false;
					}
				}
				yield false;
			}
			case "builderb0y.vertigo.mixin.ScalableLux_ChunkAccessMixin_Undoing_New" -> {
				if (ScalableLuxCompat.scalableLuxInstalled) {
					Version version = FabricLoader.getInstance().getModContainer("scalablelux").orElseThrow().getMetadata().getVersion();
					if (version.compareTo(version("0.3.*")) >= 0) {
						LOGGER.info("ScalableLux version is " + version + ". Using new mixin.");
						yield true;
					}
					else {
						LOGGER.info("ScalableLux version is " + version + ". Not using new mixin.");
						yield false;
					}
				}
				yield false;
			}
			case
				"builderb0y.vertigo.mixin.ScalableLux_LevelChunkMixin_Undoing",
				"builderb0y.vertigo.mixin.ScalableLux_ProtoChunkMixin_Undoing"
			-> {
				yield ScalableLuxCompat.scalableLuxInstalled;
			}
			default -> true;
		};
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

	}

	@Override
	public void onLoad(String mixinPackage) {

	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public List<String> getMixins() {
		return Collections.emptyList();
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

	}

	public static Version version(String spec) {
		try {
			return Version.parse(spec);
		}
		catch (VersionParsingException exception) {
			throw new AssertionError(exception);
		}
	}
}