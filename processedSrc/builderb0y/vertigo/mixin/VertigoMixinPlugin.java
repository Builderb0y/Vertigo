package builderb0y.vertigo.mixin;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import builderb0y.vertigo.compat.ScalableLuxCompat;

public class VertigoMixinPlugin implements IMixinConfigPlugin {

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return switch (mixinClassName) {
			case
				"builderb0y.vertigo.mixin.ScalableLux_ChunkAccessMixin_Undoing",
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
}